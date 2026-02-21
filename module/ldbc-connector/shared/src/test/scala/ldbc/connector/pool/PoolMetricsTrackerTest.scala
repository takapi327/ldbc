/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*

import cats.effect.*

import ldbc.connector.*

class PoolMetricsTrackerTest extends FTestPlatform:

  test("noop tracker should always return empty metrics") {
    val tracker = PoolMetricsTracker.noop[IO]

    for
      _       <- tracker.recordAcquisition(100.millis)
      _       <- tracker.recordUsage(500.millis)
      _       <- tracker.recordCreation(50.millis)
      _       <- tracker.recordTimeout()
      _       <- tracker.recordLeak()
      _       <- tracker.updateGauge("active", 10)
      metrics <- tracker.getMetrics
    yield assertEquals(metrics, PoolMetrics.empty)
  }

  test("in-memory tracker should record acquisition times and calculate average") {
    for
      tracker <- PoolMetricsTracker.inMemory[IO]
      _       <- tracker.recordAcquisition(100.millis)
      _       <- tracker.recordAcquisition(200.millis)
      _       <- tracker.recordAcquisition(300.millis)
      metrics <- tracker.getMetrics
    yield
      assertEquals(metrics.acquisitionTime, 200.millis)
      assertEquals(metrics.totalAcquisitions, 3L)
  }

  test("in-memory tracker should record usage times and calculate average") {
    for
      tracker <- PoolMetricsTracker.inMemory[IO]
      _       <- tracker.recordUsage(100.millis)
      _       <- tracker.recordUsage(300.millis)
      _       <- tracker.recordUsage(500.millis)
      metrics <- tracker.getMetrics
    yield
      assertEquals(metrics.usageTime, 300.millis)
      assertEquals(metrics.totalReleases, 3L)
  }

  test("in-memory tracker should record creation times and calculate average") {
    for
      tracker <- PoolMetricsTracker.inMemory[IO]
      _       <- tracker.recordCreation(50.millis)
      _       <- tracker.recordCreation(100.millis)
      _       <- tracker.recordCreation(150.millis)
      metrics <- tracker.getMetrics
    yield
      assertEquals(metrics.creationTime, 100.millis)
      assertEquals(metrics.totalCreations, 3L)
  }

  test("in-memory tracker should count timeouts correctly") {
    for
      tracker <- PoolMetricsTracker.inMemory[IO]
      _       <- tracker.recordTimeout()
      _       <- tracker.recordTimeout()
      _       <- tracker.recordTimeout()
      metrics <- tracker.getMetrics
    yield assertEquals(metrics.timeouts, 3L)
  }

  test("in-memory tracker should count leaks correctly") {
    for
      tracker <- PoolMetricsTracker.inMemory[IO]
      _       <- tracker.recordLeak()
      _       <- tracker.recordLeak()
      metrics <- tracker.getMetrics
    yield assertEquals(metrics.leaks, 2L)
  }

  test("in-memory tracker should handle gauge updates") {
    for
      tracker <- PoolMetricsTracker.inMemory[IO]
      _       <- tracker.updateGauge("active", 5)
      _       <- tracker.updateGauge("idle", 3)
      _       <- tracker.updateGauge("active", 7) // Update existing gauge
      metrics <- tracker.getMetrics
    yield
      assertEquals(metrics.gauges, Map("active" -> 7L, "idle" -> 3L))
  }

  test("in-memory tracker should maintain sliding window of duration samples") {
    for
      tracker <- PoolMetricsTracker.inMemory[IO]
      // Record more than maxSize (100) samples
      _ <- IO.parTraverseN(10)((1 to 110).toList) { i =>
             tracker.recordAcquisition(i.millis)
           }
      metrics <- tracker.getMetrics
    yield
      // Should keep only the last 100 samples (11-110)
      // Average of 11 to 110 = (11+110)*50/2 = 60.5
      assert(60400.micros < metrics.acquisitionTime || metrics.acquisitionTime < 60500.micros) // 60.5 millis
      assertEquals(metrics.totalAcquisitions, 110L)
  }

  test("in-memory tracker should handle empty collections") {
    for
      tracker <- PoolMetricsTracker.inMemory[IO]
      metrics <- tracker.getMetrics
    yield
      assertEquals(metrics.acquisitionTime, Duration.Zero)
      assertEquals(metrics.usageTime, Duration.Zero)
      assertEquals(metrics.creationTime, Duration.Zero)
      assertEquals(metrics.timeouts, 0L)
      assertEquals(metrics.leaks, 0L)
      assertEquals(metrics.totalAcquisitions, 0L)
      assertEquals(metrics.totalReleases, 0L)
      assertEquals(metrics.totalCreations, 0L)
      assertEquals(metrics.totalRemovals, 0L)
  }

  test("in-memory tracker should handle mixed operations") {
    for
      tracker <- PoolMetricsTracker.inMemory[IO]
      _       <- tracker.recordAcquisition(100.millis)
      _       <- tracker.recordUsage(200.millis)
      _       <- tracker.recordCreation(50.millis)
      _       <- tracker.recordTimeout()
      _       <- tracker.recordLeak()
      _       <- tracker.recordAcquisition(200.millis)
      _       <- tracker.recordUsage(300.millis)
      _       <- tracker.updateGauge("total", 10)
      metrics <- tracker.getMetrics
    yield
      assertEquals(metrics.acquisitionTime, 150.millis)
      assertEquals(metrics.usageTime, 250.millis)
      assertEquals(metrics.creationTime, 50.millis)
      assertEquals(metrics.timeouts, 1L)
      assertEquals(metrics.leaks, 1L)
      assertEquals(metrics.totalAcquisitions, 2L)
      assertEquals(metrics.totalReleases, 2L)
      assertEquals(metrics.totalCreations, 1L)
      assertEquals(metrics.totalRemovals, 0L)
  }

  test("in-memory tracker should be thread-safe") {
    for
      tracker <- PoolMetricsTracker.inMemory[IO]
      // Parallel operations
      _ <- IO.parTraverseN(10)((1 to 100).toList) { _ =>
             tracker.recordAcquisition(100.millis) *>
               tracker.recordTimeout() *>
               tracker.recordLeak()
           }
      metrics <- tracker.getMetrics
    yield
      assertEquals(metrics.acquisitionTime, 100.millis)
      assertEquals(metrics.timeouts, 100L)
      assertEquals(metrics.leaks, 100L)
      assertEquals(metrics.totalAcquisitions, 100L)
  }
