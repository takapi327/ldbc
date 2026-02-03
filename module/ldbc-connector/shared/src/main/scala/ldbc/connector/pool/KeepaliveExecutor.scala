/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*
import scala.util.Random

import cats.syntax.all.*

import cats.effect.*
import cats.effect.syntax.spawn.*

import fs2.Stream

/**
 * Background task that performs keepalive validation on idle connections.
 * 
 * This executor periodically validates idle connections to prevent them from
 * being closed by firewalls, load balancers, or database idle timeouts.
 * The validation interval includes random variance to avoid synchronized
 * validation across all connections.
 * 
 * @tparam F the effect type
 */
trait KeepaliveExecutor[F[_]]:

  /**
   * Start the keepalive executor background task.
   * 
   * @param pool the connection pool
   * @return a resource that manages the background task
   */
  def start(pool: PooledDataSource[F]): Resource[F, Unit]

object KeepaliveExecutor:

  /**
   * Creates a KeepaliveExecutor for the given configuration.
   * 
   * @param keepaliveTime the base interval for keepalive checks
   * @param metricsTracker the metrics tracker
   * @tparam F the effect type with Async capability
   * @return a new KeepaliveExecutor instance
   */
  def fromAsync[F[_]: Async](
    keepaliveTime:  FiniteDuration,
    metricsTracker: PoolMetricsTracker[F]
  ): KeepaliveExecutor[F] = Impl[F](keepaliveTime, metricsTracker)

  private case class Impl[F[_]: Async](
    keepaliveTime:  FiniteDuration,
    metricsTracker: PoolMetricsTracker[F]
  ) extends KeepaliveExecutor[F]:

    override def start(pool: PooledDataSource[F]): Resource[F, Unit] =
      // Calculate interval with up to 20% variance
      val variance        = (keepaliveTime.toMillis * 0.2).toLong
      val randomizedDelay = keepaliveTime.toMillis + Random.nextLong(variance) - (variance / 2)
      val effectiveDelay  = FiniteDuration(randomizedDelay.max(keepaliveTime.toMillis / 2), MILLISECONDS)

      val task = Stream
        .fixedDelay[F](effectiveDelay)
        .evalMap { _ => performKeepalive(pool) }
        .compile
        .drain

      Resource
        .make(task.start)(_.cancel)
        .void

    private def performKeepalive(pool: PooledDataSource[F]): F[Unit] =
      pool.poolState.get.flatMap { state =>
        if state.closed then Temporal[F].unit
        else
          // Get idle connections that need keepalive
          val idleConnections = state.connections.filter { pooled =>
            state.idleConnections.contains(pooled.id)
          }

          // Validate each idle connection
          idleConnections.traverse_ { pooled =>
            validateIdleConnection(pool, pooled)
          }
      }

    private def validateIdleConnection(
      pool:   PooledDataSource[F],
      pooled: PooledConnection[F]
    ): F[Unit] =
      // Try to temporarily acquire the connection for validation
      pooled.state.get.flatMap {
        case ConnectionState.Idle =>
          // Temporarily mark as reserved for validation
          compareAndSetConnectionState(pooled.state, ConnectionState.Idle, ConnectionState.Reserved).flatMap {
            case true =>
              // Successfully reserved for validation
              // Remove from idleConnections while validating for consistency
              pool.poolState.update(s => s.copy(idleConnections = s.idleConnections - pooled.id)) >>
                (for
                  now   <- Clock[F].realTime.map(_.toMillis)
                  valid <- pool.validateConnection(pooled.connection)
                  _     <- if valid then {
                         // Validation successful, return to idle state and add back to idleConnections
                         pooled.lastValidatedAt.set(now) >>
                           pooled.state.set(ConnectionState.Idle) >>
                           pool.poolState.update(s => s.copy(idleConnections = s.idleConnections + pooled.id))
                       } else
                         // Connection is invalid, remove it
                         pool.removeConnection(pooled)
                yield ())
            case false =>
              // Connection state changed, skip validation
              Temporal[F].unit
          }
        case _ =>
          // Connection is not idle, skip
          Temporal[F].unit
      }

    private def compareAndSetConnectionState(
      ref:    Ref[F, ConnectionState],
      expect: ConnectionState,
      update: ConnectionState
    ): F[Boolean] =
      ref.modify { current =>
        if current == expect then (update, true)
        else (current, false)
      }
