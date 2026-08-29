/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.otel4s

import cats.effect.IO

import munit.CatsEffectSuite

import org.typelevel.otel4s.trace.TracerProvider as OtelTracerProvider
import org.typelevel.otel4s.sdk.testkit.OpenTelemetrySdkTestkit
import org.typelevel.otel4s.sdk.trace.data.StatusData

import ldbc.sql.Attribute

import ldbc.telemetry.{ Meter, StatusCode, TracerProvider }

class Otel4sTelemetryTest extends CatsEffectSuite:

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
        spans  <- testkit.finishedSpans
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
        spans  <- testkit.finishedSpans
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
        spans  <- testkit.finishedSpans
      yield
        val which = spans.map(s => s.name -> s.attributes.elements.toList.find(_.key.name == "which").map(_.value)).toMap
        assertEquals(which.get("Outer"), Some(Some("outer")))
        assertEquals(which.get("Inner"), Some(Some("inner")))
    }
  }

  test("the derived given bridges an otel4s TracerProvider to the ldbc SPI, and meterProvider yields a noop Meter") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      given OtelTracerProvider[IO]     = testkit.tracerProvider
      val provider: TracerProvider[IO] = Otel4sTelemetry.derivedTracerProvider[IO]
      for
        tracer <- provider.tracer("ldbc").get
        _      <- tracer.span("Query").use(_ => IO.unit)
        meter  <- Otel4sTelemetry.meterProvider(testkit.meterProvider).meter("ldbc").withVersion("v").get
        spans  <- testkit.finishedSpans
      yield
        assertEquals(spans.map(_.name), List("Query"))
        assertEquals(meter, Meter.noop)
    }
  }
