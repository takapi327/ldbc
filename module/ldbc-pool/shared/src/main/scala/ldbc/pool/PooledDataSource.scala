/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import scala.concurrent.duration.*

import ldbc.sql.{ Connection, DataSource, SQLException }

import ldbc.effect.{ Concurrent, Fiber, Ref, Resource }
import ldbc.effect.syntax.*

/**
 * A connection pool exposed as a [[ldbc.sql.DataSource]], generic over the effect `F: Concurrent`.
 * Mirrors HikariCP's behaviour (validation, leak detection, keepalive, adaptive sizing) on the effect
 * type-classes. See `LDBC_EFFECT_TAGLESS_DESIGN.md`.
 */
trait PooledDataSource[F[_]] extends DataSource[F]:

  def minConnections:                                Int
  def maxConnections:                                Int
  def connectionTimeout:                             FiniteDuration
  def idleTimeout:                                   FiniteDuration
  def maxLifetime:                                   FiniteDuration
  def validationTimeout:                             FiniteDuration
  def leakDetectionThreshold:                        Option[FiniteDuration]
  def adaptiveSizing:                                Boolean
  def adaptiveInterval:                              FiniteDuration
  def metricsTracker:                                PoolMetricsTracker[F]
  def poolState:                                     Ref[F, PoolState[F]]
  def idGenerator:                                   F[String]
  def aliveBypassWindow:                             FiniteDuration
  def keepaliveTime:                                 Option[FiniteDuration]
  def connectionTestQuery:                           Option[String]
  def poolLogger:                                    PoolLogger[F]
  def status:                                        F[PoolStatus]
  def metrics:                                       F[PoolMetrics]
  def close:                                         F[Unit]
  def createNewConnection():                         F[PooledConnection[F]]
  def circuitBreaker:                                CircuitBreaker[F]
  def createNewConnectionForPool():                  F[PooledConnection[F]]
  def returnToPool(pooled:     PooledConnection[F]): F[Unit]
  def removeConnection(pooled: PooledConnection[F]): F[Unit]
  def validateConnection(conn: Connection[F]):       F[Boolean]

object PooledDataSource:

  private final case class Impl[F[_]](
    config:              ConnectionPoolConfig,
    create:              Resource[F, Connection[F]],
    connectionTestQuery: Option[String],
    metricsTracker:      PoolMetricsTracker[F],
    poolState:           Ref[F, PoolState[F]],
    idGenerator:         F[String],
    connectionBag:       ConcurrentBag[F, PooledConnection[F]],
    circuitBreaker:      CircuitBreaker[F],
    poolLogger:          PoolLogger[F],
    hooks:               Option[Connection[F] => Resource[F, Unit]]
  )(using F: Concurrent[F])
    extends PooledDataSource[F]:

    override def minConnections:         Int                    = config.minConnections
    override def maxConnections:         Int                    = config.maxConnections
    override def connectionTimeout:      FiniteDuration         = config.connectionTimeout
    override def idleTimeout:            FiniteDuration         = config.idleTimeout
    override def maxLifetime:            FiniteDuration         = config.maxLifetime
    override def validationTimeout:      FiniteDuration         = config.validationTimeout
    override def leakDetectionThreshold: Option[FiniteDuration] = config.leakDetectionThreshold
    override def adaptiveSizing:         Boolean                = config.adaptiveSizing
    override def adaptiveInterval:       FiniteDuration         = config.adaptiveInterval
    override def aliveBypassWindow:      FiniteDuration         = config.aliveBypassWindow
    override def keepaliveTime:          Option[FiniteDuration] = config.keepaliveTime

    override def getConnection: F[(Connection[F], F[Unit])] = connectionResource.allocatedCase

    private def connectionResource: Resource[F, Connection[F]] =
      val base = Resource.make(acquire)(release)
      hooks match
        case None       => base
        case Some(hook) => base.flatMap(conn => hook(conn).map(_ => conn))

    override def status: F[PoolStatus] = for
      state <- poolState.get
      connections = state.connections
      stateChecks <- connections.traverse(c => c.state.get.map(s => (c, s)))
      active = stateChecks.count(_._2 == ConnectionState.InUse)
      idle   = stateChecks.count(_._2 == ConnectionState.Idle)
    yield PoolStatus(
      total   = connections.size,
      active  = active,
      idle    = idle,
      waiting = state.waitQueue.size
    )

    override def metrics: F[PoolMetrics] = metricsTracker.getMetrics

    override def close: F[Unit] =
      poolState
        .modify { state =>
          if state.closed then (state, F.unit)
          else
            val newState = state.copy(closed = true)
            val closeAll = state.connections.traverse_ { pooled =>
              pooled.finalizer.attempt.flatMap {
                case Left(error) =>
                  poolLogger.debug(s"Error closing connection ${ pooled.id }: ${ error.getMessage }") >>
                    pooled.connection.close().attempt.void
                case Right(_) =>
                  F.unit
              }
            }
            val failWaiters = state.waitQueue.traverse_ { deferred =>
              deferred.complete(Left(new SQLException("Pool closed"))).attempt.void
            }
            val effect =
              poolLogger.info(s"Closing connection pool (${ config.poolName })") >>
                closeAll *> failWaiters >> poolLogger.info("Connection pool closed successfully")
            (newState, effect)
        }
        .flatMap(identity)

    private def acquire: F[Connection[F]] = for
      startTime <- F.monotonic
      result    <- acquireConnectionWithStartTime(startTime)
    yield result

    private def acquireConnectionWithStartTime(startTime: FiniteDuration): F[Connection[F]] =
      poolState.get.flatMap { state =>
        if state.closed then F.raiseError(new SQLException("Pool is closed"))
        else
          connectionBag.borrow(config.connectionTimeout).flatMap {
            case Some(pooled) =>
              for
                shouldValidate <- needsValidation(pooled)
                valid          <-
                  if shouldValidate then
                    validateConnection(pooled.connection).flatTap {
                      case true  => F.unit
                      case false => poolLogger.warn(s"Connection ${ pooled.id } failed validation, removing from pool")
                    }
                  else F.pure(true)
                result <-
                  if !valid then removeConnection(pooled) >> acquireConnectionWithStartTime(startTime)
                  else
                    for
                      _       <- pooled.state.set(ConnectionState.InUse)
                      _       <- poolState.update(s => s.copy(idleConnections = s.idleConnections - pooled.id))
                      now     <- F.realTime.map(_.toMillis)
                      _       <- pooled.lastUsedAt.set(now)
                      _       <- pooled.useCount.update(_ + 1)
                      endTime <- F.monotonic
                      _       <- metricsTracker.recordAcquisition(endTime - startTime)
                      _       <- config.leakDetectionThreshold.traverse_ { threshold =>
                             val leakTask = F.sleep(threshold).flatMap { _ =>
                               pooled.state.get.flatMap {
                                 case ConnectionState.InUse =>
                                   poolLogger.warn(
                                     s"Possible connection leak detected: Connection ${ pooled.id } has been in use for longer than $threshold"
                                   ) >> metricsTracker.recordLeak()
                                 case _ => F.unit
                               }
                             }
                             leakTask.start.flatMap(fiber => pooled.leakDetection.set(Some(fiber)))
                           }
                    yield wrapConnection(pooled)
              yield result

            case None =>
              poolState.get.flatMap { currentState =>
                if currentState.connections.size < config.maxConnections then
                  createNewConnection().flatMap { pooled =>
                    for
                      endTime <- F.monotonic
                      _       <- metricsTracker.recordAcquisition(endTime - startTime)
                    yield wrapConnection(pooled)
                  }
                else
                  currentState.connections.traverse(_.state.get).flatMap { states =>
                    val activeCount  = states.count(_ == ConnectionState.InUse)
                    val idleCount    = states.count(_ == ConnectionState.Idle)
                    val errorMessage =
                      s"Connection acquisition timeout after ${ config.connectionTimeout } " +
                        s"(pool: ${ config.poolName }, " +
                        s"size: ${ currentState.connections.size }/${ config.maxConnections }, " +
                        s"active: $activeCount, idle: $idleCount, " +
                        s"waiting: ${ currentState.waitQueue.size })"
                    metricsTracker.recordTimeout() *>
                      poolLogger.error(errorMessage) *>
                      F.raiseError(new SQLException(errorMessage))
                  }
              }
          }
      }

    private def release(conn: Connection[F]): F[Unit] = for
      startTime <- F.monotonic
      _         <- releaseConnectionWithStartTime(conn, startTime)
    yield ()

    private def releaseConnectionWithStartTime(conn: Connection[F], startTime: FiniteDuration): F[Unit] =
      val pooledF: F[Option[PooledConnection[F]]] = conn match
        case proxy: ConnectionProxy[F] @unchecked => F.pure(Some(proxy.pooled))
        case _                                    =>
          connectionBag.values.map(connections => connections.find(p => p.connection == unwrapConnection(conn)))

      val recordUse: F[Unit] =
        F.monotonic.flatMap(endTime => metricsTracker.recordUsage(endTime - startTime))

      pooledF.flatMap {
        case Some(pooled) =>
          pooled.leakDetection.get.flatMap(_.traverse_(fiber => fiber.cancel)) >>
            pooled.leakDetection.set(None) >>
            pooled.state.set(ConnectionState.Idle) >>
            resetConnection(pooled.connection).attempt.flatMap {
              case Right(_) =>
                for
                  shouldValidate <- needsValidation(pooled)
                  valid          <-
                    if shouldValidate then
                      validateConnection(pooled.connection).flatTap {
                        case true  => F.unit
                        case false =>
                          poolLogger.warn(s"Connection ${ pooled.id } failed validation on release, removing from pool")
                      }
                    else F.pure(true)
                  expired <- isExpired(pooled)
                  _       <-
                    if valid && !expired then
                      connectionBag.requite(pooled) *>
                        poolState.update(s => s.copy(idleConnections = s.idleConnections + pooled.id)) *>
                        recordUse
                    else removeConnection(pooled) *> recordUse
                yield ()
              case Left(error) =>
                poolLogger.warn(s"Failed to reset connection ${ pooled.id } on release: ${ error.getMessage }") >>
                  removeConnection(pooled) *> recordUse
            }
        case None => recordUse
      }

    override def createNewConnection(): F[PooledConnection[F]] = for
      startTime <- F.monotonic
      result    <- createNewConnectionWithState(startTime, ConnectionState.InUse, 1L)
    yield result

    override def createNewConnectionForPool(): F[PooledConnection[F]] = for
      startTime <- F.monotonic
      result    <- createNewConnectionWithState(startTime, ConnectionState.Idle, 0L)
    yield result

    private def createNewConnectionWithState(
      startTime:       FiniteDuration,
      initialState:    ConnectionState,
      initialUseCount: Long
    ): F[PooledConnection[F]] =
      val recordCreationMetric: F[Unit] =
        F.monotonic.flatMap(endTime => metricsTracker.recordCreation(endTime - startTime))

      val created = circuitBreaker.protect {
        for
          id        <- idGenerator
          allocated <- create.allocatedCase
          (conn, finalizer) = allocated
          now              <- F.realTime.map(_.toMillis)
          stateRef         <- Ref.of[F, ConnectionState](initialState)
          lastUsedRef      <- Ref.of[F, Long](now)
          useCountRef      <- Ref.of[F, Long](initialUseCount)
          lastValidatedRef <- Ref.of[F, Long](now)
          leakDetectionRef <- Ref.of[F, Option[Fiber[F, Unit]]](None)
          bagStateRef      <- Ref.of[F, Int](
                           if initialState == ConnectionState.InUse then BagEntry.STATE_IN_USE
                           else BagEntry.STATE_NOT_IN_USE
                         )
          pooled = PooledConnection[F](
                     id              = id,
                     connection      = conn,
                     finalizer       = finalizer,
                     state           = stateRef,
                     createdAt       = now,
                     lastUsedAt      = lastUsedRef,
                     useCount        = useCountRef,
                     lastValidatedAt = lastValidatedRef,
                     leakDetection   = leakDetectionRef,
                     bagState        = bagStateRef
                   )
          _ <-
            if initialState != ConnectionState.InUse then connectionBag.add(pooled)
            else F.unit
          added <- poolState.modify { poolState =>
                     if poolState.connections.size >= config.maxConnections then (poolState, false)
                     else
                       val newState = poolState.copy(
                         connections     = poolState.connections :+ pooled,
                         idleConnections =
                           if initialState == ConnectionState.Idle then poolState.idleConnections + pooled.id
                           else poolState.idleConnections
                       )
                       (newState, true)
                   }
          _ <-
            if !added then
              (if initialState != ConnectionState.InUse then connectionBag.remove(pooled).void else F.unit) *>
                poolLogger.warn(s"Cannot create new connection: pool at maximum size (${ config.maxConnections })") *>
                conn.close().attempt.void *>
                F.raiseError[Unit](new SQLException("Pool reached maximum size"))
            else F.unit
        yield pooled
      }

      created
        .flatTap(_ => recordCreationMetric)
        .handleErrorWith(error => recordCreationMetric >> F.raiseError(error))

    private def resetConnection(conn: Connection[F]): F[Unit] = for
      _ <- conn.rollback().attempt.void
      _ <- conn.setAutoCommit(true).attempt.void
    yield ()

    override def validateConnection(conn: Connection[F]): F[Boolean] =
      val timeoutError = new SQLException(s"Connection validation timed out after ${ config.validationTimeout }")
      val validation   = connectionTestQuery match
        case Some(query) =>
          for
            closed <- conn.isClosed()
            valid  <- if !closed then executeTestQuery(conn, query) else F.pure(false)
          yield !closed && valid
        case None =>
          for
            closed <- conn.isClosed()
            valid  <- if !closed then conn.isValid(config.validationTimeout.toSeconds.toInt.max(1)) else F.pure(false)
          yield !closed && valid

      F.timeout(validation, config.validationTimeout)(timeoutError)
        .handleErrorWith { error =>
          poolLogger
            .debug(
              s"Connection validation failed or timed out after ${ config.validationTimeout }: ${ error.getMessage }"
            )
            .as(false)
        }

    private def executeTestQuery(conn: Connection[F], query: String): F[Boolean] =
      conn
        .createStatement()
        .flatMap(stmt => stmt.execute(query).as(true).guarantee(stmt.close()))
        .handleError(_ => false)

    private def needsValidation(pooled: PooledConnection[F]): F[Boolean] =
      if config.aliveBypassWindow.toMillis == 0 then F.pure(true)
      else
        for
          now      <- F.realTime.map(_.toMillis)
          lastUsed <- pooled.lastUsedAt.get
          elapsed = now - lastUsed
        yield elapsed > config.aliveBypassWindow.toMillis

    private def isExpired(pooled: PooledConnection[F]): F[Boolean] =
      F.realTime.map { now =>
        val age = now.toMillis - pooled.createdAt
        age > config.maxLifetime.toMillis
      }

    override def returnToPool(pooled: PooledConnection[F]): F[Unit] =
      pooled.state.set(ConnectionState.Idle) *>
        connectionBag.requite(pooled) *>
        poolState.update(s => s.copy(idleConnections = s.idleConnections + pooled.id))

    override def removeConnection(pooled: PooledConnection[F]): F[Unit] = for
      currentState <- pooled.state.get
      _            <- poolLogger.debug(s"Removing connection ${ pooled.id } from pool (state: $currentState)")
      _            <- pooled.state.set(ConnectionState.Removed)
      _            <- connectionBag.remove(pooled)
      _            <- pooled.finalizer.attempt.void
      _            <- pooled.leakDetection.get.flatMap(_.traverse_(fiber => fiber.cancel))
      _            <- poolState.update { state =>
             state.copy(
               connections     = state.connections.filterNot(_ == pooled),
               idleConnections = state.idleConnections - pooled.id
             )
           }
      _ <- metricsTracker.recordRemoval()
    yield ()

    private def wrapConnection(pooled: PooledConnection[F]): Connection[F] =
      new ConnectionProxy[F](pooled, release)

    private def unwrapConnection(conn: Connection[F]): Connection[F] =
      conn match
        case proxy: ConnectionProxy[F] @unchecked => proxy.pooled.connection
        case _                                    => conn

  /**
   * The default connection-id generator: a random version-4 UUID string, built from `scala.util.Random`
   * (not `java.util.UUID.randomUUID`, which pulls from `SecureRandom` — absent on Scala.js / Native).
   */
  private def randomConnectionId[F[_]](using F: Concurrent[F]): F[String] = F.delay {
    val mostSignificant  = (scala.util.Random.nextLong() & ~0x000000000000f000L) | 0x0000000000004000L
    val leastSignificant = (scala.util.Random.nextLong() & ~0xc000000000000000L) | 0x8000000000000000L
    new java.util.UUID(mostSignificant, leastSignificant).toString
  }

  def fromConfig[F[_]](
    config:              ConnectionPoolConfig,
    create:              Resource[F, Connection[F]],
    metricsTracker:      Option[PoolMetricsTracker[F]] = None,
    connectionTestQuery: Option[String] = None,
    poolLogger:          Option[PoolLogger[F]] = None,
    idGenerator:         F[String] = null.asInstanceOf[F[String]]
  )(using F: Concurrent[F]): Resource[F, PooledDataSource[F]] =
    val idGen = if idGenerator == null then randomConnectionId[F] else idGenerator
    build(config, create, metricsTracker, connectionTestQuery, poolLogger, idGen, None)

  def fromConfigWithBeforeAfter[F[_], A](
    config:              ConnectionPoolConfig,
    create:              Resource[F, Connection[F]],
    before:              Connection[F] => F[A],
    after:               (A, Connection[F]) => F[Unit],
    metricsTracker:      Option[PoolMetricsTracker[F]] = None,
    connectionTestQuery: Option[String] = None,
    poolLogger:          Option[PoolLogger[F]] = None,
    idGenerator:         F[String] = null.asInstanceOf[F[String]]
  )(using F: Concurrent[F]): Resource[F, PooledDataSource[F]] =
    val idGen = if idGenerator == null then randomConnectionId[F] else idGenerator
    val hook: Connection[F] => Resource[F, Unit] =
      conn => Resource.make(before(conn))(a => after(a, conn)).map(_ => ())
    build(config, create, metricsTracker, connectionTestQuery, poolLogger, idGen, Some(hook))

  def fromDataSource[F[_]](
    config:              ConnectionPoolConfig,
    dataSource:          DataSource[F],
    metricsTracker:      Option[PoolMetricsTracker[F]] = None,
    connectionTestQuery: Option[String] = None,
    poolLogger:          Option[PoolLogger[F]] = None,
    idGenerator:         F[String] = null.asInstanceOf[F[String]]
  )(using F: Concurrent[F]): Resource[F, PooledDataSource[F]] =
    val idGen = if idGenerator == null then randomConnectionId[F] else idGenerator
    fromConfig(config, connectionResource(dataSource), metricsTracker, connectionTestQuery, poolLogger, idGen)

  def fromDataSourceWithBeforeAfter[F[_], A](
    config:              ConnectionPoolConfig,
    dataSource:          DataSource[F],
    before:              Connection[F] => F[A],
    after:               (A, Connection[F]) => F[Unit],
    metricsTracker:      Option[PoolMetricsTracker[F]] = None,
    connectionTestQuery: Option[String] = None,
    poolLogger:          Option[PoolLogger[F]] = None,
    idGenerator:         F[String] = null.asInstanceOf[F[String]]
  )(using F: Concurrent[F]): Resource[F, PooledDataSource[F]] =
    val idGen = if idGenerator == null then randomConnectionId[F] else idGenerator
    fromConfigWithBeforeAfter(
      config,
      connectionResource(dataSource),
      before,
      after,
      metricsTracker,
      connectionTestQuery,
      poolLogger,
      idGen
    )

  /** Adapts an allocated-form [[ldbc.sql.DataSource]] into the [[ldbc.effect.Resource]] the pool consumes. */
  private def connectionResource[F[_]](dataSource: DataSource[F])(using F: Concurrent[F]): Resource[F, Connection[F]] =
    Resource.make(dataSource.getConnection)((pair: (Connection[F], F[Unit])) => pair._2).map(_._1)

  private def build[F[_]](
    config:              ConnectionPoolConfig,
    create:              Resource[F, Connection[F]],
    metricsTracker:      Option[PoolMetricsTracker[F]],
    connectionTestQuery: Option[String],
    poolLogger:          Option[PoolLogger[F]],
    idGenerator:         F[String],
    hooks:               Option[Connection[F] => Resource[F, Unit]]
  )(using F: Concurrent[F]): Resource[F, PooledDataSource[F]] =
    Resource.eval(PoolConfigValidator.validate[F](config)).flatMap { _ =>
      val tracker = metricsTracker.getOrElse(PoolMetricsTracker.noop[F])
      val logger  = poolLogger.getOrElse(PoolLogger.console[F](config.debug))

      val createPool: F[PooledDataSource[F]] = for
        poolState      <- Ref.of[F, PoolState[F]](PoolState.empty[F])
        connectionBag  <- ConcurrentBag[F, PooledConnection[F]]()
        circuitBreaker <- CircuitBreaker[F](CircuitBreaker.Config(maxFailures = 5, resetTimeout = 30.seconds))
      yield Impl[F](
        config              = config,
        create              = create,
        connectionTestQuery = connectionTestQuery,
        metricsTracker      = tracker,
        poolState           = poolState,
        idGenerator         = idGenerator,
        connectionBag       = connectionBag,
        circuitBreaker      = circuitBreaker,
        poolLogger          = logger,
        hooks               = hooks
      )

      def createMinimumConnections(pool: PooledDataSource[F]): Resource[F, Unit] =
        Resource.make(
          (1 to config.minConnections).toList.traverse_(_ => pool.createNewConnectionForPool())
        )(_ => pool.close)

      def createBackgroundResources(pool: PooledDataSource[F]): Resource[F, Unit] =
        val houseKeeper       = HouseKeeper[F](config, tracker)
        val adaptivePoolSizer = AdaptivePoolSizer[F](config, tracker)
        val keepaliveExecutor = config.keepaliveTime.map(KeepaliveExecutor[F](_, tracker))
        val statusReporter    =
          if config.debug then Some(PoolStatusReporter[F](config.maintenanceInterval, logger, tracker))
          else None
        val backgroundResources = List(
          Some(houseKeeper.start(pool)),
          if config.adaptiveSizing then Some(adaptivePoolSizer.start(pool)) else None,
          keepaliveExecutor.map(_.start(pool)),
          statusReporter.map(_.start(pool, config.poolName))
        ).flatten
        backgroundResources.foldLeft(Resource.pure[F, Unit](()))((acc, res) => acc.flatMap(_ => res))

      for
        pool <- Resource.eval(createPool)
        _    <- createMinimumConnections(pool)
        _    <- createBackgroundResources(pool)
      yield pool
    }
