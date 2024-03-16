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

class ServerPreparedStatementUpdateTest extends CatsEffectSuite:

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
          _ <- conn
                 .statement("CREATE TABLE `server_statement_boolean_table`(`c1` BOOLEAN NOT NULL, `c2` BOOLEAN NULL)")
                 .executeUpdate()
          statement <-
            conn.serverPreparedStatement("INSERT INTO `server_statement_boolean_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- statement.setBoolean(1, true) *> statement.setBoolean(2, None) *> statement.executeUpdate()
          _     <- conn.statement("DROP TABLE `server_statement_boolean_table`").executeUpdate()
        yield count
      },
      1
    )
  }

  test("Byte values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          _ <- conn
                 .statement("CREATE TABLE `server_statement_byte_table`(`c1` BIT NOT NULL, `c2` BIT NULL)")
                 .executeUpdate()
          statement <-
            conn.serverPreparedStatement("INSERT INTO `server_statement_byte_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- statement.setByte(1, 1.toByte) *> statement.setByte(2, None) *> statement.executeUpdate()
          _     <- conn.statement("DROP TABLE `server_statement_byte_table`").executeUpdate()
        yield count
      },
      1
    )
  }

  test("Short values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          _ <- conn
                 .statement("CREATE TABLE `server_statement_short_table`(`c1` TINYINT NOT NULL, `c2` TINYINT NULL)")
                 .executeUpdate()
          statement <-
            conn.serverPreparedStatement("INSERT INTO `server_statement_short_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- statement.setShort(1, 1.toShort) *> statement.setShort(2, None) *> statement.executeUpdate()
          _     <- conn.statement("DROP TABLE `server_statement_short_table`").executeUpdate()
        yield count
      },
      1
    )
  }

  test("Int values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          _ <- conn
                 .statement("CREATE TABLE `server_statement_int_table`(`c1` SMALLINT NOT NULL, `c2` SMALLINT NULL)")
                 .executeUpdate()
          statement <-
            conn.serverPreparedStatement("INSERT INTO `server_statement_int_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- statement.setInt(1, 1) *> statement.setInt(2, None) *> statement.executeUpdate()
          _     <- conn.statement("DROP TABLE `server_statement_int_table`").executeUpdate()
        yield count
      },
      1
    )
  }

  test("Long values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          _ <- conn
                 .statement("CREATE TABLE `server_statement_long_table`(`c1` BIGINT NOT NULL, `c2` BIGINT NULL)")
                 .executeUpdate()
          statement <-
            conn.serverPreparedStatement("INSERT INTO `server_statement_long_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- statement.setLong(1, Long.MaxValue) *> statement.setLong(2, None) *> statement.executeUpdate()
          _     <- conn.statement("DROP TABLE `server_statement_long_table`").executeUpdate()
        yield count
      },
      1
    )
  }

  test("BigInt values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          _ <-
            conn
              .statement(
                "CREATE TABLE `server_statement_bigint_table`(`c1` BIGINT unsigned NOT NULL, `c2` BIGINT unsigned NULL)"
              )
              .executeUpdate()
          statement <-
            conn.serverPreparedStatement("INSERT INTO `server_statement_bigint_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- statement.setBigInt(1, BigInt("18446744073709551615")) *> statement.setBigInt(2, None) *> statement
                     .executeUpdate()
          _ <- conn.statement("DROP TABLE `server_statement_bigint_table`").executeUpdate()
        yield count
      },
      1
    )
  }

  test("Float values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          _ <- conn
                 .statement("CREATE TABLE `server_statement_float_table`(`c1` FLOAT NOT NULL, `c2` FLOAT NULL)")
                 .executeUpdate()
          statement <-
            conn.serverPreparedStatement("INSERT INTO `server_statement_float_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- statement.setFloat(1, 1.1f) *> statement.setFloat(2, None) *> statement.executeUpdate()
          _     <- conn.statement("DROP TABLE `server_statement_float_table`").executeUpdate()
        yield count
      },
      1
    )
  }

  test("Double values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          _ <- conn
                 .statement("CREATE TABLE `server_statement_double_table`(`c1` DOUBLE NOT NULL, `c2` DOUBLE NULL)")
                 .executeUpdate()
          statement <-
            conn.serverPreparedStatement("INSERT INTO `server_statement_double_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- statement.setDouble(1, 1.1) *> statement.setDouble(2, None) *> statement.executeUpdate()
          _     <- conn.statement("DROP TABLE `server_statement_double_table`").executeUpdate()
        yield count
      },
      1
    )
  }

  test("BigDecimal values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          _ <-
            conn
              .statement("CREATE TABLE `server_statement_bigdecimal_table`(`c1` DECIMAL NOT NULL, `c2` DECIMAL NULL)")
              .executeUpdate()
          statement <-
            conn.serverPreparedStatement("INSERT INTO `server_statement_bigdecimal_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- statement.setBigDecimal(1, BigDecimal.decimal(1.1)) *> statement.setBigDecimal(2, None) *> statement
                     .executeUpdate()
          _ <- conn.statement("DROP TABLE `server_statement_bigdecimal_table`").executeUpdate()
        yield count
      },
      1
    )
  }

  test("String values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          _ <- conn
                 .statement(
                   "CREATE TABLE `server_statement_string_table`(`c1` VARCHAR(255) NOT NULL, `c2` VARCHAR(255) NULL)"
                 )
                 .executeUpdate()
          statement <-
            conn.serverPreparedStatement("INSERT INTO `server_statement_string_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- statement.setString(1, "test") *> statement.setString(2, None) *> statement.executeUpdate()
          _     <- conn.statement("DROP TABLE `server_statement_string_table`").executeUpdate()
        yield count
      },
      1
    )
  }

  test("Array[Byte] values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          _ <- conn
                 .statement("CREATE TABLE `server_statement_bytes_table`(`c1` BINARY(10) NOT NULL, `c2` BINARY NULL)")
                 .executeUpdate()
          statement <-
            conn.serverPreparedStatement("INSERT INTO `server_statement_bytes_table`(`c1`, `c2`) VALUES (?, ?)")
          count <-
            statement.setBytes(1, Array[Byte](98, 105, 110, 97, 114, 121)) *> statement.setBytes(2, None) *> statement
              .executeUpdate()
          _ <- conn.statement("DROP TABLE `server_statement_bytes_table`").executeUpdate()
        yield count
      },
      1
    )
  }

  test("java.time.LocalTime values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          _ <- conn
                 .statement("CREATE TABLE `server_statement_time_table`(`c1` TIME NOT NULL, `c2` TIME NULL)")
                 .executeUpdate()
          statement <-
            conn.serverPreparedStatement("INSERT INTO `server_statement_time_table`(`c1`, `c2`) VALUES (?, ?)")
          count <-
            statement.setTime(1, LocalTime.of(12, 34, 56)) *> statement.setTime(2, None) *> statement.executeUpdate()
          _ <- conn.statement("DROP TABLE `server_statement_time_table`").executeUpdate()
        yield count
      },
      1
    )
  }

  test("java.time.LocalDate values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          _ <- conn
                 .statement("CREATE TABLE `server_statement_date_table`(`c1` DATE NOT NULL, `c2` DATE NULL)")
                 .executeUpdate()
          statement <-
            conn.serverPreparedStatement("INSERT INTO `server_statement_date_table`(`c1`, `c2`) VALUES (?, ?)")
          count <-
            statement.setDate(1, LocalDate.of(2020, 1, 1)) *> statement.setDate(2, None) *> statement.executeUpdate()
          _ <- conn.statement("DROP TABLE `server_statement_date_table`").executeUpdate()
        yield count
      },
      1
    )
  }

  test("java.time.LocalDateTime values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          _ <-
            conn
              .statement("CREATE TABLE `server_statement_datetime_table`(`c1` TIMESTAMP NOT NULL, `c2` TIMESTAMP NULL)")
              .executeUpdate()
          statement <-
            conn.serverPreparedStatement("INSERT INTO `server_statement_datetime_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- statement.setTimestamp(1, LocalDateTime.of(2020, 1, 1, 12, 34, 56)) *> statement.setTimestamp(
                     2,
                     None
                   ) *> statement.executeUpdate()
          _ <- conn.statement("DROP TABLE `server_statement_datetime_table`").executeUpdate()
        yield count
      },
      1
    )
  }

  test("java.time.Year values can be set as parameters.") {
    assertIO(
      connection.use { conn =>
        for
          _ <- conn
                 .statement("CREATE TABLE `server_statement_year_table`(`c1` YEAR NOT NULL, `c2` YEAR NULL)")
                 .executeUpdate()
          statement <-
            conn.serverPreparedStatement("INSERT INTO `server_statement_year_table`(`c1`, `c2`) VALUES (?, ?)")
          count <- statement.setYear(1, Year.of(2020)) *> statement.setYear(2, None) *> statement.executeUpdate()
          _     <- conn.statement("DROP TABLE `server_statement_year_table`").executeUpdate()
        yield count
      },
      1
    )
  }
