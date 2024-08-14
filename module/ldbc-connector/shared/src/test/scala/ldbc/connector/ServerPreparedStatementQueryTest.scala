/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.time.*

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import munit.CatsEffectSuite

import ldbc.connector.data.MysqlType

class ServerPreparedStatementQueryTest extends CatsEffectSuite:

  given Tracer[IO] = Tracer.noop[IO]

  private val connection = Connection[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = Some("password"),
    database = Some("connector_test"),
    ssl      = SSL.Trusted
  )

  test("The server's PreparedStatement may use NULL as a parameter.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `bit`, `bit_null` FROM `all_types` WHERE `bit_null` <=> ?")
          resultSet <- statement.setNull(1, MysqlType.BIT.jdbcType) *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(Byte, Byte)]
            while resultSet.next() do
              builder += ((resultSet.getByte(1), resultSet.getByte(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((1.toByte, 0.toByte))
    )
  }

  test("Server PreparedStatement should be able to retrieve BIT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `bit`, `bit_null` FROM `all_types` WHERE `bit` = ?")
          resultSet <- statement.setByte(1, 1.toByte) *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(Byte, Byte)]
            while resultSet.next() do
              builder += ((resultSet.getByte(1), resultSet.getByte(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((1.toByte, 0.toByte))
    )
  }

  test("Server PreparedStatement should be able to retrieve TINYINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `tinyint`, `tinyint_null` FROM `all_types` WHERE `tinyint` = ?")
          resultSet <- statement.setByte(1, 127.toByte) *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(Byte, Byte)]
            while resultSet.next() do
              builder += ((resultSet.getByte(1), resultSet.getByte(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((127.toByte, 0.toByte))
    )
  }

  test("Server PreparedStatement should be able to retrieve unsigned TINYINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement(
              "SELECT `tinyint_unsigned`, `tinyint_unsigned_null` FROM `all_types` WHERE `tinyint_unsigned` = ?"
            )
          resultSet <- statement.setShort(1, 255.toShort) *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(Short, Short)]
            while resultSet.next() do
              builder += ((resultSet.getShort(1), resultSet.getShort(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((255.toShort, 0.toShort))
    )
  }

  test("Server PreparedStatement should be able to retrieve SMALLINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `smallint`, `smallint_null` FROM `all_types` WHERE `smallint` = ?")
          resultSet <- statement.setShort(1, 32767.toShort) *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(Short, Short)]
            while resultSet.next() do
              builder += ((resultSet.getShort(1), resultSet.getShort(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((32767.toShort, 0.toShort))
    )
  }

  test("Server PreparedStatement should be able to retrieve unsigned SMALLINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement(
              "SELECT `smallint_unsigned`, `smallint_unsigned_null` FROM `all_types` WHERE `smallint_unsigned` = ?"
            )
          resultSet <- statement.setInt(1, 65535) *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(Int, Int)]
            while resultSet.next() do
              builder += ((resultSet.getInt(1), resultSet.getInt(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((65535, 0))
    )
  }

  test("Server PreparedStatement should be able to retrieve MEDIUMINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `mediumint`, `mediumint_null` FROM `all_types` WHERE `mediumint` = ?")
          resultSet <- statement.setInt(1, 8388607) *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(Int, Int)]
            while resultSet.next() do
              builder += ((resultSet.getInt(1), resultSet.getInt(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((8388607, 0))
    )
  }

  test("Server PreparedStatement should be able to retrieve INT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `int`, `int_null` FROM `all_types` WHERE `int` = ?")
          resultSet <- statement.setInt(1, 2147483647) *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(Int, Int)]
            while resultSet.next() do
              builder += ((resultSet.getInt(1), resultSet.getInt(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((2147483647, 0))
    )
  }

  test("Server PreparedStatement should be able to retrieve unsigned INT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement(
                         "SELECT `int_unsigned`, `int_unsigned_null` FROM `all_types` WHERE `int_unsigned` = ?"
                       )
          resultSet <- statement.setLong(1, 4294967295L) *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(Long, Long)]
            while resultSet.next() do
              builder += ((resultSet.getLong(1), resultSet.getLong(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((4294967295L, 0L))
    )
  }

  test("Server PreparedStatement should be able to retrieve BIGINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `bigint`, `bigint_null` FROM `all_types` WHERE `bigint` = ?")
          resultSet <- statement.setLong(1, 9223372036854775807L) *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(Long, Long)]
            while resultSet.next() do
              builder += ((resultSet.getLong(1), resultSet.getLong(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((9223372036854775807L, 0L))
    )
  }

  test("Server PreparedStatement should be able to retrieve unsigned BIGINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement(
                         "SELECT `bigint_unsigned`, `bigint_unsigned_null` FROM `all_types` WHERE `bigint_unsigned` = ?"
                       )
          resultSet <-
            statement.setString(1, "18446744073709551615") *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(String, String)]
            while resultSet.next() do
              builder += ((resultSet.getString(1), resultSet.getString(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List(("18446744073709551615", null))
    )
  }

  test("Server PreparedStatement should be able to retrieve FLOAT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `float`, `float_null` FROM `all_types` WHERE `float` > ?")
          resultSet <- statement.setFloat(1, 3.40282e38f) *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(Float, Float)]
            while resultSet.next() do
              builder += ((resultSet.getFloat(1), resultSet.getFloat(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((java.lang.Float.intBitsToFloat(2139095039), 0f))
    )
  }

  test("Server PreparedStatement should be able to retrieve DOUBLE type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `double`, `double_null` FROM `all_types` WHERE `double` = ?")
          resultSet <- statement.setDouble(1, 1.7976931348623157e308) *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(Double, Double)]
            while resultSet.next() do
              builder += ((resultSet.getDouble(1), resultSet.getDouble(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((1.7976931348623157e308, 0.toDouble))
    )
  }

  test("Server PreparedStatement should be able to retrieve DECIMAL type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `decimal`, `decimal_null` FROM `all_types` WHERE `decimal` = ?")
          resultSet <-
            statement.setBigDecimal(1, BigDecimal.decimal(9999999.99)) *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(BigDecimal, BigDecimal)]
            while resultSet.next() do
              builder += ((resultSet.getBigDecimal(1), resultSet.getBigDecimal(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((BigDecimal.decimal(9999999.99), null))
    )
  }

  test("Server PreparedStatement should be able to retrieve DATE type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `date`, `date_null` FROM `all_types` WHERE `date` = ?")
          resultSet <- statement.setDate(1, LocalDate.of(2020, 1, 1)) *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(LocalDate, LocalDate)]
            while resultSet.next() do
              builder += ((resultSet.getDate(1), resultSet.getDate(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((LocalDate.of(2020, 1, 1), null))
    )
  }

  test("Server PreparedStatement should be able to retrieve TIME type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `time`, `time_null` FROM `all_types` WHERE `time` = ?")
          resultSet <- statement.setTime(1, LocalTime.of(12, 34, 56)) *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(LocalTime, LocalTime)]
            while resultSet.next() do
              builder += ((resultSet.getTime(1), resultSet.getTime(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((LocalTime.of(12, 34, 56), null))
    )
  }

  test("Server PreparedStatement should be able to retrieve DATETIME type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `datetime`, `datetime_null` FROM `all_types` WHERE `datetime` = ?")
          resultSet <-
            statement.setTimestamp(1, LocalDateTime.of(2020, 1, 1, 12, 34, 56)) *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(LocalDateTime, LocalDateTime)]
            while resultSet.next() do
              builder += ((resultSet.getTimestamp(1), resultSet.getTimestamp(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((LocalDateTime.of(2020, 1, 1, 12, 34, 56), null))
    )
  }

  test("Server PreparedStatement should be able to retrieve TIMESTAMP type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `timestamp`, `timestamp_null` FROM `all_types` WHERE `timestamp` = ?")
          resultSet <-
            statement.setTimestamp(1, LocalDateTime.of(2020, 1, 1, 12, 34, 56)) *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(LocalDateTime, LocalDateTime)]
            while resultSet.next() do
              builder += ((resultSet.getTimestamp(1), resultSet.getTimestamp(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((LocalDateTime.of(2020, 1, 1, 12, 34, 56), null))
    )
  }

  test("Server PreparedStatement should be able to retrieve YEAR type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `year`, `year_null` FROM `all_types` WHERE `year` = ?")
          resultSet <- statement.setInt(1, 2020) *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(Short, Short)]
            while resultSet.next() do
              builder += ((resultSet.getShort(1), resultSet.getShort(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((2020.toShort, 0.toShort))
    )
  }

  test("Server PreparedStatement should be able to retrieve CHAR type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `char`, `char_null` FROM `all_types` WHERE `char` = ?")
          resultSet <- statement.setString(1, "char") *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(String, String)]
            while resultSet.next() do
              builder += ((resultSet.getString(1), resultSet.getString(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List(("char", null))
    )
  }

  test("Server PreparedStatement should be able to retrieve VARCHAR type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `varchar`, `varchar_null` FROM `all_types` WHERE `varchar` = ?")
          resultSet <- statement.setString(1, "varchar") *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(String, String)]
            while resultSet.next() do
              builder += ((resultSet.getString(1), resultSet.getString(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List(("varchar", null))
    )
  }

  test("Server PreparedStatement should be able to retrieve BINARY type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `binary`, `binary_null` FROM `all_types` WHERE `binary` = ?")
          resultSet <-
            statement.setBytes(1, Array[Byte](98, 105, 110, 97, 114, 121, 0, 0, 0, 0)) *> statement
              .executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(String, Array[Byte])]
            while resultSet.next() do
              builder += ((resultSet.getBytes(1).mkString(":"), resultSet.getBytes(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List((Array[Byte](98, 105, 110, 97, 114, 121, 0, 0, 0, 0).mkString(":"), null))
    )
  }

  test("Server PreparedStatement should be able to retrieve VARBINARY type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `varbinary`, `varbinary_null` FROM `all_types` WHERE `varbinary` = ?")
          resultSet <- statement.setString(1, "varbinary") *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(String, String)]
            while resultSet.next() do
              builder += ((resultSet.getString(1), resultSet.getString(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List(("varbinary", null))
    )
  }

  test("Server PreparedStatement should be able to retrieve TINYBLOB type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `tinyblob`, `tinyblob_null` FROM `all_types` WHERE `tinyblob` = ?")
          resultSet <- statement.setString(1, "tinyblob") *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(String, String)]
            while resultSet.next() do
              builder += ((resultSet.getString(1), resultSet.getString(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List(("tinyblob", null))
    )
  }

  test("Server PreparedStatement should be able to retrieve BLOB type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `blob`, `blob_null` FROM `all_types` WHERE `blob` = ?")
          resultSet <- statement.setString(1, "blob") *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(String, String)]
            while resultSet.next() do
              builder += ((resultSet.getString(1), resultSet.getString(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List(("blob", null))
    )
  }

  test("Server PreparedStatement should be able to retrieve MEDIUMBLOB type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement(
                         "SELECT `mediumblob`, `mediumblob_null` FROM `all_types` WHERE `mediumblob` = ?"
                       )
          resultSet <- statement.setString(1, "mediumblob") *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(String, String)]
            while resultSet.next() do
              builder += ((resultSet.getString(1), resultSet.getString(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List(("mediumblob", null))
    )
  }

  test("Server PreparedStatement should be able to retrieve LONGBLOB type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `longblob`, `longblob_null` FROM `all_types` WHERE `longblob` = ?")
          resultSet <- statement.setString(1, "longblob") *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(String, String)]
            while resultSet.next() do
              builder += ((resultSet.getString(1), resultSet.getString(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List(("longblob", null))
    )
  }

  test("Server PreparedStatement should be able to retrieve TINYTEXT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `tinytext`, `tinytext_null` FROM `all_types` WHERE `tinytext` = ?")
          resultSet <- statement.setString(1, "tinytext") *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(String, String)]
            while resultSet.next() do
              builder += ((resultSet.getString(1), resultSet.getString(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List(("tinytext", null))
    )
  }

  test("Server PreparedStatement should be able to retrieve TEXT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `text`, `text_null` FROM `all_types` WHERE `text` = ?")
          resultSet <- statement.setString(1, "text") *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(String, String)]
            while resultSet.next() do
              builder += ((resultSet.getString(1), resultSet.getString(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List(("text", null))
    )
  }

  test("Server PreparedStatement should be able to retrieve MEDIUMTEXT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement(
                         "SELECT `mediumtext`, `mediumtext_null` FROM `all_types` WHERE `mediumtext` = ?"
                       )
          resultSet <- statement.setString(1, "mediumtext") *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(String, String)]
            while resultSet.next() do
              builder += ((resultSet.getString(1), resultSet.getString(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List(("mediumtext", null))
    )
  }

  test("Server PreparedStatement should be able to retrieve LONGTEXT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `longtext`, `longtext_null` FROM `all_types` WHERE `longtext` = ?")
          resultSet <- statement.setString(1, "longtext") *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(String, String)]
            while resultSet.next() do
              builder += ((resultSet.getString(1), resultSet.getString(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List(("longtext", null))
    )
  }

  test("Server PreparedStatement should be able to retrieve ENUM type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `enum`, `enum_null` FROM `all_types` WHERE `enum` = ?")
          resultSet <- statement.setString(1, "a") *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(String, String)]
            while resultSet.next() do
              builder += ((resultSet.getString(1), resultSet.getString(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List(("a", null))
    )
  }

  test("Server PreparedStatement should be able to retrieve SET type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `set`, `set_null` FROM `all_types` WHERE `set` = ?")
          resultSet <- statement.setString(1, "a,b") *> statement.executeQuery()
          decoded <- IO {
            val builder = List.newBuilder[(String, String)]
            while resultSet.next() do
              builder += ((resultSet.getString(1), resultSet.getString(2)))
            builder.result()
          }
          _ <- statement.close()
        yield decoded
      },
      List(("a,b", null))
    )
  }
