/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import org.typelevel.otel4s.trace.Tracer

import cats.effect.*

import munit.CatsEffectSuite

import ldbc.connector.exception.MySQLException

class ConnectionTest extends CatsEffectSuite:

  given Tracer[IO] = Tracer.noop[IO]

  test("Passing an empty string to host causes MySQLException") {
    val connection = Connection.single[IO](
      host = "",
      port = 13306,
      user = "root"
    )
    interceptIO[MySQLException] {
      connection.use(_ => IO.unit)
    }
  }

  test("UnknownHostException occurs when invalid host is passed") {
    val connection = Connection.single[IO](
      host = "host",
      port = 13306,
      user = "root"
    )
    interceptIO[java.net.UnknownHostException] {
      connection.use(_ => IO.unit)
    }
  }

  test("Passing a negative value to Port causes MySQLException") {
    val connection = Connection.single[IO](
      host = "127.0.0.1",
      port = -1,
      user = "root"
    )
    interceptIO[MySQLException] {
      connection.use(_ => IO.unit)
    }
  }

  test("MySQLException occurs when passing more than 65535 values to Port") {
    val connection = Connection.single[IO](
      host = "127.0.0.1",
      port = 65536,
      user = "root"
    )
    interceptIO[MySQLException] {
      connection.use(_ => IO.unit)
    }
  }
