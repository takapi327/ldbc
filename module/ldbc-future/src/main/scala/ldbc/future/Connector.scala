/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.future

import scala.concurrent.Future

import ldbc.fx.Fx

import ldbc.sql.{ Connection, DataSource }

import ldbc.*
import ldbc.free.*
import ldbc.future.FxInstances.given
import ldbc.logging.{ LogEvent, LogHandler }

/**
 * `Future` factories for [[ldbc.Connector]].
 *
 * A [[ldbc.Connector]] built here runs `DBIO` programs against a `Fx`-native connection source: the
 * whole program is interpreted in `Fx` (its native effect) and only the final result is bridged to
 * `Future` once (see [[toFuture]]). Because `Future` has no host runtime and no reason to swap one, the
 * `Fx` program always runs on the platform-default [[ldbc.fx.FxRuntime.global]] pools. This is
 * DB-agnostic — any `DataSource[Fx]` (MySQL, PostgreSQL, ...) works.
 */
object Connector:

  private def noopLogger: LogHandler[Fx] = (_: LogEvent) => Fx.unit

  /**
   * Builds a [[ldbc.Connector]] over `Future` that acquires a connection from the given data source for
   * each run, interprets the `DBIO` in `Fx`, and releases the connection afterwards.
   *
   * @param dataSource the `Fx`-native connection source
   * @param logHandler an optional log handler (a no-op handler is used if absent)
   */
  def fromDataSource(
    dataSource: DataSource[Fx],
    logHandler: Option[LogHandler[Fx]] = None
  ): ldbc.Connector[Future] =
    new ldbc.Connector[Future]:
      private val interpreter = new KleisliInterpreter[Fx](logHandler.getOrElse(noopLogger))
      override def run[A](dbio: DBIO[A]): Future[A] =
        toFuture(
          Fx.bracket(dataSource.getConnection)((pair: (Connection[Fx], Fx[Unit])) =>
            dbio.foldMap(interpreter.ConnectionInterpreter).run(pair._1)
          )((pair: (Connection[Fx], Fx[Unit])) => pair._2)
        )

  /**
   * Builds a [[ldbc.Connector]] over `Future` that runs every `DBIO` against the supplied connection.
   * Closing the connection is the caller's responsibility.
   *
   * @param connection the `Fx`-native connection to run against
   * @param logHandler an optional log handler (a no-op handler is used if absent)
   */
  def fromConnection(
    connection: Connection[Fx],
    logHandler: Option[LogHandler[Fx]] = None
  ): ldbc.Connector[Future] =
    new ldbc.Connector[Future]:
      private val interpreter = new KleisliInterpreter[Fx](logHandler.getOrElse(noopLogger))
      override def run[A](dbio: DBIO[A]): Future[A] =
        toFuture(dbio.foldMap(interpreter.ConnectionInterpreter).run(connection))
