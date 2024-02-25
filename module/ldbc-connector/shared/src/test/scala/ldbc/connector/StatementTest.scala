/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import org.typelevel.otel4s.trace.Tracer

import cats.effect.*

import munit.CatsEffectSuite

import ldbc.connector.net.packet.response.ResultSetRowPacket

class StatementTest extends CatsEffectSuite:

  given Tracer[IO] = Tracer.noop[IO]

  test("Statement should be able to execute a query") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      ssl      = SSL.Trusted
    )
    assertIO(connection.use(_.statement("SELECT 1").executeQuery()), List(ResultSetRowPacket(List(Some("1")))))
  }
