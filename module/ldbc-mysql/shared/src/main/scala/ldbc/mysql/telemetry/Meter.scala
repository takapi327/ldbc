/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.telemetry

import ldbc.fx.Fx

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
trait MeterBuilder:

  /** Sets the instrumentation version. */
  def withVersion(version: String): MeterBuilder

  /** Sets the schema URL. */
  def withSchemaUrl(schemaUrl: String): MeterBuilder

  /** Builds the meter. */
  def get: Fx[Meter]

/**
 * A provider of [[Meter]]s, mirroring otel4s's `MeterProvider`.
 */
trait MeterProvider:

  /**
   * Starts building a meter with the given instrumentation name.
   *
   * @param name the instrumentation scope name
   */
  def meter(name: String): MeterBuilder

object MeterProvider:

  /**
   * Summons the [[MeterProvider]] instance in scope.
   *
   * @param provider the instance
   */
  def apply(using provider: MeterProvider): MeterProvider = provider

  /** A provider that yields no-op meters. */
  val noop: MeterProvider = new MeterProvider:
    override def meter(name: String): MeterBuilder = new MeterBuilder:
      override def withVersion(version:     String): MeterBuilder = this
      override def withSchemaUrl(schemaUrl: String): MeterBuilder = this
      override def get:                              Fx[Meter]    = Fx.pure(Meter.noop)
