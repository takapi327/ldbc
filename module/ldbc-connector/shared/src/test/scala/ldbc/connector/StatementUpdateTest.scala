/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import org.typelevel.otel4s.trace.Tracer

import cats.effect.*

import munit.CatsEffectSuite

import ldbc.connector.net.protocol.Statement

class StatementUpdateTest extends CatsEffectSuite:

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
        for
          statement <- conn.createStatement()
          _         <- statement.executeUpdate("CREATE TABLE `bit_table`(`bit_column` BIT NOT NULL)")
          count     <- statement.executeUpdate("INSERT INTO `bit_table`(`bit_column`) VALUES (b'1')")
          _         <- statement.executeUpdate("DROP TABLE `bit_table`")
        yield count
      },
      1
    )
  }

  test("As a result of entering data for two cases, there will be two affected rows.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _         <- statement.executeUpdate("CREATE TABLE `bit_table`(`bit_column` BIT NOT NULL)")
          count     <- statement.executeUpdate("INSERT INTO `bit_table`(`bit_column`) VALUES (b'0'),(b'1')")
          _         <- statement.executeUpdate("DROP TABLE `bit_table`")
        yield count
      },
      2
    )
  }

  test("The value generated when adding a record of AUTO_INCREMENT is returned.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _ <-
            statement
              .executeUpdate(
                "CREATE TABLE `auto_inc_table`(`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, `c1` VARCHAR(255) NOT NULL)"
              )
          _ <- statement.executeUpdate("INSERT INTO `auto_inc_table`(`id`, `c1`) VALUES (null, 'column 1')")
          resultSet <-
            statement.executeUpdate(
              "INSERT INTO `auto_inc_table`(`id`, `c1`) VALUES (null, 'column 2')",
              Statement.RETURN_GENERATED_KEYS
            ) *> statement.getGeneratedKeys()
          generated <- resultSet.next() *> resultSet.getLong(1)
          _ <- statement.executeUpdate("DROP TABLE `auto_inc_table`")
        yield generated
      },
      2L
    )
  }
