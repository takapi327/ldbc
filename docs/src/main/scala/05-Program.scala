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
import ldbc.dsl.codec.*

@main def program5(): Unit =

  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.noop[IO]

  enum Status:
    case Active, InActive

  given Encoder[Status] = Encoder[Boolean].contramap {
    case Status.Active   => true
    case Status.InActive => false
  }

  val program1: DBIO[Int] =
    sql"INSERT INTO user (name, email, status) VALUES (${ "user 1" }, ${ "user@example.com" }, ${ Status.Active })".update

  given Decoder.Elem[Status] = Decoder.Elem.mapping[Boolean, Status] {
    case true  => Status.Active
    case false => Status.InActive
  }

  val program2: DBIO[(String, String, Status)] =
    sql"SELECT name, email, status FROM user WHERE id = 1".query[(String, String, Status)].unsafe

  def connection = Connection[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = Some("password"),
    ssl      = SSL.Trusted
  )

  connection
    .use { conn =>
      program1.commit(conn) *> program2.readOnly(conn).map(println(_))
    }
    .unsafeRunSync()
