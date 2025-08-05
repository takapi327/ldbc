/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.kernel.Deferred
import cats.effect.std.Console
import cats.effect.std.UUIDGen
import cats.effect.syntax.all.*

import fs2.hashing.Hashing
import fs2.io.net.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.sql.DatabaseMetaData

import ldbc.connector.*

import ldbc.logging.LogHandler

final case class MySQLPooledDataSource[F[_]: Async: Network: Console: Hashing: UUIDGen, A] private (
  host:                    String,
  port:                    Int,
  user:                    String,
  logHandler:              Option[LogHandler[F]]                 = None,
  password:                Option[String]                        = None,
  database:                Option[String]                        = None,
  debug:                   Boolean                               = false,
  ssl:                     SSL                                   = SSL.None,
  socketOptions:           List[SocketOption]                    = MySQLConfig.defaultSocketOptions,
  readTimeout:             Duration                              = Duration.Inf,
  allowPublicKeyRetrieval: Boolean                               = false,
  databaseTerm:            Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG),
  tracer:                  Option[Tracer[F]]                     = None,
  useCursorFetch:          Boolean                               = false,
  useServerPrepStmts:      Boolean                               = false,
  before:                  Option[Connection[F] => F[A]]         = None,
  after:                   Option[(A, Connection[F]) => F[Unit]] = None,
  minConnections:          Int                                   = 5,
  maxConnections:          Int                                   = 20,
  connectionTimeout:       FiniteDuration                        = 30.seconds,
  idleTimeout:             FiniteDuration                        = 10.minutes,
  maxLifetime:             FiniteDuration                        = 30.minutes,
  validationTimeout:       FiniteDuration                        = 5.seconds,
  leakDetectionThreshold:  Option[FiniteDuration]                = None,
  adaptiveSizing:          Boolean                               = true,
  adaptiveInterval:        FiniteDuration                        = 30.seconds,
  metricsTracker:          PoolMetricsTracker[F],
  poolState:               Ref[F, PoolState[F]],
  idGenerator:             F[String],
  houseKeeper:             Option[Fiber[F, Throwable, Unit]],
  adaptiveSizer:           Option[Fiber[F, Throwable, Unit]]
) extends PooledDataSource[F]:
  given Tracer[F] = tracer.getOrElse(Tracer.noop[F])

  private enum AcquireResult:
    case Acquired(connection: Connection[F])
    case CreateNew extends AcquireResult
    case Waiting(deferred: Deferred[F, Either[Throwable, Connection[F]]])

  override def getConnection: Resource[F, Connection[F]] = Resource.make(acquire)(release)

  override def status: F[PoolStatus] = for {
    state <- poolState.get
    connections = state.connections
    // Count states by checking each connection
    stateChecks <- connections.traverse { c =>
                     c.state.get.map(s => (c, s))
                   }
    active = stateChecks.count(_._2 == ConnectionState.InUse)
    idle   = stateChecks.count(_._2 == ConnectionState.Idle)
  } yield PoolStatus(
    total   = connections.size,
    active  = active,
    idle    = idle,
    waiting = state.waitQueue.size
  )

  override def metrics: F[PoolMetrics] = metricsTracker.getMetrics

  override def close: F[Unit] = poolState.modify { state =>
    val newState = state.copy(closed = true)
    val closeAll = state.connections.traverse_ { pooled =>
      pooled.connection.close().attempt.void
    }
    val failWaiters = state.waitQueue.traverse_ { deferred =>
      deferred.complete(Left(new Exception("Pool closed"))).attempt.void
    }
    (newState, closeAll *> failWaiters)
  }.flatten

  private def acquire: F[Connection[F]] = for
    startTime <- Clock[F].monotonic
    result    <- acquireConnectionWithStartTime(startTime)
  yield result

  private def acquireConnectionWithStartTime(startTime: FiniteDuration): F[Connection[F]] =

    def tryAcquire: F[AcquireResult] = poolState
      .modify { state =>
        if state.closed then (state, Temporal[F].raiseError[AcquireResult](new Exception("Pool is closed")))
        else
          findIdleConnection(state) match
            case Some((pooled, newState)) =>
              val markInUse: F[AcquireResult] = for
                _   <- pooled.state.set(ConnectionState.InUse)
                now <- Clock[F].realTime.map(_.toMillis)
                _   <- pooled.lastUsedAt.set(now)
                _   <- pooled.useCount.update(_ + 1)
              yield AcquireResult.Acquired(wrapConnection(pooled))
              (newState, markInUse)

            case None if state.connections.size < maxConnections =>
              (state, Temporal[F].pure(AcquireResult.CreateNew: AcquireResult))

            case None =>
              val deferred = Deferred.unsafe[F, Either[Throwable, Connection[F]]]
              val newState = state.copy(waitQueue = state.waitQueue :+ deferred)
              (newState, Temporal[F].pure(AcquireResult.Waiting(deferred): AcquireResult))
      }
      .flatMap(identity)

    val acquireResult: F[Connection[F]] = tryAcquire.flatMap {
      case AcquireResult.Acquired(conn) =>
        for
          endTime <- Clock[F].monotonic
          _       <- metricsTracker.recordAcquisition(endTime - startTime)
        yield conn
      case AcquireResult.CreateNew =>
        createNewConnection()
          .flatMap { pooled =>
            for
              endTime <- Clock[F].monotonic
              _       <- metricsTracker.recordAcquisition(endTime - startTime)
            yield wrapConnection(pooled)
          }
          .handleErrorWith { error =>
            // If creation failed (e.g., over limit), try waiting instead
            val deferred = Deferred.unsafe[F, Either[Throwable, Connection[F]]]
            poolState.update { state =>
              state.copy(waitQueue = state.waitQueue :+ deferred)
            } >> deferred.get.rethrow.flatMap { conn =>
              for
                endTime <- Clock[F].monotonic
                _       <- metricsTracker.recordAcquisition(endTime - startTime)
              yield conn
            }
          }
      case AcquireResult.Waiting(deferred) =>
        deferred.get.rethrow.flatMap { conn =>
          for
            endTime <- Clock[F].monotonic
            _       <- metricsTracker.recordAcquisition(endTime - startTime)
          yield conn
        }
    }

    acquireResult.timeout(connectionTimeout).handleErrorWith { _ =>
      metricsTracker.recordTimeout() *>
        Temporal[F].raiseError(new Exception(s"Connection acquisition timeout after ${ connectionTimeout }"))
    }

  private def release(conn: Connection[F]): F[Unit] = for
    startTime <- Clock[F].monotonic
    _         <- releaseConnectionWithStartTime(conn, startTime)
  yield ()

  private def releaseConnectionWithStartTime(conn: Connection[F], startTime: FiniteDuration): F[Unit] =

    poolState.get
      .flatMap { state =>
        state.connections.find(p => p.connection == unwrapConnection(conn)) match {
          case Some(pooled) =>
            resetConnection(pooled.connection).attempt.flatMap {
              case Right(_) =>
                validateConnection(pooled.connection).flatMap { valid =>
                  if valid && !isExpired(pooled) then returnToPool(pooled) *> processWaitQueue()
                  else removeConnection(pooled)
                }
              case Left(_) =>
                removeConnection(pooled)
            }
          case None =>
            Temporal[F].unit
        }
      }
      .flatMap { _ =>
        for
          endTime <- Clock[F].monotonic
          _       <- metricsTracker.recordUsage(endTime - startTime)
        yield ()
      }

  /**
   * Find an idle connection in the pool.
   */
  private def findIdleConnection(state: PoolState[F]): Option[(PooledConnection[F], PoolState[F])] =
    state.idleConnections.headOption.flatMap { id =>
      state.connections.find(_.id == id).map { pooled =>
        val newIdleConnections = state.idleConnections - id
        val newState           = state.copy(idleConnections = newIdleConnections)
        (pooled, newState)
      }
    }

  override def createNewConnection(): F[PooledConnection[F]] = for
    startTime <- Clock[F].monotonic
    result    <- createNewConnectionWithStartTime(startTime)
  yield result

  private def createNewConnectionWithStartTime(startTime: FiniteDuration): F[PooledConnection[F]] =

    for
      id <- idGenerator
      connResource = connection
      conn             <- connResource.allocated.map(_._1)
      now              <- Clock[F].realTime.map(_.toMillis)
      stateRef         <- Ref.of[F, ConnectionState](ConnectionState.InUse)
      lastUsedRef      <- Ref[F].of(now)
      useCountRef      <- Ref[F].of(1L)
      lastValidatedRef <- Ref[F].of(now)
      leakDetectionRef <- Ref.of[F, Option[Fiber[F, Throwable, Unit]]](None)

      pooled = PooledConnection(
                 id              = id,
                 connection      = conn,
                 state           = stateRef,
                 createdAt       = now,
                 lastUsedAt      = lastUsedRef,
                 useCount        = useCountRef,
                 lastValidatedAt = lastValidatedRef,
                 leakDetection   = leakDetectionRef
               )

      // Double-check the limit before adding to prevent race conditions
      added <- poolState.modify { poolState =>
                 if poolState.connections.size >= maxConnections then
                   // Over limit, don't add
                   (poolState, false)
                 else
                   val newState = poolState.copy(
                     connections     = poolState.connections :+ pooled,
                     idleConnections = poolState.idleConnections // Don't add to idle - it's InUse
                   )
                   (newState, true)
               }

      // If we couldn't add it, close the connection and fail
      _ <- if !added then {
             conn.close().attempt.void *>
               Temporal[F].raiseError[Unit](new Exception("Pool reached maximum size"))
           } else Temporal[F].unit

      endTime <- Clock[F].monotonic
      _       <- metricsTracker.recordCreation(endTime - startTime)

      // Start leak detection if configured
      _ <- leakDetectionThreshold.traverse_ { threshold =>
             val leakFiber = Temporal[F]
               .sleep(threshold)
               .flatMap { _ =>
                 pooled.state.get.flatMap {
                   case ConnectionState.InUse => metricsTracker.recordLeak()
                   case _                     => Temporal[F].unit
                 }
               }
               .start

             leakFiber.flatMap(fiber => pooled.leakDetection.set(Some(fiber)))
           }
    yield pooled

  private def resetConnection(conn: Connection[F]): F[Unit] = for
    _ <- conn.setAutoCommit(true).attempt.void
    _ <- conn.rollback().attempt.void
  // clearWarnings is not available in ldbc Connection interface
  // _ <- conn.clearWarnings().attempt.void
  yield ()

  override def validateConnection(conn: Connection[F]): F[Boolean] =
    // Use isClosed as a basic validation
    conn
      .isClosed()
      .timeout(validationTimeout)
      .map(!_) // valid if not closed
      .handleError(_ => false)

  private def isExpired(pooled: PooledConnection[F]): Boolean =
    // Simplified check - will calculate age properly later
    false

  override def returnToPool(pooled: PooledConnection[F]): F[Unit] = for
    _ <- pooled.state.set(ConnectionState.Idle)
    _ <- pooled.leakDetection.get.flatMap(_.traverse_(_.cancel))
    _ <- pooled.leakDetection.set(None)
    _ <- poolState.update { state =>
           state.copy(idleConnections = state.idleConnections + pooled.id)
         }
  yield ()

  override def removeConnection(pooled: PooledConnection[F]): F[Unit] = for
    _ <- pooled.state.set(ConnectionState.Removed)
    _ <- pooled.connection.close().attempt.void
    _ <- pooled.leakDetection.get.flatMap(_.traverse_(_.cancel))
    _ <- poolState.update { state =>
           state.copy(
             connections     = state.connections.filterNot(_ == pooled),
             idleConnections = state.idleConnections - pooled.id
           )
         }
  yield ()

  private def processWaitQueue(): F[Unit] = poolState.modify { state =>
    state.waitQueue.headOption match
      case Some(deferred) =>
        findIdleConnection(state) match
          case Some((pooled, newStateAfterFind)) =>
            val newState = newStateAfterFind.copy(waitQueue = newStateAfterFind.waitQueue.tail)
            val complete = for
              _   <- pooled.state.set(ConnectionState.InUse)
              now <- Clock[F].realTime.map(_.toMillis)
              _   <- pooled.lastUsedAt.set(now)
              _   <- pooled.useCount.update(_ + 1)
              _   <- deferred.complete(Right(wrapConnection(pooled)))
            yield ()
            (newState, complete)
          case None =>
            (state, Temporal[F].unit)

      case None =>
        (state, Temporal[F].unit)
  }.flatten

  /**
   * Wrap a pooled connection for leak detection.
   */
  private def wrapConnection(pooled: PooledConnection[F]): Connection[F] =
    pooled.connection // In a real implementation, this would be a proxy

  /**
   * Unwrap a connection to get the original.
   */
  private def unwrapConnection(conn: Connection[F]): Connection[F] =
    conn // In a real implementation, this would unwrap the proxy

  private def connection: Resource[F, Connection[F]] =
    (before, after) match
      case (Some(b), Some(a)) =>
        Connection.withBeforeAfter(
          host                    = host,
          port                    = port,
          user                    = user,
          before                  = b,
          after                   = a,
          password                = password,
          database                = database,
          debug                   = debug,
          ssl                     = ssl,
          socketOptions           = socketOptions,
          readTimeout             = readTimeout,
          allowPublicKeyRetrieval = allowPublicKeyRetrieval,
          useCursorFetch          = useCursorFetch,
          useServerPrepStmts      = useServerPrepStmts,
          databaseTerm            = databaseTerm
        )
      case (Some(b), None) =>
        Connection.withBeforeAfter(
          host                    = host,
          port                    = port,
          user                    = user,
          before                  = b,
          after                   = (_, _) => Async[F].unit,
          password                = password,
          database                = database,
          debug                   = debug,
          ssl                     = ssl,
          socketOptions           = socketOptions,
          readTimeout             = readTimeout,
          allowPublicKeyRetrieval = allowPublicKeyRetrieval,
          useCursorFetch          = useCursorFetch,
          useServerPrepStmts      = useServerPrepStmts,
          databaseTerm            = databaseTerm
        )
      case (None, _) =>
        Connection(
          host                    = host,
          port                    = port,
          user                    = user,
          password                = password,
          database                = database,
          debug                   = debug,
          ssl                     = ssl,
          socketOptions           = socketOptions,
          readTimeout             = readTimeout,
          allowPublicKeyRetrieval = allowPublicKeyRetrieval,
          useCursorFetch          = useCursorFetch,
          useServerPrepStmts      = useServerPrepStmts,
          databaseTerm            = databaseTerm
        )

object MySQLPooledDataSource:

  def fromConfig[F[_]: Async: Network: Console: Hashing: UUIDGen](
    config:         MySQLConfig,
    logHandler:     Option[LogHandler[F]] = None,
    metricsTracker: Option[PoolMetricsTracker[F]] = None
  ): Resource[F, MySQLPooledDataSource[F, Unit]] =
    create(config, metricsTracker, Sync[F].delay(java.util.UUID.randomUUID().toString))

  private[connector] def create[F[_]: Async: Network: Console: Hashing: UUIDGen, A](
    config:         MySQLConfig,
    metricsTracker: Option[PoolMetricsTracker[F]],
    idGenerator:    F[String]
  ): Resource[F, MySQLPooledDataSource[F, Unit]] =

    val tracker     = metricsTracker.getOrElse(PoolMetricsTracker.noop[F])
    val houseKeeper = HouseKeeper.fromAsync[F](config, tracker)

    def pool = for
      poolState <- Ref[F].of(PoolState.empty[F])
      pool = new MySQLPooledDataSource[F, Unit](
               host                    = config.host,
               port                    = config.port,
               user                    = config.user,
               password                = config.password,
               database                = config.database,
               debug                   = config.debug,
               ssl                     = config.ssl,
               socketOptions           = config.socketOptions,
               readTimeout             = config.readTimeout,
               allowPublicKeyRetrieval = config.allowPublicKeyRetrieval,
               databaseTerm            = config.databaseTerm,
               useCursorFetch          = config.useCursorFetch,
               useServerPrepStmts      = config.useServerPrepStmts,
               minConnections          = config.minConnections,
               maxConnections          = config.maxConnections,
               connectionTimeout       = config.connectionTimeout,
               idleTimeout             = config.idleTimeout,
               maxLifetime             = config.maxLifetime,
               validationTimeout       = config.validationTimeout,
               leakDetectionThreshold  = config.leakDetectionThreshold,
               adaptiveSizing          = config.adaptiveSizing,
               adaptiveInterval        = config.adaptiveInterval,
               metricsTracker          = tracker,
               poolState               = poolState,
               idGenerator             = idGenerator,
               houseKeeper             = None,
               adaptiveSizer           = None
             )

      // Initialize minimum connections
      _ <- (1 to config.minConnections).toList.traverse_ { _ =>
             pool.createNewConnection().flatMap { pooled =>
               // First set to Idle state before returning to pool
               pooled.state.set(ConnectionState.Idle) *> pool.returnToPool(pooled)
             }
           }
    yield pool

    Resource
      .make(pool) { p =>
        p.close
      }
      .flatMap { pool =>
        // Start background tasks
        val backgroundTasks = List(
          houseKeeper.start(pool),
          if config.adaptiveSizing then AdaptivePoolSizer.start(pool, config, tracker)
          else Resource.pure[F, Unit](())
        )

        backgroundTasks.sequence_.as(pool)
      }
