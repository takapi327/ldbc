/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import cats.effect.*
import cats.effect.unsafe.implicits.global

import org.typelevel.otel4s.trace.Tracer

import ldbc.dsl.io.*

import ldbc.connector.*

@main def program4(): Unit =

  // #given
  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.noop[IO]
  // #given

  // #program
  val program: DBIO[Int] =
    sql"INSERT INTO user (name, email) VALUES ('Carol', 'carol@example.com')".update
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
      program.commit(conn).map(println(_))
    }
    .unsafeRunSync()
  // 1
  // #run
