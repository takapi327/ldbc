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
import cats.effect.std.Console
import cats.effect.std.UUIDGen
import cats.effect.syntax.all.*

import fs2.hashing.Hashing
import fs2.io.net.*

import org.typelevel.otel4s.metrics.Meter
import org.typelevel.otel4s.trace.Tracer

import ldbc.sql.DatabaseMetaData

import ldbc.connector.*
import ldbc.connector.exception.SQLException
import ldbc.connector.telemetry.{ DatabaseMetrics, PoolMetricsState }

import ldbc.authentication.plugin.AuthenticationPlugin
import ldbc.DataSource

/**
 * A DataSource implementation that manages a pool of reusable database connections.
 * 
 * PooledDataSource extends the basic [[ldbc.DataSource]] interface to provide connection
 * pooling capabilities, which significantly improve performance by reusing existing
 * connections rather than creating new ones for each request.
 * 
 * Key features include:
 * - Connection reuse to minimize the overhead of connection establishment
 * - Configurable pool size with minimum and maximum connections
 * - Connection validation to ensure healthy connections
 * - Idle timeout management to release unused connections
 * - Leak detection to identify connections not properly returned to the pool
 * - Adaptive sizing to dynamically adjust pool size based on load
 * - Comprehensive metrics tracking for monitoring pool health
 * 
 * The pool maintains connections in different states:
 * - Available: Ready for use
 * - In-use: Currently borrowed by a client
 * - Invalid: Failed validation and awaiting removal
 * 
 * @tparam F the effect type (e.g., IO) that wraps asynchronous operations
 */
trait PooledDataSource[F[_]] extends DataSource[F]:

  /** The minimum number of connections to maintain in the pool. */
  def minConnections: Int

  /** The maximum number of connections allowed in the pool. */
  def maxConnections: Int

  /** The maximum time to wait for a connection to become available. */
  def connectionTimeout: FiniteDuration

  /** The maximum time a connection can remain idle before being closed. */
  def idleTimeout: FiniteDuration

  /** The maximum lifetime of a connection in the pool. */
  def maxLifetime: FiniteDuration

  /** The maximum time to wait for connection validation. */
  def validationTimeout: FiniteDuration

  /** Optional threshold for detecting connection leaks. */
  def leakDetectionThreshold: Option[FiniteDuration]

  /** Whether adaptive pool sizing is enabled. */
  def adaptiveSizing: Boolean

  /** The interval at which the adaptive sizing algorithm runs. */
  def adaptiveInterval: FiniteDuration

  /** The metrics tracker for monitoring pool performance. */
  def metricsTracker: PoolMetricsTracker[F]

  /** Internal state of the connection pool. */
  def poolState: Ref[F, PoolState[F]]

  /** Generates unique identifiers for connections. */
  def idGenerator: F[String]

  /** The alive bypass window for validation optimization. */
  def aliveBypassWindow: FiniteDuration

  /** The keepalive time for idle connections. */
  def keepaliveTime: Option[FiniteDuration]

  /** The connection test query. */
  def connectionTestQuery: Option[String]

  /** The pool logger for logging pool events. */
  def poolLogger: PoolLogger[F]

  /**
   * Returns the current status of the pool.
   * 
   * @return a PoolStatus containing information about available, in-use, and total connections
   */
  def status: F[PoolStatus]

  /**
   * Returns comprehensive metrics about pool performance.
   * 
   * @return a PoolMetrics object with detailed statistics
   */
  def metrics: F[PoolMetrics]

  /**
   * Gracefully shuts down the pool, closing all connections.
   * 
   * This method will:
   * - Stop accepting new connection requests
   * - Wait for in-use connections to be returned
   * - Close all connections
   * - Cancel background maintenance tasks
   */
  def close: F[Unit]

  /**
   * Creates a new pooled connection.
   * 
   * @return a new PooledConnection wrapped in the effect type
   */
  def createNewConnection(): F[PooledConnection[F]]

  /**
   * Circuit breaker for connection creation.
   */
  def circuitBreaker: CircuitBreaker[F]

  /**
   * Creates a new connection specifically for pool initialization.
   * Unlike createNewConnection, this creates connections in idle state.
   * 
   * @return a new PooledConnection in idle state
   */
  def createNewConnectionForPool(): F[PooledConnection[F]]

  /**
   * Returns a connection to the pool for reuse.
   * 
   * @param pooled the connection to return to the pool
   */
  def returnToPool(pooled: PooledConnection[F]): F[Unit]

  /**
   * Removes a connection from the pool permanently.
   * 
   * @param pooled the connection to remove
   */
  def removeConnection(pooled: PooledConnection[F]): F[Unit]

  /**
   * Validates that a connection is still healthy and usable.
   * 
   * @param conn the connection to validate
   * @return true if the connection is valid, false otherwise
   */
  def validateConnection(conn: Connection[F]): F[Boolean]

object PooledDataSource:

  private case class Impl[F[_]: Async: Network: Console: Hashing: UUIDGen, A](
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String]                        = None,
    database:                Option[String]                        = None,
    debug:                   Boolean                               = false,
    ssl:                     SSL                                   = SSL.None,
    socketOptions:           List[SocketOption]                    = MySQLConfig.defaultSocketOptions,
    readTimeout:             Duration                              = Duration.Inf,
    allowPublicKeyRetrieval: Boolean                               = false,
    databaseTerm:            Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG),
    useCursorFetch:          Boolean                               = false,
    useServerPrepStmts:      Boolean                               = false,
    plugins:                 List[AuthenticationPlugin[F]]         = List.empty[AuthenticationPlugin[F]],
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
    databaseMetrics:         DatabaseMetrics[F],
    poolName:                String,
    poolState:               Ref[F, PoolState[F]],
    idGenerator:             F[String],
    connectionBag:           ConcurrentBag[F, PooledConnection[F]],
    circuitBreaker:          CircuitBreaker[F],
    aliveBypassWindow:       FiniteDuration,
    keepaliveTime:           Option[FiniteDuration],
    connectionTestQuery:     Option[String],
    poolLogger:              PoolLogger[F]
  )(using Tracer[F])
    extends PooledDataSource[F]:

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

    override def close: F[Unit] =
      poolLogger.info(
        s"Closing connection pool (host: $host:$port${ database.map(d => s", database: $d").getOrElse("") })"
      ) >>
        poolState.modify { state =>
          val newState = state.copy(closed = true)
          val closeAll = state.connections.traverse_ { pooled =>
            pooled.finalizer.attempt.flatMap {
              case Left(error) =>
                poolLogger.debug(s"Error closing connection ${ pooled.id }: ${ error.getMessage }") >>
                  pooled.connection.close().attempt.void
              case Right(_) =>
                Temporal[F].unit
            }
          }
          val failWaiters = state.waitQueue.traverse_ { deferred =>
            deferred.complete(Left(new SQLException("Pool closed"))).attempt.void
          }
          (newState, closeAll *> failWaiters)
        }.flatten >>
        poolLogger.info("Connection pool closed successfully")

    private def acquire: F[Connection[F]] = for
      startTime <- Clock[F].monotonic
      result    <- acquireConnectionWithStartTime(startTime)
    yield result

    private def acquireConnectionWithStartTime(startTime: FiniteDuration): F[Connection[F]] =
      poolState.get.flatMap { state =>
        if state.closed then Temporal[F].raiseError(new SQLException("Pool is closed"))
        else
          connectionBag.borrow(connectionTimeout).flatMap {
            case Some(pooled) =>
              // Successfully borrowed from bag
              for
                // Check if validation is needed
                shouldValidate <- needsValidation(pooled)
                valid          <- if shouldValidate then {
                           validateConnection(pooled.connection).flatTap {
                             case true  => Temporal[F].unit
                             case false =>
                               poolLogger.warn(s"Connection ${ pooled.id } failed validation, removing from pool")
                           }
                         } else Temporal[F].pure(true)
                result <- if !valid then {
                            // Connection is invalid, remove it and try again
                            removeConnection(pooled) >> acquireConnectionWithStartTime(startTime)
                          } else
                            for
                              _ <- pooled.state.set(ConnectionState.InUse)
                              // Remove from idleConnections when acquired
                              _       <- poolState.update(s => s.copy(idleConnections = s.idleConnections - pooled.id))
                              now     <- Clock[F].realTime.map(_.toMillis)
                              _       <- pooled.lastUsedAt.set(now)
                              _       <- pooled.useCount.update(_ + 1)
                              endTime <- Clock[F].monotonic
                              _       <- metricsTracker.recordAcquisition(endTime - startTime)
                              _       <- databaseMetrics.recordConnectionWaitTime(endTime - startTime, poolName)
                              // Start leak detection if configured
                              _ <- leakDetectionThreshold.traverse_ { threshold =>
                                     val leakFiber = Temporal[F]
                                       .sleep(threshold)
                                       .flatMap { _ =>
                                         pooled.state.get.flatMap {
                                           case ConnectionState.InUse =>
                                             poolLogger.warn(
                                               s"Possible connection leak detected: Connection ${ pooled.id } has been in use for longer than $threshold"
                                             ) >>
                                               metricsTracker.recordLeak()
                                           case _ => Temporal[F].unit
                                         }
                                       }
                                       .start

                                     leakFiber.flatMap(fiber => pooled.leakDetection.set(Some(fiber)))
                                   }
                            yield wrapConnection(pooled)
              yield result

            case None =>
              // Timeout or no connections available
              // Check if we can create a new connection
              poolState.get.flatMap { currentState =>
                if currentState.connections.size < maxConnections then
                  createNewConnection().flatMap { pooled =>
                    for
                      endTime <- Clock[F].monotonic
                      _       <- metricsTracker.recordAcquisition(endTime - startTime)
                      _       <- databaseMetrics.recordConnectionWaitTime(endTime - startTime, poolName)
                    yield wrapConnection(pooled)
                  }
                else
                  // Count active connections for more detailed error message
                  currentState.connections.traverse(_.state.get).flatMap { states =>
                    val activeCount  = states.count(_ == ConnectionState.InUse)
                    val idleCount    = states.count(_ == ConnectionState.Idle)
                    val errorMessage =
                      s"Connection acquisition timeout after $connectionTimeout " +
                        s"(host: $host:$port, db: ${ database.getOrElse("none") }, " +
                        s"pool: ${ currentState.connections.size }/${ maxConnections }, " +
                        s"active: $activeCount, idle: $idleCount, " +
                        s"waiting: ${ currentState.waitQueue.size })"
                    metricsTracker.recordTimeout() *>
                      databaseMetrics.recordConnectionTimeout(poolName) *>
                      poolLogger.error(errorMessage) *>
                      Temporal[F].raiseError(new SQLException(errorMessage))
                  }
              }
          }
      }

    private def release(conn: Connection[F]): F[Unit] = for
      startTime <- Clock[F].monotonic
      _         <- releaseConnectionWithStartTime(conn, startTime)
    yield ()

    private def releaseConnectionWithStartTime(conn: Connection[F], startTime: FiniteDuration): F[Unit] =
      // Extract the pooled connection from proxy if wrapped
      val pooledF: F[Option[PooledConnection[F]]] = conn match {
        case proxy: ProxyConnection[F] => Temporal[F].pure(Some(proxy.pooled))
        case _                         =>
          // Fallback: search by unwrapped connection
          connectionBag.values.map { connections =>
            connections.find(p => p.connection == unwrapConnection(conn))
          }
      }

      pooledF.flatMap {
        case Some(pooled) =>
          // Cancel leak detection
          pooled.leakDetection.get.flatMap(_.traverse_(_.cancel)) >>
            pooled.leakDetection.set(None) >>
            pooled.state.set(ConnectionState.Idle) >>
            resetConnection(pooled.connection).attempt.flatMap {
              case Right(_) =>
                for
                  // Skip validation if connection was recently validated
                  shouldValidate <- needsValidation(pooled)
                  valid          <- if shouldValidate then {
                             validateConnection(pooled.connection).flatTap {
                               case true  => Temporal[F].unit
                               case false =>
                                 poolLogger.warn(
                                   s"Connection ${ pooled.id } failed validation on release, removing from pool"
                                 )
                             }
                           } else Temporal[F].pure(true)
                  expired <- isExpired(pooled)
                  _       <- if valid && !expired then {
                         // Return to bag for reuse, then add to idleConnections
                         // Order matters: requite first to ensure connection is actually returned,
                         // then update idleConnections to maintain consistency
                         connectionBag.requite(pooled) *>
                           poolState.update(s => s.copy(idleConnections = s.idleConnections + pooled.id)) *>
                           Clock[F].monotonic.flatMap { endTime =>
                             metricsTracker.recordUsage(endTime - startTime) *>
                               databaseMetrics.recordConnectionUseTime(endTime - startTime, poolName)
                           }
                       } else
                         // Invalid or expired, remove from pool
                         removeConnection(pooled) *>
                           Clock[F].monotonic.flatMap { endTime =>
                             metricsTracker.recordUsage(endTime - startTime) *>
                               databaseMetrics.recordConnectionUseTime(endTime - startTime, poolName)
                           }
                yield ()
              case Left(error) =>
                // Reset failed, remove from pool
                poolLogger.warn(s"Failed to reset connection ${ pooled.id } on release: ${ error.getMessage }") >>
                  removeConnection(pooled) *>
                  Clock[F].monotonic.flatMap { endTime =>
                    metricsTracker.recordUsage(endTime - startTime) *>
                      databaseMetrics.recordConnectionUseTime(endTime - startTime, poolName)
                  }
            }
        case None =>
          // Connection not found - still record the usage time
          Clock[F].monotonic.flatMap { endTime =>
            metricsTracker.recordUsage(endTime - startTime) *>
              databaseMetrics.recordConnectionUseTime(endTime - startTime, poolName)
          }
      }

    override def createNewConnection(): F[PooledConnection[F]] = for
      startTime <- Clock[F].monotonic
      result    <- createNewConnectionWithStartTime(startTime)
    yield result

    private def createNewConnectionWithStartTime(startTime: FiniteDuration): F[PooledConnection[F]] =
      createNewConnectionWithState(startTime, ConnectionState.InUse, 1L)

    override def createNewConnectionForPool(): F[PooledConnection[F]] = for
      startTime <- Clock[F].monotonic
      result    <- createNewConnectionWithState(startTime, ConnectionState.Idle, 0L)
    yield result

    private def createNewConnectionWithState(
      startTime:       FiniteDuration,
      initialState:    ConnectionState,
      initialUseCount: Long
    ): F[PooledConnection[F]] =
      circuitBreaker.protect {
        for
          id        <- idGenerator
          allocated <- connection.allocated
          (conn, finalizer) = allocated
          now              <- Clock[F].realTime.map(_.toMillis)
          stateRef         <- Ref.of[F, ConnectionState](initialState)
          lastUsedRef      <- Ref[F].of(now)
          useCountRef      <- Ref[F].of(initialUseCount)
          lastValidatedRef <- Ref[F].of(now)
          leakDetectionRef <- Ref.of[F, Option[Fiber[F, Throwable, Unit]]](None)
          bagStateRef      <- Ref.of[F, Int](
                           if initialState == ConnectionState.InUse then BagEntry.STATE_IN_USE
                           else BagEntry.STATE_NOT_IN_USE
                         )

          pooled = PooledConnection(
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

          // For Idle state connections (pool initialization), add to bag first
          // to ensure consistency: bag contains the connection before idleConnections tracks it
          _ <-
            if initialState != ConnectionState.InUse then connectionBag.add(pooled)
            else Temporal[F].unit

          // Double-check the limit before adding to prevent race conditions
          added <- poolState.modify { poolState =>
                     if poolState.connections.size >= maxConnections then
                       // Over limit, don't add
                       (poolState, false)
                     else
                       val newState = poolState.copy(
                         connections = poolState.connections :+ pooled,
                         // Add to idleConnections if created in Idle state (for pool initialization)
                         idleConnections =
                           if initialState == ConnectionState.Idle then poolState.idleConnections + pooled.id
                           else poolState.idleConnections
                       )
                       (newState, true)
                   }

          // If we couldn't add it, clean up and fail
          _ <- if !added then {
                 // Remove from bag if we added it earlier
                 (if initialState != ConnectionState.InUse then connectionBag.remove(pooled).void
                  else Temporal[F].unit) *>
                   poolLogger.warn(s"Cannot create new connection: pool at maximum size ($maxConnections)") *>
                   conn.close().attempt.void *>
                   Temporal[F].raiseError[Unit](new SQLException("Pool reached maximum size"))
               } else Temporal[F].unit

          endTime <- Clock[F].monotonic
          _       <- metricsTracker.recordCreation(endTime - startTime)
          _       <- databaseMetrics.recordConnectionCreateTime(endTime - startTime, poolName)
        yield pooled
      }

    private def resetConnection(conn: Connection[F]): F[Unit] = for
      _ <- conn.setAutoCommit(true).attempt.void
      _ <- conn.rollback().attempt.void
    // clearWarnings is not available in ldbc Connection interface
    // _ <- conn.clearWarnings().attempt.void
    yield ()

    override def validateConnection(conn: Connection[F]): F[Boolean] =
      connectionTestQuery match
        case Some(query) =>
          // Use custom test query
          val validation = for
            closed <- conn.isClosed()
            valid  <-
              if !closed then executeTestQuery(conn, query)
              else Temporal[F].pure(false)
          yield !closed && valid

          validation
            .timeout(validationTimeout)
            .handleError { error =>
              poolLogger.debug(
                s"Connection validation failed or timed out after $validationTimeout: ${ error.getMessage }"
              )
              false
            }

        case None =>
          // Use JDBC4 isValid() method (preferred)
          val validation = for
            closed <- conn.isClosed()
            valid  <- if !closed then {
                       // Ensure minimum timeout of 1 second as per JDBC spec
                       conn.isValid(validationTimeout.toSeconds.toInt.max(1))
                     } else Temporal[F].pure(false)
          yield !closed && valid

          validation
            .timeout(validationTimeout)
            .handleError { error =>
              poolLogger.debug(
                s"Connection validation failed or timed out after $validationTimeout: ${ error.getMessage }"
              )
              false
            }

    private def executeTestQuery(conn: Connection[F], query: String): F[Boolean] =
      conn
        .createStatement()
        .flatMap { stmt =>
          stmt.execute(query).as(true).guarantee(stmt.close())
        }
        .handleError(_ => false)

    private def needsValidation(pooled: PooledConnection[F]): F[Boolean] =
      if aliveBypassWindow.toMillis == 0 then
        // Bypass disabled, always validate
        Temporal[F].pure(true)
      else
        for
          now      <- Clock[F].realTime.map(_.toMillis)
          lastUsed <- pooled.lastUsedAt.get
          elapsed = now - lastUsed
        yield elapsed > aliveBypassWindow.toMillis

    private def isExpired(pooled: PooledConnection[F]): F[Boolean] =
      Clock[F].realTime.map { now =>
        val age = now.toMillis - pooled.createdAt
        age > maxLifetime.toMillis
      }

    override def returnToPool(pooled: PooledConnection[F]): F[Unit] =
      // Update the connection state, return to bag, then update idleConnections
      // Order: state change -> bag requite -> idleConnections update
      // This ensures the connection is in the correct state and available in the bag
      // before being tracked as idle
      pooled.state.set(ConnectionState.Idle) *>
        connectionBag.requite(pooled) *>
        poolState.update(s => s.copy(idleConnections = s.idleConnections + pooled.id))

    override def removeConnection(pooled: PooledConnection[F]): F[Unit] = for
      currentState <- pooled.state.get
      _            <- poolLogger.debug(s"Removing connection ${ pooled.id } from pool (state: $currentState)")
      _            <- pooled.state.set(ConnectionState.Removed)
      _            <- connectionBag.remove(pooled)
      _            <- pooled.finalizer.attempt.void // Use the finalizer instead of close()
      _            <- pooled.leakDetection.get.flatMap(_.traverse_(_.cancel))
      _            <- poolState.update { state =>
             state.copy(
               connections     = state.connections.filterNot(_ == pooled),
               idleConnections = state.idleConnections - pooled.id
             )
           }
      _ <- metricsTracker.recordRemoval()
    yield ()

    /**
     * Wrap a pooled connection for leak detection and statement tracking.
     */
    private def wrapConnection(pooled: PooledConnection[F]): Connection[F] =
      new ProxyConnection[F](pooled, release)

    /**
     * Unwrap a connection to get the original.
     */
    private def unwrapConnection(conn: Connection[F]): Connection[F] =
      conn match
        case proxy: ProxyConnection[F] => proxy.pooled.connection
        case _                         => conn

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
            databaseTerm            = databaseTerm,
            plugins                 = plugins
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
            databaseTerm            = databaseTerm,
            plugins                 = plugins
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
            databaseTerm            = databaseTerm,
            plugins                 = plugins
          )

  private[connector] def create[F[_]: Async: Network: Console: Hashing: UUIDGen, A](
    config:          MySQLConfig,
    metricsTracker:  Option[PoolMetricsTracker[F]],
    meter:           Option[Meter[F]],
    idGenerator:     F[String],
    plugins:         List[AuthenticationPlugin[F]],
    before:          Option[Connection[F] => F[A]] = None,
    after:           Option[(A, Connection[F]) => F[Unit]] = None
  )(using Tracer[F]): Resource[F, PooledDataSource[F]] =

    // Validate configuration before creating the pool (similar to HikariDataSource)
    Resource
      .eval(PoolConfigValidator.validate(config))
      .flatMap { _ =>
        createValidatedPool(config, metricsTracker, meter, idGenerator, plugins, before, after)
      }
      .handleErrorWith { error =>
        Resource.eval(Async[F].raiseError(error))
      }

  private def createValidatedPool[F[_]: Async: Network: Console: Hashing: UUIDGen, A](
    config:          MySQLConfig,
    metricsTracker:  Option[PoolMetricsTracker[F]],
    meter:           Option[Meter[F]],
    idGenerator:     F[String],
    plugins:         List[AuthenticationPlugin[F]],
    before:          Option[Connection[F] => F[A]],
    after:           Option[(A, Connection[F]) => F[Unit]]
  )(using Tracer[F]): Resource[F, PooledDataSource[F]] =

    val trackerResource: Resource[F, PoolMetricsTracker[F]] =
      metricsTracker match
        case Some(tracker) => Resource.pure(tracker)
        case None          =>
          meter match
            case Some(_) => Resource.eval(PoolMetricsTracker.inMemory[F])
            case None    => Resource.pure(PoolMetricsTracker.noop[F])

    val databaseMetricsResource: Resource[F, DatabaseMetrics[F]] =
      meter match
        case Some(m) => DatabaseMetrics.fromMeter(m)
        case None    => Resource.pure(DatabaseMetrics.noop[F])

    val poolLogger = PoolLogger.console[F](config.debug || config.logPoolState)

    def createPool(tracker: PoolMetricsTracker[F], dbMetrics: DatabaseMetrics[F]) = for
      poolState      <- Ref[F].of(PoolState.empty[F])
      connectionBag  <- ConcurrentBag[F, PooledConnection[F]]()
      circuitBreaker <- CircuitBreaker[F](
                          CircuitBreaker.Config(
                            maxFailures  = 5,
                            resetTimeout = 30.seconds
                          )
                        )
    yield Impl[F, A](
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
      databaseMetrics         = dbMetrics,
      poolName                = config.poolName,
      poolState               = poolState,
      idGenerator             = idGenerator,
      connectionBag           = connectionBag,
      circuitBreaker          = circuitBreaker,
      aliveBypassWindow       = config.aliveBypassWindow,
      keepaliveTime           = config.keepaliveTime,
      connectionTestQuery     = config.connectionTestQuery,
      poolLogger              = poolLogger,
      plugins                 = plugins,
      before                  = before,
      after                   = after
    )

    // Initialize minimum connections within the resource scope
    def createMinimumConnections(pool: PooledDataSource[F]): Resource[F, Unit] =
      Resource.make(
        (1 to config.minConnections).toList.traverse_ { _ =>
          pool.createNewConnectionForPool()
        }
      )(_ => pool.close) // Close the pool after minimum connections are no longer needed

    def createBackgroundResources(pool: PooledDataSource[F], tracker: PoolMetricsTracker[F]): Resource[F, Unit] =
      val houseKeeper       = HouseKeeper.fromAsync[F](config, tracker)
      val adaptivePoolSizer = AdaptivePoolSizer.fromAsync[F](config, tracker)
      val keepaliveExecutor = config.keepaliveTime.map(KeepaliveExecutor.fromAsync[F](_, tracker))
      val statusReporter    =
        if config.logPoolState then Some(PoolStatusReporter[F](config.poolStateLogInterval, poolLogger, tracker))
        else None
      val backgroundResources = List(
        Some(houseKeeper.start(pool)),
        if config.adaptiveSizing then Some(adaptivePoolSizer.start(pool))
        else None,
        keepaliveExecutor.map(_.start(pool)),
        statusReporter.map(_.start(pool, config.poolName))
      ).flatten
      backgroundResources.sequence_

    def registerObservableMetrics(pool: PooledDataSource[F], dbMetrics: DatabaseMetrics[F]): Resource[F, Unit] =
      dbMetrics.registerPoolStateCallback(
        config.poolName,
        config.minConnections,
        config.maxConnections,
        pool.status.map(s => PoolMetricsState(s.idle.toLong, s.active.toLong, s.waiting.toLong))
      )

    for
      tracker   <- trackerResource
      dbMetrics <- databaseMetricsResource
      pool      <- Resource.eval(createPool(tracker, dbMetrics))
      _         <- registerObservableMetrics(pool, dbMetrics)
      _         <- createMinimumConnections(pool)
      _         <- createBackgroundResources(pool, tracker)
    yield pool

  /**
   * Creates a PooledDataSource from a MySQL configuration.
   * 
   * This is the primary way to create a connection pool. The pool will be initialized
   * with the settings specified in the configuration, including minimum and maximum
   * connection counts, timeouts, and maintenance intervals.
   * 
   * The returned Resource ensures proper lifecycle management - the pool will be
   * properly initialized when acquired and cleanly shut down when released.
   * 
   * @param config the MySQL configuration containing all pool settings
   * @param metricsTracker optional tracker for collecting pool metrics (defaults to in-memory tracker)
   * @param tracer optional OpenTelemetry tracer for distributed tracing (defaults to no-op tracer)
   * @tparam F the effect type with required type class instances
   * @return a Resource that manages the pooled data source lifecycle
   */
  def fromConfig[F[_]: Async: Network: Console: Hashing: UUIDGen](
    config:          MySQLConfig,
    metricsTracker:  Option[PoolMetricsTracker[F]] = None,
    meter:           Option[Meter[F]] = None,
    tracer:          Option[Tracer[F]] = None,
    plugins:         List[AuthenticationPlugin[F]] = List.empty[AuthenticationPlugin[F]]
  ): Resource[F, PooledDataSource[F]] =
    given Tracer[F] = tracer.getOrElse(Tracer.noop[F])
    create(config, metricsTracker, meter, UUIDGen[F].randomUUID.map(_.toString), plugins)

    /**
   * Creates a PooledDataSource with before/after hooks for each connection use.
   * 
   * This variant allows you to specify callbacks that will be executed before
   * and after each connection is used. This is useful for:
   * - Setting up connection-specific state (e.g., session variables)
   * - Logging or auditing connection usage
   * - Cleaning up after connection use
   * 
   * The before hook is called after acquiring a connection but before returning it
   * to the client. The after hook is called when the connection is returned to the pool.
   * 
   * @param config the MySQL configuration containing all pool settings
   * @param metricsTracker optional tracker for collecting pool metrics (defaults to in-memory tracker)
   * @param tracer optional OpenTelemetry tracer for distributed tracing (defaults to no-op tracer)
   * @param before optional callback executed before connection use
   * @param after optional callback executed after connection use
   * @tparam F the effect type with required type class instances
   * @tparam A the type returned by the before callback and passed to the after callback
   * @return a Resource that manages the pooled data source lifecycle
   */
  def fromConfigWithBeforeAfter[F[_]: Async: Network: Console: Hashing: UUIDGen, A](
    config:          MySQLConfig,
    metricsTracker:  Option[PoolMetricsTracker[F]] = None,
    meter:           Option[Meter[F]] = None,
    tracer:          Option[Tracer[F]] = None,
    plugins:         List[AuthenticationPlugin[F]] = List.empty[AuthenticationPlugin[F]],
    before:          Option[Connection[F] => F[A]] = None,
    after:           Option[(A, Connection[F]) => F[Unit]] = None
  ): Resource[F, PooledDataSource[F]] =
    given Tracer[F] = tracer.getOrElse(Tracer.noop[F])
    create(config, metricsTracker, meter, UUIDGen[F].randomUUID.map(_.toString), plugins, before, after)
