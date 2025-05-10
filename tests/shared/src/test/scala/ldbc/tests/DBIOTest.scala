/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import cats.effect.IO
import cats.syntax.all.*
import ldbc.connector.*
import ldbc.dsl.*
import ldbc.sql.*
import munit.CatsEffectSuite

class DBIOTest extends CatsEffectSuite:

  def connection: Provider[IO] =
    ConnectionProvider
      .default[IO]("127.0.0.1", 13306, "ldbc")
      .setPassword("password")
      .setSSL(SSL.Trusted)

  test("DBIO#pure") {
    val program = DBIO.pure(1)
    assertIO(
      connection.use { conn =>
        program.readOnly(conn)
      },
      1
    )
  }

  test("DBIO#ap") {
    val program1 = DBIO.pure(1)
    val program2 = DBIO.pure[Int => Int](_ + 1)
    val program3 = program2.ap(program1)
    assertIO(
      connection.use { conn =>
        program3.readOnly(conn)
      },
      2
    )
  }

  test("DBIO#map") {
    val program1 = DBIO.pure(1)
    val program2 = program1.map(_ + 1)
    assertIO(
      connection.use { conn =>
        program2.readOnly(conn)
      },
      2
    )
  }

  test("DBIO#flatMap") {
    val program1 = DBIO.pure(1)
    val program2 = program1.flatMap(n => DBIO.pure(n + 1))
    assertIO(
      connection.use { conn =>
        program2.readOnly(conn)
      },
      2
    )
  }

  test("DBIO#tailRecM") {
    val program1 = DBIO.pure(1)
    val program2 = program1.tailRecM[DBIO, String](_.map(n => Right(n.toString)))
    assertIO(
      connection.use { conn =>
        program2.readOnly(conn)
      },
      "1"
    )
  }

  test("DBIO#raiseError") {
    val program = DBIO.raiseError[Int](new Exception("error"))
    interceptMessageIO[Exception]("error")(
      connection.use { conn =>
        program.readOnly(conn)
      }
    )
  }

  test("DBIO#handleErrorWith") {
    val program1 = DBIO.raiseError[Int](new Exception("error"))
    val program2 = program1.handleErrorWith(_ => DBIO.pure(0))
    assertIO(
      connection.use { conn =>
        program2.readOnly(conn)
      },
      0
    )
  }

  test("DBIO#attempt#Right") {
    val program = DBIO.pure(1)
    assertIO(
      connection.use { conn =>
        program.attempt.readOnly(conn)
      },
      Right(1)
    )
  }

  test("DBIO#attempt#Left") {
    val program = DBIO.raiseError[Int](new Exception("error"))
    assertIOBoolean(
      connection.use { conn =>
        program.attempt.readOnly(conn).map(_.isLeft)
      }
    )
  }
