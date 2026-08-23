/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import scala.concurrent.duration.*

import ldbc.effect.{ Concurrent, Resource }
import ldbc.effect.syntax.*

/**
 * The background maintenance task that keeps the pool healthy: it periodically removes expired and
 * over-idle connections, validates idle connections, tops the pool back up to its minimum size, and
 * refreshes the metrics gauges. The periodic loop is scheduled via `F.sleep` recursion (no fiber)
 * and cancelled when its [[ldbc.effect.Resource]] is released.
 */
trait HouseKeeper[F[_]]:
  /**
   * Starts the housekeeper for `pool`, returning a resource that cancels it on release.
   *
   * @param pool the pool to maintain
   */
  def start(pool: PooledDataSource[F]): Resource[F, Unit]

object HouseKeeper:

  /**
   * Creates a housekeeper that runs every `config.maintenanceInterval`.
   *
   * @param config         the pool configuration
   * @param metricsTracker the tracker whose gauges are refreshed
   */
  def apply[F[_]](config: ConnectionPoolConfig, metricsTracker: PoolMetricsTracker[F])(using F: Concurrent[F]): HouseKeeper[F] =
    new Impl(config, metricsTracker)

  private final class Impl[F[_]](config: ConnectionPoolConfig, metricsTracker: PoolMetricsTracker[F])(using F: Concurrent[F]) extends HouseKeeper[F]:

    override def start(pool: PooledDataSource[F]): Resource[F, Unit] =
      def loop: F[Unit] = F.sleep(config.maintenanceInterval).flatMap(_ => runMaintenance(pool)).flatMap(_ => loop)
      Resource.make(loop.start)(fiber => fiber.cancel).map(_ => ())

    private def nowMillis: F[Long] = F.delay(System.currentTimeMillis())

    private def runMaintenance(pool: PooledDataSource[F]): F[Unit] =
      pool.poolState.get.flatMap { state =>
        if state.closed then F.unit
        else
          for
            now <- nowMillis
            _   <- reconcileIdleConnections(pool)
            _   <- removeExpiredConnections(pool, now)
            _   <- removeIdleConnections(pool)
            _   <- validateIdleConnections(pool, now)
            _   <- ensureMinimumConnections(pool)
            _   <- updateMetrics(pool)
          yield ()
      }

    private def reconcileIdleConnections(pool: PooledDataSource[F]): F[Unit] =
      pool.poolState
        .modify { state =>
          val validIds            = state.connections.map(_.id).toSet
          val reconciledIdleConns = state.idleConnections.intersect(validIds)
          if reconciledIdleConns.size != state.idleConnections.size then
            (state.copy(idleConnections = reconciledIdleConns), true)
          else (state, false)
        }
        .flatMap { wasReconciled =>
          if wasReconciled then pool.poolLogger.debug("Reconciled idleConnections: removed orphaned connection IDs")
          else F.unit
        }

    private def removeExpiredConnections(pool: PooledDataSource[F], now: Long): F[Unit] =
      pool.poolState.get.flatMap { state =>
        val expired = state.connections.filter(pooled => (now - pooled.createdAt) > config.maxLifetime.toMillis)
        expired.traverse_ { pooled =>
          pooled.state.get.flatMap {
            case ConnectionState.Idle =>
              pool.poolLogger.debug(
                s"Removing expired connection ${ pooled.id } (age: ${ (now - pooled.createdAt) / 1000 }s, maxLifetime: ${ config.maxLifetime })"
              ) >> pool.removeConnection(pooled)
            case ConnectionState.InUse =>
              pool.poolLogger.debug(s"Marking in-use connection ${ pooled.id } for removal after use (expired)") >>
                pooled.state.set(ConnectionState.Removed)
            case _ => F.unit
          }
        }
      }

    private def removeIdleConnections(pool: PooledDataSource[F]): F[Unit] =
      for
        now   <- nowMillis
        state <- pool.poolState.get
        idleConnections = state.connections.filter(conn => state.idleConnections.contains(conn.id))
        timedOutConnections <- idleConnections.filterA { conn =>
                                 conn.lastUsedAt.get.map(lastUsed => (now - lastUsed) > config.idleTimeout.toMillis)
                               }
        currentTotal   = state.connections.size
        removableCount = Math.min(timedOutConnections.size, Math.max(0, currentTotal - config.minConnections))
        _ <-
          if removableCount > 0 then timedOutConnections.take(removableCount).traverse_(pool.removeConnection)
          else F.unit
      yield ()

    private def validateIdleConnections(pool: PooledDataSource[F], now: Long): F[Unit] =
      pool.poolState.get.flatMap { state =>
        val idleConnections = state.connections.filter(conn => state.idleConnections.contains(conn.id))
        idleConnections
          .filterA { conn =>
            conn.lastValidatedAt.get.map { lastValidated =>
              (now - lastValidated) > config.keepaliveTime.getOrElse(2.minutes).toMillis
            }
          }
          .flatMap { needsValidation =>
            needsValidation.take(5).traverse_ { pooled =>
              pool.validateConnection(pooled.connection).flatMap { valid =>
                if valid then pooled.lastValidatedAt.set(now) else pool.removeConnection(pooled)
              }
            }
          }
      }

    private def ensureMinimumConnections(pool: PooledDataSource[F]): F[Unit] =
      pool.poolState.get.flatMap { state =>
        if state.closed then F.unit
        else
          val toCreate = Math.max(0, config.minConnections - state.connections.size)
          if toCreate > 0 then (1 to toCreate).toList.traverse_(_ => pool.createNewConnectionForPool().attempt.void)
          else F.unit
      }

    private def updateMetrics(pool: PooledDataSource[F]): F[Unit] =
      for
        status <- pool.status
        _      <- metricsTracker.updateGauge("pool.total", status.total.toLong)
        _      <- metricsTracker.updateGauge("pool.active", status.active.toLong)
        _      <- metricsTracker.updateGauge("pool.idle", status.idle.toLong)
        _      <- metricsTracker.updateGauge("pool.waiting", status.waiting.toLong)
      yield ()
