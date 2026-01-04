/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.time.*

import cats.effect.{ IO, Ref }

import ldbc.sql.{ ResultSet, ResultSetMetaData }

import ldbc.connector.data.*
import ldbc.connector.exception.SQLException
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.Protocol
import ldbc.connector.util.Version

class ResultSetTest extends FTestPlatform:

  test("SQLException occurs when accessing the ResultSet after closing it.") {
    for
      resultSet <- buildResultSet(Vector.empty, Vector.empty, Version(0, 0, 0))
      _         <- resultSet.close()
      _         <- interceptIO[SQLException](resultSet.next())
      _         <- resultSet.close()
      _         <- interceptIO[SQLException](resultSet.getLong(1))
    yield ()
  }

  test("ResultSet should return the correct value for getInt") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_LONG),
                       column("c2", ColumnDataType.MYSQL_TYPE_LONG),
                       column("c3", ColumnDataType.MYSQL_TYPE_LONG)
                     ),
                     Vector(ResultSetRowPacket(Array(Some("1"), Some("2"), None))),
                     Version(0, 0, 0)
                   )
      hasNext <- resultSet.next()
      c1      <- if hasNext then resultSet.getInt(1) else IO.pure(0)
      c2      <- if hasNext then resultSet.getInt("c2") else IO.pure(0)
      c3      <- if hasNext then resultSet.getInt(3) else IO.pure(0)
      result = if hasNext then List((c1, c2, c3)) else List.empty
    yield assertEquals(result, List((1, 2, 0)))
  }

  test("ResultSet should return the correct value for getLong") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_LONGLONG),
                       column("c2", ColumnDataType.MYSQL_TYPE_LONGLONG),
                       column("c3", ColumnDataType.MYSQL_TYPE_LONGLONG)
                     ),
                     Vector(ResultSetRowPacket(Array(Some("1"), Some("2"), None))),
                     Version(0, 0, 0)
                   )
      result <- collectRows(resultSet) { rs =>
                  for
                    c1 <- rs.getLong(1)
                    c2 <- rs.getLong("c2")
                    c3 <- rs.getLong(3)
                  yield (c1, c2, c3)
                }
    yield assertEquals(result, List((1L, 2L, 0L)))
  }

  test("ResultSet should return the correct value for getDouble") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_DOUBLE),
                       column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
                       column("c3", ColumnDataType.MYSQL_TYPE_DOUBLE)
                     ),
                     Vector(ResultSetRowPacket(Array(Some("1.1"), Some("2.2"), None))),
                     Version(0, 0, 0)
                   )
      result <- collectRows(resultSet) { rs =>
                  for
                    c1 <- rs.getDouble(1)
                    c2 <- rs.getDouble("c2")
                    c3 <- rs.getDouble(3)
                  yield (c1, c2, c3)
                }
    yield assertEquals(result, List((1.1, 2.2, 0.0)))
  }

  test("ResultSet should return the correct value for getString") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_STRING),
                       column("c2", ColumnDataType.MYSQL_TYPE_STRING),
                       column("c3", ColumnDataType.MYSQL_TYPE_STRING)
                     ),
                     Vector(ResultSetRowPacket(Array(Some("1"), Some("2"), None))),
                     Version(0, 0, 0)
                   )
      result <- collectRows(resultSet) { rs =>
                  for
                    c1 <- rs.getString(1)
                    c2 <- rs.getString("c2")
                    c3 <- rs.getString(3)
                  yield (Option(c1), Option(c2), Option(c3))
                }
    yield assertEquals(result, List((Some("1"), Some("2"), None)))
  }

  test("ResultSet should return the correct value for getBoolean") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_TINY),
                       column("c2", ColumnDataType.MYSQL_TYPE_TINY),
                       column("c3", ColumnDataType.MYSQL_TYPE_TINY)
                     ),
                     Vector(ResultSetRowPacket(Array(Some("1"), Some("0"), None))),
                     Version(0, 0, 0)
                   )
      result <- collectRows(resultSet) { rs =>
                  for
                    c1 <- rs.getBoolean(1)
                    c2 <- rs.getBoolean("c2")
                    c3 <- rs.getBoolean(3)
                  yield (c1, c2, c3)
                }
    yield assertEquals(result, List((true, false, false)))
  }

  test("ResultSet should return the correct value for getByte") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_TINY),
                       column("c2", ColumnDataType.MYSQL_TYPE_TINY),
                       column("c3", ColumnDataType.MYSQL_TYPE_TINY)
                     ),
                     Vector(ResultSetRowPacket(Array(Some("1"), Some("2"), None))),
                     Version(0, 0, 0)
                   )
      result <- collectRows(resultSet) { rs =>
                  for
                    c1 <- rs.getByte(1)
                    c2 <- rs.getByte("c2")
                    c3 <- rs.getByte(3)
                  yield (c1, c2, c3)
                }
    yield assertEquals(result, List((1.toByte, 2.toByte, 0.toByte)))
  }

  test("ResultSet should return the correct value for getShort") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_SHORT),
                       column("c2", ColumnDataType.MYSQL_TYPE_SHORT),
                       column("c3", ColumnDataType.MYSQL_TYPE_SHORT)
                     ),
                     Vector(ResultSetRowPacket(Array(Some("1"), Some("2"), None))),
                     Version(0, 0, 0)
                   )
      result <- collectRows(resultSet) { rs =>
                  for
                    c1 <- rs.getShort(1)
                    c2 <- rs.getShort("c2")
                    c3 <- rs.getShort(3)
                  yield (c1, c2, c3)
                }
    yield assertEquals(result, List((1.toShort, 2.toShort, 0.toShort)))
  }

  test("ResultSet should return the correct value for getFloat") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_FLOAT),
                       column("c2", ColumnDataType.MYSQL_TYPE_FLOAT),
                       column("c3", ColumnDataType.MYSQL_TYPE_FLOAT)
                     ),
                     Vector(ResultSetRowPacket(Array(Some("1.1"), Some("2.2"), None))),
                     Version(0, 0, 0)
                   )
      result <- collectRows(resultSet) { rs =>
                  for
                    c1 <- rs.getFloat(1)
                    c2 <- rs.getFloat("c2")
                    c3 <- rs.getFloat(3)
                  yield (c1, c2, c3)
                }
    yield assertEquals(result, List((1.1f, 2.2f, 0.0f)))
  }

  test("ResultSet should return the correct value for getBigDecimal") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_DECIMAL),
                       column("c2", ColumnDataType.MYSQL_TYPE_DECIMAL),
                       column("c3", ColumnDataType.MYSQL_TYPE_DECIMAL)
                     ),
                     Vector(ResultSetRowPacket(Array(Some("1.1"), Some("2.2"), None))),
                     Version(0, 0, 0)
                   )
      result <- collectRows(resultSet) { rs =>
                  for
                    c1 <- rs.getBigDecimal(1)
                    c2 <- rs.getBigDecimal("c2")
                    c3 <- rs.getBigDecimal(3)
                  yield (Option(c1), Option(c2), Option(c3))
                }
    yield assertEquals(result, List((Some(BigDecimal("1.1")), Some(BigDecimal("2.2")), None)))
  }

  test("ResultSet should return the correct value for getDate") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_DATE),
                       column("c2", ColumnDataType.MYSQL_TYPE_DATE),
                       column("c3", ColumnDataType.MYSQL_TYPE_DATE)
                     ),
                     Vector(ResultSetRowPacket(Array(Some("2023-01-01"), Some("2023-01-02"), None))),
                     Version(0, 0, 0)
                   )
      result <- collectRows(resultSet) { rs =>
                  for
                    c1 <- rs.getDate(1)
                    c2 <- rs.getDate("c2")
                    c3 <- rs.getDate(3)
                  yield (Option(c1), Option(c2), Option(c3))
                }
    yield assertEquals(result, List((Some(LocalDate.of(2023, 1, 1)), Some(LocalDate.of(2023, 1, 2)), None)))
  }

  test("ResultSet should return the correct value for getTime") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_TIME),
                       column("c2", ColumnDataType.MYSQL_TYPE_TIME),
                       column("c3", ColumnDataType.MYSQL_TYPE_TIME)
                     ),
                     Vector(ResultSetRowPacket(Array(Some("12:34:56"), Some("12:34:57"), None))),
                     Version(0, 0, 0)
                   )
      result <- collectRows(resultSet) { rs =>
                  for
                    c1 <- rs.getTime(1)
                    c2 <- rs.getTime("c2")
                    c3 <- rs.getTime(3)
                  yield (Option(c1), Option(c2), Option(c3))
                }
    yield assertEquals(result, List((Some(LocalTime.of(12, 34, 56)), Some(LocalTime.of(12, 34, 57)), None)))
  }

  test("ResultSet should return the correct value for getTimestamp") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP),
                       column("c2", ColumnDataType.MYSQL_TYPE_TIMESTAMP),
                       column("c3", ColumnDataType.MYSQL_TYPE_TIMESTAMP)
                     ),
                     Vector(ResultSetRowPacket(Array(Some("2023-01-01 12:34:56"), Some("2023-01-02 12:34:57"), None))),
                     Version(0, 0, 0)
                   )
      result <- collectRows(resultSet) { rs =>
                  for
                    c1 <- rs.getTimestamp(1)
                    c2 <- rs.getTimestamp("c2")
                    c3 <- rs.getTimestamp(3)
                  yield (Option(c1), Option(c2), Option(c3))
                }
    yield assertEquals(
      result,
      List((Some(LocalDateTime.of(2023, 1, 1, 12, 34, 56)), Some(LocalDateTime.of(2023, 1, 2, 12, 34, 57)), None))
    )
  }

  test("ResultSet should return the correct value for getLocalDateTime") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP),
                       column("c2", ColumnDataType.MYSQL_TYPE_TIMESTAMP),
                       column("c3", ColumnDataType.MYSQL_TYPE_TIMESTAMP)
                     ),
                     Vector(ResultSetRowPacket(Array(Some("2023-01-01 12:34:56"), Some("2023-01-02 12:34:57"), None))),
                     Version(0, 0, 0)
                   )
      result <- collectRows(resultSet) { rs =>
                  for
                    c1 <- rs.getTimestamp(1)
                    c2 <- rs.getTimestamp("c2")
                    c3 <- rs.getTimestamp(3)
                  yield (Option(c1), Option(c2), Option(c3))
                }
    yield assertEquals(
      result,
      List((Some(LocalDateTime.of(2023, 1, 1, 12, 34, 56)), Some(LocalDateTime.of(2023, 1, 2, 12, 34, 57)), None))
    )
  }

  test("Multiple records can be retrieved by looping through them with next.") {
    for
      resultSet <- buildResultSet(
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
      result <- collectRows(resultSet) { rs =>
                  for
                    c1 <- rs.getInt(1)
                    c2 <- rs.getInt("c2")
                    c3 <- rs.getInt(3)
                  yield (c1, c2, c3)
                }
    yield assertEquals(result, List((1, 2, 0), (3, 4, 0), (5, 6, 0), (7, 8, 0)))
  }

  test(
    "If the table name is replaced by an alias, the record can be retrieved by specifying the table name and column name together."
  ) {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_TINY, Some("t")),
                       column("c2", ColumnDataType.MYSQL_TYPE_TINY, Some("t")),
                       column("c3", ColumnDataType.MYSQL_TYPE_TINY, Some("t"))
                     ),
                     Vector(ResultSetRowPacket(Array(Some("1"), Some("0"), None))),
                     Version(0, 0, 0)
                   )
      result <- collectRows(resultSet) { rs =>
                  for
                    c1 <- rs.getBoolean("t.c1")
                    c2 <- rs.getBoolean("t.c2")
                    c3 <- rs.getBoolean("t.c3")
                  yield (c1, c2, c3)
                }
    yield assertEquals(result, List((true, false, false)))
  }

  test("The total number of columns obtained from the meta-information of ResultSet matches the specified value.") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_LONG),
                       column("c2", ColumnDataType.MYSQL_TYPE_LONG),
                       column("c3", ColumnDataType.MYSQL_TYPE_LONG)
                     ),
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      resultSetMetaData <- resultSet.getMetaData()
    yield assertEquals(resultSetMetaData.getColumnCount(), 3)
  }

  test("The column name obtained from the meta-information of ResultSet matches the specified value.") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_LONG),
                       column("c2", ColumnDataType.MYSQL_TYPE_LONG),
                       column("c3", ColumnDataType.MYSQL_TYPE_LONG)
                     ),
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      resultSetMetaData <- resultSet.getMetaData()
    yield
      assertEquals(resultSetMetaData.getColumnName(1), "c1")
      assertEquals(resultSetMetaData.getColumnName(2), "c2")
      assertEquals(resultSetMetaData.getColumnName(3), "c3")
  }

  test("The column type obtained from the meta-information of ResultSet matches the specified value.") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_LONG),
                       column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
                       column("c3", ColumnDataType.MYSQL_TYPE_STRING)
                     ),
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      resultSetMetaData <- resultSet.getMetaData()
    yield
      assertEquals(resultSetMetaData.getColumnType(1), ColumnDataType.MYSQL_TYPE_LONG.code.toInt)
      assertEquals(resultSetMetaData.getColumnType(2), ColumnDataType.MYSQL_TYPE_DOUBLE.code.toInt)
      assertEquals(resultSetMetaData.getColumnType(3), ColumnDataType.MYSQL_TYPE_STRING.code.toInt)
  }

  test("The column type name obtained from the meta-information of ResultSet matches the specified value.") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_LONG),
                       column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
                       column("c3", ColumnDataType.MYSQL_TYPE_STRING)
                     ),
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      resultSetMetaData <- resultSet.getMetaData()
    yield
      assertEquals(resultSetMetaData.getColumnTypeName(1), "INT")
      assertEquals(resultSetMetaData.getColumnTypeName(2), "DOUBLE")
      assertEquals(resultSetMetaData.getColumnTypeName(3), "CHAR")
  }

  test("The column label obtained from the meta-information of ResultSet matches the specified value.") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_LONG, alias   = Some("label1")),
                       column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE, alias = Some("label2")),
                       column("c3", ColumnDataType.MYSQL_TYPE_STRING, alias = Some("label3"))
                     ),
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      resultSetMetaData <- resultSet.getMetaData()
    yield
      assertEquals(resultSetMetaData.getColumnLabel(1), "label1")
      assertEquals(resultSetMetaData.getColumnLabel(2), "label2")
      assertEquals(resultSetMetaData.getColumnLabel(3), "label3")
  }

  test("The column display size obtained from the meta-information of ResultSet matches the specified value.") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_LONG),
                       column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
                       column("c3", ColumnDataType.MYSQL_TYPE_STRING)
                     ),
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      resultSetMetaData <- resultSet.getMetaData()
    yield
      assertEquals(resultSetMetaData.getColumnDisplaySize(1), 0)
      assertEquals(resultSetMetaData.getColumnDisplaySize(2), 0)
      assertEquals(resultSetMetaData.getColumnDisplaySize(3), 0)
  }

  test("The column precision obtained from the meta-information of ResultSet matches the specified value.") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_LONG),
                       column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
                       column("c3", ColumnDataType.MYSQL_TYPE_STRING)
                     ),
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      resultSetMetaData <- resultSet.getMetaData()
    yield
      assertEquals(resultSetMetaData.getPrecision(1), 0)
      assertEquals(resultSetMetaData.getPrecision(2), 0)
      assertEquals(resultSetMetaData.getPrecision(3), 0)
  }

  test("The column scale obtained from the meta-information of ResultSet matches the specified value.") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_LONG),
                       column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE, useScale = true),
                       column("c3", ColumnDataType.MYSQL_TYPE_STRING)
                     ),
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      resultSetMetaData <- resultSet.getMetaData()
    yield
      assertEquals(resultSetMetaData.getScale(1), 0)
      assertEquals(resultSetMetaData.getScale(2), 2)
      assertEquals(resultSetMetaData.getScale(3), 0)
  }

  test("The column is signed obtained from the meta-information of ResultSet matches the specified value.") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_LONG, isSigned = true),
                       column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
                       column("c3", ColumnDataType.MYSQL_TYPE_STRING)
                     ),
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      resultSetMetaData <- resultSet.getMetaData()
    yield
      assertEquals(resultSetMetaData.isSigned(1), true)
      assertEquals(resultSetMetaData.isSigned(2), false)
      assertEquals(resultSetMetaData.isSigned(3), false)
  }

  test("The column is nullable obtained from the meta-information of ResultSet matches the specified value.") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_LONG, isNullable = false),
                       column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
                       column("c3", ColumnDataType.MYSQL_TYPE_STRING)
                     ),
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      resultSetMetaData <- resultSet.getMetaData()
    yield
      assertEquals(resultSetMetaData.isNullable(1), ResultSetMetaData.columnNoNulls)
      assertEquals(resultSetMetaData.isNullable(2), ResultSetMetaData.columnNullable)
      assertEquals(resultSetMetaData.isNullable(3), ResultSetMetaData.columnNullable)
  }

  test("The column is case sensitive obtained from the meta-information of ResultSet matches the specified value.") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_LONG),
                       column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
                       column("c3", ColumnDataType.MYSQL_TYPE_STRING)
                     ),
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      resultSetMetaData <- resultSet.getMetaData()
    yield
      assertEquals(resultSetMetaData.isCaseSensitive(1), false)
      assertEquals(resultSetMetaData.isCaseSensitive(2), false)
      assertEquals(resultSetMetaData.isCaseSensitive(3), false)
  }

  test("The column is searchable obtained from the meta-information of ResultSet matches the specified value.") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_LONG),
                       column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
                       column("c3", ColumnDataType.MYSQL_TYPE_STRING)
                     ),
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      resultSetMetaData <- resultSet.getMetaData()
    yield
      assertEquals(resultSetMetaData.isSearchable(1), true)
      assertEquals(resultSetMetaData.isSearchable(2), true)
      assertEquals(resultSetMetaData.isSearchable(3), true)
  }

  test("The column is writable obtained from the meta-information of ResultSet matches the specified value.") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_LONG),
                       column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
                       column("c3", ColumnDataType.MYSQL_TYPE_STRING)
                     ),
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      resultSetMetaData <- resultSet.getMetaData()
    yield
      assertEquals(resultSetMetaData.isWritable(1), true)
      assertEquals(resultSetMetaData.isWritable(2), true)
      assertEquals(resultSetMetaData.isWritable(3), true)
  }

  test(
    "The column is definitely writable obtained from the meta-information of ResultSet matches the specified value."
  ) {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_LONG),
                       column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
                       column("c3", ColumnDataType.MYSQL_TYPE_STRING)
                     ),
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      resultSetMetaData <- resultSet.getMetaData()
    yield
      assertEquals(resultSetMetaData.isDefinitelyWritable(1), true)
      assertEquals(resultSetMetaData.isDefinitelyWritable(2), true)
      assertEquals(resultSetMetaData.isDefinitelyWritable(3), true)
  }

  test("The column is read only obtained from the meta-information of ResultSet matches the specified value.") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_LONG),
                       column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
                       column("c3", ColumnDataType.MYSQL_TYPE_STRING)
                     ),
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      resultSetMetaData <- resultSet.getMetaData()
    yield
      assertEquals(resultSetMetaData.isReadOnly(1), false)
      assertEquals(resultSetMetaData.isReadOnly(2), false)
      assertEquals(resultSetMetaData.isReadOnly(3), false)
  }

  test("The column is auto increment obtained from the meta-information of ResultSet matches the specified value.") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_LONG, isAutoInc = true),
                       column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
                       column("c3", ColumnDataType.MYSQL_TYPE_STRING)
                     ),
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      resultSetMetaData <- resultSet.getMetaData()
    yield
      assertEquals(resultSetMetaData.isAutoIncrement(1), true)
      assertEquals(resultSetMetaData.isAutoIncrement(2), false)
      assertEquals(resultSetMetaData.isAutoIncrement(3), false)
  }

  test("The column is currency obtained from the meta-information of ResultSet matches the specified value.") {
    for
      resultSet <- buildResultSet(
                     Vector(
                       column("c1", ColumnDataType.MYSQL_TYPE_NEWDECIMAL),
                       column("c2", ColumnDataType.MYSQL_TYPE_NEWDECIMAL),
                       column("c3", ColumnDataType.MYSQL_TYPE_NEWDECIMAL)
                     ),
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      resultSetMetaData <- resultSet.getMetaData()
    yield
      assertEquals(resultSetMetaData.isCurrency(1), false)
      assertEquals(resultSetMetaData.isCurrency(2), false)
      assertEquals(resultSetMetaData.isCurrency(3), false)
  }

  test(
    "The determination of whether the cursor position for a row in the ResultSet is before the start position matches the specified value."
  ) {
    for
      resultSet <- buildResultSet(
                     Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
                     Vector(ResultSetRowPacket(Array(Some("2023-01-01 12:34:56")))),
                     Version(0, 0, 0)
                   )
      _             <- resultSet.isBeforeFirst().map(assert(_))
      _             <- resultSet.next()
      isBeforeFirst <- resultSet.isBeforeFirst()
    yield assertEquals(isBeforeFirst, false)
  }

  test(
    "The determination of whether the cursor position in the row of the ResultSet is after the end position matches the specified value."
  ) {
    for
      resultSet <- buildResultSet(
                     Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
                     Vector(ResultSetRowPacket(Array(Some("2023-01-01 12:34:56")))),
                     Version(0, 0, 0)
                   )
      initialAfterLast <- resultSet.isAfterLast()
      _ = assertEquals(initialAfterLast, false)
      _              <- collectRows(resultSet)(_ => IO.unit)
      finalAfterLast <- resultSet.isAfterLast()
      _ = assert(finalAfterLast)
    yield ()
  }

  test(
    "The determination of whether the cursor position in the row of the ResultSet is at the start position matches the specified value."
  ) {
    for
      resultSet <- buildResultSet(
                     Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
                     Vector(ResultSetRowPacket(Array(Some("2023-01-01 12:34:56")))),
                     Version(0, 0, 0)
                   )
      initialIsFirst <- resultSet.isFirst()
      _ = assertEquals(initialIsFirst, false)
      _            <- resultSet.next()
      finalIsFirst <- resultSet.isFirst()
      _ = assert(finalIsFirst)
    yield ()
  }

  test(
    "The determination of whether the cursor position in the row of the ResultSet is at the end position matches the specified value."
  ) {
    for
      resultSet <- buildResultSet(
                     Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
                     Vector(ResultSetRowPacket(Array(Some("2023-01-01 12:34:56")))),
                     Version(0, 0, 0)
                   )
      initialIsLast <- resultSet.isLast()
      _ = assertEquals(initialIsLast, false)
      _           <- resultSet.next()
      finalIsLast <- resultSet.isLast()
      _ = assert(finalIsLast)
    yield ()
  }

  test(
    "If the cursor in the ResultSet is before the start position, the result at the cursor position matches the specified value."
  ) {
    for
      resultSet <- buildResultSet(
                     Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
                     Vector(ResultSetRowPacket(Array(Some("2023-01-01 12:34:56")))),
                     Version(0, 0, 0),
                     ResultSet.TYPE_SCROLL_INSENSITIVE,
                     ResultSet.CONCUR_READ_ONLY
                   )
      _              <- resultSet.isBeforeFirst().map(assert(_))
      isFirstInitial <- resultSet.isFirst()
      _ = assertEquals(isFirstInitial, false)
      _                      <- resultSet.next()
      isBeforeFirstAfterNext <- resultSet.isBeforeFirst()
      _ = assertEquals(isBeforeFirstAfterNext, false)
      isFirstAfterNext <- resultSet.isFirst()
      _ = assert(isFirstAfterNext)
      _                       <- resultSet.beforeFirst()
      isBeforeFirstAfterReset <- resultSet.isBeforeFirst()
      _ = assert(isBeforeFirstAfterReset)
      isFirstAfterReset <- resultSet.isFirst()
      _ = assertEquals(isFirstAfterReset, false)
    yield ()
  }

  test(
    "When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by beforeFirst throws SQLException."
  ) {
    for
      resultSet <- buildResultSet(
                     Vector.empty,
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      _ <- interceptIO[SQLException](resultSet.beforeFirst())
    yield ()
  }

  test(
    "If the cursor in the ResultSet is after the end position, the result at the cursor position matches the specified value."
  ) {
    for
      resultSet <- buildResultSet(
                     Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
                     Vector(ResultSetRowPacket(Array(Some("2023-01-01 12:34:56")))),
                     Version(0, 0, 0),
                     ResultSet.TYPE_SCROLL_INSENSITIVE,
                     ResultSet.CONCUR_READ_ONLY
                   )
      initialAfterLast <- resultSet.isAfterLast()
      _ = assertEquals(initialAfterLast, false)
      initialIsLast <- resultSet.isLast()
      _ = assertEquals(initialIsLast, false)
      _                    <- collectRows(resultSet)(_ => IO.unit)
      isAfterLastAfterNext <- resultSet.isAfterLast()
      _ = assert(isAfterLastAfterNext)
      _                    <- resultSet.afterLast()
      isAfterLastAfterCall <- resultSet.isAfterLast()
      _ = assert(isAfterLastAfterCall)
      lastResult <- resultSet.last()
      _ = assert(lastResult)
      isLastAfterLast <- resultSet.isLast()
      _ = assert(isLastAfterLast)
    yield ()
  }

  test(
    "When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by afterLast throws SQLException."
  ) {
    for
      resultSet <- buildResultSet(
                     Vector.empty,
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      _ <- interceptIO[SQLException](resultSet.afterLast())
    yield ()
  }

  test(
    "If the cursor in the ResultSet is first position, the result at the cursor position matches the specified value."
  ) {
    for
      resultSet <- buildResultSet(
                     Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
                     Vector(ResultSetRowPacket(Array(Some("2023-01-01 12:34:56")))),
                     Version(0, 0, 0),
                     ResultSet.TYPE_SCROLL_INSENSITIVE,
                     ResultSet.CONCUR_READ_ONLY
                   )
      firstResult <- resultSet.first()
      _ = assert(firstResult)
      row1 <- resultSet.getRow()
      _ = assertEquals(row1, 1)
      _    <- collectRows(resultSet)(_ => IO.unit)
      row2 <- resultSet.getRow()
      _ = assertEquals(row2, 0)
      _    <- resultSet.first()
      row3 <- resultSet.getRow()
      _ = assertEquals(row3, 1)
    yield ()
  }

  test("If the record in the ResultSet is empty, the result of executing first is false.") {
    for
      resultSet <- buildResultSet(
                     Vector.empty,
                     Vector.empty,
                     Version(0, 0, 0),
                     ResultSet.TYPE_SCROLL_INSENSITIVE,
                     ResultSet.CONCUR_READ_ONLY
                   )
      firstResult <- resultSet.first()
    yield assertEquals(firstResult, false)
  }

  test("When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by first throws SQLException.") {
    for
      resultSet <- buildResultSet(
                     Vector.empty,
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      _ <- interceptIO[SQLException](resultSet.first())
    yield ()
  }

  test(
    "If the cursor in the ResultSet is last position, the result at the cursor position matches the specified value."
  ) {
    for
      resultSet <- buildResultSet(
                     Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
                     Vector(
                       ResultSetRowPacket(Array(Some("2023-01-01 12:34:56"))),
                       ResultSetRowPacket(Array(Some("2023-01-01 12:34:56")))
                     ),
                     Version(0, 0, 0),
                     ResultSet.TYPE_SCROLL_INSENSITIVE,
                     ResultSet.CONCUR_READ_ONLY
                   )
      lastResult <- resultSet.last()
      _ = assert(lastResult)
      _    <- resultSet.last()
      row1 <- resultSet.getRow()
      _ = assertEquals(row1, 2)
      _    <- collectRows(resultSet)(_ => IO.unit)
      _    <- resultSet.last()
      row2 <- resultSet.getRow()
      _ = assertEquals(row2, 2)
    yield ()
  }

  test("If the record in the ResultSet is empty, the result of executing last is false.") {
    for
      resultSet <- buildResultSet(
                     Vector.empty,
                     Vector.empty,
                     Version(0, 0, 0),
                     ResultSet.TYPE_SCROLL_INSENSITIVE,
                     ResultSet.CONCUR_READ_ONLY
                   )
      lastResult <- resultSet.last()
    yield assertEquals(lastResult, false)
  }

  test("When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by last throws SQLException.") {
    for
      resultSet <- buildResultSet(
                     Vector.empty,
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      _ <- interceptIO[SQLException](resultSet.last())
    yield ()
  }

  test(
    "If the cursor in the ResultSet is absolute position, the result at the cursor position matches the specified value."
  ) {
    for
      resultSet <- buildResultSet(
                     Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
                     Vector(ResultSetRowPacket(Array(Some("1"))), ResultSetRowPacket(Array(Some("2")))),
                     Version(0, 0, 0),
                     ResultSet.TYPE_SCROLL_INSENSITIVE,
                     ResultSet.CONCUR_READ_ONLY
                   )
      initialRow <- resultSet.getRow()
      _ = assertEquals(initialRow, 0)
      absolute1Result <- resultSet.absolute(1)
      _ = assertEquals(absolute1Result, true)
      _    <- resultSet.absolute(1)
      row1 <- resultSet.getRow()
      _ = assertEquals(row1, 1)
      _    <- resultSet.absolute(1)
      int1 <- resultSet.getInt(1)
      _ = assertEquals(int1, 1)
      absolute0Result <- resultSet.absolute(0)
      _ = assertEquals(absolute0Result, false)
      _    <- resultSet.absolute(0)
      row0 <- resultSet.getRow()
      _ = assertEquals(row0, 0)
      absolute3Result <- resultSet.absolute(3)
      _ = assertEquals(absolute3Result, false)
      _    <- resultSet.absolute(3)
      row3 <- resultSet.getRow()
      _ = assertEquals(row3, 0)
      absoluteNeg1Result <- resultSet.absolute(-1)
      _ = assertEquals(absoluteNeg1Result, true)
      _       <- resultSet.absolute(-1)
      rowNeg1 <- resultSet.getRow()
      _ = assertEquals(rowNeg1, 2)
      _       <- resultSet.absolute(-1)
      intNeg1 <- resultSet.getInt(1)
      _ = assertEquals(intNeg1, 2)
    yield ()
  }

  test(
    "When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by absolute throws SQLException."
  ) {
    for
      resultSet <- buildResultSet(
                     Vector.empty,
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      _ <- interceptIO[SQLException](resultSet.absolute(0))
    yield ()
  }

  test(
    "If the cursor in the ResultSet is relative position, the result at the cursor position matches the specified value."
  ) {
    for
      resultSet <- buildResultSet(
                     Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
                     Vector(ResultSetRowPacket(Array(Some("1"))), ResultSetRowPacket(Array(Some("2")))),
                     Version(0, 0, 0),
                     ResultSet.TYPE_SCROLL_INSENSITIVE,
                     ResultSet.CONCUR_READ_ONLY
                   )
      initialRow <- resultSet.getRow()
      _ = assertEquals(initialRow, 0)
      relative0Result <- resultSet.relative(0)
      _ = assertEquals(relative0Result, false)
      row0 <- resultSet.getRow()
      _ = assertEquals(row0, 0)
      int0 <- resultSet.getInt(1)
      _ = assertEquals(int0, 1)
      relative1Result <- resultSet.relative(1)
      _ = assertEquals(relative1Result, true)
      row1 <- resultSet.getRow()
      _ = assertEquals(row1, 1)
      int1 <- resultSet.getInt(1)
      _ = assertEquals(int1, 1)
      relative2Result <- resultSet.relative(2)
      _ = assertEquals(relative2Result, false)
      row2 <- resultSet.getRow()
      _ = assertEquals(row2, 0)
      relativeNeg1Result <- resultSet.relative(-1)
      _ = assertEquals(relativeNeg1Result, false)
      rowNeg1 <- resultSet.getRow()
      _ = assertEquals(rowNeg1, 0)
      intNeg1 <- resultSet.getInt(1)
      _ = assertEquals(intNeg1, 1)
    yield ()
  }

  test(
    "When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by relative throws SQLException."
  ) {
    for
      resultSet <- buildResultSet(
                     Vector.empty,
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      _ <- interceptIO[SQLException](resultSet.relative(0))
    yield ()
  }

  test(
    "If the cursor in the ResultSet is previous position, the result at the cursor position matches the specified value."
  ) {
    for
      resultSet <- buildResultSet(
                     Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
                     Vector(ResultSetRowPacket(Array(Some("1"))), ResultSetRowPacket(Array(Some("2")))),
                     Version(0, 0, 0),
                     ResultSet.TYPE_SCROLL_INSENSITIVE,
                     ResultSet.CONCUR_READ_ONLY
                   )
      initialRow <- resultSet.getRow()
      _ = assertEquals(initialRow, 0)
      absolute2Result <- resultSet.absolute(2)
      _ = assertEquals(absolute2Result, true)
      _    <- resultSet.absolute(2)
      row2 <- resultSet.getRow()
      _ = assertEquals(row2, 2)
      _    <- resultSet.absolute(2)
      int2 <- resultSet.getInt(1)
      _ = assertEquals(int2, 2)
      previousResult <- resultSet.previous()
      _ = assertEquals(previousResult, true)
      _                <- resultSet.absolute(2)
      _                <- resultSet.previous()
      rowAfterPrevious <- resultSet.getRow()
      _ = assertEquals(rowAfterPrevious, 1)
      _                <- resultSet.absolute(2)
      _                <- resultSet.previous()
      intAfterPrevious <- resultSet.getInt(1)
      _ = assertEquals(intAfterPrevious, 1)
    yield ()
  }

  test(
    "When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by previous throws SQLException."
  ) {
    for
      resultSet <- buildResultSet(
                     Vector.empty,
                     Vector.empty,
                     Version(0, 0, 0)
                   )
      _ <- interceptIO[SQLException](resultSet.previous())
    yield ()
  }

  private def buildResultSet(
    columns:              Vector[ColumnDefinitionPacket],
    records:              Vector[ResultSetRowPacket],
    version:              Version,
    resultSetType:        Int = ResultSet.TYPE_FORWARD_ONLY,
    resultSetConcurrency: Int = ResultSet.CONCUR_READ_ONLY
  ): IO[ResultSetImpl[IO]] =
    for
      isClosed  <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
    yield ResultSetImpl(
      null.asInstanceOf[Protocol[IO]],
      columns,
      records,
      Map.empty,
      version,
      isClosed,
      fetchSize,
      useCursorFetch     = false,
      useServerPrepStmts = false,
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

  private def collectRows[A](resultSet: ResultSetImpl[IO])(f: ResultSetImpl[IO] => IO[A]): IO[List[A]] =
    def loop(acc: List[A]): IO[List[A]] =
      resultSet.next().flatMap { hasNext =>
        if hasNext then f(resultSet).flatMap(row => loop(row :: acc))
        else IO.pure(acc.reverse)
      }
    loop(List.empty)
