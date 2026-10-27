/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import java.util.concurrent.TimeoutException

import scala.concurrent.duration.*

import ldbc.sql.{ Connection, DataSource, SQLException, SQLTransientConnectionException }

import ldbc.effect.{ Concurrent, Fiber, Ref, Resource }
import ldbc.effect.syntax.*
import ldbc.telemetry.{ DatabaseMetrics, Meter, PoolMetricsState }

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
  def circuitBreaker:                                CircuitBreaker[F]
  def returnToPool(pooled:     PooledConnection[F]): F[Unit]
  def removeConnection(pooled: PooledConnection[F]): F[Unit]
  def validateConnection(conn: Connection[F]):       F[Boolean]

  /**
   * Starts establishing one more physical connection in the background, if the pool has room for it.
   *
   * Returns as soon as the attempt has been scheduled; it never waits for, nor reports, the outcome.
   * The connection becomes available through the pool like any other. Does nothing when the pool is
   * already at `maxConnections` counting connections that are still being established.
   */
  def requestNewConnection(): F[Unit]

  /**
   * How many creations have so far outlived their time budget.
   *
   * Such an attempt keeps running (cancelling it would leak the socket the driver has already
   * opened) but stops occupying capacity. A number that keeps climbing means connection
   * establishment regularly takes longer than the pool's acquisition timeout.
   */
  def abandonedCreations: F[Long]

object PooledDataSource:

  /** SQLSTATE class 08, connection exception, reported when the pool cannot hand out a connection. */
  private val ConnectionExceptionSqlState: String = "08001"

  /** How long to wait before the first retry of a failed connection attempt. */
  private val RetryInitialBackoff: FiniteDuration = 10.millis

  /** The ceiling the retry backoff grows to. */
  private val RetryMaxBackoff: FiniteDuration = 5.seconds

  private def nextBackoff(backoff: FiniteDuration): FiniteDuration =
    val doubled = backoff.toMillis * 2
    if doubled >= RetryMaxBackoff.toMillis then RetryMaxBackoff else doubled.millis

  private final case class Impl[F[_]](
    config:                 ConnectionPoolConfig,
    create:                 Resource[F, Connection[F]],
    connectionTestQuery:    Option[String],
    metricsTracker:         PoolMetricsTracker[F],
    databaseMetrics:        DatabaseMetrics[F],
    poolState:              Ref[F, PoolState[F]],
    idGenerator:            F[String],
    connectionBag:          ConcurrentBag[F, PooledConnection[F]],
    circuitBreaker:         CircuitBreaker[F],
    poolLogger:             PoolLogger[F],
    hooks:                  Option[Connection[F] => Resource[F, Unit]],
    lastCreationFailure:    Ref[F, Option[Throwable]],
    abandonedCreationCount: Ref[F, Long]
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
      waiting <- connectionBag.waiting
    yield PoolStatus(
      total    = connections.size,
      active   = active,
      idle     = idle,
      waiting  = waiting,
      creating = state.creating
    )

    override def abandonedCreations: F[Long] = abandonedCreationCount.get

    override def metrics: F[PoolMetrics] = metricsTracker.getMetrics

    override def close: F[Unit] =
      poolState
        .modify { state =>
          if state.closed then (state, F.unit)
          else
            val newState = state.copy(closed = true)
            def bounded(action: F[Unit], id: String): F[Unit] =
              F.timeout(action, config.connectionTimeout)(
                new TimeoutException(s"Closing connection $id timed out after ${ config.connectionTimeout }")
              )
            val closeAll = state.connections.traverse_ { pooled =>
              bounded(pooled.finalizer, pooled.id).attempt.flatMap {
                case Left(error) =>
                  poolLogger.debug(s"Error closing connection ${ pooled.id }: ${ error.getMessage }") >>
                    bounded(pooled.connection.close(), pooled.id).attempt.void
                case Right(_) =>
                  F.unit
              }
            }
            val effect =
              poolLogger.info(s"Closing connection pool (${ config.poolName })") >>
                connectionBag.close *> closeAll >> poolLogger.info("Connection pool closed successfully")
            (newState, effect)
        }
        .flatMap(identity)

    private def acquire: F[Connection[F]] = for
      startTime <- F.monotonic
      result    <- acquireConnectionWithStartTime(startTime, 0)
    yield result

    /**
     * Hands out a connection, waiting no longer than `connectionTimeout` for one to become available.
     *
     * The caller never establishes a connection itself. When the bag has nothing to give, it asks for
     * one to be created in the background and then waits on the bag like any other borrower; the new
     * connection reaches it through the bag's handoff. This keeps the wait bounded by the configured
     * timeout regardless of how long establishing a connection takes, and it stops a burst of callers
     * from each starting their own connection: only as many creations are started as the pool has
     * room for.
     *
     * `attempt` counts how many borrowed connections failed validation. Every round re-reads the
     * clock and gives up once the budget is spent, so a pool whose connections have all died cannot
     * stretch the caller's wait: each round costs up to `validationTimeout`, and without the check
     * those costs would accumulate outside the budget rather than inside it. The count is a second
     * bound, for the case where the connections fail fast enough that the clock barely moves.
     */
    private def acquireConnectionWithStartTime(startTime: FiniteDuration, attempt: Int): F[Connection[F]] =
      poolState.get.flatMap { state =>
        if state.closed then F.raiseError(new SQLException("Pool is closed"))
        else if attempt > config.maxConnections then acquisitionTimeout
        else
          remainingBudget(startTime).flatMap { budget =>
            if budget <= Duration.Zero then acquisitionTimeout
            else
              connectionBag.tryBorrow.flatMap {
                case Some(pooled) => checkout(pooled, startTime, attempt)
                case None         =>
                  requestNewConnection() *>
                    connectionBag.borrow(budget).flatMap {
                      case Some(pooled) => checkout(pooled, startTime, attempt)
                      case None         => acquisitionTimeout
                    }
              }
          }
      }

    /** What is left of the acquisition budget; never negative. */
    private def remainingBudget(startTime: FiniteDuration): F[FiniteDuration] =
      F.monotonic.map { now =>
        val left = config.connectionTimeout - (now - startTime)
        if left > Duration.Zero then left else Duration.Zero
      }

    /** Validates a borrowed connection and marks it in use, or discards it and retries. */
    private def checkout(pooled: PooledConnection[F], startTime: FiniteDuration, attempt: Int): F[Connection[F]] =
      for
        shouldValidate <- needsValidation(pooled)
        valid          <-
          if shouldValidate then
            validateConnection(pooled.connection).flatTap {
              case true  => F.unit
              case false => poolLogger.warn(s"Connection ${ pooled.id } failed validation, removing from pool")
            }
          else pooled.connection.isClosed().map(!_)
        result <-
          if !valid then removeConnection(pooled) >> acquireConnectionWithStartTime(startTime, attempt + 1)
          else
            for
              _       <- pooled.state.set(ConnectionState.InUse)
              _       <- poolState.update(s => s.copy(idleConnections = s.idleConnections - pooled.id))
              now     <- F.realTime.map(_.toMillis)
              _       <- pooled.lastUsedAt.set(now)
              _       <- pooled.useCount.update(_ + 1)
              endTime <- F.monotonic
              _       <- metricsTracker.recordAcquisition(endTime - startTime)
              _       <- databaseMetrics.recordConnectionWaitTime(endTime - startTime, config.poolName)
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

    /**
     * Reports that no connection could be obtained in time.
     *
     * The most recent creation failure is chained as the cause. Since connections are established off
     * the caller's fiber, that failure would otherwise never reach the caller, leaving it with a bare
     * timeout and no indication of why the pool could not grow.
     */
    private def acquisitionTimeout: F[Connection[F]] =
      for
        state   <- poolState.get
        states  <- state.connections.traverse(_.state.get)
        waiting <- connectionBag.waiting
        cause   <- lastCreationFailure.get
        message =
          s"Connection acquisition timeout after ${ config.connectionTimeout } " +
            s"(pool: ${ config.poolName }, " +
            s"size: ${ state.connections.size }/${ config.maxConnections }, " +
            s"active: ${ states.count(_ == ConnectionState.InUse) }, " +
            s"idle: ${ states.count(_ == ConnectionState.Idle) }, " +
            s"creating: ${ state.creating }, waiting: $waiting)"
        _      <- metricsTracker.recordTimeout()
        _      <- databaseMetrics.recordConnectionTimeout(config.poolName)
        _      <- poolLogger.error(message)
        result <- F.raiseError[Connection[F]](
                    new SQLTransientConnectionException(
                      message  = message,
                      sqlState = Some(PooledDataSource.ConnectionExceptionSqlState),
                      cause    = cause
                    )
                  )
      yield result

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
        F.monotonic.flatMap { endTime =>
          metricsTracker.recordUsage(endTime - startTime) *>
            databaseMetrics.recordConnectionUseTime(endTime - startTime, config.poolName)
        }

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
                    else pooled.connection.isClosed().map(!_)
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

    override def requestNewConnection(): F[Unit] =
      reserveSlot.flatMap {
        case false => F.unit
        case true  => runCreation.start.void
      }

    /**
     * Takes one unit of capacity, if there is any left.
     *
     * Capacity is judged on registered connections plus reservations, so that concurrent callers
     * cannot each conclude there is room for one more. A caller that gets `true` owns the reservation
     * and must eventually dispose of it, either by registering a connection or by releasing it.
     */
    private def reserveSlot: F[Boolean] =
      poolState.modify { state =>
        if state.closed || state.connections.size + state.creating >= config.maxConnections then (state, false)
        else (state.copy(creating = state.creating + 1), true)
      }

    private def releaseSlot: F[Unit] =
      poolState.update(state => state.copy(creating = Math.max(0, state.creating - 1)))

    /**
     * Claims the right to dispose of one creation's reservation.
     *
     * Three paths race for it (the connection arrived, the attempt failed, the budget ran out) and
     * exactly one receives `true`. `Ref.modify` is a compare-and-set loop, so no lock is involved.
     */
    private def claimSlot(guard: Ref[F, Boolean]): F[Boolean] =
      guard.modify(claimed => (true, claimed)).map(!_)

    /**
     * Carries out one creation while holding a reservation taken by the caller.
     *
     * Never raises; the outcome is returned so that callers who must surface a failure (the initial
     * fill) can, while the background path can log and move on.
     */
    private def runCreation: F[Either[Throwable, Unit]] =
      F.monotonic.flatMap { startedAt =>
        Ref.of[F, Boolean](false).flatMap { guard =>
          watchdog(guard).start.flatMap { timer =>
            attemptCreation(startedAt + config.connectionTimeout).attempt.flatMap { outcome =>
              timer.cancel.flatMap { _ =>
                outcome match
                  case Right(pooled) => settle(pooled, guard).map(_ => Right(()))
                  case Left(error)   =>
                    claimSlot(guard).flatMap(owned => if owned then releaseSlot else F.unit) *>
                      poolLogger
                        .warn(s"Failed to create a pooled connection: ${ error.getMessage }")
                        .map(_ => Left(error))
              }
            }
          }
        }
      }

    /**
     * Frees the reservation once the creation has taken longer than the pool is prepared to wait.
     *
     * The attempt itself is left running. Cancelling it would abandon a socket the driver may have
     * already opened, and `Resource` does not release what it acquired when its acquisition is
     * cancelled. Whatever the attempt eventually produces is handled by [[settle]].
     *
     * The claim and the release must happen as one step. `runCreation` cancels this timer as soon as
     * the creation finishes, and an interruption landing between the two would leave the reservation
     * claimed by a path that never releases it -- lost capacity that never comes back.
     */
    private def watchdog(guard: Ref[F, Boolean]): F[Unit] =
      F.sleep(config.connectionTimeout) *>
        F.uncancelable(
          claimSlot(guard).flatMap {
            case false => F.unit
            case true  =>
              releaseSlot *>
                abandonedCreationCount.update(_ + 1) *>
                lastCreationFailure.set(Some(creationTimedOut)) *>
                poolLogger.warn(
                  s"Connection creation exceeded ${ config.connectionTimeout }; " +
                    s"the reservation was released and the attempt left running (pool: ${ config.poolName })"
                )
          }
        )

    private def creationTimedOut: Throwable =
      new TimeoutException(
        s"Connection creation did not complete within ${ config.connectionTimeout } (pool: ${ config.poolName })"
      )

    /**
     * Publishes a connection that has arrived, whether or not its reservation is still held.
     *
     * A connection that outlived its budget has already given its reservation back, so it takes a
     * fresh one. Re-using it is worth more than discarding it; if the pool has since filled up, it is
     * closed instead.
     */
    private def settle(pooled: PooledConnection[F], guard: Ref[F, Boolean]): F[Unit] =
      claimSlot(guard).flatMap {
        case true  => registerHoldingSlot(pooled)
        case false =>
          reserveSlot.flatMap {
            case true  => registerHoldingSlot(pooled)
            case false => disposeConnection(pooled)
          }
      }

    /**
     * Consumes the reservation held by the caller and publishes the connection in the same step.
     *
     * Splitting the two would briefly leave the capacity unaccounted for and let another creation
     * start in that gap. The connection is only offered to borrowers once the pool has accepted it,
     * so nothing can hand out a connection the pool then rejects.
     */
    private def registerHoldingSlot(pooled: PooledConnection[F]): F[Unit] =
      poolState
        .modify { state =>
          if state.closed then (state.copy(creating = Math.max(0, state.creating - 1)), false)
          else
            val newState = state.copy(
              connections     = state.connections :+ pooled,
              idleConnections = state.idleConnections + pooled.id,
              creating        = Math.max(0, state.creating - 1)
            )
            (newState, true)
        }
        .flatMap {
          case true  => connectionBag.add(pooled)
          case false =>
            poolLogger.debug(s"Pool closed while connection ${ pooled.id } was being created; closing it") *>
              disposeConnection(pooled)
        }

    /**
     * Closes a connection the pool will not take.
     *
     * Only reachable for connections that were never published, so there is nothing to withdraw from
     * the bag. Shutdown only closes what the pool has registered, which is why an in-flight creation
     * has to clean up after itself.
     */
    private def disposeConnection(pooled: PooledConnection[F]): F[Unit] =
      pooled.finalizer.attempt.void

    /**
     * Establishes a connection, retrying transient failures until the budget or the circuit breaker
     * says to stop.
     *
     * Without this a single failed attempt would leave the waiting caller with nothing to do but run
     * out its whole timeout, even though the pool knows at once that nothing is on its way. The
     * deadline is the same instant the watchdog fires: the watchdog only frees capacity, it does not
     * stop the loop, so the loop has to watch the clock itself.
     */
    private def attemptCreation(deadline: FiniteDuration): F[PooledConnection[F]] =
      retryCreation(deadline, PooledDataSource.RetryInitialBackoff)

    private def retryCreation(deadline: FiniteDuration, backoff: FiniteDuration): F[PooledConnection[F]] =
      createOnce.handleErrorWith { error =>
        F.monotonic.flatMap { now =>
          circuitBreaker.state.flatMap {
            case CircuitBreaker.State.Closed if now + backoff < deadline =>
              F.sleep(backoff) *> retryCreation(deadline, PooledDataSource.nextBackoff(backoff))
            case _ => F.raiseError(error)
          }
        }
      }

    /**
     * One attempt, guarded by the circuit breaker.
     *
     * The failure is recorded inside the breaker so that only real connection failures are kept. The
     * breaker rejects with a generic message of its own once open, and letting that overwrite the
     * recorded cause would hide the authentication or TLS error that opened it in the first place.
     */
    private def createOnce: F[PooledConnection[F]] =
      circuitBreaker.protect {
        buildConnection.attempt.flatMap {
          case Right(pooled) => lastCreationFailure.set(None).map(_ => pooled)
          case Left(error)   => lastCreationFailure.set(Some(error)) *> F.raiseError(error)
        }
      }

    /** Opens one physical connection and wraps it, without touching any pool state. */
    private def buildConnection: F[PooledConnection[F]] =
      F.monotonic.flatMap { startTime =>
        /**
         * Measures the attempt. Never fails the attempt: a metrics backend that is down would
         * otherwise turn a healthy connection into a creation failure, one that is then retried, and
         * the connection already built would be dropped without being closed.
         */
        val recordCreationMetric: F[Unit] =
          F.monotonic.flatMap { endTime =>
            metricsTracker.recordCreation(endTime - startTime) *>
              databaseMetrics.recordConnectionCreateTime(endTime - startTime, config.poolName)
          }

        val built = for
          id        <- idGenerator
          allocated <- create.allocatedCase
          (conn, finalizer) = allocated
          now              <- F.realTime.map(_.toMillis)
          stateRef         <- Ref.of[F, ConnectionState](ConnectionState.Idle)
          lastUsedRef      <- Ref.of[F, Long](now)
          useCountRef      <- Ref.of[F, Long](0L)
          lastValidatedRef <- Ref.of[F, Long](now)
          leakDetectionRef <- Ref.of[F, Option[Fiber[F, Unit]]](None)
          bagStateRef      <- Ref.of[F, Int](BagEntry.STATE_NOT_IN_USE)
        yield PooledConnection[F](
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

        built.attempt.flatMap { result =>
          recordCreationMetric.attempt.void.flatMap(_ => result.fold(F.raiseError, F.pure))
        }
      }

    /**
     * Establishes one connection on the current fiber, failing if it cannot be established.
     *
     * Used to fill the pool before it is handed to its user, where a connection problem should stop
     * startup rather than surface later as an acquisition timeout.
     */
    private[pool] def createBlocking: F[Unit] =
      reserveSlot.flatMap {
        case false => F.unit
        case true  => runCreation.flatMap(_.fold(F.raiseError[Unit], _ => F.unit))
      }

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

    /**
     * Whether `pooled` is due for a validation round trip, i.e. whether it has been idle for longer
     * than `aliveBypassWindow`.
     *
     * Only the round trip is skipped inside that window, never the health check itself: callers
     * still consult `isClosed`, which a driver can answer from local state when its transport has
     * failed. Handing such a connection out would fail the caller's very first statement.
     */
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

    /**
     * Detaches a connection from the pool and closes it in the background.
     *
     * The close is not awaited. This runs on the acquisition path, where a connection that failed
     * validation is of no further use to the caller, and a peer that stopped responding can make the
     * close take arbitrarily long. It is not bounded with a timeout either: `Concurrent.timeout`
     * cancels its target, and `Resource` does not guarantee that a release interrupted midway frees
     * the underlying socket. Detaching it keeps the close running to completion off the hot path.
     *
     * Everything that decides whether the connection still counts towards the pool happens
     * synchronously, so on return the connection is no longer part of any pool state.
     */
    override def removeConnection(pooled: PooledConnection[F]): F[Unit] = for
      currentState <- pooled.state.get
      _            <- poolLogger.debug(s"Removing connection ${ pooled.id } from pool (state: $currentState)")
      _            <- pooled.state.set(ConnectionState.Removed)
      _            <- connectionBag.remove(pooled)
      _            <- pooled.leakDetection.get.flatMap(_.traverse_(fiber => fiber.cancel))
      _            <- poolState.update { state =>
             state.copy(
               connections     = state.connections.filterNot(_ == pooled),
               idleConnections = state.idleConnections - pooled.id
             )
           }
      _ <- pooled.finalizer.attempt.void.start.void
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

  /**
   * Builds a pool over a raw connection [[ldbc.effect.Resource]].
   *
   * The pool cannot see inside the resource, so it cannot propagate its acquisition timeout as a
   * connect timeout the way [[fromDataSource]] does. Bound connection establishment yourself, or use
   * [[fromDataSource]] with a data source that implements
   * [[ldbc.sql.DataSource.withConnectTimeout]].
   */
  def fromConfig[F[_]](
    config:              ConnectionPoolConfig,
    create:              Resource[F, Connection[F]],
    metricsTracker:      Option[PoolMetricsTracker[F]] = None,
    connectionTestQuery: Option[String] = None,
    poolLogger:          Option[PoolLogger[F]] = None,
    idGenerator:         Option[F[String]] = None,
    meter:               Option[Meter[F]] = None
  )(using F: Concurrent[F]): Resource[F, PooledDataSource[F]] =
    val idGen = idGenerator.getOrElse(randomConnectionId[F])
    build(config, create, metricsTracker, connectionTestQuery, poolLogger, idGen, None, meter)

  /**
   * Builds a pool with acquisition hooks over a raw connection [[ldbc.effect.Resource]].
   *
   * Carries the same caveat as [[fromConfig]]: the acquisition timeout is not propagated as a
   * connect timeout.
   */
  def fromConfigWithBeforeAfter[F[_], A](
    config:              ConnectionPoolConfig,
    create:              Resource[F, Connection[F]],
    before:              Connection[F] => F[A],
    after:               (A, Connection[F]) => F[Unit],
    metricsTracker:      Option[PoolMetricsTracker[F]] = None,
    connectionTestQuery: Option[String] = None,
    poolLogger:          Option[PoolLogger[F]] = None,
    idGenerator:         Option[F[String]] = None,
    meter:               Option[Meter[F]] = None
  )(using F: Concurrent[F]): Resource[F, PooledDataSource[F]] =
    val idGen = idGenerator.getOrElse(randomConnectionId[F])
    val hook: Connection[F] => Resource[F, Unit] =
      conn => Resource.make(before(conn))(a => after(a, conn)).map(_ => ())
    build(config, create, metricsTracker, connectionTestQuery, poolLogger, idGen, Some(hook), meter)

  /**
   * Builds a pool over a [[ldbc.sql.DataSource]], handing it the pool's acquisition timeout as the
   * budget for establishing one physical connection.
   */
  def fromDataSource[F[_]](
    config:              ConnectionPoolConfig,
    dataSource:          DataSource[F],
    metricsTracker:      Option[PoolMetricsTracker[F]] = None,
    connectionTestQuery: Option[String] = None,
    poolLogger:          Option[PoolLogger[F]] = None,
    idGenerator:         Option[F[String]] = None,
    meter:               Option[Meter[F]] = None
  )(using F: Concurrent[F]): Resource[F, PooledDataSource[F]] =
    fromConfig(
      config,
      connectionResource(dataSource.withConnectTimeout(config.connectionTimeout)),
      metricsTracker,
      connectionTestQuery,
      poolLogger,
      idGenerator,
      meter
    )

  /**
   * Builds a pool with acquisition hooks over a [[ldbc.sql.DataSource]], handing it the pool's
   * acquisition timeout as the budget for establishing one physical connection.
   */
  def fromDataSourceWithBeforeAfter[F[_], A](
    config:              ConnectionPoolConfig,
    dataSource:          DataSource[F],
    before:              Connection[F] => F[A],
    after:               (A, Connection[F]) => F[Unit],
    metricsTracker:      Option[PoolMetricsTracker[F]] = None,
    connectionTestQuery: Option[String] = None,
    poolLogger:          Option[PoolLogger[F]] = None,
    idGenerator:         Option[F[String]] = None,
    meter:               Option[Meter[F]] = None
  )(using F: Concurrent[F]): Resource[F, PooledDataSource[F]] =
    fromConfigWithBeforeAfter(
      config,
      connectionResource(dataSource.withConnectTimeout(config.connectionTimeout)),
      before,
      after,
      metricsTracker,
      connectionTestQuery,
      poolLogger,
      idGenerator,
      meter
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
    hooks:               Option[Connection[F] => Resource[F, Unit]],
    meter:               Option[Meter[F]]
  )(using F: Concurrent[F]): Resource[F, PooledDataSource[F]] =
    Resource.eval(PoolConfigValidator.validate[F](config)).flatMap { _ =>
      val tracker = metricsTracker.getOrElse(PoolMetricsTracker.noop[F])
      val logger  = poolLogger.getOrElse(PoolLogger.console[F](config.debug))

      def createPool(dbMetrics: DatabaseMetrics[F]): F[PooledDataSource[F]] = for
        poolState      <- Ref.of[F, PoolState[F]](PoolState.empty[F])
        connectionBag  <- ConcurrentBag[F, PooledConnection[F]]()
        circuitBreaker <- CircuitBreaker[F](CircuitBreaker.Config(maxFailures = 5, resetTimeout = 30.seconds))
        lastFailure    <- Ref.of[F, Option[Throwable]](None)
        abandoned      <- Ref.of[F, Long](0L)
      yield Impl[F](
        config                 = config,
        create                 = create,
        connectionTestQuery    = connectionTestQuery,
        metricsTracker         = tracker,
        databaseMetrics        = dbMetrics,
        poolState              = poolState,
        idGenerator            = idGenerator,
        connectionBag          = connectionBag,
        circuitBreaker         = circuitBreaker,
        poolLogger             = logger,
        hooks                  = hooks,
        lastCreationFailure    = lastFailure,
        abandonedCreationCount = abandoned
      )

      def createMinimumConnections(pool: PooledDataSource[F]): Resource[F, Unit] =
        Resource.make(
          (1 to config.minConnections).toList.traverse_ { _ =>
            pool match
              case impl: Impl[F] @unchecked => impl.createBlocking
              case other                    => other.requestNewConnection()
          }
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

      /**
       * Registers the observable pool gauges (`db.client.connection.count` and friends) for the lifetime of
       * the pool. The callback reads the live pool state on every export, so the gauges report absolute
       * values rather than deltas; releasing the resource unregisters them.
       */
      def registerObservableMetrics(pool: PooledDataSource[F], dbMetrics: DatabaseMetrics[F]): Resource[F, Unit] =
        dbMetrics.registerPoolStateCallback(
          config.poolName,
          config.minConnections,
          config.maxConnections,
          pool.status.map(status => PoolMetricsState(status.idle.toLong, status.active.toLong, status.waiting.toLong))
        )

      for
        dbMetrics <- Resource.eval(DatabaseMetrics.fromMeter[F](meter.getOrElse(Meter.noop[F])))
        pool      <- Resource.eval(createPool(dbMetrics))
        _         <- registerObservableMetrics(pool, dbMetrics)
        _         <- createMinimumConnections(pool)
        _         <- createBackgroundResources(pool)
      yield pool
    }
