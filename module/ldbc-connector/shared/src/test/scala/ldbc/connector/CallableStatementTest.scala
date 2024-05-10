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
    host                    = "127.0.0.1",
    port                    = 13306,
    user                    = "ldbc",
    password                = Some("password"),
    database                = Some("connector_test"),
    allowPublicKeyRetrieval = true
    // ssl = SSL.Trusted
  )

  test("") {
    assertIO(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("CALL demoSp(?, ?)")
          resultSet <- callableStatement.setString(1, "abcdefg") *> callableStatement.setInt(2, 1) *> callableStatement
                         .executeQuery()
          value <- resultSet.getString(1)
        yield value
      },
      Some("abcdefg")
    )
  }

  test("") {
    assertIO(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("CALL demoSp(?, ?)")
          resultSet <- callableStatement.setString(1, "abcdefg") *> callableStatement.setInt(2, 1) *> callableStatement
                         .executeQuery()
          outParam <- callableStatement.getInt(2)
        yield outParam
      },
      2
    )
  }

  test("") {
    assertIO(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("CALL demoSp(?, ?)")
          resultSet <- callableStatement.setString(1, "abcdefg") *> callableStatement.setInt(2, 1) *> callableStatement
            .executeQuery()
          outParam <- callableStatement.getInt("inOutParam")
        yield outParam
      },
      2
    )
  }
