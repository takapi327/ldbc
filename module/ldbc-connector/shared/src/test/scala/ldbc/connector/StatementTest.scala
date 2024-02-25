/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import org.typelevel.otel4s.trace.Tracer

import cats.effect.*

import munit.CatsEffectSuite

import ldbc.connector.net.packet.response.ResultSetRowPacket

class StatementTest extends CatsEffectSuite:

  given Tracer[IO] = Tracer.noop[IO]

  private val connection = Connection[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = Some("password"),
    ssl      = SSL.Trusted
  )

  test("Statement should be able to execute a query") {
    assertIO(connection.use(_.statement("SELECT 1").executeQuery()), List(ResultSetRowPacket(List(Some("1")))))
  }

  test("Statement should be able to retrieve BIT type records.") {
    assertIO(
      connection.use(_.statement("SELECT `bit`, `bit_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(ResultSetRowPacket(List(Some("\u0001"), None)))
    )
  }

  test("Statement should be able to retrieve TINYINT type records.") {
    assertIO(
      connection.use(_.statement("SELECT `tinyint`, `tinyint_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(ResultSetRowPacket(List(Some("127"), None)))
    )
  }

  test("Statement should be able to retrieve SMALLINT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `smallint`, `smallint_null` FROM `connector_test`.`all_types`").executeQuery()
      ),
      List(ResultSetRowPacket(List(Some("32767"), None)))
    )
  }

  test("Statement should be able to retrieve MEDIUMINT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `mediumint`, `mediumint_null` FROM `connector_test`.`all_types`").executeQuery()
      ),
      List(ResultSetRowPacket(List(Some("8388607"), None)))
    )
  }

  test("Statement should be able to retrieve INT type records.") {
    assertIO(
      connection.use(_.statement("SELECT `int`, `int_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(ResultSetRowPacket(List(Some("2147483647"), None)))
    )
  }

  test("Statement should be able to retrieve BIGINT type records.") {
    assertIO(
      connection.use(_.statement("SELECT `bigint`, `bigint_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(ResultSetRowPacket(List(Some("9223372036854775807"), None)))
    )
  }

  test("Statement should be able to retrieve FLOAT type records.") {
    assertIO(
      connection.use(_.statement("SELECT `float`, `float_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(ResultSetRowPacket(List(Some("3.40282e38"), None)))
    )
  }

  test("Statement should be able to retrieve DOUBLE type records.") {
    assertIO(
      connection.use(_.statement("SELECT `double`, `double_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(ResultSetRowPacket(List(Some("1.7976931348623157e308"), None)))
    )
  }

  test("Statement should be able to retrieve DECIMAL type records.") {
    assertIO(
      connection.use(_.statement("SELECT `decimal`, `decimal_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(ResultSetRowPacket(List(Some("9999999.99"), None)))
    )
  }

  test("Statement should be able to retrieve DATE type records.") {
    assertIO(
      connection.use(_.statement("SELECT `date`, `date_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(ResultSetRowPacket(List(Some("2020-01-01"), None)))
    )
  }

  test("Statement should be able to retrieve TIME type records.") {
    assertIO(
      connection.use(_.statement("SELECT `time`, `time_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(ResultSetRowPacket(List(Some("12:34:56"), None)))
    )
  }

  test("Statement should be able to retrieve DATETIME type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `datetime`, `datetime_null` FROM `connector_test`.`all_types`").executeQuery()
      ),
      List(ResultSetRowPacket(List(Some("2020-01-01 12:34:56"), None)))
    )
  }

  test("Statement should be able to retrieve TIMESTAMP type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `timestamp`, `timestamp_null` FROM `connector_test`.`all_types`").executeQuery()
      ),
      List(ResultSetRowPacket(List(Some("2020-01-01 12:34:56"), None)))
    )
  }

  test("Statement should be able to retrieve YEAR type records.") {
    assertIO(
      connection.use(_.statement("SELECT `year`, `year_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(ResultSetRowPacket(List(Some("2020"), None)))
    )
  }

  test("Statement should be able to retrieve CHAR type records.") {
    assertIO(
      connection.use(_.statement("SELECT `char`, `char_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(ResultSetRowPacket(List(Some("char"), None)))
    )
  }

  test("Statement should be able to retrieve VARCHAR type records.") {
    assertIO(
      connection.use(_.statement("SELECT `varchar`, `varchar_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(ResultSetRowPacket(List(Some("varchar"), None)))
    )
  }

  test("Statement should be able to retrieve BINARY type records.") {
    assertIO(
      connection.use(_.statement("SELECT `binary`, `binary_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(ResultSetRowPacket(List(Some("binary\u0000\u0000\u0000\u0000"), None)))
    )
  }

  test("Statement should be able to retrieve VARBINARY type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `varbinary`, `varbinary_null` FROM `connector_test`.`all_types`").executeQuery()
      ),
      List(ResultSetRowPacket(List(Some("varbinary"), None)))
    )
  }

  test("Statement should be able to retrieve TINYBLOB type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `tinyblob`, `tinyblob_null` FROM `connector_test`.`all_types`").executeQuery()
      ),
      List(ResultSetRowPacket(List(Some("tinyblob"), None)))
    )
  }

  test("Statement should be able to retrieve BLOB type records.") {
    assertIO(
      connection.use(_.statement("SELECT `blob`, `blob_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(ResultSetRowPacket(List(Some("blob"), None)))
    )
  }

  test("Statement should be able to retrieve MEDIUMBLOB type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `mediumblob`, `mediumblob_null` FROM `connector_test`.`all_types`").executeQuery()
      ),
      List(ResultSetRowPacket(List(Some("mediumblob"), None)))
    )
  }

  test("Statement should be able to retrieve LONGBLOB type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `longblob`, `longblob_null` FROM `connector_test`.`all_types`").executeQuery()
      ),
      List(ResultSetRowPacket(List(Some("longblob"), None)))
    )
  }

  test("Statement should be able to retrieve TINYTEXT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `tinytext`, `tinytext_null` FROM `connector_test`.`all_types`").executeQuery()
      ),
      List(ResultSetRowPacket(List(Some("tinytext"), None)))
    )
  }

  test("Statement should be able to retrieve TEXT type records.") {
    assertIO(
      connection.use(_.statement("SELECT `text`, `text_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(ResultSetRowPacket(List(Some("text"), None)))
    )
  }

  test("Statement should be able to retrieve MEDIUMTEXT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `mediumtext`, `mediumtext_null` FROM `connector_test`.`all_types`").executeQuery()
      ),
      List(ResultSetRowPacket(List(Some("mediumtext"), None)))
    )
  }

  test("Statement should be able to retrieve LONGTEXT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `longtext`, `longtext_null` FROM `connector_test`.`all_types`").executeQuery()
      ),
      List(ResultSetRowPacket(List(Some("longtext"), None)))
    )
  }

  test("Statement should be able to retrieve ENUM type records.") {
    assertIO(
      connection.use(_.statement("SELECT `enum`, `enum_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(ResultSetRowPacket(List(Some("a"), None)))
    )
  }

  test("Statement should be able to retrieve SET type records.") {
    assertIO(
      connection.use(_.statement("SELECT `set`, `set_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(ResultSetRowPacket(List(Some("a,b"), None)))
    )
  }

  test("Statement should be able to retrieve JSON type records.") {
    assertIO(
      connection.use(_.statement("SELECT `json`, `json_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(ResultSetRowPacket(List(Some("{\"a\": 1}"), None)))
    )
  }

  test("Statement should be able to retrieve GEOMETRY type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `geometry`, `geometry_null` FROM `connector_test`.`all_types`").executeQuery()
      ),
      List(
        ResultSetRowPacket(
          List(
            Some(
              "\u0000\u0000\u0000\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?"
            ),
            None
          )
        )
      )
    )
  }

  test("Statement should be able to retrieve POINT type records.") {
    assertIO(
      connection.use(_.statement("SELECT `point`, `point_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(
        ResultSetRowPacket(
          List(
            Some(
              "\u0000\u0000\u0000\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?"
            ),
            None
          )
        )
      )
    )
  }

  test("Statement should be able to retrieve LINESTRING type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `linestring`, `linestring_null` FROM `connector_test`.`all_types`").executeQuery()
      ),
      List(
        ResultSetRowPacket(
          List(
            Some(
              "\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000\u0000\u0002\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?"
            ),
            None
          )
        )
      )
    )
  }

  test("Statement should be able to retrieve POLYGON type records.") {
    assertIO(
      connection.use(_.statement("SELECT `polygon`, `polygon_null` FROM `connector_test`.`all_types`").executeQuery()),
      List(
        ResultSetRowPacket(
          List(
            Some(
              "\u0000\u0000\u0000\u0000\u0001\u0003\u0000\u0000\u0000\u0001\u0000\u0000\u0000\u0005\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            ),
            None
          )
        )
      )
    )
  }

  test("Statement should be able to retrieve MULTIPOINT type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `multipoint`, `multipoint_null` FROM `connector_test`.`all_types`").executeQuery()
      ),
      List(
        ResultSetRowPacket(
          List(
            Some(
              "\u0000\u0000\u0000\u0000\u0001\u0004\u0000\u0000\u0000\u0002\u0000\u0000\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?"
            ),
            None
          )
        )
      )
    )
  }

  test("Statement should be able to retrieve MULTILINESTRING type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `multilinestring`, `multilinestring_null` FROM `connector_test`.`all_types`").executeQuery()
      ),
      List(
        ResultSetRowPacket(
          List(
            Some(
              "\u0000\u0000\u0000\u0000\u0001\u0005\u0000\u0000\u0000\u0001\u0000\u0000\u0000\u0001\u0002\u0000\u0000\u0000\u0002\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?"
            ),
            None
          )
        )
      )
    )
  }

  test("Statement should be able to retrieve MULTIPOLYGON type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `multipolygon`, `multipolygon_null` FROM `connector_test`.`all_types`").executeQuery()
      ),
      List(
        ResultSetRowPacket(
          List(
            Some(
              "\u0000\u0000\u0000\u0000\u0001\u0006\u0000\u0000\u0000\u0001\u0000\u0000\u0000\u0001\u0003\u0000\u0000\u0000\u0001\u0000\u0000\u0000\u0005\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            ),
            None
          )
        )
      )
    )
  }

  test("Statement should be able to retrieve GEOMETRYCOLLECTION type records.") {
    assertIO(
      connection.use(
        _.statement("SELECT `geometrycollection`, `geometrycollection_null` FROM `connector_test`.`all_types`")
          .executeQuery()
      ),
      List(
        ResultSetRowPacket(
          List(
            Some(
              "\u0000\u0000\u0000\u0000\u0001\u0007\u0000\u0000\u0000\u0001\u0000\u0000\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?\u0000\u0000\u0000\u0000\u0000\u0000\ufffd?"
            ),
            None
          )
        )
      )
    )
  }
