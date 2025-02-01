/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

import ldbc.sql.{ ResultSet, ResultSetMetaData }

import ldbc.connector.data.Formatter.*
import ldbc.connector.exception.SQLException
import ldbc.connector.net.packet.response.*
import ldbc.connector.util.Version

/**
 * A table of data representing a database result set, which is usually generated by executing a statement that queries the database.
 */
private[ldbc] case class ResultSetImpl(
  columns:              Vector[ColumnDefinitionPacket],
  records:              Vector[ResultSetRowPacket],
  serverVariables:      Map[String, String],
  version:              Version,
  resultSetType:        Int            = ResultSet.TYPE_FORWARD_ONLY,
  resultSetConcurrency: Int            = ResultSet.CONCUR_READ_ONLY,
  statement:            Option[String] = None
) extends ResultSet:

  private final val recordSize:             Int                        = records.size
  private final var isClosed:               Boolean                    = false
  private final var lastColumnReadNullable: Boolean                    = false
  private final var currentCursor:          Int                        = 0
  private final var currentRow:             Option[ResultSetRowPacket] = records.headOption

  def next(): Boolean =
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else if currentCursor <= recordSize then
      currentRow    = records.lift(currentCursor)
      currentCursor = currentCursor + 1
      currentRow.isDefined
    else
      currentCursor = currentCursor + 1
      false

  override def close(): Unit = isClosed = true

  override def wasNull(): Boolean = lastColumnReadNullable

  override def getString(columnIndex: Int): String =
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else
      rowDecode[String](columnIndex, _.toString) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          null

  override def getBoolean(columnIndex: Int): Boolean =
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else
      rowDecode[Boolean](
        columnIndex,
        {
          case "true" | "1"  => true
          case "false" | "0" => false
          case unknown       => raiseError(s"Unknown boolean value: $unknown")
        }
      ) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          false

  override def getByte(columnIndex: Int): Byte =
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else
      rowDecode[Byte](
        columnIndex,
        str => {
          if str.length == 1 && !str.forall(_.isDigit) then str.getBytes().head
          else str.toByte
        }
      ) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          0

  override def getShort(columnIndex: Int): Short =
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else
      rowDecode[Short](columnIndex, _.toShort) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          0

  override def getInt(columnIndex: Int): Int =
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else
      rowDecode[Int](columnIndex, _.toInt) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          0

  override def getLong(columnIndex: Int): Long =
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else
      rowDecode[Long](columnIndex, _.toLong) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          0

  override def getFloat(columnIndex: Int): Float =
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else
      rowDecode[Float](columnIndex, _.toFloat) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          0f

  override def getDouble(columnIndex: Int): Double =
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else
      rowDecode[Double](columnIndex, _.toDouble) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          0.0

  override def getBytes(columnIndex: Int): Array[Byte] =
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else
      rowDecode[Array[Byte]](columnIndex, _.getBytes("UTF-8")) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          null

  override def getDate(columnIndex: Int): LocalDate =
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else
      rowDecode[LocalDate](
        columnIndex,
        str => LocalDate.parse(str, localDateFormatter)
      ) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          null

  override def getTime(columnIndex: Int): LocalTime =
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else
      rowDecode[LocalTime](
        columnIndex,
        str => LocalTime.parse(str, timeFormatter(6))
      ) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          null

  override def getTimestamp(columnIndex: Int): LocalDateTime =
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else
      rowDecode[LocalDateTime](
        columnIndex,
        str => LocalDateTime.parse(str, localDateTimeFormatter(6))
      ) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          null

  override def getString(columnLabel: String): String =
    val index = findByName(columnLabel)
    getString(index)

  override def getBoolean(columnLabel: String): Boolean =
    val index = findByName(columnLabel)
    getBoolean(index)

  override def getByte(columnLabel: String): Byte =
    val index = findByName(columnLabel)
    getByte(index)

  override def getShort(columnLabel: String): Short =
    val index = findByName(columnLabel)
    getShort(index)

  override def getInt(columnLabel: String): Int =
    val index = findByName(columnLabel)
    getInt(index)

  override def getLong(columnLabel: String): Long =
    val index = findByName(columnLabel)
    getLong(index)

  override def getFloat(columnLabel: String): Float =
    val index = findByName(columnLabel)
    getFloat(index)

  override def getDouble(columnLabel: String): Double =
    val index = findByName(columnLabel)
    getDouble(index)

  override def getBytes(columnLabel: String): Array[Byte] =
    val index = findByName(columnLabel)
    getBytes(index)

  override def getDate(columnLabel: String): LocalDate =
    val index = findByName(columnLabel)
    getDate(index)

  override def getTime(columnLabel: String): LocalTime =
    val index = findByName(columnLabel)
    getTime(index)

  override def getTimestamp(columnLabel: String): LocalDateTime =
    val index = findByName(columnLabel)
    getTimestamp(index)

  override def getMetaData(): ResultSetMetaData =
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else ResultSetMetaDataImpl(columns, serverVariables, version)

  override def getBigDecimal(columnIndex: Int): BigDecimal =
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else
      rowDecode[BigDecimal](columnIndex, str => BigDecimal(str)) match
        case Some(value) =>
          lastColumnReadNullable = false
          value
        case None =>
          lastColumnReadNullable = true
          null

  override def getBigDecimal(columnLabel: String): BigDecimal =
    val index = findByName(columnLabel)
    getBigDecimal(index)

  override def isBeforeFirst(): Boolean =
    currentCursor <= 0 && records.nonEmpty

  override def isAfterLast(): Boolean =
    currentCursor > recordSize && records.nonEmpty

  override def isFirst(): Boolean =
    currentCursor > 0

  override def isLast(): Boolean =
    currentCursor == recordSize

  override def beforeFirst(): Unit =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      raiseError("Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")
    else currentCursor = 0

  override def afterLast(): Unit =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      raiseError("Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")
    else currentCursor = recordSize + 1

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
      currentCursor = recordSize
      currentRow    = records.lastOption
      currentRow.isDefined && records.nonEmpty

  override def getRow(): Int =
    if currentCursor > recordSize then 0
    else currentCursor

  override def absolute(row: Int): Boolean =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      raiseError("Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")
    else if row > 0 then
      currentCursor = row
      currentRow    = records.lift(row - 1)
      row >= 1 && row <= recordSize
    else if row < 0 then
      val position = recordSize + row + 1
      currentCursor = position
      currentRow    = records.lift(recordSize + row)
      position >= 1 && position <= recordSize
    else
      currentCursor = 0
      currentRow    = None
      false

  override def relative(rows: Int): Boolean =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      raiseError("Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")
    else
      val position = currentCursor + rows
      if position >= 1 && position <= recordSize then
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
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else resultSetType

  override def getConcurrency(): Int =
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else resultSetConcurrency

  /**
   * Does the result set contain rows, or is it the result of a DDL or DML statement?
   *
   * @return true if result set contains rows
   */
  def hasRows(): Boolean =
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else recordSize > 0

  /**
   * Returns the number of rows in this <code>ResultSet</code> object.
   *
   * @return
   *   the number of rows
   */
  def rowLength(): Int =
    if isClosed then raiseError(ResultSetImpl.CLOSED_MESSAGE)
    else recordSize

  private def rowDecode[T](index: Int, decode: String => T): Option[T] =
    for
      row <- currentRow
      value <- row.values(index - 1)
    yield decode(value)

  private def findByName(columnLabel: String): Int =
    columns.zipWithIndex
      .find { (column: ColumnDefinitionPacket, _) =>
        column.name.equalsIgnoreCase(columnLabel) || column.fullName.equalsIgnoreCase(columnLabel)
      }
      .map(_._2 + 1)
      .getOrElse(
        raiseError(
          s"${ Console.CYAN }Column name '${ Console.RED }$columnLabel${ Console.CYAN }' does not exist in the ResultSet."
        )
      )

  private def raiseError[T](message: String): T =
    throw new SQLException(message, sql = statement)

private[ldbc] object ResultSetImpl:

  private[ldbc] final val CLOSED_MESSAGE = "Operation not allowed after ResultSet closed"

  private[ldbc] def temporalDecode[A <: TemporalAccessor](
    formatter: DateTimeFormatter,
    parse:     (String, DateTimeFormatter) => A
  ): String => A = str => parse(str, formatter)

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
