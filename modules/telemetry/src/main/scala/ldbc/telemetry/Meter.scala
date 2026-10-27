/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.telemetry

import ldbc.effect.Concurrent

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
   * Builds the database metric instruments backed by this meter.
   *
   * Instrument creation has no teardown of its own — the only thing with a lifetime is an observable
   * callback, and [[DatabaseMetrics.registerPoolStateCallback]] returns its own resource for that. Keeping
   * this a plain effect means callers may cache and share the result freely, which they could not do with a
   * resource whose release they would have to keep alive.
   */
  def databaseMetrics: F[DatabaseMetrics[F]]

object Meter:

  /** A meter whose metrics record nothing. */
  def noop[F[_]](using F: Concurrent[F]): Meter[F] = new Meter[F]:
    override def databaseMetrics: F[DatabaseMetrics[F]] = F.pure(DatabaseMetrics.noop)

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
