/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.time.*

import cats.syntax.all.*
import cats.MonadThrow

import cats.effect.Ref

import ldbc.sql.{ ResultSet, ResultSetMetaData }

import ldbc.connector.data.CharsetMapping
import ldbc.connector.data.Formatter.*
import ldbc.connector.exception.SQLException
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.Protocol
import ldbc.connector.util.Version

private[ldbc] trait SharedResultSet[F[_]](using ev: MonadThrow[F]) extends ResultSet[F]:
  def protocol:             Protocol[F]
  def columns:              Vector[ColumnDefinitionPacket]
  def records:              Vector[ResultSetRowPacket]
  def serverVariables:      Map[String, String]
  def version:              Version
  def isClosed:             Ref[F, Boolean]
  def fetchSize:            Ref[F, Int]
  def useCursorFetch:       Boolean
  def useServerPrepStmts:   Boolean
  def resultSetType:        Int
  def resultSetConcurrency: Int
  def statement:            Option[String]

  protected final var lastColumnReadNullable: Boolean                    = false
  protected final var currentCursor:          Int                        = 0
  protected final var currentRow:             Option[ResultSetRowPacket] = records.headOption
  
  private lazy val charsets: Vector[String] = columns.map {
    case _: ColumnDefinition320Packet => "UTF-8"
    case column: ColumnDefinition41Packet => CharsetMapping.getJavaCharsetFromCollationIndex(column.characterSet)
  }

  override def close(): F[Unit] = isClosed.set(true)

  override def wasNull(): F[Boolean] = ev.pure(lastColumnReadNullable)

  override def getString(columnIndex: Int): F[String] =
    rowDecode[String](columnIndex, identity, null)

  override def getBoolean(columnIndex: Int): F[Boolean] =
    rowDecode[Boolean](
      columnIndex,
      {
        case "true" | "1" => true
        case _            => false
      },
      false
    )

  override def getByte(columnIndex: Int): F[Byte] =
    rowDecode[Byte](
      columnIndex,
      str => {
        if str.length == 1 && !str.forall(_.isDigit) then str.getBytes().head
        else str.toByte
      },
      0
    )

  override def getShort(columnIndex: Int): F[Short] =
    rowDecode[Short](columnIndex, _.toShort, 0)

  override def getInt(columnIndex: Int): F[Int] =
    rowDecode[Int](columnIndex, _.toInt, 0)

  override def getLong(columnIndex: Int): F[Long] =
    rowDecode[Long](
      columnIndex,
      _.toLong,
      0L
    )

  override def getFloat(columnIndex: Int): F[Float] =
    rowDecode[Float](
      columnIndex,
      _.toFloat,
      0f
    )

  override def getDouble(columnIndex: Int): F[Double] =
    rowDecode[Double](
      columnIndex,
      _.toDouble,
      0.0
    )

  override def getBytes(columnIndex: Int): F[Array[Byte]] = {
    val charset = charsets(columnIndex - 1)
    rowDecode[Array[Byte]](
      columnIndex,
      _.getBytes(charset),
      null
    )
  }

  override def getDate(columnIndex: Int): F[LocalDate] =
    rowDecode[LocalDate](
      columnIndex,
      str => LocalDate.parse(str, localDateFormatter),
      null
    )

  override def getTime(columnIndex: Int): F[LocalTime] =
    rowDecode[LocalTime](
      columnIndex,
      str => LocalTime.parse(str, timeFormatter(6)),
      null
    )

  override def getTimestamp(columnIndex: Int): F[LocalDateTime] =
    rowDecode[LocalDateTime](
      columnIndex,
      str => LocalDateTime.parse(str, localDateTimeFormatter(6)),
      null
    )

  override def getString(columnLabel: String): F[String] =
    for
      index <- findByName(columnLabel)
      value <- getString(index)
    yield value

  override def getBoolean(columnLabel: String): F[Boolean] =
    for
      index <- findByName(columnLabel)
      value <- getBoolean(index)
    yield value

  override def getByte(columnLabel: String): F[Byte] =
    for
      index <- findByName(columnLabel)
      value <- getByte(index)
    yield value

  override def getShort(columnLabel: String): F[Short] =
    for
      index <- findByName(columnLabel)
      value <- getShort(index)
    yield value

  override def getInt(columnLabel: String): F[Int] =
    for
      index <- findByName(columnLabel)
      value <- getInt(index)
    yield value

  override def getLong(columnLabel: String): F[Long] =
    for
      index <- findByName(columnLabel)
      value <- getLong(index)
    yield value

  override def getFloat(columnLabel: String): F[Float] =
    for
      index <- findByName(columnLabel)
      value <- getFloat(index)
    yield value

  override def getDouble(columnLabel: String): F[Double] =
    for
      index <- findByName(columnLabel)
      value <- getDouble(index)
    yield value

  override def getBytes(columnLabel: String): F[Array[Byte]] =
    for
      index <- findByName(columnLabel)
      value <- getBytes(index)
    yield value

  override def getDate(columnLabel: String): F[LocalDate] =
    for
      index <- findByName(columnLabel)
      value <- getDate(index)
    yield value

  override def getTime(columnLabel: String): F[LocalTime] =
    for
      index <- findByName(columnLabel)
      value <- getTime(index)
    yield value

  override def getTimestamp(columnLabel: String): F[LocalDateTime] =
    for
      index <- findByName(columnLabel)
      value <- getTimestamp(index)
    yield value

  override def getMetaData(): F[ResultSetMetaData] =
    checkClosed().map { _ =>
      ResultSetMetaDataImpl(columns, serverVariables, version)
    }

  override def getBigDecimal(columnIndex: Int): F[BigDecimal] =
    rowDecode[BigDecimal](
      columnIndex,
      str => BigDecimal(str),
      null
    )

  override def getBigDecimal(columnLabel: String): F[BigDecimal] =
    for
      index <- findByName(columnLabel)
      value <- getBigDecimal(index)
    yield value

  override def isBeforeFirst(): F[Boolean] =
    ev.pure(currentCursor <= 0 && records.nonEmpty)

  override def isAfterLast(): F[Boolean] =
    ev.pure(currentCursor > records.length && records.nonEmpty)

  override def isFirst(): F[Boolean] =
    ev.pure(currentCursor > 0)

  override def isLast(): F[Boolean] =
    ev.pure(currentCursor == records.length)

  override def beforeFirst(): F[Unit] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      ev.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else
      currentCursor = 0
      ev.unit

  override def afterLast(): F[Unit] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      ev.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else
      currentCursor = records.length + 1
      ev.unit

  override def first(): F[Boolean] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      ev.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else
      currentCursor = 1
      currentRow    = records.headOption
      ev.pure(currentRow.isDefined && records.nonEmpty)

  override def last(): F[Boolean] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      ev.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else
      currentCursor = records.length
      currentRow    = records.lastOption
      ev.pure(currentRow.isDefined && records.nonEmpty)

  override def getRow(): F[Int] =
    ev.pure(
      if currentCursor > records.length then 0
      else currentCursor
    )

  override def absolute(row: Int): F[Boolean] =
    val recordSize = records.length
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      ev.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else if row > 0 then
      currentCursor = row
      currentRow    = records.lift(row - 1)
      ev.pure(row >= 1 && row <= recordSize)
    else if row < 0 then
      val position = recordSize + row + 1
      currentCursor = position
      currentRow    = records.lift(recordSize + row)
      ev.pure(position >= 1 && position <= recordSize)
    else
      currentCursor = 0
      currentRow    = None
      ev.pure(false)

  override def relative(rows: Int): F[Boolean] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      ev.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else
      val position = currentCursor + rows
      if position >= 1 && position <= records.length then
        currentCursor = position
        currentRow    = records.lift(position - 1)
        ev.pure(true)
      else
        currentCursor = 0
        currentRow    = records.lift(currentCursor)
        ev.pure(false)

  override def previous(): F[Boolean] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      ev.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else if currentCursor > 0 then
      currentCursor = currentCursor - 1
      currentRow    = records.lift(currentCursor - 1)
      ev.pure(currentRow.isDefined)
    else
      currentCursor = 0
      currentRow    = None
      ev.pure(false)

  override def getType(): F[Int] =
    checkClosed() *> ev.pure(resultSetType)

  override def getConcurrency(): F[Int] =
    checkClosed() *> ev.pure(resultSetConcurrency)

  /**
   * Does the result set contain rows, or is it the result of a DDL or DML statement?
   *
   * @return true if result set contains rows
   */
  def hasRows(): F[Boolean] =
    checkClosed() *>
      ev.pure(records.nonEmpty)

  /**
   * Returns the number of rows in this <code>ResultSet</code> object.
   *
   * @return
   *   the number of rows
   */
  def rowLength(): F[Int] =
    checkClosed() *>
      ev.pure(records.length)

  /**
   * Decodes a value from the current row at the specified column index.
   * Handles null values and type conversion errors appropriately.
   *
   * @param index the 1-based column index
   * @param decode the function to decode the string value to the target type
   * @param defaultValue the default value to return for null columns
   * @return the decoded value or default value for null columns
   */
  private def rowDecode[T](index: Int, decode: String => T, defaultValue: T): F[T] =
    try {
      val decoded = for
        row     <- currentRow
        value   <- row.values(index - 1)
        decoded <- Option(decode(value))
      yield decoded

      decoded match
        case None =>
          lastColumnReadNullable = true
          ev.pure(defaultValue)
        case Some(decodedValue) =>
          lastColumnReadNullable = false
          ev.pure(decodedValue)
    } catch
      case _ =>
        ev.raiseError(
          new SQLException(
            s"Column index $index does not exist in the ResultSet.",
            sql = statement
          )
        )

  /**
   * Finds the column index by column name or alias.
   * Performs case-insensitive matching against both name and full name.
   *
   * @param columnLabel the column name or alias to search for
   * @return the 1-based column index if found
   * @throws SQLException if the column name is not found
   */
  private def findByName(columnLabel: String): F[Int] =
    val column = columns.zipWithIndex
      .find { (column: ColumnDefinitionPacket, _) =>
        column.name.equalsIgnoreCase(columnLabel) || column.fullName.equalsIgnoreCase(columnLabel)
      }
    column match
      case None =>
        ev.raiseError(
          new SQLException(
            s"Column name '$columnLabel' does not exist in the ResultSet.",
            sql = statement
          )
        )
      case Some((_, index)) => ev.pure(index + 1)

  /**
   * Checks if the result set is closed and throws an exception if it is.
   * Should be called at the beginning of most public methods.
   *
   * @return unit if the result set is open
   * @throws SQLException if the result set is closed
   */
  protected def checkClosed(): F[Unit] =
    isClosed.get.flatMap { closed =>
      if closed then ev.raiseError(new SQLException(ResultSetImpl.CLOSED_MESSAGE, sql = statement))
      else ().pure[F]
    }
