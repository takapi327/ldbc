/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.telemetry

import scala.concurrent.duration.*

import cats.effect.*

import org.typelevel.otel4s.metrics.BucketBoundaries
import org.typelevel.otel4s.Attribute

import ldbc.connector.*

class DatabaseMetricsTest extends FTestPlatform:

  // ============================================================
  // Bucket boundaries tests
  // ============================================================

  test("OperationDurationBuckets should have correct values per OpenTelemetry spec") {
    val expected = BucketBoundaries(0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0, 10.0)
    assertEquals(DatabaseMetrics.operationDurationBuckets, expected)
  }

  test("ReturnedRowsBuckets should have correct values per OpenTelemetry spec") {
    val expected = BucketBoundaries(1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000)
    assertEquals(DatabaseMetrics.returnedRowsBuckets, expected)
  }

  // ============================================================
  // noop implementation tests
  // ============================================================

  test("noop should return unit for recordOperationDuration") {
    val metrics = DatabaseMetrics.noop[IO]
    for result <- metrics.recordOperationDuration(100.millis, Attribute("key", "value"))
    yield assertEquals(result, ())
  }

  test("noop should return unit for recordReturnedRows") {
    val metrics = DatabaseMetrics.noop[IO]
    for result <- metrics.recordReturnedRows(100L, Attribute("key", "value"))
    yield assertEquals(result, ())
  }

  test("noop should return unit for recordConnectionCreateTime") {
    val metrics = DatabaseMetrics.noop[IO]
    for result <- metrics.recordConnectionCreateTime(50.millis, "test-pool")
    yield assertEquals(result, ())
  }

  test("noop should return unit for recordConnectionWaitTime") {
    val metrics = DatabaseMetrics.noop[IO]
    for result <- metrics.recordConnectionWaitTime(25.millis, "test-pool")
    yield assertEquals(result, ())
  }

  test("noop should return unit for recordConnectionUseTime") {
    val metrics = DatabaseMetrics.noop[IO]
    for result <- metrics.recordConnectionUseTime(200.millis, "test-pool")
    yield assertEquals(result, ())
  }

  test("noop should return unit for recordConnectionTimeout") {
    val metrics = DatabaseMetrics.noop[IO]
    for result <- metrics.recordConnectionTimeout("test-pool")
    yield assertEquals(result, ())
  }

  test("noop should return Resource.unit for registerPoolStateCallback") {
    val metrics       = DatabaseMetrics.noop[IO]
    val stateProvider = IO.pure(PoolMetricsState(5L, 3L, 0L))
    metrics.registerPoolStateCallback("test-pool", 2, 10, stateProvider).use_
  }

  // ============================================================
  // noop with multiple calls tests
  // ============================================================

  test("noop should handle multiple calls without side effects") {
    val metrics = DatabaseMetrics.noop[IO]
    for
      _ <- metrics.recordOperationDuration(100.millis)
      _ <- metrics.recordOperationDuration(200.millis)
      _ <- metrics.recordReturnedRows(50L)
      _ <- metrics.recordConnectionCreateTime(10.millis, "pool1")
      _ <- metrics.recordConnectionWaitTime(5.millis, "pool1")
      _ <- metrics.recordConnectionUseTime(100.millis, "pool1")
      _ <- metrics.recordConnectionTimeout("pool1")
    yield assert(true)
  }

  // ============================================================
  // noop with attributes tests
  // ============================================================

  test("noop should handle recordOperationDuration with multiple attributes") {
    val metrics = DatabaseMetrics.noop[IO]
    for result <- metrics.recordOperationDuration(
                    100.millis,
                    TelemetryAttribute.dbSystemName,
                    TelemetryAttribute.dbNamespace("test_db"),
                    TelemetryAttribute.dbOperationName("SELECT")
                  )
    yield assertEquals(result, ())
  }

  test("noop should handle recordReturnedRows with multiple attributes") {
    val metrics = DatabaseMetrics.noop[IO]
    for result <- metrics.recordReturnedRows(
                    42L,
                    TelemetryAttribute.dbSystemName,
                    TelemetryAttribute.dbCollectionName("users")
                  )
    yield assertEquals(result, ())
  }

  // ============================================================
  // noop is thread-safe tests
  // ============================================================

  test("noop should be thread-safe for concurrent calls") {
    val metrics = DatabaseMetrics.noop[IO]
    for _ <- IO.parTraverseN(10)((1 to 100).toList) { i =>
               metrics.recordOperationDuration(i.millis) *>
                 metrics.recordReturnedRows(i.toLong) *>
                 metrics.recordConnectionCreateTime(i.millis, s"pool-$i")
             }
    yield assert(true)
  }
