/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.syntax

import cats.effect.kernel.MonadCancel

import ldbc.sql.{ Connection, DataSource }

/**
 * Consumption helper for an [[ldbc.sql.DataSource]] over a Cats Effect `F`.
 *
 * `DataSource.getConnection` returns the connection in "allocated" form `(connection, release)`,
 * which must be released with a bracket to stay safe under cancellation. This extension performs
 * that bracket so callers keep a concise `ds.use { conn => ... }` while the release runs on every
 * outcome (success, error, cancellation).
 */
trait DataSourceOps:

  extension [F[_]](ds: DataSource[F])
    def use[B](f: Connection[F] => F[B])(using mc: MonadCancel[F, Throwable]): F[B] =
      mc.bracket(ds.getConnection)((pair: (Connection[F], F[Unit])) => f(pair._1))(
        (pair: (
          Connection[F],
          F[Unit]
        )) => pair._2
      )
