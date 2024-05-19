/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.time.*

import cats.effect.*

import munit.CatsEffectSuite

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.data.MysqlType

class ClientPreparedStatementUpdateTest extends CatsEffectSuite:

  given Tracer[IO] = Tracer.noop[IO]

  private val connection = Connection[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = Some("password"),
    database = Some("connector_test"),
    ssl      = SSL.Trusted
  )

  test("Boolean values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _ <- statement.executeUpdate(
                 "CREATE TABLE `client_statement_boolean_table`(`c1` BOOLEAN NOT NULL, `c2` BOOLEAN NULL)"
               )
          preparedStatement <-
            conn.clientPreparedStatement("INSERT INTO `client_statement_boolean_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- preparedStatement.setBoolean(1, true) *> preparedStatement
                     .setNull(2, MysqlType.BOOLEAN.jdbcType) *> preparedStatement
                     .executeUpdate()
          _ <- statement.executeUpdate("DROP TABLE `client_statement_boolean_table`")
        yield count
      },
      1
    )
  }

  test("Byte values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _ <- statement.executeUpdate("CREATE TABLE `client_statement_byte_table`(`c1` BIT NOT NULL, `c2` BIT NULL)")
          preparedStatement <-
            conn.clientPreparedStatement("INSERT INTO `client_statement_byte_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- preparedStatement.setByte(1, 1.toByte) *> preparedStatement
                     .setNull(2, MysqlType.BIT.jdbcType) *> preparedStatement
                     .executeUpdate()
          _ <- statement.executeUpdate("DROP TABLE `client_statement_byte_table`")
        yield count
      },
      1
    )
  }

  test("Short values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _ <- statement.executeUpdate(
                 "CREATE TABLE `client_statement_short_table`(`c1` TINYINT NOT NULL, `c2` TINYINT NULL)"
               )
          preparedStatement <-
            conn.clientPreparedStatement("INSERT INTO `client_statement_short_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- preparedStatement.setShort(1, 1.toShort) *> preparedStatement
                     .setNull(2, MysqlType.TINYINT.jdbcType) *> preparedStatement
                     .executeUpdate()
          _ <- statement.executeUpdate("DROP TABLE `client_statement_short_table`")
        yield count
      },
      1
    )
  }

  test("Int values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _ <- statement.executeUpdate(
                 "CREATE TABLE `client_statement_int_table`(`c1` SMALLINT NOT NULL, `c2` SMALLINT NULL)"
               )
          preparedStatement <-
            conn.clientPreparedStatement("INSERT INTO `client_statement_int_table`(`c1`, `c2`) VALUES (?, ?)")
          count <-
            preparedStatement.setInt(1, 1) *> preparedStatement.setNull(
              2,
              MysqlType.SMALLINT.jdbcType
            ) *> preparedStatement.executeUpdate()
          _ <- statement.executeUpdate("DROP TABLE `client_statement_int_table`")
        yield count
      },
      1
    )
  }

  test("Long values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _ <- statement.executeUpdate(
                 "CREATE TABLE `client_statement_long_table`(`c1` BIGINT NOT NULL, `c2` BIGINT NULL)"
               )
          preparedStatement <-
            conn.clientPreparedStatement("INSERT INTO `client_statement_long_table`(`c1`, `c2`) VALUES (?, ?)")
          count <-
            preparedStatement.setLong(1, Long.MaxValue) *> preparedStatement
              .setNull(2, MysqlType.BIGINT.jdbcType) *> preparedStatement
              .executeUpdate()
          _ <- statement.executeUpdate("DROP TABLE `client_statement_long_table`")
        yield count
      },
      1
    )
  }

  test("BigInt values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _ <-
            statement.executeUpdate(
              "CREATE TABLE `client_statement_bigint_table`(`c1` BIGINT unsigned NOT NULL, `c2` BIGINT unsigned NULL)"
            )
          preparedStatement <-
            conn.clientPreparedStatement("INSERT INTO `client_statement_bigint_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- preparedStatement.setString(1, "18446744073709551615") *> preparedStatement
                     .setNull(2, MysqlType.BIGINT.jdbcType) *> preparedStatement
                     .executeUpdate()
          _ <- statement.executeUpdate("DROP TABLE `client_statement_bigint_table`")
        yield count
      },
      1
    )
  }

  test("Float values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _ <-
            statement.executeUpdate("CREATE TABLE `client_statement_float_table`(`c1` FLOAT NOT NULL, `c2` FLOAT NULL)")
          preparedStatement <-
            conn.clientPreparedStatement("INSERT INTO `client_statement_float_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- preparedStatement.setFloat(1, 1.1f) *> preparedStatement
                     .setNull(2, MysqlType.FLOAT.jdbcType) *> preparedStatement
                     .executeUpdate()
          _ <- statement.executeUpdate("DROP TABLE `client_statement_float_table`")
        yield count
      },
      1
    )
  }

  test("Double values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _ <- statement.executeUpdate(
                 "CREATE TABLE `client_statement_double_table`(`c1` DOUBLE NOT NULL, `c2` DOUBLE NULL)"
               )
          preparedStatement <-
            conn.clientPreparedStatement("INSERT INTO `client_statement_double_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- preparedStatement.setDouble(1, 1.1) *> preparedStatement
                     .setNull(2, MysqlType.DOUBLE.jdbcType) *> preparedStatement
                     .executeUpdate()
          _ <- statement.executeUpdate("DROP TABLE `client_statement_double_table`")
        yield count
      },
      1
    )
  }

  test("BigDecimal values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _ <- statement.executeUpdate(
                 "CREATE TABLE `client_statement_bigdecimal_table`(`c1` DECIMAL NOT NULL, `c2` DECIMAL NULL)"
               )
          preparedStatement <-
            conn.clientPreparedStatement("INSERT INTO `client_statement_bigdecimal_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- preparedStatement.setBigDecimal(1, BigDecimal.decimal(1.1)) *> preparedStatement
                     .setNull(2, MysqlType.DECIMAL.jdbcType) *> preparedStatement
                     .executeUpdate()
          _ <- statement.executeUpdate("DROP TABLE `client_statement_bigdecimal_table`")
        yield count
      },
      1
    )
  }

  test("String values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _ <- statement.executeUpdate(
                 "CREATE TABLE `client_statement_string_table`(`c1` VARCHAR(255) NOT NULL, `c2` VARCHAR(255) NULL)"
               )
          preparedStatement <-
            conn.clientPreparedStatement("INSERT INTO `client_statement_string_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- preparedStatement.setString(1, "test") *> preparedStatement
                     .setNull(2, MysqlType.VARCHAR.jdbcType) *> preparedStatement
                     .executeUpdate()
          _ <- statement.executeUpdate("DROP TABLE `client_statement_string_table`")
        yield count
      },
      1
    )
  }

  test("Array[Byte] values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _ <- statement.executeUpdate(
                 "CREATE TABLE `client_statement_bytes_table`(`c1` BINARY(10) NOT NULL, `c2` BINARY NULL)"
               )
          preparedStatement <-
            conn.clientPreparedStatement("INSERT INTO `client_statement_bytes_table`(`c1`, `c2`) VALUES (?, ?)")
          count <-
            preparedStatement.setBytes(1, Array[Byte](98, 105, 110, 97, 114, 121)) *> preparedStatement
              .setNull(2, MysqlType.BINARY.jdbcType) *> preparedStatement
              .executeUpdate()
          _ <- statement.executeUpdate("DROP TABLE `client_statement_bytes_table`")
        yield count
      },
      1
    )
  }

  test("java.time.LocalTime values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _ <- statement.executeUpdate("CREATE TABLE `client_statement_time_table`(`c1` TIME NOT NULL, `c2` TIME NULL)")
          preparedStatement <-
            conn.clientPreparedStatement("INSERT INTO `client_statement_time_table`(`c1`, `c2`) VALUES (?, ?)")
          count <-
            preparedStatement.setTime(1, LocalTime.of(12, 34, 56)) *> preparedStatement.setNull(
              2,
              MysqlType.TIME.jdbcType
            ) *> preparedStatement.executeUpdate()
          _ <- statement.executeUpdate("DROP TABLE `client_statement_time_table`")
        yield count
      },
      1
    )
  }

  test("java.time.LocalDate values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _ <- statement.executeUpdate("CREATE TABLE `client_statement_date_table`(`c1` DATE NOT NULL, `c2` DATE NULL)")
          preparedStatement <-
            conn.clientPreparedStatement("INSERT INTO `client_statement_date_table`(`c1`, `c2`) VALUES (?, ?)")
          count <-
            preparedStatement.setDate(1, LocalDate.of(2020, 1, 1)) *> preparedStatement.setNull(
              2,
              MysqlType.DATE.jdbcType
            ) *> preparedStatement.executeUpdate()
          _ <- statement.executeUpdate("DROP TABLE `client_statement_date_table`")
        yield count
      },
      1
    )
  }

  test("java.time.LocalDateTime values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _ <- statement.executeUpdate(
                 "CREATE TABLE `client_statement_datetime_table`(`c1` TIMESTAMP NOT NULL, `c2` TIMESTAMP NULL)"
               )
          preparedStatement <-
            conn.clientPreparedStatement("INSERT INTO `client_statement_datetime_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- preparedStatement.setTimestamp(1, LocalDateTime.of(2020, 1, 1, 12, 34, 56)) *> preparedStatement
                     .setNull(2, MysqlType.TIMESTAMP.jdbcType) *> preparedStatement.executeUpdate()
          _ <- statement.executeUpdate("DROP TABLE `client_statement_datetime_table`")
        yield count
      },
      1
    )
  }

  test("java.time.Year values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _ <- statement.executeUpdate("CREATE TABLE `client_statement_year_table`(`c1` YEAR NOT NULL, `c2` YEAR NULL)")
          preparedStatement <-
            conn.clientPreparedStatement("INSERT INTO `client_statement_year_table`(`c1`, `c2`) VALUES (?, ?)")
          count <-
            preparedStatement.setInt(1, 2020) *> preparedStatement
              .setNull(2, MysqlType.YEAR.jdbcType) *> preparedStatement
              .executeUpdate()
          _ <- statement.executeUpdate("DROP TABLE `client_statement_year_table`")
        yield count
      },
      1
    )
  }
