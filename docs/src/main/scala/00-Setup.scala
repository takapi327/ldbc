/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import cats.effect.*
import cats.effect.unsafe.implicits.global

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.*
import ldbc.dsl.Executor
import ldbc.dsl.io.*
import ldbc.dsl.logging.LogHandler

@main def setup(): Unit =

  // #given
  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.noop[IO]
  // #given

  // #setup
  val createDatabase: Executor[IO, Int] =
    sql"CREATE DATABASE IF NOT EXISTS todo".update

  val createTable: Executor[IO, Int] =
    sql"""
         CREATE TABLE IF NOT EXISTS `task` (
           `id` INT NOT NULL AUTO_INCREMENT,
           `name` VARCHAR(255) NOT NULL,
           `done` BOOLEAN NOT NULL DEFAULT FALSE,
           PRIMARY KEY (`id`)
         )
       """.update
  // #setup

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
      createDatabase.commit(conn) *>
        conn.setSchema("todo") *>
        createTable.commit(conn).map(println(_))
    }
    .unsafeRunSync()
  // #run
