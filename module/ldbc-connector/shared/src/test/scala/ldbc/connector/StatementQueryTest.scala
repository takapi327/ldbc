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

import ldbc.connector.codec.all.*
import ldbc.connector.net.packet.response.ResultSetRowPacket

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
      connection.use(_.statement("SELECT 1").executeQuery().map(_.decode[Int](int))),
      List(1)
    )
  }

  test("Statement should be able to retrieve BIT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `bit`, `bit_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(Byte, Option[Byte])](bit *: bit.opt))
      ),
      List((1.toByte, None))
    )
  }

  test("Statement should be able to retrieve TINYINT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `tinyint`, `tinyint_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(Byte, Option[Byte])](tinyint *: tinyint.opt))
      ),
      List((127.toByte, None))
    )
  }

  test("Statement should be able to retrieve unsigned TINYINT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `tinyint_unsigned`, `tinyint_unsigned_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(Short, Option[Short])](utinyint *: utinyint.opt))
      ),
      List((255.toShort, None))
    )
  }

  test("Statement should be able to retrieve SMALLINT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `smallint`, `smallint_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(Short, Option[Short])](smallint *: smallint.opt))
      ),
      List((32767.toShort, None))
    )
  }

  test("Statement should be able to retrieve unsigned SMALLINT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `smallint_unsigned`, `smallint_unsigned_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(Int, Option[Int])](usmallint *: usmallint.opt))
      ),
      List((65535, None))
    )
  }

  test("Statement should be able to retrieve MEDIUMINT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `mediumint`, `mediumint_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(Int, Option[Int])](mediumint *: mediumint.opt))
      ),
      List((8388607, None))
    )
  }

  test("Statement should be able to retrieve INT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `int`, `int_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(Int, Option[Int])](int *: int.opt))
      ),
      List((2147483647, None))
    )
  }

  test("Statement should be able to retrieve unsigned INT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `int_unsigned`, `int_unsigned_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(Long, Option[Long])](uint *: uint.opt))
      ),
      List((4294967295L, None))
    )
  }

  test("Statement should be able to retrieve BIGINT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `bigint`, `bigint_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(Long, Option[Long])](bigint *: bigint.opt))
      ),
      List((9223372036854775807L, None))
    )
  }

  test("Statement should be able to retrieve unsigned BIGINT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `bigint_unsigned`, `bigint_unsigned_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(BigInt, Option[BigInt])](ubigint *: ubigint.opt))
      ),
      List((BigInt("18446744073709551615"), None))
    )
  }

  test("Statement should be able to retrieve FLOAT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `float`, `float_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(Float, Option[Float])](float *: float.opt))
      ),
      List((3.40282e38f, None))
    )
  }

  test("Statement should be able to retrieve DOUBLE type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `double`, `double_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(Double, Option[Double])](double *: double.opt))
      ),
      List((1.7976931348623157e308, None))
    )
  }

  test("Statement should be able to retrieve DECIMAL type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `decimal`, `decimal_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(BigDecimal, Option[BigDecimal])](decimal(10, 2) *: decimal(10, 2).opt))
      ),
      List((BigDecimal.decimal(9999999.99), None))
    )
  }

  test("Statement should be able to retrieve DATE type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `date`, `date_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(LocalDate, Option[LocalDate])](date *: date.opt))
      ),
      List((LocalDate.of(2020, 1, 1), None))
    )
  }

  test("Statement should be able to retrieve TIME type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `time`, `time_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(LocalTime, Option[LocalTime])](time *: time.opt))
      ),
      List((LocalTime.of(12, 34, 56), None))
    )
  }

  test("Statement should be able to retrieve DATETIME type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `datetime`, `datetime_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(LocalDateTime, Option[LocalDateTime])](datetime *: datetime.opt))
      ),
      List((LocalDateTime.of(2020, 1, 1, 12, 34, 56), None))
    )
  }

  test("Statement should be able to retrieve TIMESTAMP type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `timestamp`, `timestamp_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(LocalDateTime, Option[LocalDateTime])](timestamp *: timestamp.opt))
      ),
      List((LocalDateTime.of(2020, 1, 1, 12, 34, 56), None))
    )
  }

  test("Statement should be able to retrieve YEAR type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `year`, `year_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(Year, Option[Year])](year *: year.opt))
      ),
      List((Year.of(2020), None))
    )
  }

  test("Statement should be able to retrieve CHAR type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `char`, `char_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(String, Option[String])](char(10) *: char(10).opt))
      ),
      List(("char", None))
    )
  }

  test("Statement should be able to retrieve VARCHAR type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `varchar`, `varchar_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(String, Option[String])](varchar(10) *: varchar(10).opt))
      ),
      List(("varchar", None))
    )
  }

  test("Statement should be able to retrieve BINARY type records.") {
    assertIO(
      connection.use(conn =>
        for resultSet <-
            conn.statement("SELECT `binary`, `binary_null` FROM `connector_test`.`all_types`").executeQuery()
        yield
          val decoded = resultSet.decode[(Array[Byte], Option[Array[Byte]])](binary(10) *: binary(10).opt)
          decoded.map((a, b) => (a.mkString(":"), b.map(_.mkString(":"))))
      ),
      List((Array[Byte](98, 105, 110, 97, 114, 121, 0, 0, 0, 0).mkString(":"), None))
    )
  }

  test("Statement should be able to retrieve VARBINARY type records.") {
    assertIO(
      connection.use(conn =>
        for resultSet <-
            conn.statement("SELECT `varbinary`, `varbinary_null` FROM `connector_test`.`all_types`").executeQuery()
        yield resultSet.decode[(String, Option[String])](varbinary(10) *: varbinary(10).opt)
      ),
      List(("varbinary", None))
    )
  }

  test("Statement should be able to retrieve TINYBLOB type records.") {
    assertIO(
      connection.use(conn =>
        for resultSet <-
            conn.statement("SELECT `tinyblob`, `tinyblob_null` FROM `connector_test`.`all_types`").executeQuery()
        yield resultSet.decode[(String, Option[String])](tinyblob *: tinyblob.opt)
      ),
      List(("tinyblob", None))
    )
  }

  test("Statement should be able to retrieve BLOB type records.") {
    assertIO(
      connection.use(conn =>
        for resultSet <- conn.statement("SELECT `blob`, `blob_null` FROM `connector_test`.`all_types`").executeQuery()
        yield resultSet.decode[(String, Option[String])](blob *: blob.opt)
      ),
      List(("blob", None))
    )
  }

  test("Statement should be able to retrieve MEDIUMBLOB type records.") {
    assertIO(
      connection.use(conn =>
        for resultSet <-
            conn.statement("SELECT `mediumblob`, `mediumblob_null` FROM `connector_test`.`all_types`").executeQuery()
        yield resultSet.decode[(String, Option[String])](mediumblob *: mediumblob.opt)
      ),
      List(("mediumblob", None))
    )
  }

  test("Statement should be able to retrieve LONGBLOB type records.") {
    assertIO(
      connection.use(conn =>
        for resultSet <-
            conn.statement("SELECT `longblob`, `longblob_null` FROM `connector_test`.`all_types`").executeQuery()
        yield resultSet.decode[(String, Option[String])](longblob *: longblob.opt)
      ),
      List(("longblob", None))
    )
  }

  test("Statement should be able to retrieve TINYTEXT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `tinytext`, `tinytext_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(String, Option[String])](tinytext *: tinytext.opt))
      ),
      List(("tinytext", None))
    )
  }

  test("Statement should be able to retrieve TEXT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `text`, `text_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(String, Option[String])](text *: text.opt))
      ),
      List(("text", None))
    )
  }

  test("Statement should be able to retrieve MEDIUMTEXT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `mediumtext`, `mediumtext_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(String, Option[String])](mediumtext *: mediumtext.opt))
      ),
      List(("mediumtext", None))
    )
  }

  test("Statement should be able to retrieve LONGTEXT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `longtext`, `longtext_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(String, Option[String])](longtext *: longtext.opt))
      ),
      List(("longtext", None))
    )
  }

  test("Statement should be able to retrieve ENUM type records.") {
    val t = `enum`("a", "b", "c")
    assertIO(
      connection.use(
        _.statement("SELECT `enum`, `enum_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(String, Option[String])](t *: t.opt))
      ),
      List(("a", None))
    )
  }

  test("Statement should be able to retrieve SET type records.") {
    val s = set("a", "b", "c")
    assertIO(
      connection.use(
        _.statement("SELECT `set`, `set_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(List[String], Option[List[String]])](s *: s.opt))
      ),
      List((List("a", "b"), None))
    )
  }

  test("Statement should be able to retrieve JSON type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `json`, `json_null` FROM `connector_test`.`all_types`")
          .executeQuery()
          .map(_.decode[(String, Option[String])](json *: json.opt))
      ),
      List(("{\"a\": 1}", None))
    )
  }
