/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

/**
 * A factory for database connections, abstracting over the connection lifecycle without
 * committing to any particular effect system's resource type.
 *
 * DataSource is a fundamental abstraction in ldbc that encapsulates the logic for establishing
 * database connections. It provides a uniform interface for obtaining connections regardless of
 * the underlying implementation (JDBC, native MySQL protocol, pooled, ...).
 *
 * Rather than returning an effect-specific resource (such as `cats.effect.Resource` or
 * `ldbc.fx.Resource`), [[getConnection]] returns the connection together with its release action
 * in "allocated" form. This keeps the abstraction dependent only on the effect type constructor
 * `F[_]` and [[ldbc.sql.Connection]], so a single `DataSource[F]` can be shared across every effect
 * system (Cats Effect, Fx, ZIO, ...).
 *
 * Callers are responsible for running the release action; the recommended way to consume a
 * DataSource is a bracket-based `use` helper that guarantees release on success, error, and
 * cancellation.
 *
 * @tparam F the effect type (e.g. cats.effect.IO, ldbc.fx.Fx, ...) that wraps the operations
 */
trait DataSource[F[_]]:

  /**
   * Establishes a new database connection and returns it together with its release action.
   *
   * The returned tuple is `(connection, release)`:
   * - `connection` is ready for executing SQL statements.
   * - `release` closes the connection / returns it to the pool and must be run exactly once by the
   *   caller (typically via a bracket that guarantees it on all outcomes).
   *
   * Each call may return a new connection or a pooled connection depending on the implementation.
   * Users should not assume connection identity or state between calls.
   *
   * @return an effect producing the acquired connection and its release action
   */
  def getConnection: F[(Connection[F], F[Unit])]
