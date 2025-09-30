/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import cats.effect.*
import cats.effect.unsafe.implicits.global

import ldbc.dsl.*

import ldbc.connector.*

@main def program4(): Unit =

  // #program
  val program: DBIO[Int] =
    sql"INSERT INTO user (name, email) VALUES ('Carol', 'carol@example.com')".update
  // #program

  // #connection
  val dataSource = MySQLDataSource
    .build[IO]("127.0.0.1", 13306, "ldbc")
    .setPassword("password")
    .setSSL(SSL.Trusted)

  def connector = Connector.fromDataSource(dataSource)
  // #connection

  // #run
  program.commit(connector).map(println(_))
    .unsafeRunSync()
  // 1
  // #run
