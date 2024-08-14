/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.time.*

import org.typelevel.otel4s.trace.Tracer

import cats.effect.*

import munit.CatsEffectSuite

class StatementQueryTest extends CatsEffectSuite:

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
        yield resultSet.getInt(1)
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
        yield
          val builder = List.newBuilder[(Byte, Byte)]
          while resultSet.next() do
            builder += ((resultSet.getByte(1), resultSet.getByte(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(Byte, Byte)]
          while resultSet.next() do
            builder += ((resultSet.getByte(1), resultSet.getByte(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(Short, Short)]
          while resultSet.next() do
            builder += ((resultSet.getShort(1), resultSet.getShort(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(Short, Short)]
          while resultSet.next() do
            builder += ((resultSet.getShort(1), resultSet.getShort(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(Int, Int)]
          while resultSet.next() do
            builder += ((resultSet.getInt(1), resultSet.getInt(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(Int, Int)]
          while resultSet.next() do
            builder += ((resultSet.getInt(1), resultSet.getInt(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(Int, Int)]
          while resultSet.next() do
            builder += ((resultSet.getInt(1), resultSet.getInt(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(Long, Long)]
          while resultSet.next() do
            builder += ((resultSet.getLong(1), resultSet.getLong(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(Long, Long)]
          while resultSet.next() do
            builder += ((resultSet.getLong(1), resultSet.getLong(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(String, String)]
          while resultSet.next() do
            builder += ((resultSet.getString(1), resultSet.getString(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(Float, Float)]
          while resultSet.next() do
            builder += ((resultSet.getFloat(1), resultSet.getFloat(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(Double, Double)]
          while resultSet.next() do
            builder += ((resultSet.getDouble(1), resultSet.getDouble(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(BigDecimal, BigDecimal)]
          while resultSet.next() do
            builder += ((resultSet.getBigDecimal(1), resultSet.getBigDecimal(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(LocalDate, LocalDate)]
          while resultSet.next() do
            builder += ((resultSet.getDate(1), resultSet.getDate(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(LocalTime, LocalTime)]
          while resultSet.next() do
            builder += ((resultSet.getTime(1), resultSet.getTime(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(LocalDateTime, LocalDateTime)]
          while resultSet.next() do
            builder += ((resultSet.getTimestamp(1), resultSet.getTimestamp(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(LocalDateTime, LocalDateTime)]
          while resultSet.next() do
            builder += ((resultSet.getTimestamp(1), resultSet.getTimestamp(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(Short, Short)]
          while resultSet.next() do
            builder += ((resultSet.getShort(1), resultSet.getShort(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(String, String)]
          while resultSet.next() do
            builder += ((resultSet.getString(1), resultSet.getString(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(String, String)]
          while resultSet.next() do
            builder += ((resultSet.getString(1), resultSet.getString(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(String, Array[Byte])]
          while resultSet.next() do
            builder += ((resultSet.getBytes(1).mkString(":"), resultSet.getBytes(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(String, String)]
          while resultSet.next() do
            builder += ((resultSet.getString(1), resultSet.getString(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(String, String)]
          while resultSet.next() do
            builder += ((resultSet.getString(1), resultSet.getString(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(String, String)]
          while resultSet.next() do
            builder += ((resultSet.getString(1), resultSet.getString(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(String, String)]
          while resultSet.next() do
            builder += ((resultSet.getString(1), resultSet.getString(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(String, String)]
          while resultSet.next() do
            builder += ((resultSet.getString(1), resultSet.getString(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(String, String)]
          while resultSet.next() do
            builder += ((resultSet.getString(1), resultSet.getString(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(String, String)]
          while resultSet.next() do
            builder += ((resultSet.getString(1), resultSet.getString(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(String, String)]
          while resultSet.next() do
            builder += ((resultSet.getString(1), resultSet.getString(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(String, String)]
          while resultSet.next() do
            builder += ((resultSet.getString(1), resultSet.getString(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(String, String)]
          while resultSet.next() do
            builder += ((resultSet.getString(1), resultSet.getString(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(String, String)]
          while resultSet.next() do
            builder += ((resultSet.getString(1), resultSet.getString(2)))
          builder.result()
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
        yield
          val builder = List.newBuilder[(String, String)]
          while resultSet.next() do
            builder += ((resultSet.getString(1), resultSet.getString(2)))
          builder.result()
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
