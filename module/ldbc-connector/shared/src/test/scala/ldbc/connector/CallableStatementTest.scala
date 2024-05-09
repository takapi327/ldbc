/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.effect.*

import munit.CatsEffectSuite

import org.typelevel.otel4s.trace.Tracer

class CallableStatementTest extends CatsEffectSuite:

  given Tracer[IO] = Tracer.noop[IO]

  private val connection = Connection[IO](
    host = "127.0.0.1",
    port = 13306,
    user = "ldbc",
    password = Some("password"),
    database = Some("connector_test"),
    allowPublicKeyRetrieval = true,
    //ssl = SSL.Trusted
  )

  test("") {
    assertIOBoolean(connection.use { conn =>
      for
        _ <- conn.prepareCall("{call demoSp(?, ?)}")
      yield true
    })
  }
