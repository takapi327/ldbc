/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.telemetry

import ldbc.effect.Concurrent

/**
 * A metrics meter, mirroring otel4s's `Meter`. In the CE-free core it is an opaque handle: the driver
 * obtains one and passes it to [[DatabaseMetrics.fromMeter]], which in the core produces a no-op
 * tracker. A real metrics backend is wired later by the observability bridge.
 */
trait Meter

object Meter:

  /** A meter that records nothing. */
  val noop: Meter = new Meter {}

/**
 * A builder for a [[Meter]], mirroring otel4s's meter builder.
 */
trait MeterBuilder[F[_]]:

  /** Sets the instrumentation version. */
  def withVersion(version: String): MeterBuilder[F]

  /** Sets the schema URL. */
  def withSchemaUrl(schemaUrl: String): MeterBuilder[F]

  /** Builds the meter. */
  def get: F[Meter]

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
      override def get:                              F[Meter]        = F.pure(Meter.noop)
