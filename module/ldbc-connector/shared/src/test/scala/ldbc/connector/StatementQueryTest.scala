/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.time.*

import cats.Monad

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

class StatementQueryTest extends FTestPlatform:

  given Tracer[IO] = Tracer.noop[IO]

  private val connection = Connection[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = Some("password"),
    ssl      = SSL.Trusted
  )

  test("Statement should be able to execute a query") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT 1")
          value <- resultSet.getInt(1)
        yield value
      },
      1
    )
  }

  test("Statement should be able to retrieve BIT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `bit`, `bit_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (Byte, Byte)](resultSet.next()) {
            for
              v1 <- resultSet.getByte(1)
              v2 <- resultSet.getByte(2)
            yield (v1, v2)
          }
        yield result
      },
      List((1.toByte, 0.toByte))
    )
  }

  test("Statement should be able to retrieve TINYINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `tinyint`, `tinyint_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (Byte, Byte)](resultSet.next()) {
            for
              v1 <- resultSet.getByte(1)
              v2 <- resultSet.getByte(2)
            yield (v1, v2)
          }
        yield result
      },
      List((127.toByte, 0.toByte))
    )
  }

  test("Statement should be able to retrieve unsigned TINYINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery(
                         "SELECT `tinyint_unsigned`, `tinyint_unsigned_null` FROM `connector_test`.`all_types`"
                       )
          result <- Monad[IO].whileM[List, (Short, Short)](resultSet.next()) {
            for
              v1 <- resultSet.getShort(1)
              v2 <- resultSet.getShort(2)
            yield (v1, v2)
          }
        yield result
      },
      List((255.toShort, 0.toShort))
    )
  }

  test("Statement should be able to retrieve SMALLINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `smallint`, `smallint_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (Short, Short)](resultSet.next()) {
            for
              v1 <- resultSet.getShort(1)
              v2 <- resultSet.getShort(2)
            yield (v1, v2)
          }
        yield result
      },
      List((32767.toShort, 0.toShort))
    )
  }

  test("Statement should be able to retrieve unsigned SMALLINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery(
                         "SELECT `smallint_unsigned`, `smallint_unsigned_null` FROM `connector_test`.`all_types`"
                       )
          result <- Monad[IO].whileM[List, (Int, Int)](resultSet.next()) {
            for
              v1 <- resultSet.getInt(1)
              v2 <- resultSet.getInt(2)
            yield (v1, v2)
          }
        yield result
      },
      List((65535, 0))
    )
  }

  test("Statement should be able to retrieve MEDIUMINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `mediumint`, `mediumint_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (Int, Int)](resultSet.next()) {
            for
              v1 <- resultSet.getInt(1)
              v2 <- resultSet.getInt(2)
            yield (v1, v2)
          }
        yield result
      },
      List((8388607, 0))
    )
  }

  test("Statement should be able to retrieve INT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `int`, `int_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (Int, Int)](resultSet.next()) {
            for
              v1 <- resultSet.getInt(1)
              v2 <- resultSet.getInt(2)
            yield (v1, v2)
          }
        yield result
      },
      List((2147483647, 0))
    )
  }

  test("Statement should be able to retrieve unsigned INT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <-
            statement.executeQuery("SELECT `int_unsigned`, `int_unsigned_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (Long, Long)](resultSet.next()) {
            for
              v1 <- resultSet.getLong(1)
              v2 <- resultSet.getLong(2)
            yield (v1, v2)
          }
        yield result
      },
      List((4294967295L, 0L))
    )
  }

  test("Statement should be able to retrieve BIGINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `bigint`, `bigint_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (Long, Long)](resultSet.next()) {
            for
              v1 <- resultSet.getLong(1)
              v2 <- resultSet.getLong(2)
            yield (v1, v2)
          }
        yield result
      },
      List((9223372036854775807L, 0L))
    )
  }

  test("Statement should be able to retrieve unsigned BIGINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <-
            statement.executeQuery("SELECT `bigint_unsigned`, `bigint_unsigned_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield result
      },
      List(("18446744073709551615", null))
    )
  }

  test("Statement should be able to retrieve FLOAT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `float`, `float_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (Float, Float)](resultSet.next()) {
            for
              v1 <- resultSet.getFloat(1)
              v2 <- resultSet.getFloat(2)
            yield (v1, v2)
          }
        yield result
      },
      List((3.40282e38f, 0f))
    )
  }

  test("Statement should be able to retrieve DOUBLE type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `double`, `double_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (Double, Double)](resultSet.next()) {
            for
              v1 <- resultSet.getDouble(1)
              v2 <- resultSet.getDouble(2)
            yield (v1, v2)
          }
        yield result
      },
      List((1.7976931348623157e308, 0.toDouble))
    )
  }

  test("Statement should be able to retrieve DECIMAL type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `decimal`, `decimal_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (BigDecimal, BigDecimal)](resultSet.next()) {
            for
              v1 <- resultSet.getBigDecimal(1)
              v2 <- resultSet.getBigDecimal(2)
            yield (v1, v2)
          }
        yield result
      },
      List((BigDecimal.decimal(9999999.99), null))
    )
  }

  test("Statement should be able to retrieve DATE type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `date`, `date_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (LocalDate, LocalDate)](resultSet.next()) {
            for
              v1 <- resultSet.getDate(1)
              v2 <- resultSet.getDate(2)
            yield (v1, v2)
          }
        yield result
      },
      List((LocalDate.of(2020, 1, 1), null))
    )
  }

  test("Statement should be able to retrieve TIME type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `time`, `time_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (LocalTime, LocalTime)](resultSet.next()) {
            for
              v1 <- resultSet.getTime(1)
              v2 <- resultSet.getTime(2)
            yield (v1, v2)
          }
        yield result
      },
      List((LocalTime.of(12, 34, 56), null))
    )
  }

  test("Statement should be able to retrieve DATETIME type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `datetime`, `datetime_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (LocalDateTime, LocalDateTime)](resultSet.next()) {
            for
              v1 <- resultSet.getTimestamp(1)
              v2 <- resultSet.getTimestamp(2)
            yield (v1, v2)
          }
        yield result
      },
      List((LocalDateTime.of(2020, 1, 1, 12, 34, 56), null))
    )
  }

  test("Statement should be able to retrieve TIMESTAMP type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `timestamp`, `timestamp_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (LocalDateTime, LocalDateTime)](resultSet.next()) {
            for
              v1 <- resultSet.getTimestamp(1)
              v2 <- resultSet.getTimestamp(2)
            yield (v1, v2)
          }
        yield result
      },
      List((LocalDateTime.of(2020, 1, 1, 12, 34, 56), null))
    )
  }

  test("Statement should be able to retrieve YEAR type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `year`, `year_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (Short, Short)](resultSet.next()) {
            for
              v1 <- resultSet.getShort(1)
              v2 <- resultSet.getShort(2)
            yield (v1, v2)
          }
        yield result
      },
      List((2020.toShort, 0.toShort))
    )
  }

  test("Statement should be able to retrieve CHAR type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `char`, `char_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield result
      },
      List(("char", null))
    )
  }

  test("Statement should be able to retrieve VARCHAR type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `varchar`, `varchar_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield result
      },
      List(("varchar", null))
    )
  }

  test("Statement should be able to retrieve BINARY type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `binary`, `binary_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (String, Array[Byte])](resultSet.next()) {
            for
              v1 <- resultSet.getBytes(1)
              v2 <- resultSet.getBytes(2)
            yield (v1.mkString(":"), v2)
          }
        yield result
      },
      List((Array[Byte](98, 105, 110, 97, 114, 121, 0, 0, 0, 0).mkString(":"), null))
    )
  }

  test("Statement should be able to retrieve VARBINARY type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `varbinary`, `varbinary_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield result
      },
      List(("varbinary", null))
    )
  }

  test("Statement should be able to retrieve TINYBLOB type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `tinyblob`, `tinyblob_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield result
      },
      List(("tinyblob", null))
    )
  }

  test("Statement should be able to retrieve BLOB type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `blob`, `blob_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield result
      },
      List(("blob", null))
    )
  }

  test("Statement should be able to retrieve MEDIUMBLOB type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <-
            statement.executeQuery("SELECT `mediumblob`, `mediumblob_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield result
      },
      List(("mediumblob", null))
    )
  }

  test("Statement should be able to retrieve LONGBLOB type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `longblob`, `longblob_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield result
      },
      List(("longblob", null))
    )
  }

  test("Statement should be able to retrieve TINYTEXT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `tinytext`, `tinytext_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield result
      },
      List(("tinytext", null))
    )
  }

  test("Statement should be able to retrieve TEXT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `text`, `text_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield result
      },
      List(("text", null))
    )
  }

  test("Statement should be able to retrieve MEDIUMTEXT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <-
            statement.executeQuery("SELECT `mediumtext`, `mediumtext_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield result
      },
      List(("mediumtext", null))
    )
  }

  test("Statement should be able to retrieve LONGTEXT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `longtext`, `longtext_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield result
      },
      List(("longtext", null))
    )
  }

  test("Statement should be able to retrieve ENUM type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `enum`, `enum_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield result
      },
      List(("a", null))
    )
  }

  test("Statement should be able to retrieve SET type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `set`, `set_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield result
      },
      List(("a,b", null))
    )
  }

  test("Statement should be able to retrieve JSON type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `json`, `json_null` FROM `connector_test`.`all_types`")
          result <- Monad[IO].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield result
      },
      List(("{\"a\": 1}", null))
    )
  }

  test("If the query is not being executed, getting the ResultSet is None.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.getResultSet()
        yield resultSet
      },
      None
    )
  }

  test("When the query is executed, the ResultSet is obtained Some.") {
    assertIOBoolean(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          hasResult <- statement.execute("SELECT 1")
          resultSet <- statement.getResultSet()
        yield hasResult && resultSet.nonEmpty
      }
    )
  }

  test("If the query has not been executed, the get updateCount is -1.") {
    assertIO(
      connection.use { conn =>
        for
          statement   <- conn.createStatement()
          updateCount <- statement.getUpdateCount()
        yield updateCount
      },
      -1
    )
  }

  test("When the query is executed, the updateCount is obtained 0 or 1+.") {
    assertIO(
      connection.use { conn =>
        for
          statement   <- conn.createStatement()
          updateCount <- statement.executeUpdate("USE `connector_test`") *> statement.getUpdateCount()
        yield updateCount
      },
      0
    )
  }
