/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.time.*

import ldbc.sql.{ ResultSet, ResultSetMetaData }

import ldbc.connector.util.Version
import ldbc.connector.data.*
import ldbc.connector.exception.SQLException
import ldbc.connector.net.packet.response.*

class ResultSetTest extends FTestPlatform:

  test("SQLException occurs when accessing the ResultSet after closing it.") {
    val resultSet = buildResultSet(Vector.empty, Vector.empty, Version(0, 0, 0))
    intercept[SQLException] {
      resultSet.close()
      resultSet.next()
    }
    intercept[SQLException] {
      resultSet.close()
      resultSet.getLong(1)
    }
  }

  test("ResultSet should return the correct value for getInt") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG),
        column("c2", ColumnDataType.MYSQL_TYPE_LONG),
        column("c3", ColumnDataType.MYSQL_TYPE_LONG)
      ),
      Vector(ResultSetRowPacket(Array(Some("1"), Some("2"), None))),
      Version(0, 0, 0)
    )
    val builder = List.newBuilder[(Int, Int, Int)]
    while resultSet.next() do
      val c1 = resultSet.getInt(1)
      val c2 = resultSet.getInt("c2")
      val c3 = resultSet.getInt(3)
      builder += ((c1, c2, c3))

    assertEquals(builder.result(), List((1, 2, 0)))
  }

  test("ResultSet should return the correct value for getLong") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONGLONG),
        column("c2", ColumnDataType.MYSQL_TYPE_LONGLONG),
        column("c3", ColumnDataType.MYSQL_TYPE_LONGLONG)
      ),
      Vector(ResultSetRowPacket(Array(Some("1"), Some("2"), None))),
      Version(0, 0, 0)
    )
    val builder = List.newBuilder[(Long, Long, Long)]
    while resultSet.next() do
      val c1 = resultSet.getLong(1)
      val c2 = resultSet.getLong("c2")
      val c3 = resultSet.getLong(3)
      builder += ((c1, c2, c3))

    assertEquals(builder.result(), List((1L, 2L, 0L)))
  }

  test("ResultSet should return the correct value for getDouble") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_DOUBLE),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
        column("c3", ColumnDataType.MYSQL_TYPE_DOUBLE)
      ),
      Vector(ResultSetRowPacket(Array(Some("1.1"), Some("2.2"), None))),
      Version(0, 0, 0)
    )
    val builder = List.newBuilder[(Double, Double, Double)]
    while resultSet.next() do
      val c1 = resultSet.getDouble(1)
      val c2 = resultSet.getDouble("c2")
      val c3 = resultSet.getDouble(3)
      builder += ((c1, c2, c3))

    assertEquals(builder.result(), List((1.1, 2.2, 0.0)))
  }

  test("ResultSet should return the correct value for getString") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_STRING),
        column("c2", ColumnDataType.MYSQL_TYPE_STRING),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING)
      ),
      Vector(ResultSetRowPacket(Array(Some("1"), Some("2"), None))),
      Version(0, 0, 0)
    )
    val builder = List.newBuilder[(Option[String], Option[String], Option[String])]
    while resultSet.next() do
      val c1 = resultSet.getString(1)
      val c2 = resultSet.getString("c2")
      val c3 = resultSet.getString(3)
      builder += ((Option(c1), Option(c2), Option(c3)))

    assertEquals(builder.result(), List((Some("1"), Some("2"), None)))
  }

  test("ResultSet should return the correct value for getBoolean") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_TINY),
        column("c2", ColumnDataType.MYSQL_TYPE_TINY),
        column("c3", ColumnDataType.MYSQL_TYPE_TINY)
      ),
      Vector(ResultSetRowPacket(Array(Some("1"), Some("0"), None))),
      Version(0, 0, 0)
    )
    val builder = List.newBuilder[(Boolean, Boolean, Boolean)]
    while resultSet.next() do
      val c1 = resultSet.getBoolean(1)
      val c2 = resultSet.getBoolean("c2")
      val c3 = resultSet.getBoolean(3)
      builder += ((c1, c2, c3))

    assertEquals(builder.result(), List((true, false, false)))
  }

  test("ResultSet should return the correct value for getByte") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_TINY),
        column("c2", ColumnDataType.MYSQL_TYPE_TINY),
        column("c3", ColumnDataType.MYSQL_TYPE_TINY)
      ),
      Vector(ResultSetRowPacket(Array(Some("1"), Some("2"), None))),
      Version(0, 0, 0)
    )
    val builder = List.newBuilder[(Byte, Byte, Byte)]
    while resultSet.next() do
      val c1 = resultSet.getByte(1)
      val c2 = resultSet.getByte("c2")
      val c3 = resultSet.getByte(3)
      builder += ((c1, c2, c3))

    assertEquals(builder.result(), List((1.toByte, 2.toByte, 0.toByte)))
  }

  test("ResultSet should return the correct value for getShort") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_SHORT),
        column("c2", ColumnDataType.MYSQL_TYPE_SHORT),
        column("c3", ColumnDataType.MYSQL_TYPE_SHORT)
      ),
      Vector(ResultSetRowPacket(Array(Some("1"), Some("2"), None))),
      Version(0, 0, 0)
    )
    val builder = List.newBuilder[(Short, Short, Short)]
    while resultSet.next() do
      val c1 = resultSet.getShort(1)
      val c2 = resultSet.getShort("c2")
      val c3 = resultSet.getShort(3)
      builder += ((c1, c2, c3))

    assertEquals(builder.result(), List((1.toShort, 2.toShort, 0.toShort)))
  }

  test("ResultSet should return the correct value for getFloat") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_FLOAT),
        column("c2", ColumnDataType.MYSQL_TYPE_FLOAT),
        column("c3", ColumnDataType.MYSQL_TYPE_FLOAT)
      ),
      Vector(ResultSetRowPacket(Array(Some("1.1"), Some("2.2"), None))),
      Version(0, 0, 0)
    )
    val builder = List.newBuilder[(Float, Float, Float)]
    while resultSet.next() do
      val c1 = resultSet.getFloat(1)
      val c2 = resultSet.getFloat("c2")
      val c3 = resultSet.getFloat(3)
      builder += ((c1, c2, c3))

    assertEquals(builder.result(), List((1.1f, 2.2f, 0.0f)))
  }

  test("ResultSet should return the correct value for getBigDecimal") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_DECIMAL),
        column("c2", ColumnDataType.MYSQL_TYPE_DECIMAL),
        column("c3", ColumnDataType.MYSQL_TYPE_DECIMAL)
      ),
      Vector(ResultSetRowPacket(Array(Some("1.1"), Some("2.2"), None))),
      Version(0, 0, 0)
    )
    val builder = List.newBuilder[(Option[BigDecimal], Option[BigDecimal], Option[BigDecimal])]
    while resultSet.next() do
      val c1 = resultSet.getBigDecimal(1)
      val c2 = resultSet.getBigDecimal("c2")
      val c3 = resultSet.getBigDecimal(3)
      builder += ((Option(c1), Option(c2), Option(c3)))

    assertEquals(builder.result(), List((Some(BigDecimal("1.1")), Some(BigDecimal("2.2")), None)))
  }

  test("ResultSet should return the correct value for getDate") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_DATE),
        column("c2", ColumnDataType.MYSQL_TYPE_DATE),
        column("c3", ColumnDataType.MYSQL_TYPE_DATE)
      ),
      Vector(ResultSetRowPacket(Array(Some("2023-01-01"), Some("2023-01-02"), None))),
      Version(0, 0, 0)
    )
    val builder = List.newBuilder[(Option[LocalDate], Option[LocalDate], Option[LocalDate])]
    while resultSet.next() do
      val c1 = resultSet.getDate(1)
      val c2 = resultSet.getDate("c2")
      val c3 = resultSet.getDate(3)
      builder += ((Option(c1), Option(c2), Option(c3)))

    assertEquals(builder.result(), List((Some(LocalDate.of(2023, 1, 1)), Some(LocalDate.of(2023, 1, 2)), None)))
  }

  test("ResultSet should return the correct value for getTime") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_TIME),
        column("c2", ColumnDataType.MYSQL_TYPE_TIME),
        column("c3", ColumnDataType.MYSQL_TYPE_TIME)
      ),
      Vector(ResultSetRowPacket(Array(Some("12:34:56"), Some("12:34:57"), None))),
      Version(0, 0, 0)
    )
    val builder = List.newBuilder[(Option[LocalTime], Option[LocalTime], Option[LocalTime])]
    while resultSet.next() do
      val c1 = resultSet.getTime(1)
      val c2 = resultSet.getTime("c2")
      val c3 = resultSet.getTime(3)
      builder += ((Option(c1), Option(c2), Option(c3)))

    assertEquals(builder.result(), List((Some(LocalTime.of(12, 34, 56)), Some(LocalTime.of(12, 34, 57)), None)))
  }

  test("ResultSet should return the correct value for getTimestamp") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP),
        column("c2", ColumnDataType.MYSQL_TYPE_TIMESTAMP),
        column("c3", ColumnDataType.MYSQL_TYPE_TIMESTAMP)
      ),
      Vector(ResultSetRowPacket(Array(Some("2023-01-01 12:34:56"), Some("2023-01-02 12:34:57"), None))),
      Version(0, 0, 0)
    )
    val builder = List.newBuilder[(Option[LocalDateTime], Option[LocalDateTime], Option[LocalDateTime])]
    while resultSet.next() do
      val c1 = resultSet.getTimestamp(1)
      val c2 = resultSet.getTimestamp("c2")
      val c3 = resultSet.getTimestamp(3)
      builder += ((Option(c1), Option(c2), Option(c3)))

    assertEquals(
      builder.result(),
      List((Some(LocalDateTime.of(2023, 1, 1, 12, 34, 56)), Some(LocalDateTime.of(2023, 1, 2, 12, 34, 57)), None))
    )
  }

  test("ResultSet should return the correct value for getLocalDateTime") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP),
        column("c2", ColumnDataType.MYSQL_TYPE_TIMESTAMP),
        column("c3", ColumnDataType.MYSQL_TYPE_TIMESTAMP)
      ),
      Vector(ResultSetRowPacket(Array(Some("2023-01-01 12:34:56"), Some("2023-01-02 12:34:57"), None))),
      Version(0, 0, 0)
    )
    val builder = List.newBuilder[(Option[LocalDateTime], Option[LocalDateTime], Option[LocalDateTime])]
    while resultSet.next() do
      val c1 = resultSet.getTimestamp(1)
      val c2 = resultSet.getTimestamp("c2")
      val c3 = resultSet.getTimestamp(3)
      builder += ((Option(c1), Option(c2), Option(c3)))

    assertEquals(
      builder.result(),
      List((Some(LocalDateTime.of(2023, 1, 1, 12, 34, 56)), Some(LocalDateTime.of(2023, 1, 2, 12, 34, 57)), None))
    )
  }

  test("Multiple records can be retrieved by looping through them with next.") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG),
        column("c2", ColumnDataType.MYSQL_TYPE_LONG),
        column("c3", ColumnDataType.MYSQL_TYPE_LONG)
      ),
      Vector(
        ResultSetRowPacket(Array(Some("1"), Some("2"), None)),
        ResultSetRowPacket(Array(Some("3"), Some("4"), None)),
        ResultSetRowPacket(Array(Some("5"), Some("6"), None)),
        ResultSetRowPacket(Array(Some("7"), Some("8"), None))
      ),
      Version(0, 0, 0)
    )
    val builder = List.newBuilder[(Int, Int, Int)]
    while resultSet.next() do
      val c1 = resultSet.getInt(1)
      val c2 = resultSet.getInt("c2")
      val c3 = resultSet.getInt(3)
      builder += ((c1, c2, c3))

    assertEquals(builder.result(), List((1, 2, 0), (3, 4, 0), (5, 6, 0), (7, 8, 0)))
  }

  test(
    "If the table name is replaced by an alias, the record can be retrieved by specifying the table name and column name together."
  ) {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_TINY, Some("t")),
        column("c2", ColumnDataType.MYSQL_TYPE_TINY, Some("t")),
        column("c3", ColumnDataType.MYSQL_TYPE_TINY, Some("t"))
      ),
      Vector(ResultSetRowPacket(Array(Some("1"), Some("0"), None))),
      Version(0, 0, 0)
    )
    val builder = List.newBuilder[(Boolean, Boolean, Boolean)]
    while resultSet.next() do
      val c1 = resultSet.getBoolean("t.c1")
      val c2 = resultSet.getBoolean("t.c2")
      val c3 = resultSet.getBoolean("t.c3")
      builder += ((c1, c2, c3))

    assertEquals(builder.result(), List((true, false, false)))
  }

  test("The total number of columns obtained from the meta-information of ResultSet matches the specified value.") {
    val resultSet = buildResultSet(
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
    val resultSet = buildResultSet(
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
    val resultSet = buildResultSet(
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
    val resultSet = buildResultSet(
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
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG, alias   = Some("label1")),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE, alias = Some("label2")),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING, alias = Some("label3"))
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
    val resultSet = buildResultSet(
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
    val resultSet = buildResultSet(
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
    val resultSet = buildResultSet(
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
    val resultSet = buildResultSet(
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
    val resultSet = buildResultSet(
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
    val resultSet = buildResultSet(
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
    val resultSet = buildResultSet(
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
    val resultSet = buildResultSet(
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
    val resultSet = buildResultSet(
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
    val resultSet = buildResultSet(
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
    val resultSet = buildResultSet(
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
    val resultSet = buildResultSet(
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

  test(
    "The determination of whether the cursor position for a row in the ResultSet is before the start position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(Array(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0)
    )
    assert(resultSet.isBeforeFirst())
    assertEquals(
      {
        resultSet.next()
        resultSet.isBeforeFirst()
      },
      false
    )
  }

  test(
    "The determination of whether the cursor position in the row of the ResultSet is after the end position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(Array(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0)
    )
    assertEquals(resultSet.isAfterLast(), false)
    assert({
      while resultSet.next() do ()
      resultSet.isAfterLast()
    })
  }

  test(
    "The determination of whether the cursor position in the row of the ResultSet is at the start position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(Array(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0)
    )
    assertEquals(resultSet.isFirst(), false)
    assert({
      resultSet.next()
      resultSet.isFirst()
    })
  }

  test(
    "The determination of whether the cursor position in the row of the ResultSet is at the end position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(Array(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0)
    )
    assertEquals(resultSet.isLast(), false)
    assert({
      resultSet.next()
      resultSet.isLast()
    })
  }

  test(
    "If the cursor in the ResultSet is before the start position, the result at the cursor position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(Array(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )
    assert(resultSet.isBeforeFirst())
    assertEquals(resultSet.isFirst(), false)
    assertEquals(
      {
        resultSet.next()
        resultSet.isBeforeFirst()
      },
      false
    )
    assert(resultSet.isFirst())
    assert({
      resultSet.beforeFirst()
      resultSet.isBeforeFirst()
    })
    assertEquals(resultSet.isFirst(), false)
  }

  test(
    "When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by beforeFirst throws SQLException."
  ) {
    val resultSet = buildResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0)
    )
    intercept[SQLException](resultSet.beforeFirst())
  }

  test(
    "If the cursor in the ResultSet is after the end position, the result at the cursor position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(Array(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )
    assertEquals(resultSet.isAfterLast(), false)
    assertEquals(resultSet.isLast(), false)
    assert(
      {
        while resultSet.next() do ()
        resultSet.isAfterLast()
      }
    )
    assert({
      resultSet.afterLast()
      resultSet.isAfterLast()
    })
    assert({
      resultSet.last()
      resultSet.isLast()
    })
  }

  test(
    "When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by afterLast throws SQLException."
  ) {
    val resultSet = buildResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0)
    )
    intercept[SQLException](resultSet.afterLast())
  }

  test(
    "If the cursor in the ResultSet is first position, the result at the cursor position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(Array(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )
    assert(resultSet.first())
    assertEquals(resultSet.getRow(), 1)
    assertEquals(
      {
        while resultSet.next() do ()
        resultSet.getRow()
      },
      0
    )
    assertEquals(
      {
        resultSet.first()
        resultSet.getRow()
      },
      1
    )
  }

  test("If the record in the ResultSet is empty, the result of executing first is false.") {
    val resultSet = buildResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )
    assertEquals(resultSet.first(), false)
  }

  test("When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by first throws SQLException.") {
    val resultSet = buildResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0)
    )
    intercept[SQLException](resultSet.first())
  }

  test(
    "If the cursor in the ResultSet is last position, the result at the cursor position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(
        ResultSetRowPacket(Array(Some("2023-01-01 12:34:56"))),
        ResultSetRowPacket(Array(Some("2023-01-01 12:34:56")))
      ),
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )
    assert(resultSet.last())
    assertEquals(
      {
        resultSet.last()
        resultSet.getRow()
      },
      2
    )
    assertEquals(
      {
        while resultSet.next() do ()
        resultSet.last()
        resultSet.getRow()
      },
      2
    )
  }

  test("If the record in the ResultSet is empty, the result of executing last is false.") {
    val resultSet = buildResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )
    assertEquals(resultSet.last(), false)
  }

  test("When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by last throws SQLException.") {
    val resultSet = buildResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0)
    )
    intercept[SQLException](resultSet.last())
  }

  test(
    "If the cursor in the ResultSet is absolute position, the result at the cursor position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(Array(Some("1"))), ResultSetRowPacket(Array(Some("2")))),
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )
    assertEquals(resultSet.getRow(), 0)
    assertEquals(resultSet.absolute(1), true)
    assertEquals(
      {
        resultSet.absolute(1)
        resultSet.getRow()
      },
      1
    )
    assertEquals(
      {
        resultSet.absolute(1)
        resultSet.getInt(1)
      },
      1
    )
    assertEquals(resultSet.absolute(0), false)
    assertEquals(
      {
        resultSet.absolute(0)
        resultSet.getRow()
      },
      0
    )
    assertEquals(resultSet.absolute(3), false)
    assertEquals(
      {
        resultSet.absolute(3)
        resultSet.getRow()
      },
      0
    )
    assertEquals(resultSet.absolute(-1), true)
    assertEquals(
      {
        resultSet.absolute(-1)
        resultSet.getRow()
      },
      2
    )
    assertEquals(
      {
        resultSet.absolute(-1)
        resultSet.getInt(1)
      },
      2
    )
  }

  test(
    "When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by absolute throws SQLException."
  ) {
    val resultSet = buildResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0)
    )
    intercept[SQLException](resultSet.absolute(0))
  }

  test(
    "If the cursor in the ResultSet is relative position, the result at the cursor position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(Array(Some("1"))), ResultSetRowPacket(Array(Some("2")))),
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )
    assertEquals(resultSet.getRow(), 0)
    assertEquals(resultSet.relative(0), false)
    assertEquals(resultSet.getRow(), 0)
    assertEquals(resultSet.getInt(1), 1)
    assertEquals(resultSet.relative(1), true)
    assertEquals(resultSet.getRow(), 1)
    assertEquals(resultSet.getInt(1), 1)
    assertEquals(resultSet.relative(2), false)
    assertEquals(resultSet.getRow(), 0)
    assertEquals(resultSet.relative(-1), false)
    assertEquals(resultSet.getRow(), 0)
    assertEquals(resultSet.getInt(1), 1)
  }

  test(
    "When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by relative throws SQLException."
  ) {
    val resultSet = buildResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0)
    )
    intercept[SQLException](resultSet.relative(0))
  }

  test(
    "If the cursor in the ResultSet is previous position, the result at the cursor position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(Array(Some("1"))), ResultSetRowPacket(Array(Some("2")))),
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )
    assertEquals(resultSet.getRow(), 0)
    assertEquals(resultSet.absolute(2), true)
    assertEquals(
      {
        resultSet.absolute(2)
        resultSet.getRow()
      },
      2
    )
    assertEquals(
      {
        resultSet.absolute(2)
        resultSet.getInt(1)
      },
      2
    )
    assertEquals(resultSet.previous(), true)
    assertEquals(
      {
        resultSet.absolute(2)
        resultSet.previous()
        resultSet.getRow()
      },
      1
    )
    assertEquals(
      {
        resultSet.absolute(2)
        resultSet.previous()
        resultSet.getInt(1)
      },
      1
    )
  }

  test(
    "When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by previous throws SQLException."
  ) {
    val resultSet = buildResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0)
    )
    intercept[SQLException](resultSet.previous())
  }

  private def buildResultSet(
    columns:              Vector[ColumnDefinitionPacket],
    records:              Vector[ResultSetRowPacket],
    version:              Version,
    resultSetType:        Int = ResultSet.TYPE_FORWARD_ONLY,
    resultSetConcurrency: Int = ResultSet.CONCUR_READ_ONLY
  ): ResultSetImpl =
    ResultSetImpl(
      columns,
      records,
      Map.empty,
      version,
      resultSetType,
      resultSetConcurrency
    )

  private def column(
    columnName: String,
    `type`:     ColumnDataType,
    table:      Option[String] = None,
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
      table        = table.getOrElse("test"),
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
