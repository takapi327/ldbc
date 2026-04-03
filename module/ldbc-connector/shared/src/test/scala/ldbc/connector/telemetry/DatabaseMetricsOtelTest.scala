/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.telemetry

import scala.concurrent.duration.*

import cats.effect.*

import org.typelevel.otel4s.sdk.metrics.data.{ MetricPoints, PointData }
import org.typelevel.otel4s.sdk.testkit.OpenTelemetrySdkTestkit

import ldbc.connector.*

class DatabaseMetricsOtelTest extends FTestPlatform:

  private val poolName = "test-pool"

  // ============================================================
  // db.client.operation.duration
  // ============================================================

  test("recordOperationDuration should record db.client.operation.duration as Histogram") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      for
        meter     <- testkit.meterProvider.get("ldbc")
        _         <- DatabaseMetrics.fromMeter(meter).use { metrics =>
                       metrics.recordOperationDuration(
                         100.millis,
                         TelemetryAttribute.dbSystemName,
                         TelemetryAttribute.dbNamespace("test_db"),
                         TelemetryAttribute.dbOperationName("SELECT")
                       )
                     }
        collected <- testkit.collectMetrics
        metric     = collected.find(_.name == TelemetryAttribute.METRIC_DB_CLIENT_OPERATION_DURATION)
      yield metric match
        case None    => fail(s"'${TelemetryAttribute.METRIC_DB_CLIENT_OPERATION_DURATION}' should be recorded")
        case Some(m) =>
          assertEquals(m.unit, Some("s"))
          assertEquals(m.description, Some("Duration of database client operations"))
          m.data match
            case _: MetricPoints.Histogram => ()
            case other                     => fail(s"Expected Histogram but got ${other.getClass.getSimpleName}")
    }
  }

  test("recordOperationDuration should record the correct duration value in seconds") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      for
        meter     <- testkit.meterProvider.get("ldbc")
        _         <- DatabaseMetrics.fromMeter(meter).use { metrics =>
                       metrics.recordOperationDuration(250.millis)
                     }
        collected <- testkit.collectMetrics
        metric     = collected.find(_.name == TelemetryAttribute.METRIC_DB_CLIENT_OPERATION_DURATION)
      yield metric match
        case None    => fail(s"'${TelemetryAttribute.METRIC_DB_CLIENT_OPERATION_DURATION}' should be recorded")
        case Some(m) =>
          m.data match
            case h: MetricPoints.Histogram =>
              h.points.head.stats match
                case None        => fail("Histogram stats should be defined")
                case Some(stats) =>
                  assertEqualsDouble(stats.sum, 0.25, 0.0001)
                  assertEquals(stats.count, 1L)
            case other => fail(s"Expected Histogram but got ${other.getClass.getSimpleName}")
    }
  }

  test("recordOperationDuration should accumulate multiple recordings") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      for
        meter     <- testkit.meterProvider.get("ldbc")
        _         <- DatabaseMetrics.fromMeter(meter).use { metrics =>
                       metrics.recordOperationDuration(100.millis) *>
                         metrics.recordOperationDuration(200.millis) *>
                         metrics.recordOperationDuration(300.millis)
                     }
        collected <- testkit.collectMetrics
        metric     = collected.find(_.name == TelemetryAttribute.METRIC_DB_CLIENT_OPERATION_DURATION)
      yield metric match
        case None    => fail(s"'${TelemetryAttribute.METRIC_DB_CLIENT_OPERATION_DURATION}' should be recorded")
        case Some(m) =>
          m.data match
            case h: MetricPoints.Histogram =>
              h.points.head.stats match
                case None        => fail("Histogram stats should be defined")
                case Some(stats) =>
                  assertEquals(stats.count, 3L)
                  assertEqualsDouble(stats.sum, 0.6, 0.0001)
            case other => fail(s"Expected Histogram but got ${other.getClass.getSimpleName}")
    }
  }

  // ============================================================
  // db.client.response.returned_rows
  // ============================================================

  test("recordReturnedRows should record db.client.response.returned_rows as Histogram") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      for
        meter     <- testkit.meterProvider.get("ldbc")
        _         <- DatabaseMetrics.fromMeter(meter).use { metrics =>
                       metrics.recordReturnedRows(42L, TelemetryAttribute.dbSystemName)
                     }
        collected <- testkit.collectMetrics
        metric     = collected.find(_.name == TelemetryAttribute.METRIC_DB_CLIENT_RESPONSE_RETURNED_ROWS)
      yield metric match
        case None    => fail(s"'${TelemetryAttribute.METRIC_DB_CLIENT_RESPONSE_RETURNED_ROWS}' should be recorded")
        case Some(m) =>
          assertEquals(m.unit, Some("{row}"))
          assertEquals(m.description, Some("The actual number of records returned by the database operation"))
          m.data match
            case _: MetricPoints.Histogram => ()
            case other                     => fail(s"Expected Histogram but got ${other.getClass.getSimpleName}")
    }
  }

  test("recordReturnedRows should record the correct row count") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      for
        meter     <- testkit.meterProvider.get("ldbc")
        _         <- DatabaseMetrics.fromMeter(meter).use { metrics =>
                       metrics.recordReturnedRows(100L)
                     }
        collected <- testkit.collectMetrics
        metric     = collected.find(_.name == TelemetryAttribute.METRIC_DB_CLIENT_RESPONSE_RETURNED_ROWS)
      yield metric match
        case None    => fail(s"'${TelemetryAttribute.METRIC_DB_CLIENT_RESPONSE_RETURNED_ROWS}' should be recorded")
        case Some(m) =>
          m.data match
            case h: MetricPoints.Histogram =>
              h.points.head.stats match
                case None        => fail("Histogram stats should be defined")
                case Some(stats) =>
                  assertEqualsDouble(stats.sum, 100.0, 0.0001)
                  assertEquals(stats.count, 1L)
            case other => fail(s"Expected Histogram but got ${other.getClass.getSimpleName}")
    }
  }

  // ============================================================
  // db.client.connection.create_time
  // ============================================================

  test("recordConnectionCreateTime should record db.client.connection.create_time as Histogram") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      for
        meter     <- testkit.meterProvider.get("ldbc")
        _         <- DatabaseMetrics.fromMeter(meter).use { metrics =>
                       metrics.recordConnectionCreateTime(50.millis, poolName)
                     }
        collected <- testkit.collectMetrics
        metric     = collected.find(_.name == TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_CREATE_TIME)
      yield metric match
        case None    => fail(s"'${TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_CREATE_TIME}' should be recorded")
        case Some(m) =>
          assertEquals(m.unit, Some("s"))
          assertEquals(m.description, Some("The time it took to create a new connection"))
          m.data match
            case _: MetricPoints.Histogram => ()
            case other                     => fail(s"Expected Histogram but got ${other.getClass.getSimpleName}")
    }
  }

  test("recordConnectionCreateTime should record the correct duration and pool name attribute") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      for
        meter     <- testkit.meterProvider.get("ldbc")
        _         <- DatabaseMetrics.fromMeter(meter).use { metrics =>
                       metrics.recordConnectionCreateTime(50.millis, poolName)
                     }
        collected <- testkit.collectMetrics
        metric     = collected.find(_.name == TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_CREATE_TIME)
      yield metric match
        case None    => fail(s"'${TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_CREATE_TIME}' should be recorded")
        case Some(m) =>
          m.data match
            case h: MetricPoints.Histogram =>
              val point = h.points.head
              point.stats match
                case None        => fail("Histogram stats should be defined")
                case Some(stats) =>
                  assertEqualsDouble(stats.sum, 0.05, 0.0001)
                  val poolAttr = point.attributes.get(
                    org.typelevel.otel4s.AttributeKey.string(TelemetryAttribute.DB_CLIENT_CONNECTION_POOL_NAME)
                  )
                  assertEquals(poolAttr.map(_.value), Some(poolName))
            case other => fail(s"Expected Histogram but got ${other.getClass.getSimpleName}")
    }
  }

  // ============================================================
  // db.client.connection.wait_time
  // ============================================================

  test("recordConnectionWaitTime should record db.client.connection.wait_time as Histogram") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      for
        meter     <- testkit.meterProvider.get("ldbc")
        _         <- DatabaseMetrics.fromMeter(meter).use { metrics =>
                       metrics.recordConnectionWaitTime(25.millis, poolName)
                     }
        collected <- testkit.collectMetrics
        metric     = collected.find(_.name == TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_WAIT_TIME)
      yield metric match
        case None    => fail(s"'${TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_WAIT_TIME}' should be recorded")
        case Some(m) =>
          assertEquals(m.unit, Some("s"))
          assertEquals(m.description, Some("The time it took to obtain an open connection from the pool"))
          m.data match
            case _: MetricPoints.Histogram => ()
            case other                     => fail(s"Expected Histogram but got ${other.getClass.getSimpleName}")
    }
  }

  // ============================================================
  // db.client.connection.use_time
  // ============================================================

  test("recordConnectionUseTime should record db.client.connection.use_time as Histogram") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      for
        meter     <- testkit.meterProvider.get("ldbc")
        _         <- DatabaseMetrics.fromMeter(meter).use { metrics =>
                       metrics.recordConnectionUseTime(200.millis, poolName)
                     }
        collected <- testkit.collectMetrics
        metric     = collected.find(_.name == TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_USE_TIME)
      yield metric match
        case None    => fail(s"'${TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_USE_TIME}' should be recorded")
        case Some(m) =>
          assertEquals(m.unit, Some("s"))
          assertEquals(m.description, Some("The time between borrowing a connection and returning it to the pool"))
          m.data match
            case _: MetricPoints.Histogram => ()
            case other                     => fail(s"Expected Histogram but got ${other.getClass.getSimpleName}")
    }
  }

  // ============================================================
  // db.client.connection.timeouts
  // ============================================================

  test("recordConnectionTimeout should record db.client.connection.timeouts as Sum (Counter)") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      for
        meter     <- testkit.meterProvider.get("ldbc")
        _         <- DatabaseMetrics.fromMeter(meter).use { metrics =>
                       metrics.recordConnectionTimeout(poolName)
                     }
        collected <- testkit.collectMetrics
        metric     = collected.find(_.name == TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_TIMEOUTS)
      yield metric match
        case None    => fail(s"'${TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_TIMEOUTS}' should be recorded")
        case Some(m) =>
          assertEquals(m.unit, Some("{timeout}"))
          assertEquals(
            m.description,
            Some("The number of connection timeouts that have occurred trying to obtain a connection from the pool")
          )
          m.data match
            case s: MetricPoints.Sum => assert(s.monotonic, "connection timeouts counter should be monotonic")
            case other               => fail(s"Expected Sum but got ${other.getClass.getSimpleName}")
    }
  }

  test("recordConnectionTimeout should increment count for each call") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      for
        meter     <- testkit.meterProvider.get("ldbc")
        _         <- DatabaseMetrics.fromMeter(meter).use { metrics =>
                       metrics.recordConnectionTimeout(poolName) *>
                         metrics.recordConnectionTimeout(poolName) *>
                         metrics.recordConnectionTimeout(poolName)
                     }
        collected <- testkit.collectMetrics
        metric     = collected.find(_.name == TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_TIMEOUTS)
      yield metric match
        case None    => fail(s"'${TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_TIMEOUTS}' should be recorded")
        case Some(m) =>
          m.data match
            case s: MetricPoints.Sum =>
              s.points.head match
                case p: PointData.LongNumber   => assertEquals(p.value, 3L)
                case p: PointData.DoubleNumber => fail(s"Expected LongNumber but got DoubleNumber(${p.value})")
            case other => fail(s"Expected Sum but got ${other.getClass.getSimpleName}")
    }
  }

  // ============================================================
  // registerPoolStateCallback (observable gauges)
  // ============================================================

  test("registerPoolStateCallback should record db.client.connection.count as UpDownCounter") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      for
        meter     <- testkit.meterProvider.get("ldbc")
        collected <- DatabaseMetrics.fromMeter(meter).use { metrics =>
                       metrics
                         .registerPoolStateCallback(
                           poolName,
                           minConnections = 2,
                           maxConnections = 10,
                           stateProvider  = IO.pure(PoolMetricsState(idleCount = 3L, usedCount = 2L, pendingRequestCount = 1L))
                         )
                         .surround(testkit.collectMetrics)
                     }
        countMetric = collected.find(_.name == TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_COUNT)
      yield countMetric match
        case None    => fail(s"'${TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_COUNT}' should be recorded")
        case Some(m) => assertEquals(m.unit, Some("{connection}"))
    }
  }

  test("registerPoolStateCallback should record db.client.connection.max") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      for
        meter     <- testkit.meterProvider.get("ldbc")
        collected <- DatabaseMetrics.fromMeter(meter).use { metrics =>
                       metrics
                         .registerPoolStateCallback(
                           poolName,
                           minConnections = 2,
                           maxConnections = 10,
                           stateProvider  = IO.pure(PoolMetricsState(idleCount = 0L, usedCount = 0L, pendingRequestCount = 0L))
                         )
                         .surround(testkit.collectMetrics)
                     }
        maxMetric = collected.find(_.name == TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_MAX)
      yield maxMetric match
        case None    => fail(s"'${TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_MAX}' should be recorded")
        case Some(m) => assertEquals(m.unit, Some("{connection}"))
    }
  }

  // ============================================================
  // fromMeter should register all expected metrics
  // ============================================================

  test("fromMeter should register all expected metric instruments") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val expectedMetricNames = Set(
        TelemetryAttribute.METRIC_DB_CLIENT_OPERATION_DURATION,
        TelemetryAttribute.METRIC_DB_CLIENT_RESPONSE_RETURNED_ROWS,
        TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_CREATE_TIME,
        TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_WAIT_TIME,
        TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_USE_TIME,
        TelemetryAttribute.METRIC_DB_CLIENT_CONNECTION_TIMEOUTS
      )
      for
        meter     <- testkit.meterProvider.get("ldbc")
        _         <- DatabaseMetrics.fromMeter(meter).use { metrics =>
                       metrics.recordOperationDuration(1.millis) *>
                         metrics.recordReturnedRows(1L) *>
                         metrics.recordConnectionCreateTime(1.millis, poolName) *>
                         metrics.recordConnectionWaitTime(1.millis, poolName) *>
                         metrics.recordConnectionUseTime(1.millis, poolName) *>
                         metrics.recordConnectionTimeout(poolName)
                     }
        collected <- testkit.collectMetrics
        names      = collected.map(_.name).toSet
      yield
        expectedMetricNames.foreach { expected =>
          assert(names.contains(expected), s"'$expected' should be registered")
        }
    }
  }
