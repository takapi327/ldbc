/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import cats.effect.*
import cats.effect.unsafe.implicits.global

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.*
import ldbc.dsl.io.*

@main def program4(): Unit =

  // #given
  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.noop[IO]
  // #given

  // #program
  val program: Executor[IO, Int] =
    sql"INSERT INTO task (name, done) VALUES ('task 1', false)".update
  // #program

  // #connection
  def connection = Connection[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = Some("password")
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