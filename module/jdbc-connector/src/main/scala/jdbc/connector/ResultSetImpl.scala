/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package jdbc.connector

import java.time.*

import cats.syntax.all.*

import ldbc.sql.{ ResultSet, ResultSetMetaData }

private[jdbc] case class ResultSetImpl(resultSet: java.sql.ResultSet) extends ResultSet:
  override def next(): Boolean = resultSet.next()

  override def close(): Unit = resultSet.close()

  override def wasNull(): Boolean = resultSet.wasNull()

  override def getString(columnIndex: Int): String = resultSet.getString(columnIndex)

  override def getString(columnLabel: String): String = resultSet.getString(columnLabel)

  override def getBoolean(columnIndex: Int): Boolean = resultSet.getBoolean(columnIndex)

  override def getBoolean(columnLabel: String): Boolean = resultSet.getBoolean(columnLabel)

  override def getByte(columnIndex: Int): Byte = resultSet.getByte(columnIndex)

  override def getByte(columnLabel: String): Byte = resultSet.getByte(columnLabel)

  override def getBytes(columnIndex: Int): Array[Byte] = resultSet.getBytes(columnIndex)

  override def getBytes(columnLabel: String): Array[Byte] = resultSet.getBytes(columnLabel)

  override def getShort(columnIndex: Int): Short = resultSet.getShort(columnIndex)

  override def getShort(columnLabel: String): Short = resultSet.getShort(columnLabel)

  override def getInt(columnIndex: Int): Int = resultSet.getInt(columnIndex)

  override def getInt(columnLabel: String): Int = resultSet.getInt(columnLabel)

  override def getLong(columnIndex: Int): Long = resultSet.getLong(columnIndex)

  override def getLong(columnLabel: String): Long = resultSet.getLong(columnLabel)

  override def getFloat(columnIndex: Int): Float = resultSet.getFloat(columnIndex)

  override def getFloat(columnLabel: String): Float = resultSet.getFloat(columnLabel)

  override def getDouble(columnIndex: Int): Double = resultSet.getDouble(columnIndex)

  override def getDouble(columnLabel: String): Double = resultSet.getDouble(columnLabel)

  override def getDate(columnIndex: Int): LocalDate =
    resultSet.getDate(columnIndex) match
      case null => null
      case date => date.toLocalDate

  override def getDate(columnLabel: String): LocalDate =
    resultSet.getDate(columnLabel) match
      case null => null
      case date => date.toLocalDate

  override def getTime(columnIndex: Int): LocalTime =
    resultSet.getTime(columnIndex) match
      case null => null
      case time => time.toLocalTime

  override def getTime(columnLabel: String): LocalTime =
    resultSet.getTime(columnLabel) match
      case null => null
      case time => time.toLocalTime

  override def getTimestamp(columnIndex: Int): LocalDateTime =
    resultSet.getTimestamp(columnIndex) match
      case null      => null
      case timestamp => timestamp.toLocalDateTime

  override def getTimestamp(columnLabel: String): LocalDateTime =
    resultSet.getTimestamp(columnLabel) match
      case null      => null
      case timestamp => timestamp.toLocalDateTime

  override def getMetaData(): ResultSetMetaData =
    ResultSetMetaDataImpl(resultSet.getMetaData)

  override def getBigDecimal(columnIndex: Int): BigDecimal =
    resultSet.getBigDecimal(columnIndex)

  override def getBigDecimal(columnLabel: String): BigDecimal =
    resultSet.getBigDecimal(columnLabel)

  override def isBeforeFirst(): Boolean = resultSet.isBeforeFirst

  override def isAfterLast(): Boolean = resultSet.isAfterLast

  override def isFirst(): Boolean = resultSet.first()

  override def isLast(): Boolean = resultSet.isLast

  override def beforeFirst(): Unit = resultSet.beforeFirst()

  override def afterLast(): Unit = resultSet.afterLast()

  override def first(): Boolean = resultSet.first()

  override def last(): Boolean = resultSet.last()

  override def getRow(): Int = resultSet.getRow

  override def absolute(row: Int): Boolean = resultSet.absolute(row)

  override def relative(rows: Int): Boolean = resultSet.relative(rows)

  override def previous(): Boolean = resultSet.previous()

  override def getType(): Int = resultSet.getType

  override def getConcurrency(): Int = resultSet.getConcurrency
