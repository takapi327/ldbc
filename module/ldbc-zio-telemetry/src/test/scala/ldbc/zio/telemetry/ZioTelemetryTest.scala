/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.zio.telemetry

import scala.jdk.CollectionConverters.*

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.StatusCode as JStatusCode
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter

import zio.{ Runtime, Task, Unsafe, ZIO, ZLayer }
import zio.telemetry.opentelemetry.OpenTelemetry
import zio.telemetry.opentelemetry.tracing.Tracing

import ldbc.sql.Attribute

import ldbc.telemetry.{ Span, StatusCode, TracerProvider }

class ZioTelemetryTest extends munit.FunSuite:

  private def withTracing[A](use: TracerProvider[Task] => Task[A]): (A, List[SpanData]) =
    val exporter = InMemorySpanExporter.create()
    val sdk      = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build()
    val program =
      ZIO
        .serviceWithZIO[Tracing](tracing => use(ZioTelemetry.tracerProvider(tracing)))
        .provide(ZLayer.succeed(sdk.get("ldbc")), OpenTelemetry.contextZIO, Tracing.live())
    val result = Unsafe.unsafe(implicit u => Runtime.default.unsafe.run(program).getOrThrowFiberFailure())
    (result, exporter.getFinishedSpanItems.asScala.toList)

  private def stringAttribute(span: SpanData, key: String): Option[String] =
    Option(span.getAttributes.get(AttributeKey.stringKey(key)))

  test("span name and all attribute value types are recorded natively by the zio-telemetry backend") {
    val (_, spans) = withTracing { provider =>
      for
        tracer <- provider.tracer("ldbc").withVersion("test").get
        _      <- tracer.span("Query", Attribute("db.system.name", "mysql")).use { span =>
                    span.addAttributes(
                      Attribute("s", "str"),
                      Attribute("i", 7),
                      Attribute("l", 42L),
                      Attribute("b", true),
                      Attribute("d", 1.5)
                    )
                  }
      yield ()
    }
    val attrs = spans.head.getAttributes
    assertEquals(spans.map(_.getName), List("Query"))
    assertEquals(attrs.get(AttributeKey.stringKey("db.system.name")), "mysql")
    assertEquals(attrs.get(AttributeKey.stringKey("s")), "str")
    assertEquals(attrs.get(AttributeKey.longKey("i")).longValue, 7L)
    assertEquals(attrs.get(AttributeKey.longKey("l")).longValue, 42L)
    assertEquals(attrs.get(AttributeKey.booleanKey("b")).booleanValue, true)
    assertEquals(attrs.get(AttributeKey.doubleKey("d")).doubleValue, 1.5)
  }

  test("explicit recordException and setStatus(Error) reach the underlying span") {
    val boom = new RuntimeException("boom")
    val (_, spans) = withTracing { provider =>
      for
        tracer <- provider.tracer("ldbc").get
        _      <- tracer.span("Query").use { (span: Span[Task]) =>
                    span.recordException(boom, Attribute("db.system.name", "mysql")) *>
                      span.setStatus(StatusCode.Error, "boom")
                  }
      yield ()
    }
    assertEquals(spans.head.getStatus.getStatusCode, JStatusCode.ERROR)
    assert(spans.head.getEvents.asScala.nonEmpty, "an exception event is recorded")
  }

  test("a failed span body is re-raised and the default StatusMapper marks the span ERROR") {
    val (exit, spans) = withTracing { provider =>
      for
        tracer <- provider.tracer("ldbc").get
        exit   <- tracer.span("Query").use(_ => ZIO.fail(new RuntimeException("boom"))).exit
      yield exit
    }
    assert(exit.isFailure, "the body failure is surfaced to the caller")
    assertEquals(spans.map(_.getName), List("Query"))
    assertEquals(spans.head.getStatus.getStatusCode, JStatusCode.ERROR)
  }

  test("StatusCode.Unset maps to the OpenTelemetry UNSET status") {
    val (_, spans) = withTracing { provider =>
      for
        tracer <- provider.tracer("ldbc").get
        _      <- tracer.span("Query").use(_.setStatus(StatusCode.Unset, ""))
      yield ()
    }
    assertEquals(spans.head.getStatus.getStatusCode, JStatusCode.UNSET)
  }

  test("nested spans keep attributes on their own span (current-span switches correctly)") {
    val (_, spans) = withTracing { provider =>
      for
        tracer <- provider.tracer("ldbc").get
        _      <- tracer.span("Outer").use { outer =>
                    outer.addAttribute(Attribute("which", "outer")) *>
                      tracer.span("Inner").use(inner => inner.addAttribute(Attribute("which", "inner")))
                  }
      yield ()
    }
    val which = spans.map(s => s.getName -> stringAttribute(s, "which")).toMap
    assertEquals(which.get("Outer"), Some(Some("outer")))
    assertEquals(which.get("Inner"), Some(Some("inner")))
  }

  test("concurrent spans do not cross-contaminate attributes (FiberRef context isolation)") {
    val (_, spans) = withTracing { provider =>
      for
        tracer <- provider.tracer("ldbc").get
        span    = (id: String) => tracer.span(s"Span-$id").use(_.addAttribute(Attribute("id", id)))
        _      <- span("a").zipPar(span("b"))
      yield ()
    }
    val byName = spans.map(s => s.getName -> stringAttribute(s, "id")).toMap
    assertEquals(byName.get("Span-a"), Some(Some("a")))
    assertEquals(byName.get("Span-b"), Some(Some("b")))
  }
