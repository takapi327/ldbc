/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import munit.CatsEffectSuite

import ldbc.connector.io.*

class SQLStringContextUpdateTest extends CatsEffectSuite:

  given Tracer[IO] = Tracer.noop[IO]

  private val connection = Connection[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = Some("password"),
    database = Some("connector_test"),
    ssl      = SSL.Trusted
  )

  test("As a result of entering one case of data, there will be one affected row.") {
    assertIO(
      connection.use { conn =>
        (for
          _         <- sql"CREATE TABLE `bit_table`(`bit_column` BIT NOT NULL)".update
          count     <- sql"INSERT INTO `bit_table`(`bit_column`) VALUES (b'1')".update
          _         <- sql"DROP TABLE `bit_table`".update
        yield count).run(conn)
      },
      1
    )
  }

  test("As a result of entering data for two cases, there will be two affected rows.") {
    assertIO(
      connection.use { conn =>
        (for
          _         <- sql"CREATE TABLE `bit_table`(`bit_column` BIT NOT NULL)".update
          count     <- sql"INSERT INTO `bit_table`(`bit_column`) VALUES (b'0'),(b'1')".update
          _         <- sql"DROP TABLE `bit_table`".update
        yield count).run(conn)
      },
      2
    )
  }
