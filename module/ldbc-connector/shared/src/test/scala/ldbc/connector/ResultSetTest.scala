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
import ldbc.connector.exception.SQLException
import ldbc.connector.net.packet.response.*

class ResultSetTest extends CatsEffectSuite:

  test("SQLException occurs when accessing the ResultSet after closing it.") {
    val resultSet = ResultSet(Vector.empty, Vector.empty, Version(0, 0, 0))
    resultSet.close()
    interceptMessage[SQLException]("Message: Operation not allowed after ResultSet closed")(resultSet.next())
    interceptMessage[SQLException]("Message: Operation not allowed after ResultSet closed")(resultSet.getLong(1))
  }

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

  test("The total number of columns obtained from the meta-information of ResultSet matches the specified value.") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG),
        column("c2", ColumnDataType.MYSQL_TYPE_LONG),
        column("c3", ColumnDataType.MYSQL_TYPE_LONG)
      ),
      Vector.empty,
      Version(0, 0, 0)
    )
    val resultSetMetaData = resultSet.getMetaData()
    assertEquals(resultSetMetaData.getColumnCount(), 3)
  }

  test("The column name obtained from the meta-information of ResultSet matches the specified value.") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG),
        column("c2", ColumnDataType.MYSQL_TYPE_LONG),
        column("c3", ColumnDataType.MYSQL_TYPE_LONG)
      ),
      Vector.empty,
      Version(0, 0, 0)
    )
    val resultSetMetaData = resultSet.getMetaData()
    assertEquals(resultSetMetaData.getColumnName(1), "c1")
    assertEquals(resultSetMetaData.getColumnName(2), "c2")
    assertEquals(resultSetMetaData.getColumnName(3), "c3")
  }

  test("The column type obtained from the meta-information of ResultSet matches the specified value.") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING)
      ),
      Vector.empty,
      Version(0, 0, 0)
    )
    val resultSetMetaData = resultSet.getMetaData()
    assertEquals(resultSetMetaData.getColumnType(1), ColumnDataType.MYSQL_TYPE_LONG.code.toInt)
    assertEquals(resultSetMetaData.getColumnType(2), ColumnDataType.MYSQL_TYPE_DOUBLE.code.toInt)
    assertEquals(resultSetMetaData.getColumnType(3), ColumnDataType.MYSQL_TYPE_STRING.code.toInt)
  }

  test("The column type name obtained from the meta-information of ResultSet matches the specified value.") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING)
      ),
      Vector.empty,
      Version(0, 0, 0)
    )
    val resultSetMetaData = resultSet.getMetaData()
    assertEquals(resultSetMetaData.getColumnTypeName(1), "INT")
    assertEquals(resultSetMetaData.getColumnTypeName(2), "DOUBLE")
    assertEquals(resultSetMetaData.getColumnTypeName(3), "CHAR")
  }

  test("The column label obtained from the meta-information of ResultSet matches the specified value.") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG, Some("label1")),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE, Some("label2")),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING, Some("label3"))
      ),
      Vector.empty,
      Version(0, 0, 0)
    )
    val resultSetMetaData = resultSet.getMetaData()
    assertEquals(resultSetMetaData.getColumnLabel(1), "label1")
    assertEquals(resultSetMetaData.getColumnLabel(2), "label2")
    assertEquals(resultSetMetaData.getColumnLabel(3), "label3")
  }

  test("The column display size obtained from the meta-information of ResultSet matches the specified value.") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING)
      ),
      Vector.empty,
      Version(0, 0, 0)
    )
    val resultSetMetaData = resultSet.getMetaData()
    assertEquals(resultSetMetaData.getColumnDisplaySize(1), 0)
    assertEquals(resultSetMetaData.getColumnDisplaySize(2), 0)
    assertEquals(resultSetMetaData.getColumnDisplaySize(3), 0)
  }

  test("The column precision obtained from the meta-information of ResultSet matches the specified value.") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING)
      ),
      Vector.empty,
      Version(0, 0, 0)
    )
    val resultSetMetaData = resultSet.getMetaData()
    assertEquals(resultSetMetaData.getPrecision(1), 0)
    assertEquals(resultSetMetaData.getPrecision(2), 0)
    assertEquals(resultSetMetaData.getPrecision(3), 0)
  }

  test("The column scale obtained from the meta-information of ResultSet matches the specified value.") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE, useScale = true),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING)
      ),
      Vector.empty,
      Version(0, 0, 0)
    )
    val resultSetMetaData = resultSet.getMetaData()
    assertEquals(resultSetMetaData.getScale(1), 0)
    assertEquals(resultSetMetaData.getScale(2), 2)
    assertEquals(resultSetMetaData.getScale(3), 0)
  }

  test("The column is signed obtained from the meta-information of ResultSet matches the specified value.") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG, isSigned = true),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING)
      ),
      Vector.empty,
      Version(0, 0, 0)
    )
    val resultSetMetaData = resultSet.getMetaData()
    assertEquals(resultSetMetaData.isSigned(1), true)
    assertEquals(resultSetMetaData.isSigned(2), false)
    assertEquals(resultSetMetaData.isSigned(3), false)
  }

  test("The column is nullable obtained from the meta-information of ResultSet matches the specified value.") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG, isNullable = false),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING)
      ),
      Vector.empty,
      Version(0, 0, 0)
    )
    val resultSetMetaData = resultSet.getMetaData()
    assertEquals(resultSetMetaData.isNullable(1), ResultSetMetaData.columnNoNulls)
    assertEquals(resultSetMetaData.isNullable(2), ResultSetMetaData.columnNullable)
    assertEquals(resultSetMetaData.isNullable(3), ResultSetMetaData.columnNullable)
  }

  test("The column is case sensitive obtained from the meta-information of ResultSet matches the specified value.") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING)
      ),
      Vector.empty,
      Version(0, 0, 0)
    )
    val resultSetMetaData = resultSet.getMetaData()
    assertEquals(resultSetMetaData.isCaseSensitive(1), false)
    assertEquals(resultSetMetaData.isCaseSensitive(2), false)
    assertEquals(resultSetMetaData.isCaseSensitive(3), false)
  }

  test("The column is searchable obtained from the meta-information of ResultSet matches the specified value.") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING)
      ),
      Vector.empty,
      Version(0, 0, 0)
    )
    val resultSetMetaData = resultSet.getMetaData()
    assertEquals(resultSetMetaData.isSearchable(1), true)
    assertEquals(resultSetMetaData.isSearchable(2), true)
    assertEquals(resultSetMetaData.isSearchable(3), true)
  }

  test("The column is writable obtained from the meta-information of ResultSet matches the specified value.") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING)
      ),
      Vector.empty,
      Version(0, 0, 0)
    )
    val resultSetMetaData = resultSet.getMetaData()
    assertEquals(resultSetMetaData.isWritable(1), true)
    assertEquals(resultSetMetaData.isWritable(2), true)
    assertEquals(resultSetMetaData.isWritable(3), true)
  }

  test(
    "The column is definitely writable obtained from the meta-information of ResultSet matches the specified value."
  ) {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING)
      ),
      Vector.empty,
      Version(0, 0, 0)
    )
    val resultSetMetaData = resultSet.getMetaData()
    assertEquals(resultSetMetaData.isDefinitelyWritable(1), true)
    assertEquals(resultSetMetaData.isDefinitelyWritable(2), true)
    assertEquals(resultSetMetaData.isDefinitelyWritable(3), true)
  }

  test("The column is read only obtained from the meta-information of ResultSet matches the specified value.") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING)
      ),
      Vector.empty,
      Version(0, 0, 0)
    )
    val resultSetMetaData = resultSet.getMetaData()
    assertEquals(resultSetMetaData.isReadOnly(1), false)
    assertEquals(resultSetMetaData.isReadOnly(2), false)
    assertEquals(resultSetMetaData.isReadOnly(3), false)
  }

  test("The column is auto increment obtained from the meta-information of ResultSet matches the specified value.") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG, isAutoInc = true),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING)
      ),
      Vector.empty,
      Version(0, 0, 0)
    )
    val resultSetMetaData = resultSet.getMetaData()
    assertEquals(resultSetMetaData.isAutoIncrement(1), true)
    assertEquals(resultSetMetaData.isAutoIncrement(2), false)
    assertEquals(resultSetMetaData.isAutoIncrement(3), false)
  }

  test("The column is currency obtained from the meta-information of ResultSet matches the specified value.") {
    val resultSet = ResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_NEWDECIMAL),
        column("c2", ColumnDataType.MYSQL_TYPE_NEWDECIMAL),
        column("c3", ColumnDataType.MYSQL_TYPE_NEWDECIMAL)
      ),
      Vector.empty,
      Version(0, 0, 0)
    )
    val resultSetMetaData = resultSet.getMetaData()
    assertEquals(resultSetMetaData.isCurrency(1), false)
    assertEquals(resultSetMetaData.isCurrency(2), false)
    assertEquals(resultSetMetaData.isCurrency(3), false)
  }

  test("The determination of whether the cursor position for a row in the ResultSet is before the start position matches the specified value.") {
    val resultSet = ResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(List(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0)
    )
    assertEquals(resultSet.isBeforeFirst(), true)
    resultSet.next()
    assertEquals(resultSet.isBeforeFirst(), false)
  }

  test("The determination of whether the cursor position in the row of the ResultSet is after the end position matches the specified value.") {
    val resultSet = ResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(List(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0)
    )
    assertEquals(resultSet.isAfterLast(), false)
    while resultSet.next() do ()
    assertEquals(resultSet.isAfterLast(), true)
  }

  test("The determination of whether the cursor position in the row of the ResultSet is at the start position matches the specified value.") {
    val resultSet = ResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(List(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0)
    )
    assertEquals(resultSet.isFirst(), false)
    resultSet.next()
    assertEquals(resultSet.isFirst(), true)
  }

  test("The determination of whether the cursor position in the row of the ResultSet is at the end position matches the specified value.") {
    val resultSet = ResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(List(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0)
    )
    assertEquals(resultSet.isLast(), false)
    resultSet.next()
    assertEquals(resultSet.isLast(), true)
  }

  test("If the cursor in the ResultSet is before the start position, the result at the cursor position matches the specified value.") {
    val resultSet = ResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(List(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE
    )
    assertEquals(resultSet.isBeforeFirst(), true)
    assertEquals(resultSet.isFirst(), false)
    resultSet.next()
    assertEquals(resultSet.isBeforeFirst(), false)
    assertEquals(resultSet.isFirst(), true)
    resultSet.beforeFirst()
    assertEquals(resultSet.isBeforeFirst(), true)
    assertEquals(resultSet.isFirst(), false)
  }

  test("When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by beforeFirst throws SQLException.") {
    val resultSet = ResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0),
    )
    interceptMessage[SQLException]("Message: Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")(resultSet.beforeFirst())
  }

  test("If the cursor in the ResultSet is after the end position, the result at the cursor position matches the specified value.") {
    val resultSet = ResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(List(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE
    )
    assertEquals(resultSet.isAfterLast(), false)
    assertEquals(resultSet.isLast(), false)
    while resultSet.next() do
      assertEquals(resultSet.isAfterLast(), false)
      assertEquals(resultSet.isLast(), true)
    assertEquals(resultSet.isAfterLast(), true)
    assertEquals(resultSet.isLast(), false)
    resultSet.afterLast()
    assertEquals(resultSet.isAfterLast(), true)
    assertEquals(resultSet.isLast(), false)
  }

  test("When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by afterLast throws SQLException.") {
    val resultSet = ResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0),
    )
    interceptMessage[SQLException]("Message: Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")(resultSet.afterLast())
  }

  test("If the cursor in the ResultSet is first position, the result at the cursor position matches the specified value.") {
    val resultSet = ResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(List(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE
    )
    assertEquals(resultSet.first(), true)
    assertEquals(resultSet.getRow(), 1)
    while resultSet.next() do
    assertEquals(resultSet.getRow(), 1)
    assertEquals(resultSet.first(), true)
    assertEquals(resultSet.getRow(), 1)
  }

  test("If the record in the ResultSet is empty, the result of executing first is false.") {
    val resultSet = ResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE
    )
    assertEquals(resultSet.first(), false)
  }

  test("When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by first throws SQLException.") {
    val resultSet = ResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0),
    )
    interceptMessage[SQLException]("Message: Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")(resultSet.first())
  }

  private def column(
    columnName: String,
    `type`:     ColumnDataType,
    alias:      Option[String] = None,
    useScale:   Boolean = false,
    isSigned:   Boolean = false,
    isNullable: Boolean = true,
    isAutoInc:  Boolean = false
  ): ColumnDefinitionPacket =
    val flags = Seq(
      if isSigned then Some(ColumnDefinitionFlags.UNSIGNED_FLAG) else None,
      if isNullable then None else Some(ColumnDefinitionFlags.NOT_NULL_FLAG),
      if isAutoInc then Some(ColumnDefinitionFlags.AUTO_INCREMENT_FLAG) else None
    ).flatten
    ColumnDefinition41Packet(
      catalog      = "def",
      schema       = "test_database",
      table        = "test_table",
      orgTable     = "test",
      name         = alias.getOrElse(columnName),
      orgName      = columnName,
      length       = 0,
      characterSet = 33,
      columnLength = 11,
      columnType   = `type`,
      flags        = flags,
      decimals     = if useScale then 2 else 0
    )
