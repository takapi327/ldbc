/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.telemetry

import ldbc.sql.Attribute

import ldbc.fx.Fx

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
trait Span:

  /**
   * Adds a single attribute to the span.
   *
   * @param attribute the attribute to add
   */
  def addAttribute(attribute: Attribute[?]): Fx[Unit]

  /**
   * Adds several attributes to the span.
   *
   * @param attributes the attributes to add
   */
  def addAttributes(attributes: Attribute[?]*): Fx[Unit]

  /**
   * Records an exception on the span.
   *
   * @param exception  the exception to record
   * @param attributes additional attributes describing the exception
   */
  def recordException(exception: Throwable, attributes: Attribute[?]*): Fx[Unit]

  /**
   * Sets the span's status.
   *
   * @param status      the status code
   * @param description a human-readable description of the status
   */
  def setStatus(status: StatusCode, description: String): Fx[Unit]

object Span:

  /** A span that ignores all annotations. */
  val noop: Span = new Span:
    override def addAttribute(attribute:    Attribute[?]):                           Fx[Unit] = Fx.unit
    override def addAttributes(attributes:  Attribute[?]*):                          Fx[Unit] = Fx.unit
    override def recordException(exception: Throwable, attributes:   Attribute[?]*): Fx[Unit] = Fx.unit
    override def setStatus(status:          StatusCode, description: String):        Fx[Unit] = Fx.unit

/**
 * A handle to a not-yet-started span, mirroring otel4s's `SpanOps`. [[use]] runs the body with the
 * span in scope.
 */
trait SpanOps:

  /**
   * Runs `f` with the span, returning its result.
   *
   * @param f the body to run with the span
   * @tparam A the result type
   */
  def use[A](f: Span => Fx[A]): Fx[A]

/**
 * Creates spans around database operations. This is the CE-free SPI replacing otel4s's `Tracer`; the
 * default ([[Tracer.noop]]) runs bodies with a no-op span.
 */
trait Tracer:

  /**
   * Prepares a span with the given name and initial attributes.
   *
   * @param name       the span name
   * @param attributes initial attributes
   */
  def span(name: String, attributes: Attribute[?]*): SpanOps

object Tracer:

  /**
   * Summons the [[Tracer]] instance in scope.
   *
   * @param tracer the instance
   */
  def apply(using tracer: Tracer): Tracer = tracer

  /** A tracer that creates no-op spans. */
  val noop: Tracer = new Tracer:
    override def span(name: String, attributes: Attribute[?]*): SpanOps = new SpanOps:
      override def use[A](f: Span => Fx[A]): Fx[A] = f(Span.noop)

/**
 * A builder for a [[Tracer]], mirroring otel4s's tracer builder.
 */
trait TracerBuilder:

  /** Sets the instrumentation version. */
  def withVersion(version: String): TracerBuilder

  /** Sets the schema URL. */
  def withSchemaUrl(schemaUrl: String): TracerBuilder

  /** Builds the tracer. */
  def get: Fx[Tracer]

/**
 * A provider of [[Tracer]]s, mirroring otel4s's `TracerProvider`.
 */
trait TracerProvider:

  /**
   * Starts building a tracer with the given instrumentation name.
   *
   * @param name the instrumentation scope name
   */
  def tracer(name: String): TracerBuilder

object TracerProvider:

  /**
   * Summons the [[TracerProvider]] instance in scope.
   *
   * @param provider the instance
   */
  def apply(using provider: TracerProvider): TracerProvider = provider

  /** A provider that yields no-op tracers. */
  val noop: TracerProvider = new TracerProvider:
    override def tracer(name: String): TracerBuilder = new TracerBuilder:
      override def withVersion(version:     String): TracerBuilder = this
      override def withSchemaUrl(schemaUrl: String): TracerBuilder = this
      override def get:                              Fx[Tracer]    = Fx.pure(Tracer.noop)
