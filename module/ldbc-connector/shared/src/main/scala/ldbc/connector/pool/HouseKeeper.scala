/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*

import cats.syntax.all.*

import cats.effect.*
import cats.effect.syntax.spawn.*

import fs2.Stream

import ldbc.connector.MySQLConfig

/**
 * Background maintenance task that ensures the health and efficiency of the connection pool.
 *
 * The HouseKeeper runs periodically to perform essential maintenance operations that
 * keep the pool in optimal condition. It acts as a guardian of pool health by
 * proactively managing connections and preventing resource leaks or stale connections.
 *
 * Key responsibilities:
 * - **Connection lifecycle management**: Removes connections that exceed their maximum lifetime
 * - **Idle connection cleanup**: Removes connections that have been idle beyond the timeout
 * - **Connection validation**: Periodically validates idle connections to ensure they're still alive
 * - **Minimum pool size maintenance**: Creates new connections when pool falls below minimum threshold
 * - **Metrics updates**: Keeps pool metrics current for monitoring and adaptive sizing
 *
 * The HouseKeeper operates independently of normal pool operations and ensures the pool
 * remains healthy even during periods of low activity.
 *
 * @tparam F the effect type
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

  /**
   * Creates a HouseKeeper instance for asynchronous effect types.
   * 
   * The created housekeeper will run maintenance tasks at intervals specified
   * by `config.maintenanceInterval`. It will check for expired connections,
   * validate idle connections, and ensure the pool maintains its minimum size.
   * 
   * @param config the MySQL configuration containing maintenance parameters
   * @param metricsTracker the metrics tracker for updating pool metrics
   * @tparam F the effect type (must have an Async instance)
   * @return a new HouseKeeper instance
   */
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
    ): F[Unit] =
      pool.poolState.get.flatMap { state =>
        if state.closed then Temporal[F].unit // Pool is closed, skip maintenance
        else
          for
            now <- Clock[F].realTime.map(_.toMillis)
            _   <- reconcileIdleConnections(pool)
            _   <- removeExpiredConnections(pool, now)
            _   <- removeIdleConnections(pool)
            _   <- validateIdleConnections(pool, now)
            _   <- ensureMinimumConnections(pool)
            _   <- updateMetrics(pool)
          yield ()
      }

    /**
     * Reconcile idleConnections set with actual connections.
     * This ensures consistency by removing orphaned IDs from idleConnections
     * that no longer have corresponding connections in the pool.
     */
    private def reconcileIdleConnections(
      pool: PooledDataSource[F]
    ): F[Unit] =
      pool.poolState.modify { state =>
        val validIds           = state.connections.map(_.id).toSet
        val reconciledIdleConns = state.idleConnections.intersect(validIds)

        if reconciledIdleConns.size != state.idleConnections.size then
          // Found orphaned IDs, clean them up
          (state.copy(idleConnections = reconciledIdleConns), true)
        else
          (state, false)
      }.flatMap { wasReconciled =>
        if wasReconciled then
          pool.poolLogger.debug("Reconciled idleConnections: removed orphaned connection IDs")
        else
          Temporal[F].unit
      }

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
              pool.poolLogger.debug(
                s"Removing expired connection ${ pooled.id } (age: ${ (now - pooled.createdAt) / 1000 }s, maxLifetime: ${ config.maxLifetime })"
              ) >>
                pool.removeConnection(pooled)
            case ConnectionState.InUse =>
              // Mark for removal after use
              pool.poolLogger.debug(
                s"Marking in-use connection ${ pooled.id } for removal after use (expired)"
              ) >>
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
      for
        now   <- Clock[F].realTime.map(_.toMillis)
        state <- pool.poolState.get

        // Get idle connections using the idleConnections set
        idleConnections = state.connections.filter(conn => state.idleConnections.contains(conn.id))

        // Check which idle connections have exceeded the timeout
        timedOutConnections <- idleConnections.filterA { conn =>
                                 conn.lastUsedAt.get.map { lastUsed =>
                                   (now - lastUsed) > config.idleTimeout.toMillis
                                 }
                               }

        // Keep at least minConnections
        currentTotal   = state.connections.size
        removableCount = Math.min(
                           timedOutConnections.size,
                           Math.max(0, currentTotal - config.minConnections)
                         )

        _ <- if removableCount > 0 then {
               timedOutConnections.take(removableCount).traverse_(pool.removeConnection)
             } else Temporal[F].unit
      yield ()

    /**
     * Validate idle connections periodically.
     */
    private def validateIdleConnections(
      pool: PooledDataSource[F],
      now:  Long
    ): F[Unit] =
      pool.poolState.get.flatMap { state =>
        // Get idle connections using the idleConnections set
        val idleConnections = state.connections.filter(conn => state.idleConnections.contains(conn.id))

        // Validate connections that haven't been validated recently
        // Validate at most 5 connections per cycle to avoid overloading the database
        idleConnections
          .filterA { conn =>
            conn.lastValidatedAt.get.map { lastValidated =>
              (now - lastValidated) > config.keepaliveTime.getOrElse(2.minutes).toMillis
            }
          }
          .flatMap { needsValidation =>
            needsValidation.take(5).traverse_ { pooled =>
              pool.validateConnection(pooled.connection).flatMap { valid =>
                if valid then pooled.lastValidatedAt.set(now)
                else pool.removeConnection(pooled)
              }
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
        if state.closed then Temporal[F].unit // Pool is closed, don't create new connections
        else
          val currentTotal = state.connections.size
          val toCreate     = Math.max(0, config.minConnections - currentTotal)

          if toCreate > 0 then
            (1 to toCreate).toList.traverse_ { _ =>
              // Use createNewConnectionForPool which creates in Idle state
              // and properly adds to both connectionBag and idleConnections
              pool.createNewConnectionForPool().attempt.void
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
