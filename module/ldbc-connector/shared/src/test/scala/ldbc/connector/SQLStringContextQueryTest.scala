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

import ldbc.connector.io.*

class SQLStringContextQueryTest extends CatsEffectSuite:

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
        sql"SELECT 1".toList[Tuple1[Int]].run(conn)
      },
      List(Tuple1(1))
    )
  }

  test("Statement should be able to retrieve BIT type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `bit`, `bit_null` FROM `connector_test`.`all_types`".toList[(Byte, Byte)].run(conn)
      },
      List((1.toByte, 0.toByte))
    )
  }

  test("Statement should be able to retrieve TINYINT type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `tinyint`, `tinyint_null` FROM `connector_test`.`all_types`".toList[(Byte, Byte)].run(conn)
      },
      List((127.toByte, 0.toByte))
    )
  }

  test("Statement should be able to retrieve unsigned TINYINT type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `tinyint_unsigned`, `tinyint_unsigned_null` FROM `connector_test`.`all_types`"
          .toList[(Short, Short)]
          .run(conn)
      },
      List((255.toShort, 0.toShort))
    )
  }

  test("Statement should be able to retrieve SMALLINT type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `smallint`, `smallint_null` FROM `connector_test`.`all_types`".toList[(Short, Short)].run(conn)
      },
      List((32767.toShort, 0.toShort))
    )
  }

  test("Statement should be able to retrieve unsigned SMALLINT type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `smallint_unsigned`, `smallint_unsigned_null` FROM `connector_test`.`all_types`"
          .toList[(Int, Int)]
          .run(conn)
      },
      List((65535, 0))
    )
  }

  test("Statement should be able to retrieve MEDIUMINT type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `mediumint`, `mediumint_null` FROM `connector_test`.`all_types`".toList[(Int, Int)].run(conn)
      },
      List((8388607, 0))
    )
  }

  test("Statement should be able to retrieve INT type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `int`, `int_null` FROM `connector_test`.`all_types`".toList[(Int, Int)].run(conn)
      },
      List((2147483647, 0))
    )
  }

  test("Statement should be able to retrieve unsigned INT type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `int_unsigned`, `int_unsigned_null` FROM `connector_test`.`all_types`".toList[(Long, Long)].run(conn)
      },
      List((4294967295L, 0L))
    )
  }

  test("Statement should be able to retrieve BIGINT type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `bigint`, `bigint_null` FROM `connector_test`.`all_types`".toList[(Long, Long)].run(conn)
      },
      List((9223372036854775807L, 0L))
    )
  }

  test("Statement should be able to retrieve unsigned BIGINT type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `bigint_unsigned`, `bigint_unsigned_null` FROM `connector_test`.`all_types`"
          .toList[(String, Option[String])]
          .run(conn)
      },
      List(("18446744073709551615", None))
    )
  }

  test("Statement should be able to retrieve FLOAT type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `float`, `float_null` FROM `connector_test`.`all_types`".toList[(Float, Float)].run(conn)
      },
      List((3.40282e38f, 0f))
    )
  }

  test("Statement should be able to retrieve DOUBLE type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `double`, `double_null` FROM `connector_test`.`all_types`".toList[(Double, Double)].run(conn)
      },
      List((1.7976931348623157e308, 0.toDouble))
    )
  }

  test("Statement should be able to retrieve DECIMAL type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `decimal`, `decimal_null` FROM `connector_test`.`all_types`"
          .toList[(BigDecimal, Option[BigDecimal])]
          .run(conn)
      },
      List((BigDecimal.decimal(9999999.99), None))
    )
  }

  test("Statement should be able to retrieve DATE type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `date`, `date_null` FROM `connector_test`.`all_types`"
          .toList[(LocalDate, Option[LocalDate])]
          .run(conn)
      },
      List((LocalDate.of(2020, 1, 1), None))
    )
  }

  test("Statement should be able to retrieve TIME type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `time`, `time_null` FROM `connector_test`.`all_types`"
          .toList[(LocalTime, Option[LocalTime])]
          .run(conn)
      },
      List((LocalTime.of(12, 34, 56), None))
    )
  }

  test("Statement should be able to retrieve DATETIME type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `datetime`, `datetime_null` FROM `connector_test`.`all_types`"
          .toList[(LocalDateTime, Option[LocalDateTime])]
          .run(conn)
      },
      List((LocalDateTime.of(2020, 1, 1, 12, 34, 56), None))
    )
  }

  test("Statement should be able to retrieve TIMESTAMP type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `timestamp`, `timestamp_null` FROM `connector_test`.`all_types`"
          .toList[(LocalDateTime, Option[LocalDateTime])]
          .run(conn)
      },
      List((LocalDateTime.of(2020, 1, 1, 12, 34, 56), None))
    )
  }

  test("Statement should be able to retrieve YEAR type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `year`, `year_null` FROM `connector_test`.`all_types`".toList[(Short, Option[Short])].run(conn)
      },
      List((2020.toShort, None))
    )
  }

  test("Statement should be able to retrieve CHAR type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `char`, `char_null` FROM `connector_test`.`all_types`".toList[(String, Option[String])].run(conn)
      },
      List(("char", None))
    )
  }

  test("Statement should be able to retrieve VARCHAR type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `varchar`, `varchar_null` FROM `connector_test`.`all_types`"
          .toList[(String, Option[String])]
          .run(conn)
      },
      List(("varchar", None))
    )
  }

  test("Statement should be able to retrieve BINARY type records.") {
    assertIO(
      connection.use { conn =>
        (for result <- sql"SELECT `binary`, `binary_null` FROM `connector_test`.`all_types`"
                         .toList[(Array[Byte], Option[Array[Byte]])]
        yield result.map { case (v1, v2) => (v1.mkString(":"), v2) }).run(conn)
      },
      List((Array[Byte](98, 105, 110, 97, 114, 121, 0, 0, 0, 0).mkString(":"), None))
    )
  }

  test("Statement should be able to retrieve VARBINARY type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `varbinary`, `varbinary_null` FROM `connector_test`.`all_types`"
          .toList[(String, Option[String])]
          .run(conn)
      },
      List(("varbinary", None))
    )
  }

  test("Statement should be able to retrieve MEDIUMBLOB type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `mediumblob`, `mediumblob_null` FROM `connector_test`.`all_types`"
          .toList[(String, Option[String])]
          .run(conn)
      },
      List(("mediumblob", None))
    )
  }

  test("Statement should be able to retrieve LONGBLOB type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `longblob`, `longblob_null` FROM `connector_test`.`all_types`"
          .toList[(String, Option[String])]
          .run(conn)
      },
      List(("longblob", None))
    )
  }

  test("Statement should be able to retrieve TINYTEXT type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `tinytext`, `tinytext_null` FROM `connector_test`.`all_types`"
          .toList[(String, Option[String])]
          .run(conn)
      },
      List(("tinytext", None))
    )
  }

  test("Statement should be able to retrieve TEXT type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `text`, `text_null` FROM `connector_test`.`all_types`".toList[(String, Option[String])].run(conn)
      },
      List(("text", None))
    )
  }

  test("Statement should be able to retrieve MEDIUMTEXT type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `mediumtext`, `mediumtext_null` FROM `connector_test`.`all_types`"
          .toList[(String, Option[String])]
          .run(conn)
      },
      List(("mediumtext", None))
    )
  }

  test("Statement should be able to retrieve LONGTEXT type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `longtext`, `longtext_null` FROM `connector_test`.`all_types`"
          .toList[(String, Option[String])]
          .run(conn)
      },
      List(("longtext", None))
    )
  }

  test("Statement should be able to retrieve ENUM type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `enum`, `enum_null` FROM `connector_test`.`all_types`".toList[(String, Option[String])].run(conn)
      },
      List(("a", None))
    )
  }

  test("Statement should be able to retrieve SET type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `set`, `set_null` FROM `connector_test`.`all_types`".toList[(String, Option[String])].run(conn)
      },
      List(("a,b", None))
    )
  }

  test("Statement should be able to retrieve JSON type records.") {
    assertIO(
      connection.use { conn =>
        sql"SELECT `json`, `json_null` FROM `connector_test`.`all_types`".toList[(String, Option[String])].run(conn)
      },
      List(("{\"a\": 1}", None))
    )
  }

  test("Statement and can be converted to any model.") {
    case class LongClass(int: Long, intNull: Option[Long])
    assertIO(
      connection.use { conn =>
        sql"SELECT `int_unsigned`, `int_unsigned_null` FROM `connector_test`.`all_types`".toList[LongClass].run(conn)
      },
      List(LongClass(4294967295L, None))
    )
  }
