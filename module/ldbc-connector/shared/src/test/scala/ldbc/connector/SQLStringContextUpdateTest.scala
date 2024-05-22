/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import munit.CatsEffectSuite

import ldbc.sql.logging.LogHandler
import ldbc.connector.io.*

class SQLStringContextUpdateTest extends CatsEffectSuite:

  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.noop[IO]

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
          _     <- sql"CREATE TABLE `string_context_bit_table1`(`bit_column` BIT NOT NULL)".update
          count <- sql"INSERT INTO `string_context_bit_table1`(`bit_column`) VALUES (b'1')".update
          _     <- sql"DROP TABLE `string_context_bit_table1`".update
        yield count).run(conn)
      },
      1
    )
  }

  test("As a result of entering data for two cases, there will be two affected rows.") {
    assertIO(
      connection.use { conn =>
        (for
          _     <- sql"CREATE TABLE `string_context_bit_table2`(`bit_column` BIT NOT NULL)".update
          count <- sql"INSERT INTO `string_context_bit_table2`(`bit_column`) VALUES (b'0'),(b'1')".update
          _     <- sql"DROP TABLE `string_context_bit_table2`".update
        yield count).run(conn)
      },
      2
    )
  }

  test("The value generated when adding a record of AUTO_INCREMENT is returned.") {
    assertIO(
      connection.use { conn =>
        (for
          _ <-
            sql"CREATE TABLE `returning_auto_inc`(`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, `c1` VARCHAR(255) NOT NULL)".update
          _         <- sql"INSERT INTO `returning_auto_inc`(`id`, `c1`) VALUES (null, 'column 1')".update
          generated <- sql"INSERT INTO `returning_auto_inc`(`id`, `c1`) VALUES (null, 'column 2')".returning[Long]
          _         <- sql"DROP TABLE `returning_auto_inc`".update
        yield generated).run(conn)
      },
      2L
    )
  }
