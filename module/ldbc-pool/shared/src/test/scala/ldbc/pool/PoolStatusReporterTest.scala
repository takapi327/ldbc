/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import ldbc.fx.FxSuite

import scala.concurrent.duration.*

import ldbc.fx.Fx
import ldbc.fx.concurrentFx
import ldbc.effect.Ref

class PoolStatusReporterTest extends FxSuite:

  /** A [[PoolLogger]] that counts `logPoolState` calls (via a Ref) for assertion. */
  private class CountingPoolLogger(count: Ref[Fx, Int], debugEnabled: Boolean) extends PoolLogger[Fx]:
    override def logPoolState(poolName: String, status: PoolStatus, metrics: Option[PoolMetrics]): Fx[Unit] =
      count.update(_ + 1)
    override def debug(message: String): Fx[Unit]                          = Fx.unit
    override def info(message:  String): Fx[Unit]                          = Fx.unit
    override def warn(message:  String): Fx[Unit]                          = Fx.unit
    override def error(message: String, error: Option[Throwable]): Fx[Unit] = Fx.unit
    override def isDebugEnabled: Fx[Boolean] = Fx.pure(debugEnabled)

  private def mockPool(logger: PoolLogger[Fx]): Fx[MockPool] =
    Ref.of(PoolState.empty[Fx]).map { state =>
      new MockPool(
        poolState      = state,
        metricsTracker = PoolMetricsTracker.noop[Fx],
        poolLogger     = logger,
        statusEffect   = Some(Fx.pure(PoolStatus(total = 10, active = 3, idle = 7, waiting = 0)))
      )
    }

  test("PoolStatusReporter should report pool status periodically when enabled") {
    for
      count <- Ref.of(0)
      logger = new CountingPoolLogger(count, debugEnabled = true)
      pool   <- mockPool(logger)
      reporter = PoolStatusReporter(100.milliseconds, logger, PoolMetricsTracker.noop[Fx])
      _        <- reporter.start(pool, "test-pool").use(_ => Fx.sleep(500.milliseconds))
      logCount <- count.get
    yield assert(logCount >= 2, s"Expected at least 2 logs, but got $logCount")
  }

  test("PoolStatusReporter.noop should not report anything") {
    for
      count <- Ref.of(0)
      logger = new CountingPoolLogger(count, debugEnabled = true)
      pool   <- mockPool(logger)
      reporter = PoolStatusReporter.noop[Fx]
      _        <- reporter.start(pool, "test-pool").use(_ => Fx.sleep(200.milliseconds))
      logCount <- count.get
    yield assertEquals(logCount, 0)
  }

  test("PoolStatusReporter should only log when debug is enabled") {
    for
      count <- Ref.of(0)
      logger = new CountingPoolLogger(count, debugEnabled = false)
      pool   <- mockPool(logger)
      reporter = PoolStatusReporter(50.milliseconds, logger, PoolMetricsTracker.noop[Fx])
      _        <- reporter.start(pool, "test-pool").use(_ => Fx.sleep(150.milliseconds))
      logCount <- count.get
    yield assert(logCount == 0, "Expected no logs when debug is disabled")
  }
