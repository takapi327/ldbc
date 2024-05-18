/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.time.*

import cats.Monad

import cats.effect.*

import munit.CatsEffectSuite

import ldbc.sql.{ ResultSet, ResultSetMetaData }

import ldbc.connector.util.Version
import ldbc.connector.data.*
import ldbc.connector.exception.SQLException
import ldbc.connector.net.packet.response.*

class ResultSetTest extends CatsEffectSuite:

  test("SQLException occurs when accessing the ResultSet after closing it.") {
    val resultSet = buildResultSet(Vector.empty, Vector.empty, Version(0, 0, 0))
    interceptMessageIO[SQLException]("Message: Operation not allowed after ResultSet closed")(
      resultSet.close() *> resultSet.next()
    )
    interceptMessageIO[SQLException]("Message: Operation not allowed after ResultSet closed")(
      resultSet.close() *> resultSet.getLong(1)
    )
  }

  test("ResultSet should return the correct value for getInt") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG),
        column("c2", ColumnDataType.MYSQL_TYPE_LONG),
        column("c3", ColumnDataType.MYSQL_TYPE_LONG)
      ),
      Vector(ResultSetRowPacket(List(Some("1"), Some("2"), None))),
      Version(0, 0, 0)
    )
    val records = Monad[IO].whileM[List, (Int, Int, Int)](resultSet.next()) {
      for
        c1 <- resultSet.getInt(1)
        c2 <- resultSet.getInt("c2")
        c3 <- resultSet.getInt(3)
      yield (c1, c2, c3)
    }
    assertIO(records, List((1, 2, 0)))
  }

  test("ResultSet should return the correct value for getLong") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONGLONG),
        column("c2", ColumnDataType.MYSQL_TYPE_LONGLONG),
        column("c3", ColumnDataType.MYSQL_TYPE_LONGLONG)
      ),
      Vector(ResultSetRowPacket(List(Some("1"), Some("2"), None))),
      Version(0, 0, 0)
    )
    val records = Monad[IO].whileM[List, (Long, Long, Long)](resultSet.next()) {
      for
        c1 <- resultSet.getLong(1)
        c2 <- resultSet.getLong("c2")
        c3 <- resultSet.getLong(3)
      yield (c1, c2, c3)
    }
    assertIO(records, List((1L, 2L, 0L)))
  }

  test("ResultSet should return the correct value for getDouble") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_DOUBLE),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE),
        column("c3", ColumnDataType.MYSQL_TYPE_DOUBLE)
      ),
      Vector(ResultSetRowPacket(List(Some("1.1"), Some("2.2"), None))),
      Version(0, 0, 0)
    )
    val records = Monad[IO].whileM[List, (Double, Double, Double)](resultSet.next()) {
      for
        c1 <- resultSet.getDouble(1)
        c2 <- resultSet.getDouble("c2")
        c3 <- resultSet.getDouble(3)
      yield (c1, c2, c3)
    }
    assertIO(records, List((1.1, 2.2, 0.0)))
  }

  test("ResultSet should return the correct value for getString") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_STRING),
        column("c2", ColumnDataType.MYSQL_TYPE_STRING),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING)
      ),
      Vector(ResultSetRowPacket(List(Some("1"), Some("2"), None))),
      Version(0, 0, 0)
    )
    val records = Monad[IO].whileM[List, (Option[String], Option[String], Option[String])](resultSet.next()) {
      for
        c1 <- resultSet.getString(1)
        c2 <- resultSet.getString("c2")
        c3 <- resultSet.getString(3)
      yield (Option(c1), Option(c2), Option(c3))
    }
    assertIO(records, List((Some("1"), Some("2"), None)))
  }

  test("ResultSet should return the correct value for getBoolean") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_TINY),
        column("c2", ColumnDataType.MYSQL_TYPE_TINY),
        column("c3", ColumnDataType.MYSQL_TYPE_TINY)
      ),
      Vector(ResultSetRowPacket(List(Some("1"), Some("0"), None))),
      Version(0, 0, 0)
    )
    val records = Monad[IO].whileM[List, (Boolean, Boolean, Boolean)](resultSet.next()) {
      for
        c1 <- resultSet.getBoolean(1)
        c2 <- resultSet.getBoolean("c2")
        c3 <- resultSet.getBoolean(3)
      yield (c1, c2, c3)
    }
    assertIO(records, List((true, false, false)))
  }

  test("ResultSet should return the correct value for getByte") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_TINY),
        column("c2", ColumnDataType.MYSQL_TYPE_TINY),
        column("c3", ColumnDataType.MYSQL_TYPE_TINY)
      ),
      Vector(ResultSetRowPacket(List(Some("1"), Some("2"), None))),
      Version(0, 0, 0)
    )
    val records = Monad[IO].whileM[List, (Byte, Byte, Byte)](resultSet.next()) {
      for
        c1 <- resultSet.getByte(1)
        c2 <- resultSet.getByte("c2")
        c3 <- resultSet.getByte(3)
      yield (c1, c2, c3)
    }
    assertIO(records, List((49.toByte, 50.toByte, 0.toByte)))
  }

  test("ResultSet should return the correct value for getShort") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_SHORT),
        column("c2", ColumnDataType.MYSQL_TYPE_SHORT),
        column("c3", ColumnDataType.MYSQL_TYPE_SHORT)
      ),
      Vector(ResultSetRowPacket(List(Some("1"), Some("2"), None))),
      Version(0, 0, 0)
    )
    val records = Monad[IO].whileM[List, (Short, Short, Short)](resultSet.next()) {
      for
        c1 <- resultSet.getShort(1)
        c2 <- resultSet.getShort("c2")
        c3 <- resultSet.getShort(3)
      yield (c1, c2, c3)
    }
    assertIO(records, List((1.toShort, 2.toShort, 0.toShort)))
  }

  test("ResultSet should return the correct value for getFloat") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_FLOAT),
        column("c2", ColumnDataType.MYSQL_TYPE_FLOAT),
        column("c3", ColumnDataType.MYSQL_TYPE_FLOAT)
      ),
      Vector(ResultSetRowPacket(List(Some("1.1"), Some("2.2"), None))),
      Version(0, 0, 0)
    )
    val records = Monad[IO].whileM[List, (Float, Float, Float)](resultSet.next()) {
      for
        c1 <- resultSet.getFloat(1)
        c2 <- resultSet.getFloat("c2")
        c3 <- resultSet.getFloat(3)
      yield (c1, c2, c3)
    }
    assertIO(records, List((1.1f, 2.2f, 0.0f)))
  }

  test("ResultSet should return the correct value for getBigDecimal") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_DECIMAL),
        column("c2", ColumnDataType.MYSQL_TYPE_DECIMAL),
        column("c3", ColumnDataType.MYSQL_TYPE_DECIMAL)
      ),
      Vector(ResultSetRowPacket(List(Some("1.1"), Some("2.2"), None))),
      Version(0, 0, 0)
    )
    val records =
      Monad[IO].whileM[List, (Option[BigDecimal], Option[BigDecimal], Option[BigDecimal])](resultSet.next()) {
        for
          c1 <- resultSet.getBigDecimal(1)
          c2 <- resultSet.getBigDecimal("c2")
          c3 <- resultSet.getBigDecimal(3)
        yield (Option(c1), Option(c2), Option(c3))
      }
    assertIO(records, List((Some(BigDecimal("1.1")), Some(BigDecimal("2.2")), None)))
  }

  test("ResultSet should return the correct value for getDate") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_DATE),
        column("c2", ColumnDataType.MYSQL_TYPE_DATE),
        column("c3", ColumnDataType.MYSQL_TYPE_DATE)
      ),
      Vector(ResultSetRowPacket(List(Some("2023-01-01"), Some("2023-01-02"), None))),
      Version(0, 0, 0)
    )
    val records = Monad[IO].whileM[List, (Option[LocalDate], Option[LocalDate], Option[LocalDate])](resultSet.next()) {
      for
        c1 <- resultSet.getDate(1)
        c2 <- resultSet.getDate("c2")
        c3 <- resultSet.getDate(3)
      yield (Option(c1), Option(c2), Option(c3))
    }
    assertIO(records, List((Some(LocalDate.of(2023, 1, 1)), Some(LocalDate.of(2023, 1, 2)), None)))
  }

  test("ResultSet should return the correct value for getTime") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_TIME),
        column("c2", ColumnDataType.MYSQL_TYPE_TIME),
        column("c3", ColumnDataType.MYSQL_TYPE_TIME)
      ),
      Vector(ResultSetRowPacket(List(Some("12:34:56"), Some("12:34:57"), None))),
      Version(0, 0, 0)
    )
    val records = Monad[IO].whileM[List, (Option[LocalTime], Option[LocalTime], Option[LocalTime])](resultSet.next()) {
      for
        c1 <- resultSet.getTime(1)
        c2 <- resultSet.getTime("c2")
        c3 <- resultSet.getTime(3)
      yield (Option(c1), Option(c2), Option(c3))
    }
    assertIO(records, List((Some(LocalTime.of(12, 34, 56)), Some(LocalTime.of(12, 34, 57)), None)))
  }

  test("ResultSet should return the correct value for getTimestamp") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP),
        column("c2", ColumnDataType.MYSQL_TYPE_TIMESTAMP),
        column("c3", ColumnDataType.MYSQL_TYPE_TIMESTAMP)
      ),
      Vector(ResultSetRowPacket(List(Some("2023-01-01 12:34:56"), Some("2023-01-02 12:34:57"), None))),
      Version(0, 0, 0)
    )
    val records =
      Monad[IO].whileM[List, (Option[LocalDateTime], Option[LocalDateTime], Option[LocalDateTime])](resultSet.next()) {
        for
          c1 <- resultSet.getTimestamp(1)
          c2 <- resultSet.getTimestamp("c2")
          c3 <- resultSet.getTimestamp(3)
        yield (Option(c1), Option(c2), Option(c3))
      }
    assertIO(
      records,
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
      Vector(ResultSetRowPacket(List(Some("2023-01-01 12:34:56"), Some("2023-01-02 12:34:57"), None))),
      Version(0, 0, 0)
    )
    val records =
      Monad[IO].whileM[List, (Option[LocalDateTime], Option[LocalDateTime], Option[LocalDateTime])](resultSet.next()) {
        for
          c1 <- resultSet.getTimestamp(1)
          c2 <- resultSet.getTimestamp("c2")
          c3 <- resultSet.getTimestamp(3)
        yield (Option(c1), Option(c2), Option(c3))
      }
    assertIO(
      records,
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
        ResultSetRowPacket(List(Some("1"), Some("2"), None)),
        ResultSetRowPacket(List(Some("3"), Some("4"), None)),
        ResultSetRowPacket(List(Some("5"), Some("6"), None)),
        ResultSetRowPacket(List(Some("7"), Some("8"), None))
      ),
      Version(0, 0, 0)
    )
    val records = Monad[IO].whileM[List, (Int, Int, Int)](resultSet.next()) {
      for
        c1 <- resultSet.getInt(1)
        c2 <- resultSet.getInt("c2")
        c3 <- resultSet.getInt(3)
      yield (c1, c2, c3)
    }
    assertIO(records, List((1, 2, 0), (3, 4, 0), (5, 6, 0), (7, 8, 0)))
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
    assertIO(resultSetMetaData.map(_.getColumnCount()), 3)
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
    assertIO(resultSetMetaData.map(_.getColumnName(1)), "c1")
    assertIO(resultSetMetaData.map(_.getColumnName(2)), "c2")
    assertIO(resultSetMetaData.map(_.getColumnName(3)), "c3")
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
    assertIO(resultSetMetaData.map(_.getColumnType(1)), ColumnDataType.MYSQL_TYPE_LONG.code.toInt)
    assertIO(resultSetMetaData.map(_.getColumnType(2)), ColumnDataType.MYSQL_TYPE_DOUBLE.code.toInt)
    assertIO(resultSetMetaData.map(_.getColumnType(3)), ColumnDataType.MYSQL_TYPE_STRING.code.toInt)
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
    assertIO(resultSetMetaData.map(_.getColumnTypeName(1)), "INT")
    assertIO(resultSetMetaData.map(_.getColumnTypeName(2)), "DOUBLE")
    assertIO(resultSetMetaData.map(_.getColumnTypeName(3)), "CHAR")
  }

  test("The column label obtained from the meta-information of ResultSet matches the specified value.") {
    val resultSet = buildResultSet(
      Vector(
        column("c1", ColumnDataType.MYSQL_TYPE_LONG, Some("label1")),
        column("c2", ColumnDataType.MYSQL_TYPE_DOUBLE, Some("label2")),
        column("c3", ColumnDataType.MYSQL_TYPE_STRING, Some("label3"))
      ),
      Vector.empty,
      Version(0, 0, 0)
    )
    val resultSetMetaData = resultSet.getMetaData()
    assertIO(resultSetMetaData.map(_.getColumnLabel(1)), "label1")
    assertIO(resultSetMetaData.map(_.getColumnLabel(2)), "label2")
    assertIO(resultSetMetaData.map(_.getColumnLabel(3)), "label3")
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
    assertIO(resultSetMetaData.map(_.getColumnDisplaySize(1)), 0)
    assertIO(resultSetMetaData.map(_.getColumnDisplaySize(2)), 0)
    assertIO(resultSetMetaData.map(_.getColumnDisplaySize(3)), 0)
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
    assertIO(resultSetMetaData.map(_.getPrecision(1)), 0)
    assertIO(resultSetMetaData.map(_.getPrecision(2)), 0)
    assertIO(resultSetMetaData.map(_.getPrecision(3)), 0)
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
    assertIO(resultSetMetaData.map(_.getScale(1)), 0)
    assertIO(resultSetMetaData.map(_.getScale(2)), 2)
    assertIO(resultSetMetaData.map(_.getScale(3)), 0)
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
    assertIO(resultSetMetaData.map(_.isSigned(1)), true)
    assertIO(resultSetMetaData.map(_.isSigned(2)), false)
    assertIO(resultSetMetaData.map(_.isSigned(3)), false)
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
    assertIO(resultSetMetaData.map(_.isNullable(1)), ResultSetMetaData.columnNoNulls)
    assertIO(resultSetMetaData.map(_.isNullable(2)), ResultSetMetaData.columnNullable)
    assertIO(resultSetMetaData.map(_.isNullable(3)), ResultSetMetaData.columnNullable)
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
    assertIO(resultSetMetaData.map(_.isCaseSensitive(1)), false)
    assertIO(resultSetMetaData.map(_.isCaseSensitive(2)), false)
    assertIO(resultSetMetaData.map(_.isCaseSensitive(3)), false)
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
    assertIO(resultSetMetaData.map(_.isSearchable(1)), true)
    assertIO(resultSetMetaData.map(_.isSearchable(2)), true)
    assertIO(resultSetMetaData.map(_.isSearchable(3)), true)
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
    assertIO(resultSetMetaData.map(_.isWritable(1)), true)
    assertIO(resultSetMetaData.map(_.isWritable(2)), true)
    assertIO(resultSetMetaData.map(_.isWritable(3)), true)
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
    assertIO(resultSetMetaData.map(_.isDefinitelyWritable(1)), true)
    assertIO(resultSetMetaData.map(_.isDefinitelyWritable(2)), true)
    assertIO(resultSetMetaData.map(_.isDefinitelyWritable(3)), true)
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
    assertIO(resultSetMetaData.map(_.isReadOnly(1)), false)
    assertIO(resultSetMetaData.map(_.isReadOnly(2)), false)
    assertIO(resultSetMetaData.map(_.isReadOnly(3)), false)
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
    assertIO(resultSetMetaData.map(_.isAutoIncrement(1)), true)
    assertIO(resultSetMetaData.map(_.isAutoIncrement(2)), false)
    assertIO(resultSetMetaData.map(_.isAutoIncrement(3)), false)
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
    assertIO(resultSetMetaData.map(_.isCurrency(1)), false)
    assertIO(resultSetMetaData.map(_.isCurrency(2)), false)
    assertIO(resultSetMetaData.map(_.isCurrency(3)), false)
  }

  test(
    "The determination of whether the cursor position for a row in the ResultSet is before the start position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(List(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0)
    )
    assertIO(resultSet.isBeforeFirst(), true)
    assertIO(resultSet.next() *> resultSet.isBeforeFirst(), false)
  }

  test(
    "The determination of whether the cursor position in the row of the ResultSet is after the end position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(List(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0)
    )
    assertIO(resultSet.isAfterLast(), false)
    assertIO(Monad[IO].whileM_(resultSet.next())(IO.unit) *> resultSet.isAfterLast(), true)
  }

  test(
    "The determination of whether the cursor position in the row of the ResultSet is at the start position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(List(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0)
    )
    assertIO(resultSet.isFirst(), false)
    assertIO(resultSet.next() *> resultSet.isFirst(), true)
  }

  test(
    "The determination of whether the cursor position in the row of the ResultSet is at the end position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(List(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0)
    )
    assertIO(resultSet.isLast(), false)
    assertIO(resultSet.next() *> resultSet.isLast(), true)
  }

  test(
    "If the cursor in the ResultSet is before the start position, the result at the cursor position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(List(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )
    assertIO(resultSet.isBeforeFirst(), true)
    assertIO(resultSet.isFirst(), false)
    assertIO(resultSet.next() *> resultSet.isBeforeFirst(), false)
    assertIO(resultSet.isFirst(), true)
    assertIO(resultSet.beforeFirst() *> resultSet.isBeforeFirst(), true)
    assertIO(resultSet.isFirst(), false)
  }

  test(
    "When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by beforeFirst throws SQLException."
  ) {
    val resultSet = buildResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0)
    )
    interceptMessageIO[SQLException](
      "Message: Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY."
    )(resultSet.beforeFirst())
  }

  test(
    "If the cursor in the ResultSet is after the end position, the result at the cursor position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(List(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )
    assertIO(resultSet.isAfterLast(), false)
    assertIO(resultSet.isLast(), false)
    assertIO(Monad[IO].whileM_(resultSet.next())(IO.unit) *> resultSet.isAfterLast(), false)
    assertIO(resultSet.isLast(), true)
    assertIO(resultSet.isAfterLast(), true)
    assertIO(resultSet.isLast(), false)
    assertIO(resultSet.afterLast() *> resultSet.isAfterLast(), true)
    assertIO(resultSet.isLast(), false)
  }

  test(
    "When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by afterLast throws SQLException."
  ) {
    val resultSet = buildResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0)
    )
    interceptMessageIO[SQLException](
      "Message: Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY."
    )(resultSet.afterLast())
  }

  test(
    "If the cursor in the ResultSet is first position, the result at the cursor position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(List(Some("2023-01-01 12:34:56")))),
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )
    assertIO(resultSet.first(), true)
    assertIO(resultSet.getRow(), 1)
    assertIO(Monad[IO].whileM_(resultSet.next())(IO.unit) *> resultSet.getRow(), 1)
    assertIO(resultSet.first() *> resultSet.getRow(), 1)
  }

  test("If the record in the ResultSet is empty, the result of executing first is false.") {
    val resultSet = buildResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )
    assertIO(resultSet.first(), false)
  }

  test("When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by first throws SQLException.") {
    val resultSet = buildResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0)
    )
    interceptMessageIO[SQLException](
      "Message: Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY."
    )(resultSet.first())
  }

  test(
    "If the cursor in the ResultSet is last position, the result at the cursor position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(
        ResultSetRowPacket(List(Some("2023-01-01 12:34:56"))),
        ResultSetRowPacket(List(Some("2023-01-01 12:34:56")))
      ),
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )
    assertIO(resultSet.last(), true)
    assertIO(resultSet.last() *> resultSet.getRow(), 2)
    assertIO(Monad[IO].whileM_(resultSet.next())(IO.unit) *> resultSet.last() *> resultSet.getRow(), 2)
  }

  test("If the record in the ResultSet is empty, the result of executing last is false.") {
    val resultSet = buildResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )
    assertIO(resultSet.last(), false)
  }

  test("When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by last throws SQLException.") {
    val resultSet = buildResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0)
    )
    interceptMessageIO[SQLException](
      "Message: Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY."
    )(resultSet.last())
  }

  test(
    "If the cursor in the ResultSet is absolute position, the result at the cursor position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(List(Some("1"))), ResultSetRowPacket(List(Some("2")))),
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )
    assertIO(resultSet.getRow(), 0)
    assertIO(resultSet.absolute(1), true)
    assertIO(resultSet.absolute(1) *> resultSet.getRow(), 1)
    assertIO(resultSet.absolute(1) *> resultSet.getInt(1), 1)
    assertIO(resultSet.absolute(0), false)
    assertIO(resultSet.absolute(0) *> resultSet.getRow(), 0)
    assertIO(resultSet.absolute(3), false)
    assertIO(resultSet.absolute(3) *> resultSet.getRow(), 0)
    assertIO(resultSet.absolute(-1), true)
    assertIO(resultSet.absolute(-1) *> resultSet.getRow(), 2)
    assertIO(resultSet.absolute(-1) *> resultSet.getInt(1), 2)
  }

  test(
    "When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by absolute throws SQLException."
  ) {
    val resultSet = buildResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0)
    )
    interceptMessageIO[SQLException](
      "Message: Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY."
    )(resultSet.absolute(0))
  }

  test(
    "If the cursor in the ResultSet is relative position, the result at the cursor position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(List(Some("1"))), ResultSetRowPacket(List(Some("2")))),
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )
    assertIO(resultSet.getRow(), 0)
    assertIO(resultSet.relative(0), false)
    assertIO(resultSet.getRow(), 0)
    assertIO(resultSet.relative(0) *> resultSet.getInt(1), 1)
    assertIO(resultSet.relative(1), true)
    assertIO(resultSet.relative(1) *> resultSet.getRow(), 1)
    assertIO(resultSet.relative(1) *> resultSet.getInt(1), 1)
    assertIO(resultSet.relative(2), false)
    assertIO(resultSet.relative(2) *> resultSet.getRow(), 0)
    assertIO(resultSet.relative(-1), false)
    assertIO(resultSet.relative(-1) *> resultSet.getRow(), 0)
    assertIO(resultSet.relative(-1) *> resultSet.getInt(1), 1)
  }

  test(
    "When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by relative throws SQLException."
  ) {
    val resultSet = buildResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0)
    )
    interceptMessageIO[SQLException](
      "Message: Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY."
    )(resultSet.relative(0))
  }

  test(
    "If the cursor in the ResultSet is previous position, the result at the cursor position matches the specified value."
  ) {
    val resultSet = buildResultSet(
      Vector(column("c1", ColumnDataType.MYSQL_TYPE_TIMESTAMP)),
      Vector(ResultSetRowPacket(List(Some("1"))), ResultSetRowPacket(List(Some("2")))),
      Version(0, 0, 0),
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )
    assertIO(resultSet.getRow(), 0)
    assertIO(resultSet.absolute(2), true)
    assertIO(resultSet.absolute(2) *> resultSet.getRow(), 2)
    assertIO(resultSet.absolute(2) *> resultSet.getInt(1), 2)
    assertIO(resultSet.previous(), true)
    assertIO(resultSet.absolute(2) *> resultSet.previous() *> resultSet.getRow(), 1)
    assertIO(resultSet.absolute(2) *> resultSet.previous() *> resultSet.getInt(1), 1)
  }

  test(
    "When the type of ResultSet is TYPE_FORWARD_ONLY, the cursor position operation by previous throws SQLException."
  ) {
    val resultSet = buildResultSet(
      Vector.empty,
      Vector.empty,
      Version(0, 0, 0)
    )
    interceptMessageIO[SQLException](
      "Message: Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY."
    )(resultSet.previous())
  }

  private def buildResultSet(
    columns:              Vector[ColumnDefinitionPacket],
    records:              Vector[ResultSetRowPacket],
    version:              Version,
    resultSetType:        Int = ResultSet.TYPE_FORWARD_ONLY,
    resultSetConcurrency: Int = ResultSet.CONCUR_READ_ONLY
  ): ResultSetImpl[IO] =
    ResultSetImpl[IO](
      columns,
      records,
      Map.empty,
      version,
      Ref.unsafe[IO, Boolean](false),
      Ref.unsafe[IO, Boolean](true),
      Ref.unsafe[IO, Int](0),
      Ref.unsafe[IO, Option[ResultSetRowPacket]](None),
      resultSetType,
      resultSetConcurrency
    )

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
