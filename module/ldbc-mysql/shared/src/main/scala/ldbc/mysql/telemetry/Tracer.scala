/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.telemetry

import ldbc.sql.Attribute

import ldbc.effect.Concurrent

/**
 * The status of a span, mirroring otel4s's `StatusCode`.
 */
enum StatusCode:
  /** No status has been set. */
  case Unset

  /** The operation completed successfully. */
  case Ok

  /** The operation failed. */
  case Error

/**
 * A tracing span the driver annotates while executing an operation. This is the CE-free SPI replacing
 * otel4s's `Span`; the default ([[Span.noop]]) does nothing, and a real tracing backend is wired later
 * by the observability bridge.
 */
trait Span[F[_]]:

  /**
   * Adds a single attribute to the span.
   *
   * @param attribute the attribute to add
   */
  def addAttribute(attribute: Attribute[?]): F[Unit]

  /**
   * Adds several attributes to the span.
   *
   * @param attributes the attributes to add
   */
  def addAttributes(attributes: Attribute[?]*): F[Unit]

  /**
   * Records an exception on the span.
   *
   * @param exception  the exception to record
   * @param attributes additional attributes describing the exception
   */
  def recordException(exception: Throwable, attributes: Attribute[?]*): F[Unit]

  /**
   * Sets the span's status.
   *
   * @param status      the status code
   * @param description a human-readable description of the status
   */
  def setStatus(status: StatusCode, description: String): F[Unit]

object Span:

  /** A span that ignores all annotations. */
  def noop[F[_]](using F: Concurrent[F]): Span[F] = new Span[F]:
    override def addAttribute(attribute:    Attribute[?]):                           F[Unit] = F.unit
    override def addAttributes(attributes:  Attribute[?]*):                          F[Unit] = F.unit
    override def recordException(exception: Throwable, attributes:   Attribute[?]*): F[Unit] = F.unit
    override def setStatus(status:          StatusCode, description: String):        F[Unit] = F.unit

/**
 * A handle to a not-yet-started span, mirroring otel4s's `SpanOps`. [[use]] runs the body with the
 * span in scope.
 */
trait SpanOps[F[_]]:

  /**
   * Runs `f` with the span, returning its result.
   *
   * @param f the body to run with the span
   * @tparam A the result type
   */
  def use[A](f: Span[F] => F[A]): F[A]

/**
 * Creates spans around database operations. This is the CE-free SPI replacing otel4s's `Tracer`; the
 * default ([[Tracer.noop]]) runs bodies with a no-op span.
 */
trait Tracer[F[_]]:

  /**
   * Prepares a span with the given name and initial attributes.
   *
   * @param name       the span name
   * @param attributes initial attributes
   */
  def span(name: String, attributes: Attribute[?]*): SpanOps[F]

object Tracer:

  /**
   * Summons the [[Tracer]] instance in scope.
   *
   * @param tracer the instance
   */
  def apply[F[_]](using tracer: Tracer[F]): Tracer[F] = tracer

  /** A tracer that creates no-op spans. */
  def noop[F[_]](using F: Concurrent[F]): Tracer[F] = new Tracer[F]:
    override def span(name: String, attributes: Attribute[?]*): SpanOps[F] = new SpanOps[F]:
      override def use[A](f: Span[F] => F[A]): F[A] = f(Span.noop)

/**
 * A builder for a [[Tracer]], mirroring otel4s's tracer builder.
 */
trait TracerBuilder[F[_]]:

  /** Sets the instrumentation version. */
  def withVersion(version: String): TracerBuilder[F]

  /** Sets the schema URL. */
  def withSchemaUrl(schemaUrl: String): TracerBuilder[F]

  /** Builds the tracer. */
  def get: F[Tracer[F]]

/**
 * A provider of [[Tracer]]s, mirroring otel4s's `TracerProvider`.
 */
trait TracerProvider[F[_]]:

  /**
   * Starts building a tracer with the given instrumentation name.
   *
   * @param name the instrumentation scope name
   */
  def tracer(name: String): TracerBuilder[F]

object TracerProvider:

  /**
   * Summons the [[TracerProvider]] instance in scope.
   *
   * @param provider the instance
   */
  def apply[F[_]](using provider: TracerProvider[F]): TracerProvider[F] = provider

  /** A provider that yields no-op tracers. */
  def noop[F[_]](using F: Concurrent[F]): TracerProvider[F] = new TracerProvider[F]:
    override def tracer(name: String): TracerBuilder[F] = new TracerBuilder[F]:
      override def withVersion(version:     String): TracerBuilder[F] = this
      override def withSchemaUrl(schemaUrl: String): TracerBuilder[F] = this
      override def get:                              F[Tracer[F]]     = F.pure(Tracer.noop)
