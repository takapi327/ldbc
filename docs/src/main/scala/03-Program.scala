/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import cats.effect.*
import cats.effect.unsafe.implicits.global

import ldbc.dsl.*

import ldbc.connector.*

@main def program3(): Unit =

  // #program
  val program: DBIO[(List[Int], Option[Int], Int)] =
    for
      result1 <- sql"SELECT 1".query[Int].to[List]
      result2 <- sql"SELECT 2".query[Int].to[Option]
      result3 <- sql"SELECT 3".query[Int].unsafe
    yield (result1, result2, result3)
  // #program

  // #connection
  def connection = MySQLProvider
    .default[IO]("127.0.0.1", 13306, "ldbc")
    .setPassword("password")
    .setSSL(SSL.Trusted)
  // #connection

  // #run
  connection
    .use { conn =>
      program.readOnly(conn).map(println(_))
    }
    .unsafeRunSync()
  // (List(1), Some(2), 3)
  // #run
