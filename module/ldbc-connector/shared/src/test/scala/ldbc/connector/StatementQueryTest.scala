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
          decoded   <- resultSet.decode[Int](int)
        yield decoded
      },
      List(1)
    )
  }

  test("Statement should be able to retrieve BIT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `bit`, `bit_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(Byte, Option[Byte])](bit *: bit.opt)
        yield decoded
      },
      List((1.toByte, None))
    )
  }

  test("Statement should be able to retrieve TINYINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `tinyint`, `tinyint_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(Byte, Option[Byte])](tinyint *: tinyint.opt)
        yield decoded
      },
      List((127.toByte, None))
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
          decoded <- resultSet.decode[(Short, Option[Short])](utinyint *: utinyint.opt)
        yield decoded
      },
      List((255.toShort, None))
    )
  }

  test("Statement should be able to retrieve SMALLINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `smallint`, `smallint_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(Short, Option[Short])](smallint *: smallint.opt)
        yield decoded
      },
      List((32767.toShort, None))
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
          decoded <- resultSet.decode[(Int, Option[Int])](usmallint *: usmallint.opt)
        yield decoded
      },
      List((65535, None))
    )
  }

  test("Statement should be able to retrieve MEDIUMINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `mediumint`, `mediumint_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(Int, Option[Int])](mediumint *: mediumint.opt)
        yield decoded
      },
      List((8388607, None))
    )
  }

  test("Statement should be able to retrieve INT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `int`, `int_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(Int, Option[Int])](int *: int.opt)
        yield decoded
      },
      List((2147483647, None))
    )
  }

  test("Statement should be able to retrieve unsigned INT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <-
            statement.executeQuery("SELECT `int_unsigned`, `int_unsigned_null` FROM `connector_test`.`all_types`")
          decoded <- resultSet.decode[(Long, Option[Long])](uint *: uint.opt)
        yield decoded
      },
      List((4294967295L, None))
    )
  }

  test("Statement should be able to retrieve BIGINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `bigint`, `bigint_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(Long, Option[Long])](bigint *: bigint.opt)
        yield decoded
      },
      List((9223372036854775807L, None))
    )
  }

  test("Statement should be able to retrieve unsigned BIGINT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <-
            statement.executeQuery("SELECT `bigint_unsigned`, `bigint_unsigned_null` FROM `connector_test`.`all_types`")
          decoded <- resultSet.decode[(BigInt, Option[BigInt])](ubigint *: ubigint.opt)
        yield decoded
      },
      List((BigInt("18446744073709551615"), None))
    )
  }

  test("Statement should be able to retrieve FLOAT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `float`, `float_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(Float, Option[Float])](float *: float.opt)
        yield decoded
      },
      List((3.40282e38f, None))
    )
  }

  test("Statement should be able to retrieve DOUBLE type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `double`, `double_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(Double, Option[Double])](double *: double.opt)
        yield decoded
      },
      List((1.7976931348623157e308, None))
    )
  }

  test("Statement should be able to retrieve DECIMAL type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `decimal`, `decimal_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(BigDecimal, Option[BigDecimal])](decimal(10, 2) *: decimal(10, 2).opt)
        yield decoded
      },
      List((BigDecimal.decimal(9999999.99), None))
    )
  }

  test("Statement should be able to retrieve DATE type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `date`, `date_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(LocalDate, Option[LocalDate])](date *: date.opt)
        yield decoded
      },
      List((LocalDate.of(2020, 1, 1), None))
    )
  }

  test("Statement should be able to retrieve TIME type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `time`, `time_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(LocalTime, Option[LocalTime])](time *: time.opt)
        yield decoded
      },
      List((LocalTime.of(12, 34, 56), None))
    )
  }

  test("Statement should be able to retrieve DATETIME type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `datetime`, `datetime_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(LocalDateTime, Option[LocalDateTime])](datetime *: datetime.opt)
        yield decoded
      },
      List((LocalDateTime.of(2020, 1, 1, 12, 34, 56), None))
    )
  }

  test("Statement should be able to retrieve TIMESTAMP type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `timestamp`, `timestamp_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(LocalDateTime, Option[LocalDateTime])](timestamp *: timestamp.opt)
        yield decoded
      },
      List((LocalDateTime.of(2020, 1, 1, 12, 34, 56), None))
    )
  }

  test("Statement should be able to retrieve YEAR type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `year`, `year_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(Year, Option[Year])](year *: year.opt)
        yield decoded
      },
      List((Year.of(2020), None))
    )
  }

  test("Statement should be able to retrieve CHAR type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `char`, `char_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(String, Option[String])](char(10) *: char(10).opt)
        yield decoded
      },
      List(("char", None))
    )
  }

  test("Statement should be able to retrieve VARCHAR type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `varchar`, `varchar_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(String, Option[String])](varchar(10) *: varchar(10).opt)
        yield decoded
      },
      List(("varchar", None))
    )
  }

  test("Statement should be able to retrieve BINARY type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `binary`, `binary_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(Array[Byte], Option[Array[Byte]])](binary(10) *: binary(10).opt)
        yield decoded.map((a, b) => (a.mkString(":"), b.map(_.mkString(":"))))
      },
      List((Array[Byte](98, 105, 110, 97, 114, 121, 0, 0, 0, 0).mkString(":"), None))
    )
  }

  test("Statement should be able to retrieve VARBINARY type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `varbinary`, `varbinary_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(String, Option[String])](varbinary(10) *: varbinary(10).opt)
        yield decoded
      },
      List(("varbinary", None))
    )
  }

  test("Statement should be able to retrieve TINYBLOB type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `tinyblob`, `tinyblob_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(String, Option[String])](tinyblob *: tinyblob.opt)
        yield decoded
      },
      List(("tinyblob", None))
    )
  }

  test("Statement should be able to retrieve BLOB type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `blob`, `blob_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(String, Option[String])](blob *: blob.opt)
        yield decoded
      },
      List(("blob", None))
    )
  }

  test("Statement should be able to retrieve MEDIUMBLOB type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <-
            statement.executeQuery("SELECT `mediumblob`, `mediumblob_null` FROM `connector_test`.`all_types`")
          decoded <- resultSet.decode[(String, Option[String])](mediumblob *: mediumblob.opt)
        yield decoded
      },
      List(("mediumblob", None))
    )
  }

  test("Statement should be able to retrieve LONGBLOB type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `longblob`, `longblob_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(String, Option[String])](longblob *: longblob.opt)
        yield decoded
      },
      List(("longblob", None))
    )
  }

  test("Statement should be able to retrieve TINYTEXT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `tinytext`, `tinytext_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(String, Option[String])](tinytext *: tinytext.opt)
        yield decoded
      },
      List(("tinytext", None))
    )
  }

  test("Statement should be able to retrieve TEXT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `text`, `text_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(String, Option[String])](text *: text.opt)
        yield decoded
      },
      List(("text", None))
    )
  }

  test("Statement should be able to retrieve MEDIUMTEXT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <-
            statement.executeQuery("SELECT `mediumtext`, `mediumtext_null` FROM `connector_test`.`all_types`")
          decoded <- resultSet.decode[(String, Option[String])](mediumtext *: mediumtext.opt)
        yield decoded
      },
      List(("mediumtext", None))
    )
  }

  test("Statement should be able to retrieve LONGTEXT type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `longtext`, `longtext_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(String, Option[String])](longtext *: longtext.opt)
        yield decoded
      },
      List(("longtext", None))
    )
  }

  test("Statement should be able to retrieve ENUM type records.") {
    val t = `enum`("a", "b", "c")
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `enum`, `enum_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(String, Option[String])](t *: t.opt)
        yield decoded
      },
      List(("a", None))
    )
  }

  test("Statement should be able to retrieve SET type records.") {
    val s = set("a", "b", "c")
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `set`, `set_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(List[String], Option[List[String]])](s *: s.opt)
        yield decoded
      },
      List((List("a", "b"), None))
    )
  }

  test("Statement should be able to retrieve JSON type records.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery("SELECT `json`, `json_null` FROM `connector_test`.`all_types`")
          decoded   <- resultSet.decode[(String, Option[String])](json *: json.opt)
        yield decoded
      },
      List(("{\"a\": 1}", None))
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
