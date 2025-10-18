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
import cats.effect.syntax.spawn.*

import fs2.Stream

/**
 * Background task that periodically reports connection pool status.
 * 
 * The PoolStatusReporter runs at configured intervals to log the current state
 * of the connection pool. This provides visibility into pool health and usage
 * patterns, similar to HikariCP's pool state logging.
 * 
 * Key features:
 * - Periodic logging of pool statistics
 * - Includes both basic status (total/active/idle/waiting) and detailed metrics
 * - Runs as a background fiber that can be cancelled
 * - Only logs when debug logging is enabled to avoid noise in production
 * 
 * @tparam F the effect type
 */
trait PoolStatusReporter[F[_]]:

  /**
   * Start the reporter background task.
   * 
   * @param pool the connection pool to monitor
   * @param poolName the name of the pool for logging
   * @return a resource that manages the background task lifecycle
   */
  def start(pool: PooledDataSource[F], poolName: String): Resource[F, Unit]

object PoolStatusReporter:

  /**
   * Creates a PoolStatusReporter for asynchronous effect types.
   * 
   * The created reporter will log pool status at intervals specified by
   * `reportInterval`. It uses the provided `poolLogger` for output and
   * `metricsTracker` to get detailed metrics.
   * 
   * The reporter only logs when debug logging is enabled to avoid cluttering
   * logs in production environments.
   * 
   * @param reportInterval the interval between status reports
   * @param poolLogger the logger to use for output
   * @param metricsTracker the metrics tracker for detailed statistics
   * @tparam F the effect type (must have Async and Temporal instances)
   * @return a new PoolStatusReporter instance
   */
  def apply[F[_]: Async](
    reportInterval: FiniteDuration,
    poolLogger:     PoolLogger[F],
    metricsTracker: PoolMetricsTracker[F]
  ): PoolStatusReporter[F] = new PoolStatusReporter[F]:

    override def start(pool: PooledDataSource[F], poolName: String): Resource[F, Unit] =
      val task = Stream
        .fixedDelay[F](reportInterval)
        .evalMap { _ =>
          poolLogger.isDebugEnabled.flatMap { enabled =>
            if enabled then reportStatus(pool, poolName)
            else Temporal[F].unit
          }
        }
        .compile
        .drain

      Resource
        .make(task.start)(_.cancel)
        .void

    private def reportStatus(pool: PooledDataSource[F], poolName: String): F[Unit] =
      for
        status  <- pool.status
        metrics <- metricsTracker.getMetrics
        _       <- poolLogger.logPoolState(poolName, status, Some(metrics))
      yield ()

  /**
   * Creates a no-op PoolStatusReporter that performs no reporting.
   * 
   * This implementation is useful when status reporting is disabled
   * or not needed. The start method returns immediately without
   * creating any background tasks.
   * 
   * @return a PoolStatusReporter that performs no operations
   */
  def noop[F[_]]: PoolStatusReporter[F] = (_: PooledDataSource[F], _: String) =>
    Resource.pure[F, Unit](())
