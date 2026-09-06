/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.otel4s

import scala.concurrent.duration.*

import cats.effect.IO

import org.typelevel.otel4s.sdk.metrics.data.{ MetricData, MetricPoints, PointData }
import org.typelevel.otel4s.sdk.testkit.OpenTelemetrySdkTestkit
import org.typelevel.otel4s.sdk.trace.data.StatusData
import org.typelevel.otel4s.trace.TracerProvider as OtelTracerProvider

import munit.CatsEffectSuite

import ldbc.sql.Attribute

import ldbc.catseffect.concurrentIO
import ldbc.telemetry.{ DbMetricSpecs, InstrumentKind, PoolMetricsState, StatusCode, TracerProvider }

class Otel4sTelemetryTest extends CatsEffectSuite:

  /** The single metric with the given name, failing with the collected names when it is absent. */
  private def metric(metrics: Iterable[MetricData], name: String)(using munit.Location): MetricData =
    metrics.find(_.name == name).getOrElse(fail(s"'$name' was not exported; got ${ metrics.map(_.name).toList }"))

  /** The aggregated stats of a histogram metric's single data point. */
  private def stats(metrics: Iterable[MetricData], name: String)(using
    munit.Location
  ): PointData.Histogram.Stats =
    metric(metrics, name).data match
      case histogram: MetricPoints.Histogram =>
        histogram.points.head.stats
          .getOrElse(fail(s"'$name' has no aggregated stats: ${ histogram.points }"))
      case other => fail(s"'$name' is not a histogram: $other")

  /** The explicit bucket boundaries a histogram metric was created with. */
  private def boundaries(metrics: Iterable[MetricData], name: String)(using munit.Location): List[Double] =
    metric(metrics, name).data match
      case histogram: MetricPoints.Histogram => histogram.points.head.boundaries.boundaries.toList
      case other                             => fail(s"'$name' is not a histogram: $other")

  /** The numeric data points of a counter / gauge metric. */
  private def points(metrics: Iterable[MetricData], name: String)(using
    munit.Location
  ): Vector[PointData.NumberPoint] =
    metric(metrics, name).data match
      case sum: MetricPoints.Sum     => sum.points.toVector
      case gauge: MetricPoints.Gauge => gauge.points.toVector
      case other                     => fail(s"'$name' has no numeric points: $other")

  /** The value of the single point of `name` whose attributes contain every one of `expected`. */
  private def pointValue(metrics: Iterable[MetricData], name: String, expected: (String, String)*)(using
    munit.Location
  ): Long =
    val matching = points(metrics, name).filter { point =>
      expected.forall { (key, value) =>
        point.attributes.exists(attribute => attribute.key.name == key && attribute.value == value)
      }
    }
    matching match
      case Vector(single) => single.value.asInstanceOf[Long]
      case other          =>
        fail(s"expected exactly one '$name' point matching ${ expected.toList }, got ${ other.map(_.attributes) }")

  test("span name, all attribute value types, and Ok status are recorded natively by the otel4s backend") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val provider: TracerProvider[IO] = Otel4sTelemetry.tracerProvider(testkit.tracerProvider)
      for
        tracer <- provider.tracer("ldbc").withVersion("test").get
        _      <- tracer.span("Query", Attribute("db.system.name", "mysql")).use { span =>
               span.addAttributes(
                 Attribute("s", "str"),
                 Attribute("i", 7),
                 Attribute("l", 42L),
                 Attribute("b", true),
                 Attribute("d", 1.5)
               ) *> span.setStatus(StatusCode.Ok, "ok")
             }
        spans <- testkit.finishedSpans
      yield
        val attrs = spans.head.attributes.elements.toList.map(a => a.key.name -> a.value).toMap
        assertEquals(spans.map(_.name), List("Query"))
        assertEquals(attrs.get("db.system.name"), Some("mysql"))
        assertEquals(attrs.get("s"), Some("str"))
        assertEquals(attrs.get("i"), Some(7L))
        assertEquals(attrs.get("l"), Some(42L))
        assertEquals(attrs.get("b"), Some(true))
        assertEquals(attrs.get("d"), Some(1.5))
        assertEquals(spans.head.status, StatusData.Ok)
    }
  }

  test("recordException and Error status propagate to the otel4s span") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val provider: TracerProvider[IO] = Otel4sTelemetry.tracerProvider(testkit.tracerProvider)
      val boom = new RuntimeException("boom")
      for
        tracer <- provider.tracer("ldbc").get
        _      <- tracer.span("Query").use { span =>
               span.recordException(boom, Attribute("db.system.name", "mysql")) *>
                 span.setStatus(StatusCode.Error, "boom")
             }
        spans <- testkit.finishedSpans
      yield
        assert(spans.head.events.elements.nonEmpty, "an exception event is recorded")
        assert(spans.head.status.isInstanceOf[StatusData.Error], s"expected Error status, got ${ spans.head.status }")
    }
  }

  test("a failure in the span body is re-raised and the span is still ended") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val provider: TracerProvider[IO] = Otel4sTelemetry.tracerProvider(testkit.tracerProvider)
      for
        tracer    <- provider.tracer("ldbc").get
        attempted <- tracer.span("Query").use(_ => IO.raiseError[Int](new RuntimeException("boom"))).attempt
        spans     <- testkit.finishedSpans
      yield
        assert(attempted.isLeft, "the body failure is re-raised to the caller")
        assertEquals(spans.map(_.name), List("Query"))
    }
  }

  test("StatusCode.Unset maps to the otel4s Unset status") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val provider: TracerProvider[IO] = Otel4sTelemetry.tracerProvider(testkit.tracerProvider)
      for
        tracer <- provider.tracer("ldbc").get
        _      <- tracer.span("Query").use(_.setStatus(StatusCode.Unset, ""))
        spans  <- testkit.finishedSpans
      yield assertEquals(spans.head.status, StatusData.Unset)
    }
  }

  test("nested spans keep attributes on their own span") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val provider: TracerProvider[IO] = Otel4sTelemetry.tracerProvider(testkit.tracerProvider)
      for
        tracer <- provider.tracer("ldbc").get
        _      <- tracer.span("Outer").use { outer =>
               outer.addAttribute(Attribute("which", "outer")) *>
                 tracer.span("Inner").use(inner => inner.addAttribute(Attribute("which", "inner")))
             }
        spans <- testkit.finishedSpans
      yield
        val which =
          spans.map(s => s.name -> s.attributes.elements.toList.find(_.key.name == "which").map(_.value)).toMap
        assertEquals(which.get("Outer"), Some(Some("outer")))
        assertEquals(which.get("Inner"), Some(Some("inner")))
    }
  }

  test("the derived given bridges an otel4s TracerProvider to the ldbc SPI") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      given OtelTracerProvider[IO] = testkit.tracerProvider
      val provider: TracerProvider[IO] = Otel4sTelemetry.derivedTracerProvider[IO]
      for
        tracer <- provider.tracer("ldbc").get
        _      <- tracer.span("Query").use(_ => IO.unit)
        spans  <- testkit.finishedSpans
      yield assertEquals(spans.map(_.name), List("Query"))
    }
  }

  test("operation metrics are recorded as real otel4s instruments, with the semconv names and units") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val meterProvider = Otel4sTelemetry.meterProvider(testkit.meterProvider)
      for
        meter   <- meterProvider.meter("ldbc").withVersion("v").get
        metrics <- meter.databaseMetrics.use { databaseMetrics =>
                     databaseMetrics.recordOperationDuration(
                       250.millis,
                       Attribute("db.system.name", "mysql"),
                       Attribute("server.port", 3306L)
                     ) *>
                       databaseMetrics.recordReturnedRows(42L, Attribute("db.system.name", "mysql")) *>
                       testkit.collectMetrics
                   }
      yield
        val byName = metrics.map(metric => metric.name -> metric.unit).toMap
        assertEquals(byName.get("db.client.operation.duration"), Some(Some("s")))
        assertEquals(byName.get("db.client.response.returned_rows"), Some(Some("{row}")))

        // the duration is exported in seconds, so 250ms must land as 0.25 and not as 250
        val duration = stats(metrics, "db.client.operation.duration")
        assertEqualsDouble(duration.sum, 0.25, 1e-9)
        assertEquals(duration.count, 1L)

        val rows = stats(metrics, "db.client.response.returned_rows")
        assertEqualsDouble(rows.sum, 42.0, 1e-9)
        assertEquals(rows.count, 1L)
    }
  }

  test("durations are converted to seconds across every duration instrument") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val meterProvider = Otel4sTelemetry.meterProvider(testkit.meterProvider)
      for
        meter   <- meterProvider.meter("ldbc").get
        metrics <- meter.databaseMetrics.use { databaseMetrics =>
                     databaseMetrics.recordConnectionCreateTime(1500.millis, "p") *>
                       databaseMetrics.recordConnectionWaitTime(2.seconds, "p") *>
                       databaseMetrics.recordConnectionUseTime(500.micros, "p") *>
                       testkit.collectMetrics
                   }
      yield
        assertEqualsDouble(stats(metrics, "db.client.connection.create_time").sum, 1.5, 1e-9)
        assertEqualsDouble(stats(metrics, "db.client.connection.wait_time").sum, 2.0, 1e-9)
        assertEqualsDouble(stats(metrics, "db.client.connection.use_time").sum, 0.0005, 1e-9)
    }
  }

  test("the histograms are created with the bucket boundaries from the semantic conventions") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val meterProvider = Otel4sTelemetry.meterProvider(testkit.meterProvider)
      for
        meter   <- meterProvider.meter("ldbc").get
        metrics <- meter.databaseMetrics.use { databaseMetrics =>
                     databaseMetrics.recordOperationDuration(1.second) *>
                       databaseMetrics.recordReturnedRows(1L) *>
                       databaseMetrics.recordConnectionWaitTime(1.second, "p") *>
                       testkit.collectMetrics
                   }
      yield
        assertEquals(
          boundaries(metrics, "db.client.operation.duration"),
          List(0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0, 10.0)
        )
        assertEquals(
          boundaries(metrics, "db.client.response.returned_rows"),
          List(1.0, 2.0, 5.0, 10.0, 20.0, 50.0, 100.0, 200.0, 500.0, 1000.0, 2000.0, 5000.0, 10000.0)
        )
        assertEquals(
          boundaries(metrics, "db.client.connection.wait_time"),
          List(0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0, 10.0)
        )
    }
  }

  test("an attribute value type the mapper does not special-case is exported as its string form") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val meterProvider = Otel4sTelemetry.meterProvider(testkit.meterProvider)
      for
        meter   <- meterProvider.meter("ldbc").get
        metrics <- meter.databaseMetrics.use { databaseMetrics =>
                     databaseMetrics.recordOperationDuration(1.second, Attribute("weird", List(1, 2))) *>
                       testkit.collectMetrics
                   }
      yield
        val attributes =
          metric(metrics, "db.client.operation.duration").data.points.toVector.flatMap(_.attributes.toVector)
        assert(
          attributes.exists(attribute => attribute.key.name == "weird" && attribute.value == "List(1, 2)"),
          s"the unsupported value is stringified, but got $attributes"
        )
    }
  }

  test("attributes passed to the metrics SPI reach the exported data point") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val meterProvider = Otel4sTelemetry.meterProvider(testkit.meterProvider)
      for
        meter   <- meterProvider.meter("ldbc").get
        metrics <- meter.databaseMetrics.use { databaseMetrics =>
                     databaseMetrics.recordOperationDuration(1.second, Attribute("db.operation.name", "SELECT")) *>
                       testkit.collectMetrics
                   }
      yield
        val duration = metrics.find(_.name == "db.client.operation.duration")
        assert(duration.isDefined, "db.client.operation.duration was exported")
        assert(
          duration.get.data.points.exists(_.attributes.exists { attribute =>
            attribute.key.name == "db.operation.name" && attribute.value == "SELECT"
          }),
          s"the operation attribute is attached to the data point: ${ duration.get.data.points }"
        )
    }
  }

  test("connection pool metrics are recorded and the observable gauges report the pool state") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val meterProvider = Otel4sTelemetry.meterProvider(testkit.meterProvider)
      val state         = PoolMetricsState(idleCount = 3L, usedCount = 2L, pendingRequestCount = 1L)
      for
        meter   <- meterProvider.meter("ldbc").get
        metrics <- meter.databaseMetrics.use { databaseMetrics =>
                     databaseMetrics.recordConnectionCreateTime(10.millis, "ldbc-pool") *>
                       databaseMetrics.recordConnectionWaitTime(5.millis, "ldbc-pool") *>
                       databaseMetrics.recordConnectionUseTime(20.millis, "ldbc-pool") *>
                       databaseMetrics.recordConnectionTimeout("ldbc-pool") *>
                       databaseMetrics
                         .registerPoolStateCallback("ldbc-pool", 1, 10, IO.pure(state))
                         .use(_ => testkit.collectMetrics)
                   }
      yield
        val pool = "db.client.connection.pool.name" -> "ldbc-pool"

        // the three pool histograms and the timeout counter
        assertEquals(stats(metrics, "db.client.connection.create_time").count, 1L)
        assertEquals(stats(metrics, "db.client.connection.wait_time").count, 1L)
        assertEquals(stats(metrics, "db.client.connection.use_time").count, 1L)
        assertEquals(pointValue(metrics, "db.client.connection.timeouts", pool), 1L)

        // idle and used share one instrument and are told apart by db.client.connection.state
        assertEquals(
          pointValue(metrics, "db.client.connection.count", pool, "db.client.connection.state" -> "idle"),
          3L
        )
        assertEquals(
          pointValue(metrics, "db.client.connection.count", pool, "db.client.connection.state" -> "used"),
          2L
        )

        // the bounds are reported from the pool configuration: min = 1, max = 10
        assertEquals(pointValue(metrics, "db.client.connection.idle.min", pool), 1L)
        assertEquals(pointValue(metrics, "db.client.connection.idle.max", pool), 10L)
        assertEquals(pointValue(metrics, "db.client.connection.max", pool), 10L)
        assertEquals(pointValue(metrics, "db.client.connection.pending_requests", pool), 1L)
    }
  }

  test("the meter builder's version and schema URL reach the exported instrumentation scope") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val meterProvider = Otel4sTelemetry.meterProvider(testkit.meterProvider)
      for
        meter   <- meterProvider.meter("ldbc").withVersion("1.2.3").withSchemaUrl("https://example.test/schema").get
        metrics <- meter.databaseMetrics.use { databaseMetrics =>
                     databaseMetrics.recordOperationDuration(1.second) *> testkit.collectMetrics
                   }
      yield
        val scope = metric(metrics, "db.client.operation.duration").instrumentationScope
        assertEquals(scope.name, "ldbc")
        assertEquals(scope.version, Some("1.2.3"))
        assertEquals(scope.schemaUrl, Some("https://example.test/schema"))
    }
  }

  test("the observable pool gauges stop being reported once the callback resource is released") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val meterProvider = Otel4sTelemetry.meterProvider(testkit.meterProvider)
      val state         = PoolMetricsState(idleCount = 1L, usedCount = 0L, pendingRequestCount = 0L)
      for
        meter   <- meterProvider.meter("ldbc").get
        metrics <- meter.databaseMetrics.use { databaseMetrics =>
                     databaseMetrics
                       .registerPoolStateCallback("ldbc-pool", 1, 10, IO.pure(state))
                       .use(_ => IO.unit) *> testkit.collectMetrics
                   }
      yield assert(
        !metrics.map(_.name).contains("db.client.connection.count"),
        s"the gauge is unregistered on release, but got ${ metrics.map(_.name) }"
      )
    }
  }

  test("every instrument matches the shared DbMetricSpecs definition") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val meterProvider = Otel4sTelemetry.meterProvider(testkit.meterProvider)
      val state         = PoolMetricsState(1L, 1L, 1L)
      for
        meter   <- meterProvider.meter("ldbc").get
        metrics <- meter.databaseMetrics.use { databaseMetrics =>
                     databaseMetrics.recordOperationDuration(1.second) *>
                       databaseMetrics.recordReturnedRows(1L) *>
                       databaseMetrics.recordConnectionCreateTime(1.second, "p") *>
                       databaseMetrics.recordConnectionWaitTime(1.second, "p") *>
                       databaseMetrics.recordConnectionUseTime(1.second, "p") *>
                       databaseMetrics.recordConnectionTimeout("p") *>
                       databaseMetrics
                         .registerPoolStateCallback("p", 1, 2, IO.pure(state))
                         .use(_ => testkit.collectMetrics)
                   }
      yield DbMetricSpecs.all.foreach { spec =>
        val exported = metric(metrics, spec.name)
        assertEquals(exported.unit, Some(spec.unit), s"unit of ${ spec.name }")
        assertEquals(exported.description, Some(spec.description), s"description of ${ spec.name }")
        if spec.kind == InstrumentKind.Histogram then
          assertEquals(boundaries(metrics, spec.name), spec.boundaries, s"boundaries of ${ spec.name }")
      }
    }
  }

  test("registerPoolStateCallback registers exactly the gauges listed in DbMetricSpecs.poolStateGauges") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val meterProvider = Otel4sTelemetry.meterProvider(testkit.meterProvider)
      val state         = PoolMetricsState(1L, 1L, 1L)
      for
        meter   <- meterProvider.meter("ldbc").get
        metrics <- meter.databaseMetrics.use { databaseMetrics =>
                     databaseMetrics
                       .registerPoolStateCallback("p", 1, 2, IO.pure(state))
                       .use(_ => testkit.collectMetrics)
                   }
      yield assertEquals(metrics.map(_.name).toSet, DbMetricSpecs.poolStateGauges.map(_.name).toSet)
    }
  }

  test("a Meter.noop-backed DatabaseMetrics accepts every call and exports nothing") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      ldbc.telemetry.Meter
        .noop[IO]
        .databaseMetrics
        .use(_.recordOperationDuration(1.second) *> testkit.collectMetrics)
        .map(metrics => assertEquals(metrics.toList, Nil))
    }
  }
