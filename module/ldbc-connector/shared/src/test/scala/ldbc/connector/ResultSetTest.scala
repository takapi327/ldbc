/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.time.*

import munit.CatsEffectSuite

import ldbc.connector.util.Version
import ldbc.connector.data.*
import ldbc.connector.net.packet.response.*

class ResultSetTest extends CatsEffectSuite:

  test("ResultSet should return the correct value for getInt") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG),
        column("c2", ColumnDataType.MYSQL_TYPE_LONG),
        column("c3", ColumnDataType.MYSQL_TYPE_LONG)
      ),
      Vector(ResultSetRowPacket(List(Some("1"), Some("2"), None))),
      Version(0, 0, 0)
    )
    val records = List.newBuilder[(Int, Int, Int)]
    while resultSet.next() do {
      val c1 = resultSet.getInt(1)
      val c2 = resultSet.getInt("c2")
      val c3 = resultSet.getInt(3)
      records += ((c1, c2, c3))
    }
    assertEquals(records.result(), List((1, 2, 0)))
  }

  test("ResultSet should return the correct value for getLong") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONGLONG),
        column("c2", ColumnDataType.MYSQL_TYPE_LONGLONG),
        column("c3", ColumnDataType.MYSQL_TYPE_LONGLONG)
      ),
      Vector(ResultSetRowPacket(List(Some("1"), Some("2"), None))),
      Version(0, 0, 0)
    )
    val records = List.newBuilder[(Long, Long, Long)]
    while resultSet.next() do {
      val c1 = resultSet.getLong(1)
      val c2 = resultSet.getLong("c2")
      val c3 = resultSet.getLong(3)
      records += ((c1, c2, c3))
    }
    assertEquals(records.result(), List((1L, 2L, 0L)))
  }

  test("ResultSet should return the correct value for getDouble") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_DOUBLE),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
        column("c3", ColumnDataType.MYSQL_TYPE_DOUBLE)
      ),
      Vector(ResultSetRowPacket(List(Some("1.1"), Some("2.2"), None))),
      Version(0, 0, 0)
    )
    val records = List.newBuilder[(Double, Double, Double)]
    while resultSet.next() do {
      val c1 = resultSet.getDouble(1)
      val c2 = resultSet.getDouble("c2")
      val c3 = resultSet.getDouble(3)
      records += ((c1, c2, c3))
    }
    assertEquals(records.result(), List((1.1, 2.2, 0.0)))
  }

  test("ResultSet should return the correct value for getString") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_STRING),
        column("c2", ColumnDataType.MYSQL_TYPE_STRING),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING)
      ),
      Vector(ResultSetRowPacket(List(Some("1"), Some("2"), None))),
      Version(0, 0, 0)
    )
    val records = List.newBuilder[(Option[String], Option[String], Option[String])]
    while resultSet.next() do {
      val c1 = resultSet.getString(1)
      val c2 = resultSet.getString("c2")
      val c3 = resultSet.getString(3)
      records += ((c1, c2, c3))
    }
    assertEquals(records.result(), List((Some("1"), Some("2"), None)))
  }

  test("ResultSet should return the correct value for getBoolean") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_TINY),
        column("c2", ColumnDataType.MYSQL_TYPE_TINY),
        column("c3", ColumnDataType.MYSQL_TYPE_TINY)
      ),
      Vector(ResultSetRowPacket(List(Some("1"), Some("0"), None))),
      Version(0, 0, 0)
    )
    val records = List.newBuilder[(Boolean, Boolean, Boolean)]
    while resultSet.next() do {
      val c1 = resultSet.getBoolean(1)
      val c2 = resultSet.getBoolean("c2")
      val c3 = resultSet.getBoolean(3)
      records += ((c1, c2, c3))
    }
    assertEquals(records.result(), List((true, false, false)))
  }

  test("ResultSet should return the correct value for getByte") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_TINY),
        column("c2", ColumnDataType.MYSQL_TYPE_TINY),
        column("c3", ColumnDataType.MYSQL_TYPE_TINY)
      ),
      Vector(ResultSetRowPacket(List(Some("1"), Some("2"), None))),
      Version(0, 0, 0)
    )
    val records = List.newBuilder[(Byte, Byte, Byte)]
    while resultSet.next() do {
      val c1 = resultSet.getByte(1)
      val c2 = resultSet.getByte("c2")
      val c3 = resultSet.getByte(3)
      records += ((c1, c2, c3))
    }
    assertEquals(records.result(), List((1.toByte, 2.toByte, 0.toByte)))
  }

  test("ResultSet should return the correct value for getShort") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_SHORT),
        column("c2", ColumnDataType.MYSQL_TYPE_SHORT),
        column("c3", ColumnDataType.MYSQL_TYPE_SHORT)
      ),
      Vector(ResultSetRowPacket(List(Some("1"), Some("2"), None))),
      Version(0, 0, 0)
    )
    val records = List.newBuilder[(Short, Short, Short)]
    while resultSet.next() do {
      val c1 = resultSet.getShort(1)
      val c2 = resultSet.getShort("c2")
      val c3 = resultSet.getShort(3)
      records += ((c1, c2, c3))
    }
    assertEquals(records.result(), List((1.toShort, 2.toShort, 0.toShort)))
  }

  test("ResultSet should return the correct value for getFloat") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_FLOAT),
        column("c2", ColumnDataType.MYSQL_TYPE_FLOAT),
        column("c3", ColumnDataType.MYSQL_TYPE_FLOAT)
      ),
      Vector(ResultSetRowPacket(List(Some("1.1"), Some("2.2"), None))),
      Version(0, 0, 0)
    )
    val records = List.newBuilder[(Float, Float, Float)]
    while resultSet.next() do {
      val c1 = resultSet.getFloat(1)
      val c2 = resultSet.getFloat("c2")
      val c3 = resultSet.getFloat(3)
      records += ((c1, c2, c3))
    }
    assertEquals(records.result(), List((1.1f, 2.2f, 0.0f)))
  }

  test("ResultSet should return the correct value for getBigDecimal") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_DECIMAL),
        column("c2", ColumnDataType.MYSQL_TYPE_DECIMAL),
        column("c3", ColumnDataType.MYSQL_TYPE_DECIMAL)
      ),
      Vector(ResultSetRowPacket(List(Some("1.1"), Some("2.2"), None))),
      Version(0, 0, 0)
    )
    val records = List.newBuilder[(Option[BigDecimal], Option[BigDecimal], Option[BigDecimal])]
    while resultSet.next() do {
      val c1 = resultSet.getBigDecimal(1)
      val c2 = resultSet.getBigDecimal("c2")
      val c3 = resultSet.getBigDecimal(3)
      records += ((c1, c2, c3))
    }
    assertEquals(records.result(), List((Some(BigDecimal("1.1")), Some(BigDecimal("2.2")), None)))
  }

  test("ResultSet should return the correct value for getDate") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_DATE),
        column("c2", ColumnDataType.MYSQL_TYPE_DATE),
        column("c3", ColumnDataType.MYSQL_TYPE_DATE)
      ),
      Vector(ResultSetRowPacket(List(Some("2023-01-01"), Some("2023-01-02"), None))),
      Version(0, 0, 0)
    )
    val records = List.newBuilder[(Option[LocalDate], Option[LocalDate], Option[LocalDate])]
    while resultSet.next() do {
      val c1 = resultSet.getDate(1)
      val c2 = resultSet.getDate("c2")
      val c3 = resultSet.getDate(3)
      records += ((c1, c2, c3))
    }
    assertEquals(records.result(), List((Some(LocalDate.of(2023, 1, 1)), Some(LocalDate.of(2023, 1, 2)), None)))
  }

  test("ResultSet should return the correct value for getTime") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_TIME),
        column("c2", ColumnDataType.MYSQL_TYPE_TIME),
        column("c3", ColumnDataType.MYSQL_TYPE_TIME)
      ),
      Vector(ResultSetRowPacket(List(Some("12:34:56"), Some("12:34:57"), None))),
      Version(0, 0, 0)
    )
    val records = List.newBuilder[(Option[LocalTime], Option[LocalTime], Option[LocalTime])]
    while resultSet.next() do {
      val c1 = resultSet.getTime(1)
      val c2 = resultSet.getTime("c2")
      val c3 = resultSet.getTime(3)
      records += ((c1, c2, c3))
    }
    assertEquals(records.result(), List((Some(LocalTime.of(12, 34, 56)), Some(LocalTime.of(12, 34, 57)), None)))
  }

  test("ResultSet should return the correct value for getTimestamp") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP),
        column("c2", ColumnDataType.MYSQL_TYPE_TIMESTAMP),
        column("c3", ColumnDataType.MYSQL_TYPE_TIMESTAMP)
      ),
      Vector(ResultSetRowPacket(List(Some("2023-01-01 12:34:56"), Some("2023-01-02 12:34:57"), None))),
      Version(0, 0, 0)
    )
    val records = List.newBuilder[(Option[LocalDateTime], Option[LocalDateTime], Option[LocalDateTime])]
    while resultSet.next() do {
      val c1 = resultSet.getTimestamp(1)
      val c2 = resultSet.getTimestamp("c2")
      val c3 = resultSet.getTimestamp(3)
      records += ((c1, c2, c3))
    }
    assertEquals(
      records.result(),
      List((Some(LocalDateTime.of(2023, 1, 1, 12, 34, 56)), Some(LocalDateTime.of(2023, 1, 2, 12, 34, 57)), None))
    )
  }

  test("ResultSet should return the correct value for getLocalDateTime") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP),
        column("c2", ColumnDataType.MYSQL_TYPE_TIMESTAMP),
        column("c3", ColumnDataType.MYSQL_TYPE_TIMESTAMP)
      ),
      Vector(ResultSetRowPacket(List(Some("2023-01-01 12:34:56"), Some("2023-01-02 12:34:57"), None))),
      Version(0, 0, 0)
    )
    val records = List.newBuilder[(Option[LocalDateTime], Option[LocalDateTime], Option[LocalDateTime])]
    while resultSet.next() do {
      val c1 = resultSet.getTimestamp(1)
      val c2 = resultSet.getTimestamp("c2")
      val c3 = resultSet.getTimestamp(3)
      records += ((c1, c2, c3))
    }
    assertEquals(
      records.result(),
      List((Some(LocalDateTime.of(2023, 1, 1, 12, 34, 56)), Some(LocalDateTime.of(2023, 1, 2, 12, 34, 57)), None))
    )
  }

  test("Multiple records can be retrieved by looping through them with next.") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG),
        column("c2", ColumnDataType.MYSQL_TYPE_LONG),
        column("c3", ColumnDataType.MYSQL_TYPE_LONG)
      ),
      Vector(
        ResultSetRowPacket(List(Some("1"), Some("2"), None)),
        ResultSetRowPacket(List(Some("3"), Some("4"), None)),
        ResultSetRowPacket(List(Some("5"), Some("6"), None)),
        ResultSetRowPacket(List(Some("7"), Some("8"), None))
      ),
      Version(0, 0, 0)
    )
    val records = List.newBuilder[(Int, Int, Int)]
    while resultSet.next() do {
      val c1 = resultSet.getInt(1)
      val c2 = resultSet.getInt("c2")
      val c3 = resultSet.getInt(3)
      records += ((c1, c2, c3))
    }
    assertEquals(records.result(), List((1, 2, 0), (3, 4, 0), (5, 6, 0), (7, 8, 0)))
  }

  private def column(columnName: String, `type`: ColumnDataType): ColumnDefinitionPacket =
    new ColumnDefinitionPacket:
      override def table:      String                     = "table"
      override def name:       String                     = columnName
      override def columnType: ColumnDataType             = `type`
      override def flags:      Seq[ColumnDefinitionFlags] = Seq.empty
