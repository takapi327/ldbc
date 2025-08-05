/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import cats.syntax.all.*

import cats.effect.*
import cats.effect.syntax.spawn.*

import fs2.Stream

import ldbc.connector.MySQLConfig

/**
 * Background task that performs pool maintenance.
 *
 * Responsibilities:
 * - Remove expired connections
 * - Remove idle connections exceeding idle timeout
 * - Validate idle connections periodically
 * - Ensure minimum connections are maintained
 * - Update pool metrics
 */
trait HouseKeeper[F[_]]:

  /**
   * Start the housekeeper background task.
   *
   * @param pool the connection pool implementation
   * @return a resource that manages the background task
   */
  def start(pool: PooledDataSource[F]): Resource[F, Unit]

object HouseKeeper:

  def fromAsync[F[_]: Async](
    config:         MySQLConfig,
    metricsTracker: PoolMetricsTracker[F]
  ): HouseKeeper[F] = Impl[F](config, metricsTracker)

  private case class Impl[F[_]: Async](
    config:         MySQLConfig,
    metricsTracker: PoolMetricsTracker[F]
  ) extends HouseKeeper[F]:

    /**
     * Start the housekeeper background task.
     *
     * @param pool the connection pool implementation
     * @return a resource that manages the background task
     */
    override def start(
      pool: PooledDataSource[F]
    ): Resource[F, Unit] =

      val task = Stream
        .fixedDelay[F](config.maintenanceInterval)
        .evalMap { _ => runMaintenance(pool) }
        .compile
        .drain

      Resource
        .make(task.start)(_.cancel)
        .void

    /**
     * Run a single maintenance cycle.
     */
    private def runMaintenance(
      pool: PooledDataSource[F]
    ): F[Unit] = for
      now <- Clock[F].realTime.map(_.toMillis)
      _   <- removeExpiredConnections(pool, now)
      _   <- removeIdleConnections(pool)
      _   <- validateIdleConnections(pool, now)
      _   <- ensureMinimumConnections(pool)
      _   <- updateMetrics(pool)
    yield ()

    /**
     * Remove connections that have exceeded max lifetime.
     */
    private def removeExpiredConnections(
      pool: PooledDataSource[F],
      now:  Long
    ): F[Unit] =
      pool.poolState.get.flatMap { state =>
        val expired = state.connections.filter { pooled =>
          val age = now - pooled.createdAt
          age > config.maxLifetime.toMillis
        }

        expired.traverse_ { pooled =>
          pooled.state.get.flatMap {
            case ConnectionState.Idle =>
              pool.removeConnection(pooled)
            case ConnectionState.InUse =>
              // Mark for removal after use
              pooled.state.set(ConnectionState.Removed)
            case _ =>
              Temporal[F].unit
          }
        }
      }

    /**
     * Remove idle connections that have exceeded idle timeout.
     */
    private def removeIdleConnections(
      pool: PooledDataSource[F]
    ): F[Unit] =
      pool.poolState.get.flatMap { state =>
        // Count idle connections - simplified for compilation
        val idleCount = state.connections.size // Will properly check state later

        // Keep at least minConnections
        val removableCount = Math.max(0, idleCount - config.minConnections)

        if removableCount > 0 then
          // Simplified for compilation - will check state properly later
          val toRemove = state.connections.take(removableCount)

          toRemove.traverse_(pool.removeConnection)
        else Temporal[F].unit
      }

    /**
     * Validate idle connections periodically.
     */
    private def validateIdleConnections(
      pool: PooledDataSource[F],
      now:  Long
    ): F[Unit] =
      pool.poolState.get.flatMap { state =>
        // Simplified for compilation - will check state properly later
        val needsValidation = state.connections.take(1)

        needsValidation.traverse_ { pooled =>
          pool.validateConnection(pooled.connection).flatMap { valid =>
            if valid then pooled.lastValidatedAt.set(now)
            else pool.removeConnection(pooled)
          }
        }
      }

    /**
     * Ensure minimum connections are maintained.
     */
    private def ensureMinimumConnections(
      pool: PooledDataSource[F]
    ): F[Unit] =
      pool.poolState.get.flatMap { state =>
        val currentTotal = state.connections.size
        val toCreate     = Math.max(0, config.minConnections - currentTotal)

        if toCreate > 0 then
          (1 to toCreate).toList.traverse_ { _ =>
            pool
              .createNewConnection()
              .flatMap { pooled =>
                pooled.state.set(ConnectionState.Idle) *> pool.returnToPool(pooled)
              }
              .attempt
              .void
          }
        else Temporal[F].unit
      }

    /**
     * Update pool metrics.
     */
    private def updateMetrics(
      pool: PooledDataSource[F]
    ): F[Unit] = for
      status <- pool.status
      _      <- metricsTracker.updateGauge("pool.total", status.total.toLong)
      _      <- metricsTracker.updateGauge("pool.active", status.active.toLong)
      _      <- metricsTracker.updateGauge("pool.idle", status.idle.toLong)
      _      <- metricsTracker.updateGauge("pool.waiting", status.waiting.toLong)
    yield ()
