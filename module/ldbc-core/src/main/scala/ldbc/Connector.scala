/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc

import cats.effect.*

import ldbc.sql.*
import ldbc.sql.logging.{LogEvent, LogHandler}

import ldbc.free.*

trait Connector[F[_]]:
  
  def logHandler: Option[LogHandler[F]]
  
  def connection: Connection[F]

  def run[A](dbio: DBIO[A]): F[A]
  
object Connector:
  
  private case class Impl[F[_]: Sync](
    logHandler: Option[LogHandler[F]],
    connection: Connection[F]
  ) extends Connector[F]:

    private val noopLogHandler: LogHandler[F] = (logEvent: LogEvent) => Sync[F].unit

    val interpreter: Interpreter[F] = new KleisliInterpreter[F](logHandler.getOrElse(noopLogHandler))

    override def run[A](dbio: DBIO[A]): F[A] = dbio.foldMap(interpreter.ConnectionInterpreter).run(connection)

  def fromConnection[F[_]: Sync](connection: Connection[F]): Connector[F] =
    Impl[F](
      logHandler = Some(connection.logHandler),
      connection = connection
    )
