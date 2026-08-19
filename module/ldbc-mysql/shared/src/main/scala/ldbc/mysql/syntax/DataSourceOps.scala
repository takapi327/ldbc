/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.syntax

import ldbc.sql.{ Connection, DataSource }

import ldbc.fx.Fx

/**
 * Consumption helper for an [[ldbc.sql.DataSource]] over the [[ldbc.fx.Fx]] effect.
 *
 * `DataSource.getConnection` returns the connection in "allocated" form `(connection, release)`, which
 * must be released with a bracket to stay safe under cancellation. This extension performs that bracket
 * so callers keep a concise `ds.use { conn => ... }` while the release runs on every outcome.
 */
trait DataSourceOps:

  extension (ds: DataSource[Fx])
    def use[B](f: Connection[Fx] => Fx[B]): Fx[B] =
      Fx.bracket(ds.getConnection)((pair: (Connection[Fx], Fx[Unit])) => f(pair._1))(
        (pair: (
          Connection[Fx],
          Fx[Unit]
        )) => pair._2
      )
