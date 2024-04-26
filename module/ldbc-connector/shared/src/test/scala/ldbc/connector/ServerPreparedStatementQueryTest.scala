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

import ldbc.connector.codec.all.*

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
          resultSet <- statement.setNull(1) *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(Byte, Option[Byte])](bit *: bit.opt)
        yield decoded
      },
      List((1.toByte, None))
    )
  }

  test("Server PreparedStatement should be able to retrieve BIT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `bit`, `bit_null` FROM `all_types` WHERE `bit` = ?")
          resultSet <- statement.setByte(1, 1.toByte) *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(Byte, Option[Byte])](bit *: bit.opt)
        yield decoded
      },
      List((1.toByte, None))
    )
  }

  test("Server PreparedStatement should be able to retrieve TINYINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `tinyint`, `tinyint_null` FROM `all_types` WHERE `tinyint` = ?")
          resultSet <- statement.setByte(1, 127.toByte) *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(Byte, Option[Byte])](tinyint *: tinyint.opt)
        yield decoded
      },
      List((127.toByte, None))
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
          resultSet <- statement.setShort(1, 255.toShort) *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(Short, Option[Short])](utinyint *: utinyint.opt)
        yield decoded
      },
      List((255.toShort, None))
    )
  }

  test("Server PreparedStatement should be able to retrieve SMALLINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `smallint`, `smallint_null` FROM `all_types` WHERE `smallint` = ?")
          resultSet <- statement.setShort(1, 32767.toShort) *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(Short, Option[Short])](smallint *: smallint.opt)
        yield decoded
      },
      List((32767.toShort, None))
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
          resultSet <- statement.setInt(1, 65535) *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(Int, Option[Int])](usmallint *: usmallint.opt)
        yield decoded
      },
      List((65535, None))
    )
  }

  test("Server PreparedStatement should be able to retrieve MEDIUMINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `mediumint`, `mediumint_null` FROM `all_types` WHERE `mediumint` = ?")
          resultSet <- statement.setInt(1, 8388607) *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(Int, Option[Int])](mediumint *: mediumint.opt)
        yield decoded
      },
      List((8388607, None))
    )
  }

  test("Server PreparedStatement should be able to retrieve INT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `int`, `int_null` FROM `all_types` WHERE `int` = ?")
          resultSet <- statement.setInt(1, 2147483647) *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(Int, Option[Int])](int *: int.opt)
        yield decoded
      },
      List((2147483647, None))
    )
  }

  test("Server PreparedStatement should be able to retrieve unsigned INT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement(
                         "SELECT `int_unsigned`, `int_unsigned_null` FROM `all_types` WHERE `int_unsigned` = ?"
                       )
          resultSet <- statement.setLong(1, 4294967295L) *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(Long, Option[Long])](uint *: uint.opt)
        yield decoded
      },
      List((4294967295L, None))
    )
  }

  test("Server PreparedStatement should be able to retrieve BIGINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `bigint`, `bigint_null` FROM `all_types` WHERE `bigint` = ?")
          resultSet <- statement.setLong(1, 9223372036854775807L) *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(Long, Option[Long])](bigint *: bigint.opt)
        yield decoded
      },
      List((9223372036854775807L, None))
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
            statement.setBigInt(1, BigInt("18446744073709551615")) *> statement.executeQuery() <* statement.close()
          decoded <- resultSet.decode[(BigInt, Option[BigInt])](ubigint *: ubigint.opt)
        yield decoded
      },
      List((BigInt("18446744073709551615"), None))
    )
  }

  test("Server PreparedStatement should be able to retrieve FLOAT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `float`, `float_null` FROM `all_types` WHERE `float` > ?")
          resultSet <- statement.setFloat(1, 3.40282e38f) *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(Float, Option[Float])](float *: float.opt)
        yield decoded
      },
      List((java.lang.Float.intBitsToFloat(2139095039), None))
    )
  }

  test("Server PreparedStatement should be able to retrieve DOUBLE type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `double`, `double_null` FROM `all_types` WHERE `double` = ?")
          resultSet <- statement.setDouble(1, 1.7976931348623157e308) *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(Double, Option[Double])](double *: double.opt)
        yield decoded
      },
      List((1.7976931348623157e308, None))
    )
  }

  test("Server PreparedStatement should be able to retrieve DECIMAL type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `decimal`, `decimal_null` FROM `all_types` WHERE `decimal` = ?")
          resultSet <-
            statement.setBigDecimal(1, BigDecimal.decimal(9999999.99)) *> statement.executeQuery() <* statement.close()
          decoded <- resultSet.decode[(BigDecimal, Option[BigDecimal])](decimal(10, 2) *: decimal(10, 2).opt)
        yield decoded
      },
      List((BigDecimal.decimal(9999999.99), None))
    )
  }

  test("Server PreparedStatement should be able to retrieve DATE type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `date`, `date_null` FROM `all_types` WHERE `date` = ?")
          resultSet <- statement.setDate(1, LocalDate.of(2020, 1, 1)) *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(LocalDate, Option[LocalDate])](date *: date.opt)
        yield decoded
      },
      List((LocalDate.of(2020, 1, 1), None))
    )
  }

  test("Server PreparedStatement should be able to retrieve TIME type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `time`, `time_null` FROM `all_types` WHERE `time` = ?")
          resultSet <- statement.setTime(1, LocalTime.of(12, 34, 56)) *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(LocalTime, Option[LocalTime])](time *: time.opt)
        yield decoded
      },
      List((LocalTime.of(12, 34, 56), None))
    )
  }

  test("Server PreparedStatement should be able to retrieve DATETIME type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `datetime`, `datetime_null` FROM `all_types` WHERE `datetime` = ?")
          resultSet <-
            statement.setTimestamp(1, LocalDateTime.of(2020, 1, 1, 12, 34, 56)) *> statement.executeQuery() <* statement
              .close()
          decoded <- resultSet.decode[(LocalDateTime, Option[LocalDateTime])](datetime *: datetime.opt)
        yield decoded
      },
      List((LocalDateTime.of(2020, 1, 1, 12, 34, 56), None))
    )
  }

  test("Server PreparedStatement should be able to retrieve TIMESTAMP type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `timestamp`, `timestamp_null` FROM `all_types` WHERE `timestamp` = ?")
          resultSet <-
            statement.setTimestamp(1, LocalDateTime.of(2020, 1, 1, 12, 34, 56)) *> statement.executeQuery() <* statement
              .close()
          decoded <- resultSet.decode[(LocalDateTime, Option[LocalDateTime])](timestamp *: timestamp.opt)
        yield decoded
      },
      List((LocalDateTime.of(2020, 1, 1, 12, 34, 56), None))
    )
  }

  test("Server PreparedStatement should be able to retrieve YEAR type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `year`, `year_null` FROM `all_types` WHERE `year` = ?")
          resultSet <- statement.setYear(1, Year.of(2020)) *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(Year, Option[Year])](year *: year.opt)
        yield decoded
      },
      List((Year.of(2020), None))
    )
  }

  test("Server PreparedStatement should be able to retrieve CHAR type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `char`, `char_null` FROM `all_types` WHERE `char` = ?")
          resultSet <- statement.setString(1, "char") *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(String, Option[String])](char(10) *: char(10).opt)
        yield decoded
      },
      List(("char", None))
    )
  }

  test("Server PreparedStatement should be able to retrieve VARCHAR type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `varchar`, `varchar_null` FROM `all_types` WHERE `varchar` = ?")
          resultSet <- statement.setString(1, "varchar") *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(String, Option[String])](varchar(10) *: varchar(10).opt)
        yield decoded
      },
      List(("varchar", None))
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
              .executeQuery() <* statement.close()
          decoded <- resultSet.decode[(Array[Byte], Option[Array[Byte]])](binary(10) *: binary(10).opt)
        yield decoded.map((a, b) => (a.mkString(":"), b.map(_.mkString(":"))))
      },
      List((Array[Byte](98, 105, 110, 97, 114, 121, 0, 0, 0, 0).mkString(":"), None))
    )
  }

  test("Server PreparedStatement should be able to retrieve VARBINARY type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `varbinary`, `varbinary_null` FROM `all_types` WHERE `varbinary` = ?")
          resultSet <- statement.setString(1, "varbinary") *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(String, Option[String])](varbinary(10) *: varbinary(10).opt)
        yield decoded
      },
      List(("varbinary", None))
    )
  }

  test("Server PreparedStatement should be able to retrieve TINYBLOB type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `tinyblob`, `tinyblob_null` FROM `all_types` WHERE `tinyblob` = ?")
          resultSet <- statement.setString(1, "tinyblob") *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(String, Option[String])](tinyblob *: tinyblob.opt)
        yield decoded
      },
      List(("tinyblob", None))
    )
  }

  test("Server PreparedStatement should be able to retrieve BLOB type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `blob`, `blob_null` FROM `all_types` WHERE `blob` = ?")
          resultSet <- statement.setString(1, "blob") *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(String, Option[String])](blob *: blob.opt)
        yield decoded
      },
      List(("blob", None))
    )
  }

  test("Server PreparedStatement should be able to retrieve MEDIUMBLOB type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement(
                         "SELECT `mediumblob`, `mediumblob_null` FROM `all_types` WHERE `mediumblob` = ?"
                       )
          resultSet <- statement.setString(1, "mediumblob") *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(String, Option[String])](mediumblob *: mediumblob.opt)
        yield decoded
      },
      List(("mediumblob", None))
    )
  }

  test("Server PreparedStatement should be able to retrieve LONGBLOB type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `longblob`, `longblob_null` FROM `all_types` WHERE `longblob` = ?")
          resultSet <- statement.setString(1, "longblob") *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(String, Option[String])](longblob *: longblob.opt)
        yield decoded
      },
      List(("longblob", None))
    )
  }

  test("Server PreparedStatement should be able to retrieve TINYTEXT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `tinytext`, `tinytext_null` FROM `all_types` WHERE `tinytext` = ?")
          resultSet <- statement.setString(1, "tinytext") *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(String, Option[String])](tinytext *: tinytext.opt)
        yield decoded
      },
      List(("tinytext", None))
    )
  }

  test("Server PreparedStatement should be able to retrieve TEXT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `text`, `text_null` FROM `all_types` WHERE `text` = ?")
          resultSet <- statement.setString(1, "text") *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(String, Option[String])](text *: text.opt)
        yield decoded
      },
      List(("text", None))
    )
  }

  test("Server PreparedStatement should be able to retrieve MEDIUMTEXT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement(
                         "SELECT `mediumtext`, `mediumtext_null` FROM `all_types` WHERE `mediumtext` = ?"
                       )
          resultSet <- statement.setString(1, "mediumtext") *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(String, Option[String])](mediumtext *: mediumtext.opt)
        yield decoded
      },
      List(("mediumtext", None))
    )
  }

  test("Server PreparedStatement should be able to retrieve LONGTEXT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <-
            conn.serverPreparedStatement("SELECT `longtext`, `longtext_null` FROM `all_types` WHERE `longtext` = ?")
          resultSet <- statement.setString(1, "longtext") *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(String, Option[String])](longtext *: longtext.opt)
        yield decoded
      },
      List(("longtext", None))
    )
  }

  test("Server PreparedStatement should be able to retrieve ENUM type records.") {
    val t = `enum`("a", "b", "c")
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `enum`, `enum_null` FROM `all_types` WHERE `enum` = ?")
          resultSet <- statement.setString(1, "a") *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(String, Option[String])](t *: t.opt)
        yield decoded
      },
      List(("a", None))
    )
  }

  test("Server PreparedStatement should be able to retrieve SET type records.") {
    val s = set("a", "b", "c")
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.serverPreparedStatement("SELECT `set`, `set_null` FROM `all_types` WHERE `set` = ?")
          resultSet <- statement.setString(1, "a,b") *> statement.executeQuery() <* statement.close()
          decoded   <- resultSet.decode[(List[String], Option[List[String]])](s *: s.opt)
        yield decoded
      },
      List((List("a", "b"), None))
    )
  }
