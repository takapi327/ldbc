/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.Applicative

import cats.effect.*

import ldbc.sql.*

import ldbc.*
import ldbc.free.*
import ldbc.logging.{ LogEvent, LogHandler }

object Connector:

  private def noopLogger[F[_]: Applicative]: LogHandler[F] = (logEvent: LogEvent) => Applicative[F].unit

  private case class Impl[F[_]: Sync](
                                       logHandler: LogHandler[F],
                                       connection: Connection[F]
                                     ) extends Connector[F]:

    private val interpreter: Interpreter[F] = new KleisliInterpreter[F](logHandler)

    override def run[A](dbio: DBIO[A]): F[A] = dbio.foldMap(interpreter.ConnectionInterpreter).run(connection)

  def fromConnection[F[_]: Sync](connection: Connection[F]): Connector[F] =
    Impl[F](
      logHandler = noopLogger[F],
      connection = connection
    )

  def fromConnection[F[_]: Sync](connection: Connection[F], logHandler: Option[LogHandler[F]] = None): Connector[F] =
    Impl[F](
      logHandler = logHandler.getOrElse(noopLogger),
      connection = connection
    )

  def fromDataSource[F[_]: Sync](dataSource: DataSource[F], logHandler: Option[LogHandler[F]] = None): Connector[F] =
    new Connector[F]:
      private val interpreter:            Interpreter[F] = new KleisliInterpreter[F](logHandler.getOrElse(noopLogger))
      override def run[A](dbio: DBIO[A]): F[A]           = dataSource.createConnection().use { connection =>
        dbio.foldMap(interpreter.ConnectionInterpreter).run(connection)
      }
