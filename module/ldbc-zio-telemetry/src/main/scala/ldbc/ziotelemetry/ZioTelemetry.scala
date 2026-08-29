/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.ziotelemetry

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.StatusCode as JStatusCode

import zio.{ Task, ZIO }
import zio.telemetry.opentelemetry.tracing.Tracing

import ldbc.sql.Attribute

import ldbc.telemetry.*

/**
 * zio-telemetry-backed implementation of the DB-agnostic `ldbc.telemetry` SPI, running natively on ZIO's
 * `Task` (no `zio-interop-cats`). Because zio-telemetry wraps the JVM-only OpenTelemetry Java SDK, this
 * adapter is JVM only.
 *
 * A span is opened with `Tracing.span`, whose default `StatusMapper` already marks the span `ERROR` when the
 * body fails. The SPI's imperative `Span` operations (`addAttribute` / `recordException` / `setStatus`) run
 * against the current span — `setAttribute` via zio-telemetry, and exception/status via the underlying
 * OpenTelemetry Java span obtained from `getCurrentSpanUnsafe`.
 *
 * The provided [[zio.telemetry.opentelemetry.tracing.Tracing]] already fixes the instrumentation name and
 * version, so [[ldbc.telemetry.TracerBuilder]]'s `withVersion` / `withSchemaUrl` and `tracer`'s name are
 * accepted but ignored.
 */
object ZioTelemetry:

  private def toJavaAttributes(attributes: Seq[Attribute[?]]): Attributes =
    val builder = Attributes.builder()
    attributes.foreach { attribute =>
      (attribute.value: Any) match
        case value: String  => builder.put(attribute.key, value)
        case value: Long    => builder.put(attribute.key, value)
        case value: Int     => builder.put(attribute.key, value.toLong)
        case value: Boolean => builder.put(attribute.key, value)
        case value: Double  => builder.put(attribute.key, value)
        case value          => builder.put(attribute.key, value.toString)
    }
    builder.build()

  private def toJavaStatus(status: StatusCode): JStatusCode = status match
    case StatusCode.Unset => JStatusCode.UNSET
    case StatusCode.Ok    => JStatusCode.OK
    case StatusCode.Error => JStatusCode.ERROR

  private def wrapSpan(tracing: Tracing): Span[Task] = new Span[Task]:
    override def addAttribute(attribute: Attribute[?]): Task[Unit] =
      (attribute.value: Any) match
        case value: String  => tracing.setAttribute(attribute.key, value)
        case value: Long    => tracing.setAttribute(attribute.key, value)
        case value: Int     => tracing.setAttribute(attribute.key, value.toLong)
        case value: Boolean => tracing.setAttribute(attribute.key, value)
        case value: Double  => tracing.setAttribute(attribute.key, value)
        case value          => tracing.setAttribute(attribute.key, value.toString)

    override def addAttributes(attributes: Attribute[?]*): Task[Unit] =
      ZIO.foreachDiscard(attributes)(addAttribute)

    override def recordException(exception: Throwable, attributes: Attribute[?]*): Task[Unit] =
      tracing.getCurrentSpanUnsafe.map(_.recordException(exception, toJavaAttributes(attributes))).unit

    override def setStatus(status: StatusCode, description: String): Task[Unit] =
      tracing.getCurrentSpanUnsafe.map(_.setStatus(toJavaStatus(status), description)).unit

  private def wrapTracer(tracing: Tracing): Tracer[Task] = new Tracer[Task]:
    override def span(name: String, attributes: Attribute[?]*): SpanOps[Task] = new SpanOps[Task]:
      override def use[A](f: Span[Task] => Task[A]): Task[A] =
        tracing.span(name, attributes = toJavaAttributes(attributes))(f(wrapSpan(tracing)))

  /** Wraps a zio-telemetry `Tracing` as the `ldbc.telemetry.TracerProvider` the driver consumes. */
  def tracerProvider(tracing: Tracing): TracerProvider[Task] = new TracerProvider[Task]:
    override def tracer(name: String): TracerBuilder[Task] = new TracerBuilder[Task]:
      override def withVersion(version:     String): TracerBuilder[Task] = this
      override def withSchemaUrl(schemaUrl: String): TracerBuilder[Task] = this
      override def get:                              Task[Tracer[Task]]  = ZIO.succeed(wrapTracer(tracing))
