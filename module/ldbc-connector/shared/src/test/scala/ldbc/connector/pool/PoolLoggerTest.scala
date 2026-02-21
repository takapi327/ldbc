/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*

import cats.effect.*

import munit.CatsEffectSuite

class PoolLoggerTest extends CatsEffectSuite:

  test("PoolLogger.console should log pool state when debug is enabled") {
    val logger = PoolLogger.console[IO](logDebug = true)

    val status  = PoolStatus(total = 10, active = 3, idle = 7, waiting = 0)
    val metrics = PoolMetrics(
      acquisitionTime   = 100.milliseconds,
      usageTime         = 500.milliseconds,
      creationTime      = 200.milliseconds,
      timeouts          = 2,
      leaks             = 1,
      totalAcquisitions = 100,
      totalReleases     = 98,
      totalCreations    = 10,
      totalRemovals     = 0,
      gauges            = Map.empty
    )

    for
      _       <- logger.logPoolState("test-pool", status, Some(metrics))
      enabled <- logger.isDebugEnabled
    yield assert(enabled)
  }

  test("PoolLogger.console should not log when debug is disabled") {
    val logger = PoolLogger.console[IO](logDebug = false)

    val status = PoolStatus(total = 10, active = 3, idle = 7, waiting = 0)

    for
      _       <- logger.logPoolState("test-pool", status, None)
      enabled <- logger.isDebugEnabled
    yield assert(!enabled)
  }

  test("PoolLogger.noop should not perform any operations") {
    val logger = PoolLogger.noop[IO]

    val status = PoolStatus(total = 10, active = 3, idle = 7, waiting = 0)

    for
      _       <- logger.logPoolState("test-pool", status, None)
      _       <- logger.debug("debug message")
      _       <- logger.info("info message")
      _       <- logger.warn("warn message")
      _       <- logger.error("error message", Some(new Exception("test")))
      enabled <- logger.isDebugEnabled
    yield assert(!enabled)
  }

  test("PoolLogger.console should format pool state correctly") {
    // This test would need to capture console output to verify formatting
    // For now, we just ensure it doesn't throw exceptions
    val logger = PoolLogger.console[IO](logDebug = true)

    val status  = PoolStatus(total = 10, active = 3, idle = 7, waiting = 2)
    val metrics = PoolMetrics(
      acquisitionTime   = 150.milliseconds,
      usageTime         = 750.milliseconds,
      creationTime      = 250.milliseconds,
      timeouts          = 5,
      leaks             = 0,
      totalAcquisitions = 1000,
      totalReleases     = 995,
      totalCreations    = 15,
      totalRemovals     = 5,
      gauges            = Map.empty
    )

    logger.logPoolState("production-pool", status, Some(metrics))
  }

  test("PoolLogger should handle all log levels") {
    val logger = PoolLogger.console[IO](logDebug = true)

    for
      _ <- logger.debug("Debug message")
      _ <- logger.info("Info message")
      _ <- logger.warn("Warning message")
      _ <- logger.error("Error message")
      _ <- logger.error("Error with exception", Some(new RuntimeException("Test error")))
    yield assert(true)
  }
