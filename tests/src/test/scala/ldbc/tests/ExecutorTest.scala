/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import munit.CatsEffectSuite

import cats.syntax.all.*

import cats.effect.IO

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.*
import ldbc.dsl.io.*

class DBIOTest extends CatsEffectSuite:

  given Tracer[IO] = Tracer.noop[IO]

  private val connection = Connection[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = Some("password"),
    ssl      = SSL.Trusted
  )

  test("DBIO#pure") {
    val program = DBIO.pure[IO, Int](1)
    assertIO(
      connection.use { conn =>
        program.execute(conn)
      },
      1
    )
  }

  test("DBIO#ap") {
    val program1 = DBIO.pure[IO, Int](1)
    val program2 = DBIO.pure[IO, Int => Int](_ + 1)
    val program3 = program2.ap(program1)
    assertIO(
      connection.use { conn =>
        program3.execute(conn)
      },
      2
    )
  }

  test("DBIO#map") {
    val program1 = DBIO.pure[IO, Int](1)
    val program2 = program1.map(_ + 1)
    assertIO(
      connection.use { conn =>
        program2.execute(conn)
      },
      2
    )
  }

  test("DBIO#flatMap") {
    val program1 = DBIO.pure[IO, Int](1)
    val program2 = program1.flatMap(n => DBIO.pure[IO, Int](n + 1))
    assertIO(
      connection.use { conn =>
        program2.execute(conn)
      },
      2
    )
  }

  test("DBIO#tailRecM") {
    val program1 = DBIO.pure[IO, Int](1)
    val program2 = program1.tailRecM[DBIO, String](_.map(n => Right(n.toString)))
    assertIO(
      connection.use { conn =>
        program2.execute(conn)
      },
      "1"
    )
  }

  test("DBIO#raiseError") {
    val program = DBIO.raiseError[IO, Int](new Exception("error"))
    interceptMessageIO[Exception]("error")(
      connection.use { conn =>
        program.execute(conn)
      }
    )
  }

  test("DBIO#handleErrorWith") {
    val program1 = DBIO.raiseError[IO, Int](new Exception("error"))
    val program2 = program1.handleErrorWith(e => DBIO.pure[IO, Int](0))
    assertIO(
      connection.use { conn =>
        program2.execute(conn)
      },
      0
    )
  }

  test("DBIO#attempt#Right") {
    val program = DBIO.pure[IO, Int](1)
    assertIO(
      connection.use { conn =>
        program.attempt.execute(conn)
      },
      Right(1)
    )
  }

  test("DBIO#attempt#Left") {
    val program: DBIO[IO, Int] = DBIO.raiseError[IO, Int](new Exception("error"))
    assertIOBoolean(
      connection.use { conn =>
        program.attempt.execute(conn).map(_.isLeft)
      }
    )
  }
