/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

import scala.concurrent.duration.FiniteDuration

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

  /**
   * Returns a data source that establishes physical connections within the given budget.
   *
   * A connection pool calls this with its own acquisition timeout, so that the driver never spends
   * longer establishing one connection than the pool is prepared to wait for one.
   *
   * The budget is an **upper bound, not an assignment**. An implementation that already has a shorter
   * connect timeout keeps it. Pooling is orthogonal to how long a connection attempt may take, so
   * handing a data source to a pool must not loosen a limit its owner set: the same data source
   * behaves the same whether it is pooled or used directly. Relaxing it would also erase what a short
   * connect timeout is for — giving up quickly so that another attempt can be made sooner.
   *
   * Implementations that cannot bound connection establishment return themselves unchanged, which is
   * the default.
   *
   * @param timeout the longest this data source may spend establishing one physical connection
   * @return a data source bounded by `timeout`, or `this` when the implementation cannot bound it
   */
  def withConnectTimeout(timeout: FiniteDuration): DataSource[F] = this
