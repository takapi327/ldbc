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
 * A background task that periodically logs the pool status (total/active/idle/waiting plus detailed
 * metrics) when debug logging is enabled, similar to HikariCP. The periodic loop is scheduled via
 * `F.sleep` recursion and cancelled when its [[ldbc.effect.Resource]] is released.
 */
trait PoolStatusReporter[F[_]]:
  /**
   * Starts the reporter for `pool`.
   *
   * @param pool     the pool to monitor
   * @param poolName the pool name used in log output
   */
  def start(pool: PooledDataSource[F], poolName: String): Resource[F, Unit]

object PoolStatusReporter:

  /**
   * Creates a reporter that logs every `reportInterval`.
   *
   * @param reportInterval the interval between reports
   * @param poolLogger     the logger to write to
   * @param metricsTracker the tracker providing detailed metrics
   */
  def apply[F[_]](reportInterval: FiniteDuration, poolLogger: PoolLogger[F], metricsTracker: PoolMetricsTracker[F])(
    using F: Concurrent[F]
  ): PoolStatusReporter[F] =
    new PoolStatusReporter[F]:

      override def start(pool: PooledDataSource[F], poolName: String): Resource[F, Unit] =
        def loop: F[Unit] =
          F.sleep(reportInterval)
            .flatMap { _ =>
              poolLogger.isDebugEnabled.flatMap(enabled => if enabled then reportStatus(pool, poolName) else F.unit)
            }
            .flatMap(_ => loop)
        Resource.make(loop.start)(fiber => fiber.cancel).map(_ => ())

      private def reportStatus(pool: PooledDataSource[F], poolName: String): F[Unit] =
        for
          status  <- pool.status
          metrics <- metricsTracker.getMetrics
          _       <- poolLogger.logPoolState(poolName, status, Some(metrics))
        yield ()

  /** A reporter that does nothing. */
  def noop[F[_]](using F: Concurrent[F]): PoolStatusReporter[F] = (_: PooledDataSource[F], _: String) =>
    Resource.pure[F, Unit](())
