/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import scala.language.implicitConversions

import cats.effect.*
import cats.effect.unsafe.implicits.global

import ldbc.dsl.*
import ldbc.dsl.codec.*

import ldbc.connector.*

@main def program5(): Unit =

  enum Status:
    case Active, InActive

  given Encoder[Status] = Encoder[Boolean].contramap {
    case Status.Active   => true
    case Status.InActive => false
  }

  val program1: DBIO[Int] =
    sql"INSERT INTO user (name, email, status) VALUES (${ "user 1" }, ${ "user@example.com" }, ${ Status.Active })".update

  given Decoder[Status] = Decoder[Boolean].map {
    case true  => Status.Active
    case false => Status.InActive
  }

  val program2: DBIO[(String, String, Status)] =
    sql"SELECT name, email, status FROM user WHERE id = 1".query[(String, String, Status)].unsafe

  def connection = MySQLProvider
    .default[IO]("127.0.0.1", 13306, "ldbc")
    .setPassword("password")
    .setSSL(SSL.Trusted)

  connection
    .use { conn =>
      program1.commit(conn) *> program2.readOnly(conn).map(println(_))
    }
    .unsafeRunSync()
