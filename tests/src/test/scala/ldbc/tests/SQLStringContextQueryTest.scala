/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import java.time.*

import com.mysql.cj.jdbc.MysqlDataSource

import cats.syntax.all.*

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import munit.CatsEffectSuite

import ldbc.sql.Connection
import ldbc.connector.SSL
import ldbc.dsl.io.*

class LdbcSQLStringContextQueryTest extends SQLStringContextQueryTest:
  override def connection: Resource[IO, Connection[IO]] =
    ldbc.connector.Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("world"),
      ssl      = SSL.Trusted
    )

  test(
    "If the acquired column name and the field name of the class to be mapped are different, an exception is raised."
  ) {
    case class City(id: Int, title: String)

    interceptIO[ldbc.connector.exception.SQLException](
      connection.use { conn =>
        sql"SELECT Id, Name FROM city LIMIT 1"
          .query[City]
          .to[Option]
          .readOnly(conn)
      }
    )
  }

  test(
    "If the number of columns retrieved is different from the number of fields in the class to be mapped, an exception is raised."
  ) {
    case class City(id: Int, name: String, age: Int)

    interceptIO[ldbc.connector.exception.SQLException](
      connection.use { conn =>
        sql"SELECT Id, Name FROM city LIMIT 1"
          .query[City]
          .to[Option]
          .readOnly(conn)
      }
    )
  }

class JdbcSQLStringContextQueryTest extends SQLStringContextQueryTest:

  val ds = new MysqlDataSource()
  ds.setServerName("127.0.0.1")
  ds.setPortNumber(13306)
  ds.setDatabaseName("world")
  ds.setUser("ldbc")
  ds.setPassword("password")

  override def connection: Resource[IO, Connection[IO]] =
    Resource.make(jdbc.connector.MysqlDataSource[IO](ds).getConnection)(_.close())

  test(
    "If the acquired column name and the field name of the class to be mapped are different, an exception is raised."
  ) {
    case class City(id: Int, title: String)

    interceptIO[java.sql.SQLException](
      connection.use { conn =>
        sql"SELECT Id, Name FROM city LIMIT 1"
          .query[City]
          .to[Option]
          .readOnly(conn)
      }
    )
  }

  test(
    "If the number of columns retrieved is different from the number of fields in the class to be mapped, an exception is raised."
  ) {
    case class City(id: Int, name: String, age: Int)

    interceptIO[java.sql.SQLException](
      connection.use { conn =>
        sql"SELECT Id, Name FROM city LIMIT 1"
          .query[City]
          .to[Option]
          .readOnly(conn)
      }
    )
  }

trait SQLStringContextQueryTest extends CatsEffectSuite:

  given Tracer[IO] = Tracer.noop[IO]

  def connection: Resource[IO, Connection[IO]]

  test("Statement should be able to execute a query") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <- sql"SELECT 1".query[Int].to[List]
          result2 <- sql"SELECT 2".query[Int].to[Option]
          result3 <- sql"SELECT 3".query[Int].unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List(1), Some(2), 3)
    )
  }

  test("Statement should be able to retrieve BIT type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <- sql"SELECT `bit`, `bit_null` FROM `connector_test`.`all_types`".query[(Byte, Byte)].to[List]
          result2 <- sql"SELECT `bit`, `bit_null` FROM `connector_test`.`all_types`".query[(Byte, Byte)].to[Option]
          result3 <- sql"SELECT `bit`, `bit_null` FROM `connector_test`.`all_types`".query[(Byte, Byte)].unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List((1.toByte, 0.toByte)), Some((1.toByte, 0.toByte)), (1.toByte, 0.toByte))
    )
  }

  test("Statement should be able to retrieve TINYINT type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `tinyint`, `tinyint_null` FROM `connector_test`.`all_types`".query[(Byte, Byte)].to[List]
          result2 <-
            sql"SELECT `tinyint`, `tinyint_null` FROM `connector_test`.`all_types` WHERE `tinyint` = ${ 127.toByte }"
              .query[(Byte, Byte)]
              .to[Option]
          result3 <- sql"SELECT `tinyint`, `tinyint_null` FROM `connector_test`.`all_types`".query[(Byte, Byte)].unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List((127.toByte, 0.toByte)), Some((127.toByte, 0.toByte)), (127.toByte, 0.toByte))
    )
  }

  test("Statement should be able to retrieve unsigned TINYINT type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <- sql"SELECT `tinyint_unsigned`, `tinyint_unsigned_null` FROM `connector_test`.`all_types`"
                       .query[(Short, Short)]
                       .to[List]
          result2 <-
            sql"SELECT `tinyint_unsigned`, `tinyint_unsigned_null` FROM `connector_test`.`all_types` WHERE `tinyint_unsigned` = ${ 255.toShort }"
              .query[(Short, Short)]
              .to[Option]
          result3 <- sql"SELECT `tinyint_unsigned`, `tinyint_unsigned_null` FROM `connector_test`.`all_types`"
                       .query[(Short, Short)]
                       .unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List((255.toShort, 0.toShort)), Some((255.toShort, 0.toShort)), (255.toShort, 0.toShort))
    )
  }

  test("Statement should be able to retrieve SMALLINT type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `smallint`, `smallint_null` FROM `connector_test`.`all_types`".query[(Short, Short)].to[List]
          result2 <-
            sql"SELECT `smallint`, `smallint_null` FROM `connector_test`.`all_types` WHERE `smallint` = ${ 32767.toShort }"
              .query[(Short, Short)]
              .to[Option]
          result3 <-
            sql"SELECT `smallint`, `smallint_null` FROM `connector_test`.`all_types`".query[(Short, Short)].unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List((32767.toShort, 0.toShort)), Some((32767.toShort, 0.toShort)), (32767.toShort, 0.toShort))
    )
  }

  test("Statement should be able to retrieve unsigned SMALLINT type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <- sql"SELECT `smallint_unsigned`, `smallint_unsigned_null` FROM `connector_test`.`all_types`"
                       .query[(Int, Int)]
                       .to[List]
          result2 <-
            sql"SELECT `smallint_unsigned`, `smallint_unsigned_null` FROM `connector_test`.`all_types` WHERE `smallint_unsigned` = ${ 65535 }"
              .query[(Int, Int)]
              .to[Option]
          result3 <- sql"SELECT `smallint_unsigned`, `smallint_unsigned_null` FROM `connector_test`.`all_types`"
                       .query[(Int, Int)]
                       .unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List((65535, 0)), Some((65535, 0)), (65535, 0))
    )
  }

  test("Statement should be able to retrieve MEDIUMINT type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `mediumint`, `mediumint_null` FROM `connector_test`.`all_types`".query[(Int, Int)].to[List]
          result2 <-
            sql"SELECT `mediumint`, `mediumint_null` FROM `connector_test`.`all_types` WHERE `mediumint` = ${ 8388607 }"
              .query[(Int, Int)]
              .to[Option]
          result3 <-
            sql"SELECT `mediumint`, `mediumint_null` FROM `connector_test`.`all_types`".query[(Int, Int)].unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List((8388607, 0)), Some((8388607, 0)), (8388607, 0))
    )
  }

  test("Statement should be able to retrieve INT type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <- sql"SELECT `int`, `int_null` FROM `connector_test`.`all_types`".query[(Int, Int)].to[List]
          result2 <- sql"SELECT `int`, `int_null` FROM `connector_test`.`all_types` WHERE `int` = ${ 2147483647 }"
                       .query[(Int, Int)]
                       .to[Option]
          result3 <- sql"SELECT `int`, `int_null` FROM `connector_test`.`all_types`".query[(Int, Int)].unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List((2147483647, 0)), Some((2147483647, 0)), (2147483647, 0))
    )
  }

  test("Statement should be able to retrieve unsigned INT type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `int_unsigned`, `int_unsigned_null` FROM `connector_test`.`all_types`"
              .query[(Long, Long)]
              .to[List]
          result2 <-
            sql"SELECT `int_unsigned`, `int_unsigned_null` FROM `connector_test`.`all_types` WHERE `int_unsigned` = ${ 4294967295L }"
              .query[(Long, Long)]
              .to[Option]
          result3 <-
            sql"SELECT `int_unsigned`, `int_unsigned_null` FROM `connector_test`.`all_types`".query[(Long, Long)].unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List((4294967295L, 0L)), Some((4294967295L, 0L)), (4294967295L, 0L))
    )
  }

  test("Statement should be able to retrieve BIGINT type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <- sql"SELECT `bigint`, `bigint_null` FROM `connector_test`.`all_types`".query[(Long, Long)].to[List]
          result2 <-
            sql"SELECT `bigint`, `bigint_null` FROM `connector_test`.`all_types` WHERE `bigint` = ${ 9223372036854775807L }"
              .query[(Long, Long)]
              .to[Option]
          result3 <- sql"SELECT `bigint`, `bigint_null` FROM `connector_test`.`all_types`".query[(Long, Long)].unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List((9223372036854775807L, 0L)), Some((9223372036854775807L, 0L)), (9223372036854775807L, 0L))
    )
  }

  test("Statement should be able to retrieve unsigned BIGINT type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <- sql"SELECT `bigint_unsigned`, `bigint_unsigned_null` FROM `connector_test`.`all_types`"
                       .query[(String, Option[String])]
                       .to[List]
          result2 <-
            sql"SELECT `bigint_unsigned`, `bigint_unsigned_null` FROM `connector_test`.`all_types` WHERE `bigint_unsigned` = ${ "18446744073709551615" }"
              .query[(String, Option[String])]
              .to[Option]
          result3 <- sql"SELECT `bigint_unsigned`, `bigint_unsigned_null` FROM `connector_test`.`all_types`"
                       .query[(String, Option[String])]
                       .unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List(("18446744073709551615", None)), Some(("18446744073709551615", None)), ("18446744073709551615", None))
    )
  }

  test("Statement should be able to retrieve FLOAT type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <- sql"SELECT `float`, `float_null` FROM `connector_test`.`all_types`".query[(Float, Float)].to[List]
          result2 <- sql"SELECT `float`, `float_null` FROM `connector_test`.`all_types` WHERE `float` >= ${ 3.3f }"
                       .query[(Float, Float)]
                       .to[Option]
          result3 <- sql"SELECT `float`, `float_null` FROM `connector_test`.`all_types`".query[(Float, Float)].unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List((3.40282e38f, 0f)), Some((3.40282e38f, 0f)), (3.40282e38f, 0f))
    )
  }

  test("Statement should be able to retrieve DOUBLE type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `double`, `double_null` FROM `connector_test`.`all_types`".query[(Double, Double)].to[List]
          result2 <-
            sql"SELECT `double`, `double_null` FROM `connector_test`.`all_types` WHERE `double` = ${ 1.7976931348623157e308 }"
              .query[(Double, Double)]
              .to[Option]
          result3 <-
            sql"SELECT `double`, `double_null` FROM `connector_test`.`all_types`".query[(Double, Double)].unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (
        List((1.7976931348623157e308, 0.toDouble)),
        Some((1.7976931348623157e308, 0.toDouble)),
        (1.7976931348623157e308, 0.toDouble)
      )
    )
  }

  test("Statement should be able to retrieve DECIMAL type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <- sql"SELECT `decimal`, `decimal_null` FROM `connector_test`.`all_types`"
                       .query[(BigDecimal, Option[BigDecimal])]
                       .to[List]
          result2 <-
            sql"SELECT `decimal`, `decimal_null` FROM `connector_test`.`all_types` WHERE `decimal` = ${ 9999999.99 }"
              .query[(BigDecimal, Option[BigDecimal])]
              .to[Option]
          result3 <- sql"SELECT `decimal`, `decimal_null` FROM `connector_test`.`all_types`"
                       .query[(BigDecimal, Option[BigDecimal])]
                       .unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (
        List((BigDecimal.decimal(9999999.99), None)),
        Some((BigDecimal.decimal(9999999.99), None)),
        (BigDecimal.decimal(9999999.99), None)
      )
    )
  }

  test("Statement should be able to retrieve DATE type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `date`, `date_null` FROM `connector_test`.`all_types`"
              .query[(LocalDate, Option[LocalDate])]
              .to[List]
          result2 <-
            sql"SELECT `date`, `date_null` FROM `connector_test`.`all_types` WHERE `date` = ${ LocalDate.of(2020, 1, 1) }"
              .query[(LocalDate, Option[LocalDate])]
              .to[Option]
          result3 <-
            sql"SELECT `date`, `date_null` FROM `connector_test`.`all_types`"
              .query[(LocalDate, Option[LocalDate])]
              .unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List((LocalDate.of(2020, 1, 1), None)), Some((LocalDate.of(2020, 1, 1), None)), (LocalDate.of(2020, 1, 1), None))
    )
  }

  test("Statement should be able to retrieve TIME type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `time`, `time_null` FROM `connector_test`.`all_types`"
              .query[(LocalTime, Option[LocalTime])]
              .to[List]
          result2 <-
            sql"SELECT `time`, `time_null` FROM `connector_test`.`all_types` WHERE `time` = ${ LocalTime.of(12, 34, 56) }"
              .query[(LocalTime, Option[LocalTime])]
              .to[Option]
          result3 <-
            sql"SELECT `time`, `time_null` FROM `connector_test`.`all_types`"
              .query[(LocalTime, Option[LocalTime])]
              .unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List((LocalTime.of(12, 34, 56), None)), Some((LocalTime.of(12, 34, 56), None)), (LocalTime.of(12, 34, 56), None))
    )
  }

  test("Statement should be able to retrieve DATETIME type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <- sql"SELECT `datetime`, `datetime_null` FROM `connector_test`.`all_types`"
                       .query[(LocalDateTime, Option[LocalDateTime])]
                       .to[List]
          result2 <-
            sql"SELECT `datetime`, `datetime_null` FROM `connector_test`.`all_types` WHERE `datetime` = ${ LocalDateTime
                .of(2020, 1, 1, 12, 34, 56) }"
              .query[(LocalDateTime, Option[LocalDateTime])]
              .to[Option]
          result3 <- sql"SELECT `datetime`, `datetime_null` FROM `connector_test`.`all_types`"
                       .query[(LocalDateTime, Option[LocalDateTime])]
                       .unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (
        List((LocalDateTime.of(2020, 1, 1, 12, 34, 56), None)),
        Some((LocalDateTime.of(2020, 1, 1, 12, 34, 56), None)),
        (LocalDateTime.of(2020, 1, 1, 12, 34, 56), None)
      )
    )
  }

  test("Statement should be able to retrieve TIMESTAMP type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <- sql"SELECT `timestamp`, `timestamp_null` FROM `connector_test`.`all_types`"
                       .query[(LocalDateTime, Option[LocalDateTime])]
                       .to[List]
          result2 <-
            sql"SELECT `timestamp`, `timestamp_null` FROM `connector_test`.`all_types` WHERE `timestamp` = ${ LocalDateTime
                .of(2020, 1, 1, 12, 34, 56) }"
              .query[(LocalDateTime, Option[LocalDateTime])]
              .to[Option]
          result3 <- sql"SELECT `timestamp`, `timestamp_null` FROM `connector_test`.`all_types`"
                       .query[(LocalDateTime, Option[LocalDateTime])]
                       .unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (
        List((LocalDateTime.of(2020, 1, 1, 12, 34, 56), None)),
        Some((LocalDateTime.of(2020, 1, 1, 12, 34, 56), None)),
        (LocalDateTime.of(2020, 1, 1, 12, 34, 56), None)
      )
    )
  }

  test("Statement should be able to retrieve YEAR type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `year`, `year_null` FROM `connector_test`.`all_types`".query[(Short, Option[Short])].to[List]
          result2 <-
            sql"SELECT `year`, `year_null` FROM `connector_test`.`all_types` WHERE `year` = ${ 2020 }"
              .query[(Short, Option[Short])]
              .to[Option]
          result3 <-
            sql"SELECT `year`, `year_null` FROM `connector_test`.`all_types`".query[(Short, Option[Short])].unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List((2020.toShort, None)), Some((2020.toShort, None)), (2020.toShort, None))
    )
  }

  test("Statement should be able to retrieve CHAR type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `char`, `char_null` FROM `connector_test`.`all_types`".query[(String, Option[String])].to[List]
          result2 <-
            sql"SELECT `char`, `char_null` FROM `connector_test`.`all_types` WHERE `char` = ${ "char" }"
              .query[(String, Option[String])]
              .to[Option]
          result3 <-
            sql"SELECT `char`, `char_null` FROM `connector_test`.`all_types`".query[(String, Option[String])].unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List(("char", None)), Some(("char", None)), ("char", None))
    )
  }

  test("Statement should be able to retrieve VARCHAR type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `varchar`, `varchar_null` FROM `connector_test`.`all_types`"
              .query[(String, Option[String])]
              .to[List]
          result2 <-
            sql"SELECT `varchar`, `varchar_null` FROM `connector_test`.`all_types` WHERE `varchar` = ${ "varchar" }"
              .query[(String, Option[String])]
              .to[Option]
          result3 <-
            sql"SELECT `varchar`, `varchar_null` FROM `connector_test`.`all_types`"
              .query[(String, Option[String])]
              .unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List(("varchar", None)), Some(("varchar", None)), ("varchar", None))
    )
  }

  test("Statement should be able to retrieve BINARY type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <- sql"SELECT `binary`, `binary_null` FROM `connector_test`.`all_types`"
                       .query[(Array[Byte], Option[Array[Byte]])]
                       .to[List]
          result2 <- sql"SELECT `binary`, `binary_null` FROM `connector_test`.`all_types`"
                       .query[(Array[Byte], Option[Array[Byte]])]
                       .to[Option]
          result3 <- sql"SELECT `binary`, `binary_null` FROM `connector_test`.`all_types`"
                       .query[(Array[Byte], Option[Array[Byte]])]
                       .unsafe
        yield (
          result1.map { case (v1, v2) => (v1.mkString(":"), v2) },
          result2.map { case (v1, v2) => (v1.mkString(":"), v2) },
          (result3._1.mkString(":"), result3._2)
        )).readOnly(conn)
      },
      (
        List((Array[Byte](98, 105, 110, 97, 114, 121, 0, 0, 0, 0).mkString(":"), None)),
        Some((Array[Byte](98, 105, 110, 97, 114, 121, 0, 0, 0, 0).mkString(":"), None)),
        (Array[Byte](98, 105, 110, 97, 114, 121, 0, 0, 0, 0).mkString(":"), None)
      )
    )
  }

  test("Statement should be able to retrieve VARBINARY type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `varbinary`, `varbinary_null` FROM `connector_test`.`all_types`"
              .query[(String, Option[String])]
              .to[List]
          result2 <-
            sql"SELECT `varbinary`, `varbinary_null` FROM `connector_test`.`all_types` WHERE `varbinary` = ${ "varbinary" }"
              .query[(String, Option[String])]
              .to[Option]
          result3 <- sql"SELECT `varbinary`, `varbinary_null` FROM `connector_test`.`all_types`"
                       .query[(String, Option[String])]
                       .unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List(("varbinary", None)), Some(("varbinary", None)), ("varbinary", None))
    )
  }

  test("Statement should be able to retrieve MEDIUMBLOB type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <- sql"SELECT `mediumblob`, `mediumblob_null` FROM `connector_test`.`all_types`"
                       .query[(String, Option[String])]
                       .to[List]
          result2 <-
            sql"SELECT `mediumblob`, `mediumblob_null` FROM `connector_test`.`all_types` WHERE `mediumblob` = ${ "mediumblob" }"
              .query[(String, Option[String])]
              .to[Option]
          result3 <- sql"SELECT `mediumblob`, `mediumblob_null` FROM `connector_test`.`all_types`"
                       .query[(String, Option[String])]
                       .unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List(("mediumblob", None)), Some(("mediumblob", None)), ("mediumblob", None))
    )
  }

  test("Statement should be able to retrieve LONGBLOB type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `longblob`, `longblob_null` FROM `connector_test`.`all_types`"
              .query[(String, Option[String])]
              .to[List]
          result2 <-
            sql"SELECT `longblob`, `longblob_null` FROM `connector_test`.`all_types` WHERE `longblob` = ${ "longblob" }"
              .query[(String, Option[String])]
              .to[Option]
          result3 <- sql"SELECT `longblob`, `longblob_null` FROM `connector_test`.`all_types`"
                       .query[(String, Option[String])]
                       .unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List(("longblob", None)), Some(("longblob", None)), ("longblob", None))
    )
  }

  test("Statement should be able to retrieve TINYTEXT type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `tinytext`, `tinytext_null` FROM `connector_test`.`all_types`"
              .query[(String, Option[String])]
              .to[List]
          result2 <-
            sql"SELECT `tinytext`, `tinytext_null` FROM `connector_test`.`all_types` WHERE `tinytext` = ${ "tinytext" }"
              .query[(String, Option[String])]
              .to[Option]
          result3 <- sql"SELECT `tinytext`, `tinytext_null` FROM `connector_test`.`all_types`"
                       .query[(String, Option[String])]
                       .unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List(("tinytext", None)), Some(("tinytext", None)), ("tinytext", None))
    )
  }

  test("Statement should be able to retrieve TEXT type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `text`, `text_null` FROM `connector_test`.`all_types`".query[(String, Option[String])].to[List]
          result2 <-
            sql"SELECT `text`, `text_null` FROM `connector_test`.`all_types` WHERE `text` = ${ "text" }"
              .query[(String, Option[String])]
              .to[Option]
          result3 <-
            sql"SELECT `text`, `text_null` FROM `connector_test`.`all_types`".query[(String, Option[String])].unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List(("text", None)), Some(("text", None)), ("text", None))
    )
  }

  test("Statement should be able to retrieve MEDIUMTEXT type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <- sql"SELECT `mediumtext`, `mediumtext_null` FROM `connector_test`.`all_types`"
                       .query[(String, Option[String])]
                       .to[List]
          result2 <-
            sql"SELECT `mediumtext`, `mediumtext_null` FROM `connector_test`.`all_types` WHERE `mediumtext` = ${ "mediumtext" }"
              .query[(String, Option[String])]
              .to[Option]
          result3 <- sql"SELECT `mediumtext`, `mediumtext_null` FROM `connector_test`.`all_types`"
                       .query[(String, Option[String])]
                       .unsafe
        yield (result1, result2, result3))
          .readOnly(conn)
      },
      (List(("mediumtext", None)), Some(("mediumtext", None)), ("mediumtext", None))
    )
  }

  test("Statement should be able to retrieve LONGTEXT type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `longtext`, `longtext_null` FROM `connector_test`.`all_types`"
              .query[(String, Option[String])]
              .to[List]
          result2 <-
            sql"SELECT `longtext`, `longtext_null` FROM `connector_test`.`all_types` WHERE `longtext` = ${ "longtext" }"
              .query[(String, Option[String])]
              .to[Option]
          result3 <- sql"SELECT `longtext`, `longtext_null` FROM `connector_test`.`all_types`"
                       .query[(String, Option[String])]
                       .unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List(("longtext", None)), Some(("longtext", None)), ("longtext", None))
    )
  }

  test("Statement should be able to retrieve ENUM type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `enum`, `enum_null` FROM `connector_test`.`all_types`".query[(String, Option[String])].to[List]
          result2 <-
            sql"SELECT `enum`, `enum_null` FROM `connector_test`.`all_types` WHERE `enum` = ${ "a" }"
              .query[(String, Option[String])]
              .to[Option]
          result3 <-
            sql"SELECT `enum`, `enum_null` FROM `connector_test`.`all_types`".query[(String, Option[String])].unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List(("a", None)), Some(("a", None)), ("a", None))
    )
  }

  test("Statement should be able to retrieve SET type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `set`, `set_null` FROM `connector_test`.`all_types`".query[(String, Option[String])].to[List]
          result2 <-
            sql"SELECT `set`, `set_null` FROM `connector_test`.`all_types` WHERE `set` = ${ "a,b" }"
              .query[(String, Option[String])]
              .to[Option]
          result3 <-
            sql"SELECT `set`, `set_null` FROM `connector_test`.`all_types`".query[(String, Option[String])].unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List(("a,b", None)), Some(("a,b", None)), ("a,b", None))
    )
  }

  test("Statement should be able to retrieve JSON type records.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `json`, `json_null` FROM `connector_test`.`all_types`".query[(String, Option[String])].to[List]
          result2 <-
            sql"SELECT `json`, `json_null` FROM `connector_test`.`all_types`".query[(String, Option[String])].to[Option]
          result3 <-
            sql"SELECT `json`, `json_null` FROM `connector_test`.`all_types`".query[(String, Option[String])].unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List(("{\"a\": 1}", None)), Some(("{\"a\": 1}", None)), ("{\"a\": 1}", None))
    )
  }

  test("Statement and can be converted to any model.") {
    assertIO(
      connection.use { conn =>
        (for
          result1 <-
            sql"SELECT `int_unsigned`, `int_unsigned_null` FROM `connector_test`.`all_types`"
              .query[(Long, Option[Long])]
              .to[List]
          result2 <-
            sql"SELECT `int_unsigned`, `int_unsigned_null` FROM `connector_test`.`all_types` WHERE `int_unsigned` = ${ 4294967295L }"
              .query[(Long, Option[Long])]
              .to[Option]
          result3 <-
            sql"SELECT `int_unsigned`, `int_unsigned_null` FROM `connector_test`.`all_types`"
              .query[(Long, Option[Long])]
              .unsafe
        yield (result1, result2, result3)).readOnly(conn)
      },
      (List((4294967295L, None)), Some((4294967295L, None)), (4294967295L, None))
    )
  }

  test("The results obtained by JOIN can be converted to a model based on property names.") {
    case class City(id: Int, name: String)
    case class Country(code: String, name: String)
    case class CityWithCountry(c: City, ct: Country)

    assertIO(
      connection.use { conn =>
        sql"SELECT c.Id, c.Name, ct.Code, ct.Name FROM city AS c JOIN country AS ct ON c.CountryCode = ct.Code LIMIT 1"
          .query[CityWithCountry]
          .to[Option]
          .readOnly(conn)
      },
      Some(CityWithCountry(City(1, "Kabul"), Country("AFG", "Afghanistan")))
    )
  }

  test("The results obtained by JOIN can be mapped to the class Tuple.") {
    case class City(id: Int, name: String)
    case class Country(code: String, name: String)

    assertIO(
      connection.use { conn =>
        sql"SELECT city.Id, city.Name, country.Code, country.Name FROM city JOIN country ON city.CountryCode = country.Code LIMIT 1"
          .query[(City, Country)]
          .to[Option]
          .readOnly(conn)
      },
      Some((City(1, "Kabul"), Country("AFG", "Afghanistan")))
    )
  }
