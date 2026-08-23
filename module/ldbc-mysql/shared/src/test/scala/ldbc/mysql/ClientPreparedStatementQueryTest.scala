/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import ldbc.fx.concurrentFx

import java.time.*

import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.mysql.data.MysqlType
import ldbc.mysql.syntax.*
import ldbc.mysql.telemetry.*
import ldbc.net.SSL

class ClientPreparedStatementQueryTest extends FTestPlatform:

  given Tracer[Fx] = Tracer.noop[Fx]

  private val connection = Connection[Fx](
    host     = TestConfig.host,
    port     = TestConfig.port,
    user     = TestConfig.user,
    password = Some(TestConfig.password),
    database = Some("connector_test"),
    ssl      = SSL.Trusted
  )

  test("The client's PreparedStatement may use NULL as a parameter.") {
    assertFx(
      connection.use { conn =>
        for
          statement <- conn.clientPreparedStatement("SELECT `bit`, `bit_null` FROM `all_types` WHERE `bit_null` is ?")
          resultSet <- statement.setNull(1, MysqlType.BIT.jdbcType) *> statement.executeQuery()
          result    <- resultSet.whileM[List, (Byte, Byte)] {
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

  test("Client PreparedStatement should be able to retrieve BIT type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <- conn.clientPreparedStatement("SELECT `bit`, `bit_null` FROM `all_types` WHERE `bit` = ?")
          resultSet <- statement.setByte(1, 1.toByte) *> statement.executeQuery()
          result    <- resultSet.whileM[List, (Byte, Byte)] {
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

  test("Client PreparedStatement should be able to retrieve TINYINT type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <-
            conn.clientPreparedStatement("SELECT `tinyint`, `tinyint_null` FROM `all_types` WHERE `tinyint` = ?")
          resultSet <- statement.setByte(1, 127.toByte) *> statement.executeQuery()
          result    <- resultSet.whileM[List, (Byte, Byte)] {
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

  test("Client PreparedStatement should be able to retrieve unsigned TINYINT type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <-
            conn.clientPreparedStatement(
              "SELECT `tinyint_unsigned`, `tinyint_unsigned_null` FROM `all_types` WHERE `tinyint_unsigned` = ?"
            )
          resultSet <- statement.setShort(1, 255.toShort) *> statement.executeQuery()
          result    <- resultSet.whileM[List, (Short, Short)] {
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

  test("Client PreparedStatement should be able to retrieve SMALLINT type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <-
            conn.clientPreparedStatement("SELECT `smallint`, `smallint_null` FROM `all_types` WHERE `smallint` = ?")
          resultSet <- statement.setShort(1, 32767.toShort) *> statement.executeQuery()
          result    <- resultSet.whileM[List, (Short, Short)] {
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

  test("Client PreparedStatement should be able to retrieve unsigned SMALLINT type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <-
            conn.clientPreparedStatement(
              "SELECT `smallint_unsigned`, `smallint_unsigned_null` FROM `all_types` WHERE `smallint_unsigned` = ?"
            )
          resultSet <- statement.setInt(1, 65535) *> statement.executeQuery()
          result    <- resultSet.whileM[List, (Int, Int)] {
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

  test("Client PreparedStatement should be able to retrieve MEDIUMINT type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <-
            conn.clientPreparedStatement("SELECT `mediumint`, `mediumint_null` FROM `all_types` WHERE `mediumint` = ?")
          resultSet <- statement.setInt(1, 8388607) *> statement.executeQuery()
          result    <- resultSet.whileM[List, (Int, Int)] {
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

  test("Client PreparedStatement should be able to retrieve INT type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <- conn.clientPreparedStatement("SELECT `int`, `int_null` FROM `all_types` WHERE `int` = ?")
          resultSet <- statement.setInt(1, 2147483647) *> statement.executeQuery()
          result    <- resultSet.whileM[List, (Int, Int)] {
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

  test("Client PreparedStatement should be able to retrieve unsigned INT type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <- conn.clientPreparedStatement(
                         "SELECT `int_unsigned`, `int_unsigned_null` FROM `all_types` WHERE `int_unsigned` = ?"
                       )
          resultSet <- statement.setLong(1, 4294967295L) *> statement.executeQuery()
          result    <- resultSet.whileM[List, (Long, Long)] {
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

  test("Client PreparedStatement should be able to retrieve BIGINT type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <-
            conn.clientPreparedStatement("SELECT `bigint`, `bigint_null` FROM `all_types` WHERE `bigint` = ?")
          resultSet <- statement.setLong(1, 9223372036854775807L) *> statement.executeQuery()
          result    <- resultSet.whileM[List, (Long, Long)] {
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

  test("Client PreparedStatement should be able to retrieve unsigned BIGINT type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <- conn.clientPreparedStatement(
                         "SELECT `bigint_unsigned`, `bigint_unsigned_null` FROM `all_types` WHERE `bigint_unsigned` = ?"
                       )
          resultSet <- statement.setString(1, "18446744073709551615") *> statement.executeQuery()
          result    <- resultSet.whileM[List, (String, String)] {
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

  test("Client PreparedStatement should be able to retrieve FLOAT type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <- conn.clientPreparedStatement("SELECT `float`, `float_null` FROM `all_types` WHERE `float` > ?")
          resultSet <- statement.setFloat(1, 3.4f) *> statement.executeQuery()
          result    <- resultSet.whileM[List, (Float, Float)] {
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

  test("Client PreparedStatement should be able to retrieve DOUBLE type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <-
            conn.clientPreparedStatement("SELECT `double`, `double_null` FROM `all_types` WHERE `double` = ?")
          resultSet <- statement.setDouble(1, 1.7976931348623157e308) *> statement.executeQuery()
          result    <- resultSet.whileM[List, (Double, Double)] {
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

  test("Client PreparedStatement should be able to retrieve DECIMAL type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <-
            conn.clientPreparedStatement("SELECT `decimal`, `decimal_null` FROM `all_types` WHERE `decimal` = ?")
          resultSet <- statement.setBigDecimal(1, BigDecimal.decimal(9999999.99)) *> statement.executeQuery()
          result    <- resultSet.whileM[List, (BigDecimal, BigDecimal)] {
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

  test("Client PreparedStatement should be able to retrieve DATE type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <- conn.clientPreparedStatement("SELECT `date`, `date_null` FROM `all_types` WHERE `date` = ?")
          resultSet <- statement.setDate(1, LocalDate.of(2020, 1, 1)) *> statement.executeQuery()
          result    <- resultSet.whileM[List, (LocalDate, LocalDate)] {
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

  test("Client PreparedStatement should be able to retrieve TIME type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <- conn.clientPreparedStatement("SELECT `time`, `time_null` FROM `all_types` WHERE `time` = ?")
          resultSet <- statement.setTime(1, LocalTime.of(12, 34, 56)) *> statement.executeQuery()
          result    <- resultSet.whileM[List, (LocalTime, LocalTime)] {
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

  test("Client PreparedStatement should be able to retrieve DATETIME type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <-
            conn.clientPreparedStatement("SELECT `datetime`, `datetime_null` FROM `all_types` WHERE `datetime` = ?")
          resultSet <- statement.setTimestamp(1, LocalDateTime.of(2020, 1, 1, 12, 34, 56)) *> statement.executeQuery()
          result    <- resultSet.whileM[List, (LocalDateTime, LocalDateTime)] {
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

  test("Client PreparedStatement should be able to retrieve TIMESTAMP type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <-
            conn.clientPreparedStatement("SELECT `timestamp`, `timestamp_null` FROM `all_types` WHERE `timestamp` = ?")
          resultSet <- statement.setTimestamp(1, LocalDateTime.of(2020, 1, 1, 12, 34, 56)) *> statement.executeQuery()
          result    <- resultSet.whileM[List, (LocalDateTime, LocalDateTime)] {
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

  test("Client PreparedStatement should be able to retrieve YEAR type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <- conn.clientPreparedStatement("SELECT `year`, `year_null` FROM `all_types` WHERE `year` = ?")
          resultSet <- statement.setInt(1, 2020) *> statement.executeQuery()
          result    <- resultSet.whileM[List, (Int, Int)] {
                      for
                        v1 <- resultSet.getInt(1)
                        v2 <- resultSet.getInt(2)
                      yield (v1, v2)
                    }
        yield result
      },
      List((2020, 0))
    )
  }

  test("Client PreparedStatement should be able to retrieve CHAR type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <- conn.clientPreparedStatement("SELECT `char`, `char_null` FROM `all_types` WHERE `char` = ?")
          resultSet <- statement.setString(1, "char") *> statement.executeQuery()
          result    <- resultSet.whileM[List, (String, String)] {
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

  test("Client PreparedStatement should be able to retrieve VARCHAR type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <-
            conn.clientPreparedStatement("SELECT `varchar`, `varchar_null` FROM `all_types` WHERE `varchar` = ?")
          resultSet <- statement.setString(1, "varchar") *> statement.executeQuery()
          result    <- resultSet.whileM[List, (String, String)] {
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

  test("Client PreparedStatement should be able to retrieve BINARY type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <-
            conn.clientPreparedStatement("SELECT `binary`, `binary_null` FROM `all_types` WHERE `binary` = ?")
          resultSet <-
            statement.setBytes(1, Array[Byte](98, 105, 110, 97, 114, 121, 0, 0, 0, 0)) *> statement.executeQuery()
          result <- resultSet.whileM[List, (String, Array[Byte])] {
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

  test("Client PreparedStatement should be able to retrieve VARBINARY type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <-
            conn.clientPreparedStatement("SELECT `varbinary`, `varbinary_null` FROM `all_types` WHERE `varbinary` = ?")
          resultSet <- statement.setString(1, "varbinary") *> statement.executeQuery()
          result    <- resultSet.whileM[List, (String, String)] {
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

  test("Client PreparedStatement should be able to retrieve TINYBLOB type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <-
            conn.clientPreparedStatement("SELECT `tinyblob`, `tinyblob_null` FROM `all_types` WHERE `tinyblob` = ?")
          resultSet <- statement.setString(1, "tinyblob") *> statement.executeQuery()
          result    <- resultSet.whileM[List, (String, String)] {
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

  test("Client PreparedStatement should be able to retrieve BLOB type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <- conn.clientPreparedStatement("SELECT `blob`, `blob_null` FROM `all_types` WHERE `blob` = ?")
          resultSet <- statement.setString(1, "blob") *> statement.executeQuery()
          result    <- resultSet.whileM[List, (String, String)] {
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

  test("Client PreparedStatement should be able to retrieve MEDIUMBLOB type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <- conn.clientPreparedStatement(
                         "SELECT `mediumblob`, `mediumblob_null` FROM `all_types` WHERE `mediumblob` = ?"
                       )
          resultSet <- statement.setString(1, "mediumblob") *> statement.executeQuery()
          result    <- resultSet.whileM[List, (String, String)] {
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

  test("Client PreparedStatement should be able to retrieve LONGBLOB type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <-
            conn.clientPreparedStatement("SELECT `longblob`, `longblob_null` FROM `all_types` WHERE `longblob` = ?")
          resultSet <- statement.setString(1, "longblob") *> statement.executeQuery()
          result    <- resultSet.whileM[List, (String, String)] {
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

  test("Client PreparedStatement should be able to retrieve TINYTEXT type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <-
            conn.clientPreparedStatement("SELECT `tinytext`, `tinytext_null` FROM `all_types` WHERE `tinytext` = ?")
          resultSet <- statement.setString(1, "tinytext") *> statement.executeQuery()
          result    <- resultSet.whileM[List, (String, String)] {
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

  test("Client PreparedStatement should be able to retrieve TEXT type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <- conn.clientPreparedStatement("SELECT `text`, `text_null` FROM `all_types` WHERE `text` = ?")
          resultSet <- statement.setString(1, "text") *> statement.executeQuery()
          result    <- resultSet.whileM[List, (String, String)] {
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

  test("Client PreparedStatement should be able to retrieve MEDIUMTEXT type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <- conn.clientPreparedStatement(
                         "SELECT `mediumtext`, `mediumtext_null` FROM `all_types` WHERE `mediumtext` = ?"
                       )
          resultSet <- statement.setString(1, "mediumtext") *> statement.executeQuery()
          result    <- resultSet.whileM[List, (String, String)] {
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

  test("Client PreparedStatement should be able to retrieve LONGTEXT type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <-
            conn.clientPreparedStatement("SELECT `longtext`, `longtext_null` FROM `all_types` WHERE `longtext` = ?")
          resultSet <- statement.setString(1, "longtext") *> statement.executeQuery()
          result    <- resultSet.whileM[List, (String, String)] {
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

  test("Client PreparedStatement should be able to retrieve ENUM type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <- conn.clientPreparedStatement("SELECT `enum`, `enum_null` FROM `all_types` WHERE `enum` = ?")
          resultSet <- statement.setString(1, "a") *> statement.executeQuery()
          result    <- resultSet.whileM[List, (String, String)] {
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

  test("Client PreparedStatement should be able to retrieve SET type records.") {
    assertFx(
      connection.use { conn =>
        for
          statement <- conn.clientPreparedStatement("SELECT `set`, `set_null` FROM `all_types` WHERE `set` = ?")
          resultSet <- statement.setString(1, "a,b") *> statement.executeQuery()
          result    <- resultSet.whileM[List, (String, String)] {
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
