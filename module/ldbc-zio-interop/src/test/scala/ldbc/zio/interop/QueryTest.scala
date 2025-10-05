/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.zio.interop

import java.time.*

import cats.Monad

import zio.*
import zio.test.*
import zio.test.Assertion.*

import ldbc.connector.*
import ldbc.connector.data.*

object QueryTest extends ZIOSpecDefault:

  private val datasource =
    MySQLDataSource
      .build[Task]("127.0.0.1", 13306, "ldbc")
      .setPassword("password")
      .setDatabase("connector_test")
      .setSSL(SSL.Trusted)

  def spec = suite("QueryTest")(
    test("The client's PreparedStatement may use NULL as a parameter.") {
      datasource.getConnection.use { conn =>
        for
          statement <- conn.prepareStatement("SELECT `bit`, `bit_null` FROM `all_types` WHERE `bit_null` is ?")
          resultSet <- statement.setNull(1, MysqlType.BIT.jdbcType) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (Byte, Byte)](resultSet.next()) {
            for
              v1 <- resultSet.getByte(1)
              v2 <- resultSet.getByte(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List((1.toByte, 0.toByte))))
      }
    },

    test("Client PreparedStatement should be able to retrieve BIT type records") {
      datasource.getConnection.use { conn =>
        for
          statement <- conn.prepareStatement("SELECT `bit`, `bit_null` FROM `all_types` WHERE `bit` = ?")
          resultSet <- statement.setByte(1, 1.toByte) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (Byte, Byte)](resultSet.next()) {
            for
              v1 <- resultSet.getByte(1)
              v2 <- resultSet.getByte(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List((1.toByte, 0.toByte))))
      }
    },

    test("Client PreparedStatement should be able to retrieve TINYINT type records") {
      datasource.getConnection.use { conn =>
        for
          statement <-
            conn.prepareStatement("SELECT `tinyint`, `tinyint_null` FROM `all_types` WHERE `tinyint` = ?")
          resultSet <- statement.setByte(1, 127.toByte) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (Byte, Byte)](resultSet.next()) {
            for
              v1 <- resultSet.getByte(1)
              v2 <- resultSet.getByte(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List((127.toByte, 0.toByte))))
      }
    },

    test("Client PreparedStatement should be able to retrieve unsigned TINYINT type records") {
      datasource.getConnection.use { conn =>
        for
          statement <-
            conn.prepareStatement(
              "SELECT `tinyint_unsigned`, `tinyint_unsigned_null` FROM `all_types` WHERE `tinyint_unsigned` = ?"
            )
          resultSet <- statement.setShort(1, 255.toShort) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (Short, Short)](resultSet.next()) {
            for
              v1 <- resultSet.getShort(1)
              v2 <- resultSet.getShort(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List((255.toShort, 0.toShort))))
      }
    },

    test("Client PreparedStatement should be able to retrieve SMALLINT type records") {
      datasource.getConnection.use { conn =>
        for
          statement <-
            conn.prepareStatement("SELECT `smallint`, `smallint_null` FROM `all_types` WHERE `smallint` = ?")
          resultSet <- statement.setShort(1, 32767.toShort) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (Short, Short)](resultSet.next()) {
            for
              v1 <- resultSet.getShort(1)
              v2 <- resultSet.getShort(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List((32767.toShort, 0.toShort))))
      }
    },

    test("Client PreparedStatement should be able to retrieve unsigned SMALLINT type records") {
      datasource.getConnection.use { conn =>
        for
          statement <-
            conn.prepareStatement(
              "SELECT `smallint_unsigned`, `smallint_unsigned_null` FROM `all_types` WHERE `smallint_unsigned` = ?"
            )
          resultSet <- statement.setInt(1, 65535) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (Int, Int)](resultSet.next()) {
            for
              v1 <- resultSet.getInt(1)
              v2 <- resultSet.getInt(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List((65535, 0))))
      }
    },

    test("Client PreparedStatement should be able to retrieve MEDIUMINT type records") {
      datasource.getConnection.use { conn =>
        for
          statement <-
            conn.prepareStatement("SELECT `mediumint`, `mediumint_null` FROM `all_types` WHERE `mediumint` = ?")
          resultSet <- statement.setInt(1, 8388607) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (Int, Int)](resultSet.next()) {
            for
              v1 <- resultSet.getInt(1)
              v2 <- resultSet.getInt(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List((8388607, 0))))
      }
    },

    test("Client PreparedStatement should be able to retrieve INT type records") {
      datasource.getConnection.use { conn =>
        for
          statement <- conn.prepareStatement("SELECT `int`, `int_null` FROM `all_types` WHERE `int` = ?")
          resultSet <- statement.setInt(1, 2147483647) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (Int, Int)](resultSet.next()) {
            for
              v1 <- resultSet.getInt(1)
              v2 <- resultSet.getInt(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List((2147483647, 0))))
      }
    },

    test("Client PreparedStatement should be able to retrieve unsigned INT type records") {
      datasource.getConnection.use { conn =>
        for
          statement <- conn.prepareStatement(
                         "SELECT `int_unsigned`, `int_unsigned_null` FROM `all_types` WHERE `int_unsigned` = ?"
                       )
          resultSet <- statement.setLong(1, 4294967295L) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (Long, Long)](resultSet.next()) {
            for
              v1 <- resultSet.getLong(1)
              v2 <- resultSet.getLong(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List((4294967295L, 0L))))
      }
    },

    test("Client PreparedStatement should be able to retrieve BIGINT type records") {
      datasource.getConnection.use { conn =>
        for
          statement <-
            conn.prepareStatement("SELECT `bigint`, `bigint_null` FROM `all_types` WHERE `bigint` = ?")
          resultSet <- statement.setLong(1, 9223372036854775807L) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (Long, Long)](resultSet.next()) {
            for
              v1 <- resultSet.getLong(1)
              v2 <- resultSet.getLong(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List((9223372036854775807L, 0L))))
      }
    },

    test("Client PreparedStatement should be able to retrieve unsigned BIGINT type records") {
      datasource.getConnection.use { conn =>
        for
          statement <- conn.prepareStatement(
                         "SELECT `bigint_unsigned`, `bigint_unsigned_null` FROM `all_types` WHERE `bigint_unsigned` = ?"
                       )
          resultSet <- statement.setString(1, "18446744073709551615") *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List(("18446744073709551615", null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve FLOAT type records") {
      datasource.getConnection.use { conn =>
        for
          statement <- conn.prepareStatement("SELECT `float`, `float_null` FROM `all_types` WHERE `float` > ?")
          resultSet <- statement.setFloat(1, 3.4f) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (Float, Float)](resultSet.next()) {
            for
              v1 <- resultSet.getFloat(1)
              v2 <- resultSet.getFloat(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List((3.40282e38f, 0f))))
      }
    },

    test("Client PreparedStatement should be able to retrieve DOUBLE type records") {
      datasource.getConnection.use { conn =>
        for
          statement <-
            conn.prepareStatement("SELECT `double`, `double_null` FROM `all_types` WHERE `double` = ?")
          resultSet <- statement.setDouble(1, 1.7976931348623157e308) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (Double, Double)](resultSet.next()) {
            for
              v1 <- resultSet.getDouble(1)
              v2 <- resultSet.getDouble(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List((1.7976931348623157e308, 0.toDouble))))
      }
    },

    test("Client PreparedStatement should be able to retrieve DECIMAL type records") {
      datasource.getConnection.use { conn =>
        for
          statement <-
            conn.prepareStatement("SELECT `decimal`, `decimal_null` FROM `all_types` WHERE `decimal` = ?")
          resultSet <- statement.setBigDecimal(1, BigDecimal.decimal(9999999.99)) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (BigDecimal, BigDecimal)](resultSet.next()) {
            for
              v1 <- resultSet.getBigDecimal(1)
              v2 <- resultSet.getBigDecimal(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List((BigDecimal.decimal(9999999.99), null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve DATE type records") {
      datasource.getConnection.use { conn =>
        for
          statement <- conn.prepareStatement("SELECT `date`, `date_null` FROM `all_types` WHERE `date` = ?")
          resultSet <- statement.setDate(1, LocalDate.of(2020, 1, 1)) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (LocalDate, LocalDate)](resultSet.next()) {
            for
              v1 <- resultSet.getDate(1)
              v2 <- resultSet.getDate(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List((LocalDate.of(2020, 1, 1), null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve TIME type records") {
      datasource.getConnection.use { conn =>
        for
          statement <- conn.prepareStatement("SELECT `time`, `time_null` FROM `all_types` WHERE `time` = ?")
          resultSet <- statement.setTime(1, LocalTime.of(12, 34, 56)) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (LocalTime, LocalTime)](resultSet.next()) {
            for
              v1 <- resultSet.getTime(1)
              v2 <- resultSet.getTime(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List((LocalTime.of(12, 34, 56), null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve DATETIME type records") {
      datasource.getConnection.use { conn =>
        for
          statement <-
            conn.prepareStatement("SELECT `datetime`, `datetime_null` FROM `all_types` WHERE `datetime` = ?")
          resultSet <- statement.setTimestamp(1, LocalDateTime.of(2020, 1, 1, 12, 34, 56)) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (LocalDateTime, LocalDateTime)](resultSet.next()) {
            for
              v1 <- resultSet.getTimestamp(1)
              v2 <- resultSet.getTimestamp(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List((LocalDateTime.of(2020, 1, 1, 12, 34, 56), null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve TIMESTAMP type records") {
      datasource.getConnection.use { conn =>
        for
          statement <-
            conn.prepareStatement("SELECT `timestamp`, `timestamp_null` FROM `all_types` WHERE `timestamp` = ?")
          resultSet <- statement.setTimestamp(1, LocalDateTime.of(2020, 1, 1, 12, 34, 56)) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (LocalDateTime, LocalDateTime)](resultSet.next()) {
            for
              v1 <- resultSet.getTimestamp(1)
              v2 <- resultSet.getTimestamp(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List((LocalDateTime.of(2020, 1, 1, 12, 34, 56), null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve YEAR type records") {
      datasource.getConnection.use { conn =>
        for
          statement <- conn.prepareStatement("SELECT `year`, `year_null` FROM `all_types` WHERE `year` = ?")
          resultSet <- statement.setInt(1, 2020) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (Int, Int)](resultSet.next()) {
            for
              v1 <- resultSet.getInt(1)
              v2 <- resultSet.getInt(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List((2020, 0))))
      }
    },

    test("Client PreparedStatement should be able to retrieve CHAR type records") {
      datasource.getConnection.use { conn =>
        for
          statement <- conn.prepareStatement("SELECT `char`, `char_null` FROM `all_types` WHERE `char` = ?")
          resultSet <- statement.setString(1, "char") *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List(("char", null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve VARCHAR type records") {
      datasource.getConnection.use { conn =>
        for
          statement <-
            conn.prepareStatement("SELECT `varchar`, `varchar_null` FROM `all_types` WHERE `varchar` = ?")
          resultSet <- statement.setString(1, "varchar") *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List(("varchar", null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve BINARY type records") {
      datasource.getConnection.use { conn =>
        for
          statement <-
            conn.prepareStatement("SELECT `binary`, `binary_null` FROM `all_types` WHERE `binary` = ?")
          resultSet <-
            statement.setBytes(1, Array[Byte](98, 105, 110, 97, 114, 121, 0, 0, 0, 0)) *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (String, Array[Byte])](resultSet.next()) {
            for
              v1 <- resultSet.getBytes(1)
              v2 <- resultSet.getBytes(2)
            yield (v1.mkString(":"), v2)
          }
        yield assert(result)(equalTo(List((Array[Byte](98, 105, 110, 97, 114, 121, 0, 0, 0, 0).mkString(":"), null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve VARBINARY type records") {
      datasource.getConnection.use { conn =>
        for
          statement <-
            conn.prepareStatement("SELECT `varbinary`, `varbinary_null` FROM `all_types` WHERE `varbinary` = ?")
          resultSet <- statement.setString(1, "varbinary") *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List(("varbinary", null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve TINYBLOB type records") {
      datasource.getConnection.use { conn =>
        for
          statement <-
            conn.prepareStatement("SELECT `tinyblob`, `tinyblob_null` FROM `all_types` WHERE `tinyblob` = ?")
          resultSet <- statement.setString(1, "tinyblob") *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List(("tinyblob", null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve BLOB type records") {
      datasource.getConnection.use { conn =>
        for
          statement <- conn.prepareStatement("SELECT `blob`, `blob_null` FROM `all_types` WHERE `blob` = ?")
          resultSet <- statement.setString(1, "blob") *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List(("blob", null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve MEDIUMBLOB type records") {
      datasource.getConnection.use { conn =>
        for
          statement <- conn.prepareStatement(
                         "SELECT `mediumblob`, `mediumblob_null` FROM `all_types` WHERE `mediumblob` = ?"
                       )
          resultSet <- statement.setString(1, "mediumblob") *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List(("mediumblob", null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve LONGBLOB type records") {
      datasource.getConnection.use { conn =>
        for
          statement <-
            conn.prepareStatement("SELECT `longblob`, `longblob_null` FROM `all_types` WHERE `longblob` = ?")
          resultSet <- statement.setString(1, "longblob") *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List(("longblob", null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve TINYTEXT type records") {
      datasource.getConnection.use { conn =>
        for
          statement <-
            conn.prepareStatement("SELECT `tinytext`, `tinytext_null` FROM `all_types` WHERE `tinytext` = ?")
          resultSet <- statement.setString(1, "tinytext") *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List(("tinytext", null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve TEXT type records") {
      datasource.getConnection.use { conn =>
        for
          statement <- conn.prepareStatement("SELECT `text`, `text_null` FROM `all_types` WHERE `text` = ?")
          resultSet <- statement.setString(1, "text") *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List(("text", null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve MEDIUMTEXT type records") {
      datasource.getConnection.use { conn =>
        for
          statement <- conn.prepareStatement(
                         "SELECT `mediumtext`, `mediumtext_null` FROM `all_types` WHERE `mediumtext` = ?"
                       )
          resultSet <- statement.setString(1, "mediumtext") *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List(("mediumtext", null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve LONGTEXT type records") {
      datasource.getConnection.use { conn =>
        for
          statement <-
            conn.prepareStatement("SELECT `longtext`, `longtext_null` FROM `all_types` WHERE `longtext` = ?")
          resultSet <- statement.setString(1, "longtext") *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List(("longtext", null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve ENUM type records") {
      datasource.getConnection.use { conn =>
        for
          statement <- conn.prepareStatement("SELECT `enum`, `enum_null` FROM `all_types` WHERE `enum` = ?")
          resultSet <- statement.setString(1, "a") *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List(("a", null))))
      }
    },

    test("Client PreparedStatement should be able to retrieve SET type records") {
      datasource.getConnection.use { conn =>
        for
          statement <- conn.prepareStatement("SELECT `set`, `set_null` FROM `all_types` WHERE `set` = ?")
          resultSet <- statement.setString(1, "a,b") *> statement.executeQuery()
          result <- Monad[Task].whileM[List, (String, String)](resultSet.next()) {
            for
              v1 <- resultSet.getString(1)
              v2 <- resultSet.getString(2)
            yield (v1, v2)
          }
        yield assert(result)(equalTo(List(("a,b", null))))
      }
    }
  )
