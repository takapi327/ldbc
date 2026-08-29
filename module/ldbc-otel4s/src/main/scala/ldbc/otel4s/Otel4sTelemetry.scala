/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.otel4s

import cats.Functor
import cats.syntax.functor.*

import org.typelevel.otel4s.Attribute as OtelAttribute
import org.typelevel.otel4s.trace as oteltrace
import org.typelevel.otel4s.metrics as otelmetrics

import ldbc.sql.Attribute

import ldbc.telemetry.*

/**
 * otel4s-backed implementation of the DB-agnostic `ldbc.telemetry` SPI.
 *
 * Because the SPI mirrors otel4s's own `Tracer` / `Span` / `SpanOps` shape, each operation delegates directly
 * to the corresponding otel4s effect — spans run natively on the caller's `F` (`IO`) with no intermediate
 * effect bridge. The only translation is `ldbc.sql.Attribute` to `org.typelevel.otel4s.Attribute`.
 *
 * The metrics side is a thin pass-through: the SPI's [[ldbc.telemetry.Meter]] is an opaque handle, so an
 * otel4s meter is obtained but mapped to [[ldbc.telemetry.Meter.noop]] until the metrics SPI is fleshed out.
 */
object Otel4sTelemetry:

  private def toOtelAttribute(attribute: Attribute[?]): OtelAttribute[?] =
    (attribute.value: Any) match
      case value: String  => OtelAttribute(attribute.key, value)
      case value: Long    => OtelAttribute(attribute.key, value)
      case value: Int     => OtelAttribute(attribute.key, value.toLong)
      case value: Boolean => OtelAttribute(attribute.key, value)
      case value: Double  => OtelAttribute(attribute.key, value)
      case value          => OtelAttribute(attribute.key, value.toString)

  private def toOtelStatus(status: StatusCode): oteltrace.StatusCode = status match
    case StatusCode.Unset => oteltrace.StatusCode.Unset
    case StatusCode.Ok    => oteltrace.StatusCode.Ok
    case StatusCode.Error => oteltrace.StatusCode.Error

  private def wrapSpan[F[_]](span: oteltrace.Span[F]): Span[F] = new Span[F]:
    override def addAttribute(attribute: Attribute[?]): F[Unit] =
      span.addAttribute(toOtelAttribute(attribute))
    override def addAttributes(attributes: Attribute[?]*): F[Unit] =
      span.addAttributes(attributes.map(toOtelAttribute)*)
    override def recordException(exception: Throwable, attributes: Attribute[?]*): F[Unit] =
      span.recordException(exception, attributes.map(toOtelAttribute)*)
    override def setStatus(status: StatusCode, description: String): F[Unit] =
      span.setStatus(toOtelStatus(status), description)

  private def wrapSpanOps[F[_]](spanOps: oteltrace.SpanOps[F]): SpanOps[F] = new SpanOps[F]:
    override def use[A](f: Span[F] => F[A]): F[A] = spanOps.use(span => f(wrapSpan(span)))

  private def wrapTracer[F[_]](tracer: oteltrace.Tracer[F]): Tracer[F] = new Tracer[F]:
    override def span(name: String, attributes: Attribute[?]*): SpanOps[F] =
      wrapSpanOps(tracer.span(name, attributes.map(toOtelAttribute)*))

  private def wrapTracerBuilder[F[_]: Functor](builder: oteltrace.TracerBuilder[F]): TracerBuilder[F] =
    new TracerBuilder[F]:
      override def withVersion(version: String): TracerBuilder[F] =
        wrapTracerBuilder(builder.withVersion(version))
      override def withSchemaUrl(schemaUrl: String): TracerBuilder[F] =
        wrapTracerBuilder(builder.withSchemaUrl(schemaUrl))
      override def get: F[Tracer[F]] = builder.get.map(wrapTracer)

  private def wrapMeterBuilder[F[_]: Functor](builder: otelmetrics.MeterBuilder[F]): MeterBuilder[F] =
    new MeterBuilder[F]:
      override def withVersion(version: String): MeterBuilder[F] =
        wrapMeterBuilder(builder.withVersion(version))
      override def withSchemaUrl(schemaUrl: String): MeterBuilder[F] =
        wrapMeterBuilder(builder.withSchemaUrl(schemaUrl))
      override def get: F[Meter] = builder.get.as(Meter.noop)

  /** Wraps an otel4s `TracerProvider` as the `ldbc.telemetry.TracerProvider` the driver consumes. */
  def tracerProvider[F[_]: Functor](provider: oteltrace.TracerProvider[F]): TracerProvider[F] =
    new TracerProvider[F]:
      override def tracer(name: String): TracerBuilder[F] = wrapTracerBuilder(provider.tracer(name))

  /** Wraps an otel4s `MeterProvider` as the `ldbc.telemetry.MeterProvider` the driver consumes. */
  def meterProvider[F[_]: Functor](provider: otelmetrics.MeterProvider[F]): MeterProvider[F] =
    new MeterProvider[F]:
      override def meter(name: String): MeterBuilder[F] = wrapMeterBuilder(provider.meter(name))

  given derivedTracerProvider[F[_]: Functor](using provider: oteltrace.TracerProvider[F]): TracerProvider[F] =
    tracerProvider(provider)

  given derivedMeterProvider[F[_]: Functor](using provider: otelmetrics.MeterProvider[F]): MeterProvider[F] =
    meterProvider(provider)
