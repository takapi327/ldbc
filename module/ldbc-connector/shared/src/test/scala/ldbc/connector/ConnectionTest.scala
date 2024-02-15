/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import org.scalatest.flatspec.AnyFlatSpec

import org.typelevel.otel4s.trace.Tracer

import cats.effect.*
import cats.effect.unsafe.implicits.global

import ldbc.connector.exception.MySQLException

class ConnectionTest extends AnyFlatSpec:
  
  given Tracer[IO] = Tracer.noop[IO]
  
  it should "Passing an empty string to host causes MySQLException" in {
    val connection = Connection.single[IO](
      host = "",
      port = 13306,
      user = "root",
    )
    assertThrows[MySQLException] {
      connection.use(_ => IO.unit).unsafeRunSync()
    }
  }

  it should "UnknownHostException occurs when invalid host is passed" in {
    val connection = Connection.single[IO](
      host = "host",
      port = 13306,
      user = "root",
    )
    assertThrows[java.net.UnknownHostException] {
      connection.use(_ => IO.unit).unsafeRunSync()
    }
  }

  it should "Passing a negative value to Port causes MySQLException" in {
    val connection = Connection.single[IO](
      host = "127.0.0.1",
      port = -1,
      user = "root",
    )
    assertThrows[MySQLException] {
      connection.use(_ => IO.unit).unsafeRunSync()
    }
  }

  it should "MySQLException occurs when passing more than 65535 values to Port" in {
    val connection = Connection.single[IO](
      host = "127.0.0.1",
      port = 65536,
      user = "root",
    )
    assertThrows[MySQLException] {
      connection.use(_ => IO.unit).unsafeRunSync()
    }
  }
