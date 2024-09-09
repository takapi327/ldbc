/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.time.*

import ldbc.sql.{ ResultSet, ResultSetMetaData }

import ldbc.connector.util.Version
import ldbc.connector.exception.SQLException
import ldbc.connector.net.packet.response.*
import ldbc.connector.codec.all.*
import ldbc.connector.codec.Codec

/**
 * A table of data representing a database result set, which is usually generated by executing a statement that queries the database.
 */
private[ldbc] case class ResultSetImpl(
  columns:              Vector[ColumnDefinitionPacket],
  records:              Vector[ResultSetRowPacket],
  serverVariables:      Map[String, String],
  version:              Version,
  resultSetType:        Int = ResultSet.TYPE_FORWARD_ONLY,
  resultSetConcurrency: Int = ResultSet.CONCUR_READ_ONLY
) extends ResultSet:

  private final var isClosed:               Boolean                    = false
  private final var lastColumnReadNullable: Boolean                    = false
  private final var currentCursor:          Int                        = 0
  private final var currentRow:             Option[ResultSetRowPacket] = records.headOption

  def next(): Boolean =
    checkClose {
      if currentCursor <= records.size then
        currentRow    = records.lift(currentCursor)
        currentCursor = currentCursor + 1
        currentRow.isDefined
      else
        currentCursor = currentCursor + 1
        false
    }

  override def close(): Unit = isClosed = true

  override def wasNull(): Boolean = lastColumnReadNullable

  override def getString(columnIndex: Int): String =
    checkClose {
      rowDecode(row => text.decode(columnIndex, List(row.values(columnIndex - 1))).toOption) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          null
    }

  override def getBoolean(columnIndex: Int): Boolean =
    checkClose {
      rowDecode(row => boolean.decode(columnIndex, List(row.values(columnIndex - 1))).toOption) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          false
    }

  override def getByte(columnIndex: Int): Byte =
    checkClose {
      rowDecode(row => bit.decode(columnIndex, List(row.values(columnIndex - 1))).toOption) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          0
    }

  override def getShort(columnIndex: Int): Short =
    checkClose {
      rowDecode(row => smallint.decode(columnIndex, List(row.values(columnIndex - 1))).toOption) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          0
    }

  override def getInt(columnIndex: Int): Int =
    checkClose {
      rowDecode(row => int.decode(columnIndex, List(row.values(columnIndex - 1))).toOption) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          0
    }

  override def getLong(columnIndex: Int): Long =
    checkClose {
      rowDecode(row => bigint.decode(columnIndex, List(row.values(columnIndex - 1))).toOption) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          0L
    }

  override def getFloat(columnIndex: Int): Float =
    checkClose {
      rowDecode(row => float.decode(columnIndex, List(row.values(columnIndex - 1))).toOption) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          0f
    }

  override def getDouble(columnIndex: Int): Double =
    checkClose {
      rowDecode(row => double.decode(columnIndex, List(row.values(columnIndex - 1))).toOption) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          0.toDouble
    }

  override def getBytes(columnIndex: Int): Array[Byte] =
    checkClose {
      rowDecode(row => binary(255).decode(columnIndex, List(row.values(columnIndex - 1))).toOption) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          null
    }

  override def getDate(columnIndex: Int): LocalDate =
    checkClose {
      rowDecode(row => date.decode(columnIndex, List(row.values(columnIndex - 1))).toOption) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          null
    }

  override def getTime(columnIndex: Int): LocalTime =
    checkClose {
      rowDecode(row => time.decode(columnIndex, List(row.values(columnIndex - 1))).toOption) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          null
    }

  override def getTimestamp(columnIndex: Int): LocalDateTime =
    checkClose {
      rowDecode(row => timestamp.decode(columnIndex, List(row.values(columnIndex - 1))).toOption) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          null
    }

  override def getString(columnLabel: String): String =
    checkClose {
      findByName(columnLabel) match
        case Some((_, index)) => getString(index + 1)
        case None =>
          lastColumnReadNullable = true
          null
    }

  override def getBoolean(columnLabel: String): Boolean =
    checkClose {
      findByName(columnLabel) match
        case Some((_, index)) => getBoolean(index + 1)
        case None =>
          lastColumnReadNullable = true
          false
    }

  override def getByte(columnLabel: String): Byte =
    checkClose {
      findByName(columnLabel) match
        case Some((_, index)) => getByte(index + 1)
        case None =>
          lastColumnReadNullable = true
          0
    }

  override def getShort(columnLabel: String): Short =
    checkClose {
      findByName(columnLabel) match
        case Some((_, index)) => getShort(index + 1)
        case None =>
          lastColumnReadNullable = true
          0
    }

  override def getInt(columnLabel: String): Int =
    checkClose {
      findByName(columnLabel) match
        case Some((_, index)) => getInt(index + 1)
        case None =>
          lastColumnReadNullable = true
          0
    }

  override def getLong(columnLabel: String): Long =
    checkClose {
      findByName(columnLabel) match
        case Some((_, index)) => getLong(index + 1)
        case None =>
          lastColumnReadNullable = true
          0L
    }

  override def getFloat(columnLabel: String): Float =
    checkClose {
      findByName(columnLabel) match
        case Some((_, index)) => getFloat(index + 1)
        case None =>
          lastColumnReadNullable = true
          0f
    }

  override def getDouble(columnLabel: String): Double =
    checkClose {
      findByName(columnLabel) match
        case Some((_, index)) => getDouble(index + 1)
        case None =>
          lastColumnReadNullable = true
          0.toDouble
    }

  override def getBytes(columnLabel: String): Array[Byte] =
    checkClose {
      findByName(columnLabel) match
        case Some((_, index)) => getBytes(index + 1)
        case None =>
          lastColumnReadNullable = true
          null
    }

  override def getDate(columnLabel: String): LocalDate =
    checkClose {
      findByName(columnLabel) match
        case Some((_, index)) => getDate(index + 1)
        case None =>
          lastColumnReadNullable = true
          null
    }

  override def getTime(columnLabel: String): LocalTime =
    checkClose {
      findByName(columnLabel) match
        case Some((_, index)) => getTime(index + 1)
        case None =>
          lastColumnReadNullable = true
          null
    }

  override def getTimestamp(columnLabel: String): LocalDateTime =
    checkClose {
      findByName(columnLabel) match
        case Some((_, index)) => getTimestamp(index + 1)
        case None =>
          lastColumnReadNullable = true
          null
    }

  override def getMetaData(): ResultSetMetaData =
    checkClose {
      ResultSetMetaDataImpl(columns, serverVariables, version)
    }

  override def getBigDecimal(columnIndex: Int): BigDecimal =
    checkClose {
      rowDecode(row => decimal().decode(columnIndex, List(row.values(columnIndex - 1))).toOption) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          null
    }

  override def getBigDecimal(columnLabel: String): BigDecimal =
    checkClose {
      columns.zipWithIndex.find(_._1.name == columnLabel) match
        case Some((_, index)) => getBigDecimal(index + 1)
        case None =>
          lastColumnReadNullable = true
          null
    }

  override def isBeforeFirst(): Boolean =
    currentCursor <= 0 && records.nonEmpty

  override def isAfterLast(): Boolean =
    currentCursor > records.size && records.nonEmpty

  override def isFirst(): Boolean =
    currentCursor > 0

  override def isLast(): Boolean =
    currentCursor == records.size

  override def beforeFirst(): Unit =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      raiseError("Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")
    else currentCursor = 0

  override def afterLast(): Unit =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      raiseError("Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")
    else currentCursor = records.size + 1

  override def first(): Boolean =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      raiseError("Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")
    else
      currentCursor = 1
      currentRow    = records.headOption
      currentRow.isDefined && records.nonEmpty

  override def last(): Boolean =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      raiseError("Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")
    else
      currentCursor = records.size
      currentRow    = records.lastOption
      currentRow.isDefined && records.nonEmpty

  override def getRow(): Int =
    if currentCursor > records.size then 0
    else currentCursor

  override def absolute(row: Int): Boolean =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      raiseError("Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")
    else if row > 0 then
      currentCursor = row
      currentRow    = records.lift(row - 1)
      row >= 1 && row <= records.size
    else if row < 0 then
      val position = records.size + row + 1
      currentCursor = position
      currentRow    = records.lift(records.size + row)
      position >= 1 && position <= records.size
    else
      currentCursor = 0
      currentRow    = None
      false

  override def relative(rows: Int): Boolean =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      raiseError("Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")
    else
      val position = currentCursor + rows
      if position >= 1 && position <= records.size then
        currentCursor = position
        currentRow    = records.lift(position - 1)
        true
      else
        currentCursor = 0
        currentRow    = records.lift(currentCursor)
        false

  override def previous(): Boolean =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      raiseError("Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")
    else if currentCursor > 0 then
      currentCursor = currentCursor - 1
      currentRow    = records.lift(currentCursor - 1)
      currentRow.isDefined
    else
      currentCursor = 0
      currentRow    = None
      false

  override def getType(): Int =
    checkClose {
      resultSetType
    }

  override def getConcurrency(): Int =
    checkClose {
      resultSetConcurrency
    }

  /**
   * Function to decode all lines with the specified type.
   *
   * @param codec
   *   The codec to decode the value
   * @tparam T
   *   The type of the value
   * @return
   *   A list of values decoded with the specified type.
   */
  def decode[T](codec: Codec[T]): List[T] =
    checkClose {
      records.flatMap(row => codec.decode(0, row.values.toList).toOption).toList
    }

  /**
   * Does the result set contain rows, or is it the result of a DDL or DML statement?
   *
   * @return true if result set contains rows
   */
  def hasRows(): Boolean =
    checkClose {
      records.nonEmpty
    }

  /**
   * Returns the number of rows in this <code>ResultSet</code> object.
   *
   * @return
   *   the number of rows
   */
  def rowLength(): Int =
    checkClose {
      records.size
    }

  private def checkClose[T](f: => T): T =
    if isClosed then raiseError("Operation not allowed after ResultSet closed")
    else f

  private def rowDecode[T](decode: ResultSetRowPacket => Option[T]): Option[T] =
    currentRow.flatMap(decode)

  private def findByName(columnLabel: String): Option[(ColumnDefinitionPacket, Int)] =
    columns.zipWithIndex.find {
      case (column: ColumnDefinition41Packet, _) =>
        if column.table != column.orgTable then
          (column.table + "." + column.name).toLowerCase == columnLabel.toLowerCase
        else column.name == columnLabel
      case (column: ColumnDefinition320Packet, _) => column.name == columnLabel
    }

  private def raiseError[T](message: String): T =
    throw new SQLException(message)

private[ldbc] object ResultSetImpl:

  def apply(
    columns:         Vector[ColumnDefinitionPacket],
    records:         Vector[ResultSetRowPacket],
    serverVariables: Map[String, String],
    version:         Version
  ): ResultSetImpl =
    ResultSetImpl(
      columns,
      records,
      serverVariables,
      version,
      ResultSet.TYPE_FORWARD_ONLY
    )

  def empty(
    serverVariables: Map[String, String],
    version:         Version
  ): ResultSetImpl =
    this.apply(
      Vector.empty,
      Vector.empty,
      serverVariables,
      version
    )
