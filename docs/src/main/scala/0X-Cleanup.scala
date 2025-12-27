/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
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
  val dataSource = MySQLDataSource
    .build[IO]("127.0.0.1", 13306, "ldbc")
    .setPassword("password")
  def connector = Connector.fromDataSource(dataSource)
  // #connection

  // #run
  dropDatabase
    .commit(connector)
    .as(println("Database dropped"))
    .unsafeRunSync()
  // #run
