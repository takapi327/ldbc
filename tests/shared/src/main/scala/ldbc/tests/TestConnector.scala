/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import cats.MonadError

import ldbc.sql.{ Connection, DataSource }

import ldbc.effect.Async
import ldbc.free.KleisliInterpreter
import ldbc.logging.{ LogEvent, LogHandler }
import ldbc.DBIO

/**
 * Test-only generic connector factories — the effect-generic `fromConnection` / `fromDataSource` that the
 * driver used to expose from `ldbc.mysql.Connector` (minus `fromConfig`), kept here now that production ships
 * only per-effect connectors (`ldbc.catseffect` / `ldbc.zio` / `ldbc.future`).
 *
 * Used by the effect-agnostic fixtures (`ConnectionFixture[F]`) and the `Fx`-direct suites, where an effect
 * that has no public connector is still exercised. Not part of the public API.
 */
object TestConnector:

  private def noopLogger[F[_]](using F: MonadError[F, Throwable]): LogHandler[F] = (_: LogEvent) => F.pure(())

  def fromConnection[F[_]](
    connection: Connection[F],
    logHandler: Option[LogHandler[F]] = None
  )(using F: MonadError[F, Throwable]): ldbc.Connector[F] =
    new ldbc.Connector[F]:
      private val interpreter = new KleisliInterpreter[F](logHandler.getOrElse(noopLogger))
      override def run[A](dbio: DBIO[A]): F[A] =
        dbio.foldMap(interpreter.ConnectionInterpreter).run(connection)

  def fromDataSource[F[_]](
    dataSource: DataSource[F],
    logHandler: Option[LogHandler[F]] = None
  )(using MonadError[F, Throwable], Async[F]): ldbc.Connector[F] =
    new ldbc.Connector[F]:
      private val interpreter = new KleisliInterpreter[F](logHandler.getOrElse(noopLogger))
      override def run[A](dbio: DBIO[A]): F[A] =
        summon[Async[F]].bracket(dataSource.getConnection)(pair =>
          dbio.foldMap(interpreter.ConnectionInterpreter).run(pair._1)
        )(pair => pair._2)
