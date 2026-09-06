/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.ziotelemetry

import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

import ldbc.sql.Attribute

import ldbc.telemetry.{ DbMetricSpecs, MeterProvider, PoolMetricsState }
import ldbc.zio.concurrentTask

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import io.opentelemetry.sdk.OpenTelemetrySdk
import zio.{ Runtime, Task, Unsafe, ZIO, ZLayer }
import zio.telemetry.opentelemetry.metrics.Meter as ZMeter
import zio.telemetry.opentelemetry.OpenTelemetry

/**
 * Verifies that the zio-telemetry backend produces the same instruments as `ldbc-otel4s` — both build
 * them from [[ldbc.telemetry.DbMetricSpecs]], so the two must agree on names, units and descriptions.
 *
 * Metrics are collected through an [[io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader]], which
 * also drives the observable callbacks, so the pool state gauges are exercised for real.
 */
class ZioTelemetryMetricsTest extends munit.FunSuite:

  /**
   * Runs `use` against a real SDK meter, handing it a `collect` effect so a test can read the exporter at a
   * chosen moment — observable gauges are only reported while their callback resource is open, so those
   * tests must collect inside the `use` block. The pair also carries a final collection taken after the
   * program has finished, for tests that assert on what remains once everything is released.
   */
  private def withMetrics[A](
    use: (MeterProvider[Task], Task[List[MetricData]]) => Task[A]
  ): (A, List[MetricData]) =
    val reader        = InMemoryMetricReader.create()
    val meterProvider = SdkMeterProvider.builder().registerMetricReader(reader).build()
    val otel: io.opentelemetry.api.OpenTelemetry =
      OpenTelemetrySdk.builder().setMeterProvider(meterProvider).build()
    val collect: Task[List[MetricData]] = ZIO.attempt(reader.collectAllMetrics().asScala.toList)
    val program                         =
      ZIO
        .serviceWithZIO[ZMeter](zmeter => use(ZioTelemetry.meterProvider(zmeter), collect))
        .provide(ZLayer.succeed(otel), OpenTelemetry.contextZIO, OpenTelemetry.metrics("ldbc"))
    val result = Unsafe.unsafe(implicit u => Runtime.default.unsafe.run(program).getOrThrowFiberFailure())
    (result, reader.collectAllMetrics().asScala.toList)

  private def find(metrics: List[MetricData], name: String): MetricData =
    metrics.find(_.getName == name).getOrElse(fail(s"'$name' was not exported; got ${ metrics.map(_.getName) }"))

  private def histogramSum(metrics: List[MetricData], name: String): Double =
    find(metrics, name).getHistogramData.getPoints.asScala.map(_.getSum).sum

  private def longPoint(metrics: List[MetricData], name: String, attributes: (String, String)*): Long =
    val points = find(metrics, name).getLongSumData.getPoints.asScala.toList.filter { point =>
      attributes.forall((key, value) => point.getAttributes.get(AttributeKey.stringKey(key)) == value)
    }
    points match
      case single :: Nil => single.getValue
      case other         => fail(s"expected one '$name' point matching ${ attributes.toList }, got $other")

  test("operation metrics are recorded, with the durations converted to seconds") {
    val (_, metrics) = withMetrics { (provider, _) =>
      for
        meter <- provider.meter("ldbc").withVersion("test").get
        _     <- meter.databaseMetrics.use { databaseMetrics =>
               databaseMetrics.recordOperationDuration(250.millis, Attribute("db.system.name", "mysql")) *>
                 databaseMetrics.recordReturnedRows(42L)
             }
      yield ()
    }
    assertEqualsDouble(histogramSum(metrics, "db.client.operation.duration"), 0.25, 1e-9)
    assertEqualsDouble(histogramSum(metrics, "db.client.response.returned_rows"), 42.0, 1e-9)
  }

  test("connection metrics are recorded and carry the pool name") {
    val (_, metrics) = withMetrics { (provider, _) =>
      for
        meter <- provider.meter("ldbc").get
        _     <- meter.databaseMetrics.use { databaseMetrics =>
               databaseMetrics.recordConnectionCreateTime(1500.millis, "zio-pool") *>
                 databaseMetrics.recordConnectionWaitTime(2.seconds, "zio-pool") *>
                 databaseMetrics.recordConnectionUseTime(500.micros, "zio-pool") *>
                 databaseMetrics.recordConnectionTimeout("zio-pool")
             }
      yield ()
    }
    assertEqualsDouble(histogramSum(metrics, "db.client.connection.create_time"), 1.5, 1e-9)
    assertEqualsDouble(histogramSum(metrics, "db.client.connection.wait_time"), 2.0, 1e-9)
    assertEqualsDouble(histogramSum(metrics, "db.client.connection.use_time"), 0.0005, 1e-9)
    assertEquals(longPoint(metrics, "db.client.connection.timeouts", "db.client.connection.pool.name" -> "zio-pool"), 1L)

    val waitPoint = find(metrics, "db.client.connection.wait_time").getHistogramData.getPoints.asScala.head
    assertEquals(waitPoint.getAttributes.get(AttributeKey.stringKey("db.client.connection.pool.name")), "zio-pool")
  }

  test("the pool state gauges report the live idle / used / bounds while registered") {
    val state = PoolMetricsState(idleCount = 3L, usedCount = 2L, pendingRequestCount = 1L)
    val (metrics, _) = withMetrics { (provider, collect) =>
      for
        meter   <- provider.meter("ldbc").get
        metrics <- meter.databaseMetrics.use { databaseMetrics =>
                     databaseMetrics.registerPoolStateCallback("zio-pool", 1, 10, ZIO.succeed(state)).use(_ => collect)
                   }
      yield metrics
    }
    val pool = "db.client.connection.pool.name" -> "zio-pool"
    assertEquals(longPoint(metrics, "db.client.connection.count", pool, "db.client.connection.state" -> "idle"), 3L)
    assertEquals(longPoint(metrics, "db.client.connection.count", pool, "db.client.connection.state" -> "used"), 2L)
    assertEquals(longPoint(metrics, "db.client.connection.idle.min", pool), 1L)
    assertEquals(longPoint(metrics, "db.client.connection.idle.max", pool), 10L)
    assertEquals(longPoint(metrics, "db.client.connection.max", pool), 10L)
    assertEquals(longPoint(metrics, "db.client.connection.pending_requests", pool), 1L)
  }

  test("every instrument matches the shared DbMetricSpecs definition") {
    val state        = PoolMetricsState(1L, 1L, 1L)
    val (metrics, _) = withMetrics { (provider, collect) =>
      for
        meter   <- provider.meter("ldbc").get
        metrics <- meter.databaseMetrics.use { databaseMetrics =>
                     databaseMetrics.recordOperationDuration(1.second) *>
                       databaseMetrics.recordReturnedRows(1L) *>
                       databaseMetrics.recordConnectionCreateTime(1.second, "p") *>
                       databaseMetrics.recordConnectionWaitTime(1.second, "p") *>
                       databaseMetrics.recordConnectionUseTime(1.second, "p") *>
                       databaseMetrics.recordConnectionTimeout("p") *>
                       databaseMetrics.registerPoolStateCallback("p", 1, 2, ZIO.succeed(state)).use(_ => collect)
                   }
      yield metrics
    }
    DbMetricSpecs.all.foreach { spec =>
      val exported = find(metrics, spec.name)
      assertEquals(exported.getUnit, spec.unit, s"unit of ${ spec.name }")
      assertEquals(exported.getDescription, spec.description, s"description of ${ spec.name }")
    }
  }

  test("the observable gauges stop being reported once the callback resource is released") {
    val state = PoolMetricsState(1L, 0L, 0L)
    val (_, metrics) = withMetrics { (provider, _) =>
      for
        meter <- provider.meter("ldbc").get
        _     <- meter.databaseMetrics.use { databaseMetrics =>
               databaseMetrics.registerPoolStateCallback("zio-pool", 1, 10, ZIO.succeed(state)).use(_ => ZIO.unit) *>
                 ZIO.unit
             }
      yield ()
    }
    // the reader collects after the resource is closed, so the gauges must no longer be observable
    assert(
      !metrics.map(_.getName).contains("db.client.connection.count"),
      s"the gauges are unregistered on release, but got ${ metrics.map(_.getName) }"
    )
  }
