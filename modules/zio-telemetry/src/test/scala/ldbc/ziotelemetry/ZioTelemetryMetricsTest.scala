/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.ziotelemetry

import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

import ldbc.sql.Attribute

import ldbc.telemetry.{ DbMetricSpecs, InstrumentKind, MeterProvider, PoolMetricsState }
import ldbc.zio.concurrentTask

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import io.opentelemetry.sdk.OpenTelemetrySdk
import zio.{ Chunk, Runtime, Scope, Task, Trace, UIO, Unsafe, ZIO, ZLayer }
import zio.telemetry.opentelemetry.metrics.{ Counter, Histogram, ObservableMeasurement, UpDownCounter }
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
   * A meter that delegates to `underlying` but fails the `failOn`-th observable registration, so a test can
   * drive the partial-failure path of [[ZioTelemetry]]'s scoped gauge registration.
   */
  private final class FailingMeter(underlying: ZMeter, failOn: Int) extends ZMeter:
    private val registrations = new java.util.concurrent.atomic.AtomicInteger(0)

    override def counter(name: String, unit: Option[String], description: Option[String])(implicit
      trace: Trace
    ): UIO[Counter[Long]] = underlying.counter(name, unit, description)

    override def upDownCounter(name: String, unit: Option[String], description: Option[String])(implicit
      trace: Trace
    ): UIO[UpDownCounter[Long]] = underlying.upDownCounter(name, unit, description)

    override def histogram(
      name:        String,
      unit:        Option[String],
      description: Option[String],
      boundaries:  Option[Chunk[Double]]
    )(implicit trace: Trace): UIO[Histogram[Double]] = underlying.histogram(name, unit, description, boundaries)

    override def observableCounter(name: String, unit: Option[String], description: Option[String])(
      callback: ObservableMeasurement[Long] => Task[Unit]
    )(implicit trace: Trace): ZIO[Scope, Throwable, Unit] =
      underlying.observableCounter(name, unit, description)(callback)

    override def observableUpDownCounter(name: String, unit: Option[String], description: Option[String])(
      callback: ObservableMeasurement[Long] => Task[Unit]
    )(implicit trace: Trace): ZIO[Scope, Throwable, Unit] =
      if registrations.incrementAndGet() == failOn then ZIO.fail(new RuntimeException(s"cannot register $name"))
      else underlying.observableUpDownCounter(name, unit, description)(callback)

    override def observableGauge(name: String, unit: Option[String], description: Option[String])(
      callback: ObservableMeasurement[Double] => Task[Unit]
    )(implicit trace: Trace): ZIO[Scope, Throwable, Unit] =
      underlying.observableGauge(name, unit, description)(callback)

  /**
   * Runs `use` against a real SDK meter, handing it a `collect` effect so a test can read the exporter at a
   * chosen moment — observable gauges are only reported while their callback resource is open, so those
   * tests must collect inside the `use` block. The pair also carries a final collection taken after the
   * program has finished, for tests that assert on what remains once everything is released.
   */
  private def withMetrics[A](
    use: (MeterProvider[Task], Task[List[MetricData]]) => Task[A]
  ): (A, List[MetricData]) =
    withZMeter((zmeter, collect) => use(ZioTelemetry.meterProvider(zmeter), collect))

  /** As [[withMetrics]], but hands over the raw zio-telemetry meter so a test can wrap it. */
  private def withZMeter[A](use: (ZMeter, Task[List[MetricData]]) => Task[A]): (A, List[MetricData]) =
    val reader        = InMemoryMetricReader.create()
    val meterProvider = SdkMeterProvider.builder().registerMetricReader(reader).build()
    val otel: io.opentelemetry.api.OpenTelemetry =
      OpenTelemetrySdk.builder().setMeterProvider(meterProvider).build()
    val collect: Task[List[MetricData]] = ZIO.attempt(reader.collectAllMetrics().asScala.toList)
    val program =
      ZIO
        .serviceWithZIO[ZMeter](zmeter => use(zmeter, collect))
        .provide(ZLayer.succeed(otel), OpenTelemetry.contextZIO, OpenTelemetry.metrics("ldbc"))
    val result = Unsafe.unsafe(implicit u => Runtime.default.unsafe.run(program).getOrThrowFiberFailure())
    (result, reader.collectAllMetrics().asScala.toList)

  /** The explicit bucket boundaries a histogram metric was created with. */
  private def boundaries(metrics: List[MetricData], name: String): List[Double] =
    find(metrics, name).getHistogramData.getPoints.asScala.head.getBoundaries.asScala.toList.map(_.doubleValue)

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

  test("attributes passed to the operation metrics reach the exported data point") {
    val (_, metrics) = withMetrics { (provider, _) =>
      for
        meter <- provider.meter("ldbc").get
        _     <- meter.databaseMetrics.use { databaseMetrics =>
               databaseMetrics.recordOperationDuration(
                 1.second,
                 Attribute("db.operation.name", "SELECT"),
                 Attribute("server.port", 3306L),
                 Attribute("retries", 2),
                 Attribute("cached", true),
                 Attribute("ratio", 1.5),
                 Attribute("weird", List(1, 2))
               )
             }
      yield ()
    }
    val attrs = find(metrics, "db.client.operation.duration").getHistogramData.getPoints.asScala.head.getAttributes
    assertEquals(attrs.get(AttributeKey.stringKey("db.operation.name")), "SELECT")
    assertEquals(attrs.get(AttributeKey.longKey("server.port")).longValue, 3306L)
    assertEquals(attrs.get(AttributeKey.longKey("retries")).longValue, 2L)
    assertEquals(attrs.get(AttributeKey.booleanKey("cached")).booleanValue, true)
    assertEquals(attrs.get(AttributeKey.doubleKey("ratio")).doubleValue, 1.5)
    assertEquals(attrs.get(AttributeKey.stringKey("weird")), "List(1, 2)")
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
    assertEquals(
      longPoint(metrics, "db.client.connection.timeouts", "db.client.connection.pool.name" -> "zio-pool"),
      1L
    )

    val waitPoint = find(metrics, "db.client.connection.wait_time").getHistogramData.getPoints.asScala.head
    assertEquals(waitPoint.getAttributes.get(AttributeKey.stringKey("db.client.connection.pool.name")), "zio-pool")
  }

  test("the pool state gauges report the live idle / used / bounds while registered") {
    val state        = PoolMetricsState(idleCount = 3L, usedCount = 2L, pendingRequestCount = 1L)
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
      if spec.kind == InstrumentKind.Histogram then
        assertEquals(boundaries(metrics, spec.name), spec.boundaries, s"boundaries of ${ spec.name }")
    }
  }

  test("registerPoolStateCallback registers exactly the gauges listed in DbMetricSpecs.poolStateGauges") {
    val state        = PoolMetricsState(1L, 1L, 1L)
    val (metrics, _) = withMetrics { (provider, collect) =>
      for
        meter   <- provider.meter("ldbc").get
        metrics <- meter.databaseMetrics.use { databaseMetrics =>
                     databaseMetrics.registerPoolStateCallback("p", 1, 2, ZIO.succeed(state)).use(_ => collect)
                   }
      yield metrics
    }
    assertEquals(metrics.map(_.getName).toSet, DbMetricSpecs.poolStateGauges.map(_.name).toSet)
  }

  test("a failed gauge registration closes the scope instead of leaking the gauges already registered") {
    val state                 = PoolMetricsState(3L, 2L, 1L)
    val (outcome, afterwards) = withZMeter { (zmeter, _) =>
      // the third registration is db.client.connection.idle.min; the first two must not survive it
      val failing = new FailingMeter(zmeter, failOn = 3)
      for
        meter  <- ZioTelemetry.meterProvider(failing).meter("ldbc").get
        result <- meter.databaseMetrics
                    .use(_.registerPoolStateCallback("p", 1, 2, ZIO.succeed(state)).use(_ => ZIO.unit))
                    .either
      yield result
    }
    assert(outcome.isLeft, s"the registration failure is propagated, but got $outcome")
    assertEquals(
      afterwards.map(_.getName),
      Nil,
      "the scope is closed, so the gauges registered before the failure are no longer observable"
    )
  }

  test("the observable gauges stop being reported once the callback resource is released") {
    val state        = PoolMetricsState(1L, 0L, 0L)
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
