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

  /** Background fiber performing pool maintenance tasks. */
  def houseKeeper: Option[Fiber[F, Throwable, Unit]]

  /** Background fiber performing adaptive pool sizing. */
  def adaptiveSizer: Option[Fiber[F, Throwable, Unit]]

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
        pooled.finalizer.attempt.void // Use the finalizer to properly clean up
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
              } >> deferred.get.rethrow
                .flatMap { conn =>
                  for
                    endTime <- Clock[F].monotonic
                    _       <- metricsTracker.recordAcquisition(endTime - startTime)
                  yield conn
                }
                .onCancel {
                  // Remove from wait queue on cancellation
                  poolState.update { state =>
                    state.copy(waitQueue = state.waitQueue.filterNot(_ eq deferred))
                  }
                }
            }
        case AcquireResult.Waiting(deferred) =>
          deferred.get.rethrow
            .flatMap { conn =>
              for
                endTime <- Clock[F].monotonic
                _       <- metricsTracker.recordAcquisition(endTime - startTime)
              yield conn
            }
            .onCancel {
              // Remove from wait queue on cancellation
              poolState.update { state =>
                state.copy(waitQueue = state.waitQueue.filterNot(_ eq deferred))
              }
            }
      }

      acquireResult.timeout(connectionTimeout).handleErrorWith { _ =>
        poolState.get.flatMap { state =>
          val errorMessage =
            s"Connection acquisition timeout after $connectionTimeout (host: $host:$port, db: ${ database.getOrElse("none") }, pool closed: ${ state.closed }, total: ${ state.connections.size }, waiting: ${ state.waitQueue.size })"
          metricsTracker.recordTimeout() *>
            Temporal[F].raiseError(new Exception(errorMessage))
        }
      }

    private def release(conn: Connection[F]): F[Unit] = for
      startTime <- Clock[F].monotonic
      _         <- releaseConnectionWithStartTime(conn, startTime)
    yield ()

    private def releaseConnectionWithStartTime(conn: Connection[F], startTime: FiniteDuration): F[Unit] =
      // Extract the pooled connection from proxy if wrapped
      val pooledF: F[Option[PooledConnection[F]]] = conn match {
        case proxy: ConnectionProxy[F] => Temporal[F].pure(Some(proxy.pooled))
        case _                         =>
          // Fallback: search by unwrapped connection
          poolState.get.map { state =>
            state.connections.find(p => p.connection == unwrapConnection(conn))
          }
      }

      pooledF.flatMap {
        case Some(pooled) =>
          resetConnection(pooled.connection).attempt.flatMap {
            case Right(_) =>
              validateConnection(pooled.connection).flatMap { valid =>
                if valid && !isExpired(pooled) then
                  returnToPool(pooled) *> processWaitQueue() *>
                    Clock[F].monotonic.flatMap { endTime =>
                      metricsTracker.recordUsage(endTime - startTime)
                    }
                else
                  removeConnection(pooled) *>
                    Clock[F].monotonic.flatMap { endTime =>
                      metricsTracker.recordUsage(endTime - startTime)
                    }
              }
            case Left(_) =>
              removeConnection(pooled) *>
                Clock[F].monotonic.flatMap { endTime =>
                  metricsTracker.recordUsage(endTime - startTime)
                }
          }
        case None =>
          // Connection not found - still record the usage time
          Clock[F].monotonic.flatMap { endTime =>
            metricsTracker.recordUsage(endTime - startTime)
          }
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
      createNewConnectionWithState(startTime, ConnectionState.InUse, 1L)

    def createNewConnectionForPool(): F[PooledConnection[F]] = for
      startTime <- Clock[F].monotonic
      result    <- createNewConnectionWithState(startTime, ConnectionState.Idle, 0L)
    yield result

    private def createNewConnectionWithState(
      startTime:       FiniteDuration,
      initialState:    ConnectionState,
      initialUseCount: Long
    ): F[PooledConnection[F]] =
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

        pooled = PooledConnection(
                   id              = id,
                   connection      = conn,
                   finalizer       = finalizer,
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
      _ <- pooled.finalizer.attempt.void // Use the finalizer instead of close()
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
      new ConnectionProxy[F](pooled, release)

    /**
     * Unwrap a connection to get the original.
     */
    private def unwrapConnection(conn: Connection[F]): Connection[F] =
      conn match
        case proxy: ConnectionProxy[F] => proxy.pooled.connection
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

  private[connector] def create[F[_]: Async: Network: Console: Hashing: UUIDGen, A](
    config:         MySQLConfig,
    metricsTracker: Option[PoolMetricsTracker[F]],
    idGenerator:    F[String],
    before:         Option[Connection[F] => F[A]] = None,
    after:          Option[(A, Connection[F]) => F[Unit]] = None
  ): Resource[F, PooledDataSource[F]] =

    val tracker           = metricsTracker.getOrElse(PoolMetricsTracker.noop[F])
    val houseKeeper       = HouseKeeper.fromAsync[F](config, tracker)
    val adaptivePoolSizer = AdaptivePoolSizer.fromAsync[F](config, tracker)

    def createPool = for poolState <- Ref[F].of(PoolState.empty[F])
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
      poolState               = poolState,
      idGenerator             = idGenerator,
      houseKeeper             = None,
      adaptiveSizer           = None
    )

    Resource
      .eval(createPool)
      .flatMap { pool =>
        Resource.make {
          // Initialize minimum connections within the resource scope
          val init = (1 to config.minConnections).toList.traverse_ { _ =>
            pool.createNewConnectionForPool().flatMap { pooled =>
              pool.returnToPool(pooled)
            }
          }
          init.as(pool)
        } { pool =>
          // Ensure background tasks are stopped before closing the pool
          pool.close
        }
      }
      .flatMap { pool =>
        // Start background tasks
        val backgroundTasks = List(
          houseKeeper.start(pool),
          if config.adaptiveSizing then adaptivePoolSizer.start(pool)
          else Resource.pure[F, Unit](())
        )

        backgroundTasks.sequence_.as(pool)
      }

  def fromConfig[F[_]: Async: Network: Console: Hashing: UUIDGen](
    config:         MySQLConfig,
    logHandler:     Option[LogHandler[F]] = None,
    metricsTracker: Option[PoolMetricsTracker[F]] = None
  ): Resource[F, PooledDataSource[F]] =
    create(config, metricsTracker, UUIDGen[F].randomUUID.map(_.toString))

  def fromConfigWithBeforeAfter[F[_]: Async: Network: Console: Hashing: UUIDGen, A](
    config:         MySQLConfig,
    logHandler:     Option[LogHandler[F]] = None,
    metricsTracker: Option[PoolMetricsTracker[F]] = None,
    before:         Option[Connection[F] => F[A]] = None,
    after:          Option[(A, Connection[F]) => F[Unit]] = None
  ): Resource[F, PooledDataSource[F]] =
    create(config, metricsTracker, UUIDGen[F].randomUUID.map(_.toString), before, after)
