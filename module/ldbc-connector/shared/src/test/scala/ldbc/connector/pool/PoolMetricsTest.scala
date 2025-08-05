/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*

import ldbc.connector.*

class PoolMetricsTest extends FTestPlatform:

  test("PoolMetrics should be created with correct values") {
    val metrics = PoolMetrics(
      acquisitionTime   = 100.millis,
      usageTime         = 500.millis,
      creationTime      = 50.millis,
      timeouts          = 5,
      leaks             = 2,
      totalAcquisitions = 1000,
      totalReleases     = 995,
      totalCreations    = 100,
      totalRemovals     = 10
    )

    assertEquals(metrics.acquisitionTime, 100.millis)
    assertEquals(metrics.usageTime, 500.millis)
    assertEquals(metrics.creationTime, 50.millis)
    assertEquals(metrics.timeouts, 5L)
    assertEquals(metrics.leaks, 2L)
    assertEquals(metrics.totalAcquisitions, 1000L)
    assertEquals(metrics.totalReleases, 995L)
    assertEquals(metrics.totalCreations, 100L)
    assertEquals(metrics.totalRemovals, 10L)
  }

  test("PoolMetrics.empty should create metrics with zero values") {
    val empty = PoolMetrics.empty

    assertEquals(empty.acquisitionTime, Duration.Zero)
    assertEquals(empty.usageTime, Duration.Zero)
    assertEquals(empty.creationTime, Duration.Zero)
    assertEquals(empty.timeouts, 0L)
    assertEquals(empty.leaks, 0L)
    assertEquals(empty.totalAcquisitions, 0L)
    assertEquals(empty.totalReleases, 0L)
    assertEquals(empty.totalCreations, 0L)
    assertEquals(empty.totalRemovals, 0L)
  }

  test("PoolMetrics should support copy with updated values") {
    val original = PoolMetrics(
      acquisitionTime   = 100.millis,
      usageTime         = 500.millis,
      creationTime      = 50.millis,
      timeouts          = 5,
      leaks             = 2,
      totalAcquisitions = 1000,
      totalReleases     = 995,
      totalCreations    = 100,
      totalRemovals     = 10
    )

    val updated = original.copy(
      timeouts      = 10,
      leaks         = 3,
      totalReleases = 1000
    )

    assertEquals(updated.acquisitionTime, 100.millis)
    assertEquals(updated.usageTime, 500.millis)
    assertEquals(updated.creationTime, 50.millis)
    assertEquals(updated.timeouts, 10L)
    assertEquals(updated.leaks, 3L)
    assertEquals(updated.totalAcquisitions, 1000L)
    assertEquals(updated.totalReleases, 1000L)
    assertEquals(updated.totalCreations, 100L)
    assertEquals(updated.totalRemovals, 10L)

    // Original should remain unchanged
    assertEquals(original.timeouts, 5L)
    assertEquals(original.leaks, 2L)
    assertEquals(original.totalReleases, 995L)
  }

  test("PoolMetrics should have correct equality behavior") {
    val metrics1 = PoolMetrics(
      acquisitionTime   = 100.millis,
      usageTime         = 500.millis,
      creationTime      = 50.millis,
      timeouts          = 5,
      leaks             = 2,
      totalAcquisitions = 1000,
      totalReleases     = 995,
      totalCreations    = 100,
      totalRemovals     = 10
    )

    val metrics2 = PoolMetrics(
      acquisitionTime   = 100.millis,
      usageTime         = 500.millis,
      creationTime      = 50.millis,
      timeouts          = 5,
      leaks             = 2,
      totalAcquisitions = 1000,
      totalReleases     = 995,
      totalCreations    = 100,
      totalRemovals     = 10
    )

    val metrics3 = PoolMetrics(
      acquisitionTime   = 200.millis, // Different value
      usageTime         = 500.millis,
      creationTime      = 50.millis,
      timeouts          = 5,
      leaks             = 2,
      totalAcquisitions = 1000,
      totalReleases     = 995,
      totalCreations    = 100,
      totalRemovals     = 10
    )

    assertEquals(metrics1, metrics2)
    assertNotEquals(metrics1, metrics3)
  }

  test("PoolMetrics should handle different duration units correctly") {
    val metrics = PoolMetrics(
      acquisitionTime   = 1.second,
      usageTime         = 2.minutes,
      creationTime      = 500.microseconds,
      timeouts          = 0,
      leaks             = 0,
      totalAcquisitions = 0,
      totalReleases     = 0,
      totalCreations    = 0,
      totalRemovals     = 0
    )

    assertEquals(metrics.acquisitionTime, 1000.millis)
    assertEquals(metrics.usageTime, 120.seconds)
    assertEquals(metrics.creationTime, 500.microseconds)
  }

  test("PoolMetrics should have a meaningful toString representation") {
    val metrics = PoolMetrics(
      acquisitionTime   = 100.millis,
      usageTime         = 500.millis,
      creationTime      = 50.millis,
      timeouts          = 5,
      leaks             = 2,
      totalAcquisitions = 1000,
      totalReleases     = 995,
      totalCreations    = 100,
      totalRemovals     = 10
    )

    val str = metrics.toString
    assert(str.contains("PoolMetrics"), "toString should contain class name")
    assert(str.contains("100"), "toString should contain acquisitionTime value")
    assert(str.contains("500"), "toString should contain usageTime value")
    assert(str.contains("50"), "toString should contain creationTime value")
    assert(str.contains("5"), "toString should contain timeouts value")
    assert(str.contains("2"), "toString should contain leaks value")
    assert(str.contains("1000"), "toString should contain totalAcquisitions value")
    assert(str.contains("995"), "toString should contain totalReleases value")
  }

  test("PoolMetrics should handle edge cases with very large values") {
    val metrics = PoolMetrics(
      acquisitionTime   = 365.days,
      usageTime         = 24.hours,
      creationTime      = 1.minute,
      timeouts          = Long.MaxValue,
      leaks             = Long.MaxValue,
      totalAcquisitions = Long.MaxValue,
      totalReleases     = Long.MaxValue,
      totalCreations    = Long.MaxValue,
      totalRemovals     = Long.MaxValue
    )

    assertEquals(metrics.acquisitionTime, 365.days)
    assertEquals(metrics.usageTime, 24.hours)
    assertEquals(metrics.creationTime, 1.minute)
    assertEquals(metrics.timeouts, Long.MaxValue)
    assertEquals(metrics.leaks, Long.MaxValue)
    assertEquals(metrics.totalAcquisitions, Long.MaxValue)
    assertEquals(metrics.totalReleases, Long.MaxValue)
    assertEquals(metrics.totalCreations, Long.MaxValue)
    assertEquals(metrics.totalRemovals, Long.MaxValue)
  }