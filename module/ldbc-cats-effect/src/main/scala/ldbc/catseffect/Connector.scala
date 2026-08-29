/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.catseffect

import cats.effect.IO

import ldbc.sql.{ Connection, DataSource }

import ldbc.effect.Async
import ldbc.free.KleisliInterpreter
import ldbc.logging.{ LogEvent, LogHandler }
import ldbc.DBIO

/**
 * Cats Effect factories for `ldbc.Connector[IO]` — the same set `ldbc-mysql` offers minus `fromConfig`
 * ([[fromConnection]] / [[fromDataSource]]).
 *
 * `IO` is `ldbc.effect.Async` and `cats.MonadError` natively, so the `DBIO` interpreter runs directly on `IO`.
 * Streaming for `IO` is fs2-based (see `ldbc.catseffect.*`'s `Query#stream` and `Sync[DBIO]`, consumed via
 * `connector.run`), so — unlike the ZIO connector — no extra streaming capability is needed here.
 */
object Connector:

  private val noopLogHandler: LogHandler[IO] = (_: LogEvent) => IO.unit

  /**
   * A connector that runs every `DBIO` against the supplied connection. Closing the connection is the
   * caller's responsibility.
   */
  def fromConnection(
    connection: Connection[IO],
    logHandler: LogHandler[IO] = noopLogHandler
  ): ldbc.Connector[IO] =
    new ldbc.Connector[IO]:
      private val interpreter = new KleisliInterpreter[IO](logHandler)

      override def run[A](dbio: DBIO[A]): IO[A] =
        dbio.foldMap(interpreter.ConnectionInterpreter).run(connection)

  /**
   * A connector that acquires a connection from `source` for each `DBIO` run, interprets the `DBIO` in `IO`,
   * and releases the connection afterwards.
   */
  def fromDataSource(
    source:     DataSource[IO],
    logHandler: LogHandler[IO] = noopLogHandler
  ): ldbc.Connector[IO] =
    new ldbc.Connector[IO]:
      private val interpreter = new KleisliInterpreter[IO](logHandler)

      override def run[A](dbio: DBIO[A]): IO[A] =
        summon[Async[IO]].bracket(source.getConnection)(pair =>
          dbio.foldMap(interpreter.ConnectionInterpreter).run(pair._1)
        )(pair => pair._2)
