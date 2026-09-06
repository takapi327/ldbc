/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.telemetry

import ldbc.effect.{ Concurrent, Resource }

/**
 * The metrics entry point of the telemetry SPI: a handle that can build the driver's [[DatabaseMetrics]]
 * instruments.
 *
 * Unlike tracing — where the driver annotates spans with arbitrary names and attributes at arbitrary
 * points, so the SPI has to mirror a general `Tracer` / `Span` shape — the metrics the driver emits are a
 * closed set fixed by the OpenTelemetry database semantic conventions. The SPI therefore does not mirror a
 * general instrument API: a backend only has to produce a [[DatabaseMetrics]], and it expresses the
 * instrument names, units, descriptions and bucket boundaries with whatever its metrics library provides
 * natively.
 *
 * @tparam F the effect type
 */
trait Meter[F[_]]:

  /**
   * Builds the database metric instruments backed by this meter. Instruments are created when the resource
   * is acquired, and any observable callback registered through
   * [[DatabaseMetrics.registerPoolStateCallback]] is unregistered when it is released.
   */
  def databaseMetrics: Resource[F, DatabaseMetrics[F]]

object Meter:

  /** A meter whose metrics record nothing. */
  def noop[F[_]](using F: Concurrent[F]): Meter[F] = new Meter[F]:
    override def databaseMetrics: Resource[F, DatabaseMetrics[F]] = Resource.pure(DatabaseMetrics.noop)

/**
 * A builder for a [[Meter]], mirroring otel4s's meter builder.
 */
trait MeterBuilder[F[_]]:

  /** Sets the instrumentation version. */
  def withVersion(version: String): MeterBuilder[F]

  /** Sets the schema URL. */
  def withSchemaUrl(schemaUrl: String): MeterBuilder[F]

  /** Builds the meter. */
  def get: F[Meter[F]]

/**
 * A provider of [[Meter]]s, mirroring otel4s's `MeterProvider`.
 */
trait MeterProvider[F[_]]:

  /**
   * Starts building a meter with the given instrumentation name.
   *
   * @param name the instrumentation scope name
   */
  def meter(name: String): MeterBuilder[F]

object MeterProvider:

  /**
   * Summons the [[MeterProvider]] instance in scope.
   *
   * @param provider the instance
   */
  def apply[F[_]](using provider: MeterProvider[F]): MeterProvider[F] = provider

  /** A provider that yields no-op meters. */
  def noop[F[_]](using F: Concurrent[F]): MeterProvider[F] = new MeterProvider[F]:
    override def meter(name: String): MeterBuilder[F] = new MeterBuilder[F]:
      override def withVersion(version:     String): MeterBuilder[F] = this
      override def withSchemaUrl(schemaUrl: String): MeterBuilder[F] = this
      override def get:                              F[Meter[F]]     = F.pure(Meter.noop)
