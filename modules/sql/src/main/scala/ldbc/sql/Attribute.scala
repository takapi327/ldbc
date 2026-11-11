/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

/**
 * A typed telemetry attribute — a key paired with its value. This is the CE-free, database-agnostic
 * replacement for otel4s's `Attribute`, carrying just enough structure for a driver's tracing and
 * metrics SPI. It lives in `ldbc-sql` so every driver (and the shared exception model) can produce
 * attributes without depending on any observability library; the real OpenTelemetry mapping is
 * supplied later by an observability bridge.
 *
 * @param key   the attribute name (e.g. `"db.query.text"`)
 * @param value the attribute value
 * @tparam A the value type (typically `String`, `Long`, `Boolean`, or `Double`)
 */
final case class Attribute[A](key: String, value: A)

/**
 * A typed attribute key that produces an [[Attribute]] when applied to a value. Mirrors otel4s's
 * `AttributeKey`, letting semantic-convention keys be written as `Key(value)`.
 *
 * @param name the attribute name
 * @tparam A the value type the key expects
 */
final class AttributeKey[A](val name: String):

  /**
   * Builds an [[Attribute]] for this key with the given value.
   *
   * @param value the attribute value
   */
  def apply(value: A): Attribute[A] = Attribute(name, value)
