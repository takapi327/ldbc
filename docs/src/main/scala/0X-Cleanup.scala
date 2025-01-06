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

@main def cleanup(): Unit =

  // #given
  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.noop[IO]
  // #given

  // #cleanupDatabase
  val dropDatabase: DBIO[Int] =
    sql"DROP DATABASE IF EXISTS sandbox_db".update
  // #cleanupDatabase

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
      dropDatabase.commit(conn).as(println("Database dropped"))
    }
    .unsafeRunSync()
  // #run
