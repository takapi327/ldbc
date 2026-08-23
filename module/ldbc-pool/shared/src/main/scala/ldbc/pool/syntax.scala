/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import ldbc.effect.Async

import ldbc.sql.{ Connection, DataSource }

/**
 * Consumption helper for an [[ldbc.sql.DataSource]] over any effect `F` with an [[ldbc.effect.Async]]
 * instance: `use` acquires a connection, runs `f`, and releases the connection afterwards (on success
 * and error), all within a single `bracket`.
 */
extension [F[_]](ds: DataSource[F])
  def use[B](f: Connection[F] => F[B])(using F: Async[F]): F[B] =
    F.bracket(ds.getConnection)((pair: (Connection[F], F[Unit])) => f(pair._1))((pair: (Connection[F], F[Unit])) =>
      pair._2
    )
