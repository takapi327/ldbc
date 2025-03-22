/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import cats.effect.*
import cats.effect.unsafe.implicits.global

import ldbc.dsl.*

import ldbc.connector.*

@main def cleanup(): Unit =

  // #cleanupDatabase
  val dropDatabase: DBIO[Int] =
    sql"DROP DATABASE IF EXISTS sandbox_db".update
  // #cleanupDatabase

  // #connection
  def connection = ConnectionProvider
    .default[IO]("127.0.0.1", 13306, "ldbc")
    .setPassword("password")
  // #connection

  // #run
  connection
    .use { conn =>
      dropDatabase.commit(conn).as(println("Database dropped"))
    }
    .unsafeRunSync()
  // #run
