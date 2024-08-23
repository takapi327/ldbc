/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import scala.language.implicitConversions

import cats.effect.*
import cats.effect.unsafe.implicits.global

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.*
import ldbc.dsl.io.*

@main def program5(): Unit =

  // #given
  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.noop[IO]
  // #given

  // #customType
  enum Status:
    case Active, InActive
  // #customType

  // #customParameter
  given Parameter[Status] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, status: Status): F[Unit] =
      status match
        case Status.Active   => statement.setBoolean(index, true)
        case Status.InActive => statement.setBoolean(index, false)
  // #customParameter

  // #program1
  val program1: Executor[IO, Int] =
    sql"INSERT INTO user (name, email, status) VALUES (${ "user 1" }, ${ "user@example.com" }, ${ Status.Active })".update
  // #program1

  // #customReader
  given ResultSetReader[Status] =
    ResultSetReader.mapping[Boolean, Status] {
      case true  => Status.Active
      case false => Status.InActive
    }
  // #customReader

  // #program2
  val program2: Executor[IO, (String, String, Status)] =
    sql"SELECT name, email, status FROM user WHERE id = 1".query[(String, String, Status)].unsafe
  // #program2

  // #connection
  def connection = Connection[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = Some("password"),
    ssl      = SSL.Trusted
  )
  // #connection

  // #run
  connection
    .use { conn =>
      program1.commit(conn) *> program2.readOnly(conn).map(println(_))
    }
    .unsafeRunSync()
  // #run
