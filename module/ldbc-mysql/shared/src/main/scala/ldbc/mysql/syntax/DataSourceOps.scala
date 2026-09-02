/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.syntax

import ldbc.sql.{ Connection, DataSource }

import ldbc.effect.Async

/**
 * Consumption helper for an [[ldbc.sql.DataSource]] over any effect `F` with an `Async` instance.
 *
 * `DataSource.getConnection` returns the connection in "allocated" form `(connection, release)`, which
 * must be released with a bracket to stay safe under cancellation. This extension performs that bracket
 * so callers keep a concise `ds.use { conn => ... }` while the release runs on every outcome.
 */
trait DataSourceOps:

  extension [F[_]](ds: DataSource[F])(using F: Async[F])
    def use[B](f: Connection[F] => F[B]): F[B] =
      F.bracket(ds.getConnection)((pair: (Connection[F], F[Unit])) => f(pair._1))(
        (pair: (
          Connection[F],
          F[Unit]
        )) => pair._2
      )
