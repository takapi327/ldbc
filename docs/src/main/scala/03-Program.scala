/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import cats.syntax.all.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.*
import ldbc.dsl.io.*

@main def program3(): Unit =

  // #given
  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.noop[IO]
  // #given

  // #program
  val program: DBIO[(List[Int], Option[Int], Int)] =
    for
      result1 <- sql"SELECT 1".query[Int].to[List]
      result2 <- sql"SELECT 2".query[Int].to[Option]
      result3 <- sql"SELECT 3".query[Int].unsafe
    yield (result1, result2, result3)
  // #program

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
      program.readOnly(conn).map(println(_))
    }
    .unsafeRunSync()
  // (List(1), Some(2), 3)
  // #run
