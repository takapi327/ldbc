/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import java.time.*

import ldbc.sql.{ ResultSet, ResultSetMetaData }
import ldbc.sql.SQLException

import ldbc.fx.{ Fx, Ref }
import ldbc.fx.syntax.*
import ldbc.mysql.data.*
import ldbc.mysql.net.packet.response.*
import ldbc.mysql.net.Protocol
import ldbc.mysql.util.Version

private[ldbc] trait SharedResultSet extends ResultSet[Fx]:
  def protocol:             Protocol
  def columns:              Vector[ColumnDefinitionPacket]
  def records:              Vector[ResultSetRowPacket]
  def serverVariables:      Map[String, String]
  def version:              Version
  def isClosed:             Ref[Boolean]
  def fetchSize:            Ref[Int]
  def useCursorFetch:       Boolean
  def useServerPrepStmts:   Boolean
  def resultSetType:        Int
  def resultSetConcurrency: Int
  def statement:            Option[String]
  def decoder:              ColumnValueDecoder

  protected final var lastColumnReadNullable: Boolean                    = false
  protected final var currentCursor:          Int                        = 0
  protected final var currentRow:             Option[ResultSetRowPacket] = records.headOption

  private lazy val charsets:      Vector[String]         = columns.map(_.charset)
  private lazy val columnTypes:   Vector[ColumnDataType] = columns.map(_.columnType)
  private lazy val unsignedFlags: Vector[Boolean]        =
    columns.map(_.flags.contains(ColumnDefinitionFlags.UNSIGNED_FLAG))

  override def close(): Fx[Unit] = isClosed.set(true)

  override def wasNull(): Fx[Boolean] = Fx.pure(lastColumnReadNullable)

  override def getString(columnIndex: Int): Fx[String] =
    checkClosed() *> rowDecode[String](columnIndex, _.decodeString, null)

  override def getBoolean(columnIndex: Int): Fx[Boolean] =
    checkClosed() *> rowDecode[Boolean](columnIndex, _.decodeBoolean, false)

  override def getByte(columnIndex: Int): Fx[Byte] =
    checkClosed() *> rowDecode[Byte](columnIndex, _.decodeByte, 0)

  override def getShort(columnIndex: Int): Fx[Short] =
    checkClosed() *> rowDecode[Short](columnIndex, _.decodeShort, 0)

  override def getInt(columnIndex: Int): Fx[Int] =
    checkClosed() *> rowDecode[Int](columnIndex, _.decodeInt, 0)

  override def getLong(columnIndex: Int): Fx[Long] =
    checkClosed() *> rowDecode[Long](columnIndex, _.decodeLong, 0L)

  override def getFloat(columnIndex: Int): Fx[Float] =
    checkClosed() *> rowDecode[Float](columnIndex, _.decodeFloat, 0f)

  override def getDouble(columnIndex: Int): Fx[Double] =
    checkClosed() *> rowDecode[Double](columnIndex, _.decodeDouble, 0.0)

  override def getBytes(columnIndex: Int): Fx[Array[Byte]] =
    checkClosed() *> rowDecode[Array[Byte]](columnIndex, _.decodeBytes, null)

  override def getDate(columnIndex: Int): Fx[LocalDate] =
    checkClosed() *> rowDecode[LocalDate](columnIndex, _.decodeDate, null)

  override def getTime(columnIndex: Int): Fx[LocalTime] =
    checkClosed() *> rowDecode[LocalTime](columnIndex, _.decodeTime, null)

  override def getTimestamp(columnIndex: Int): Fx[LocalDateTime] =
    checkClosed() *> rowDecode[LocalDateTime](columnIndex, _.decodeTimestamp, null)

  override def getString(columnLabel: String): Fx[String] =
    for
      index <- findByName(columnLabel)
      value <- getString(index)
    yield value

  override def getBoolean(columnLabel: String): Fx[Boolean] =
    for
      index <- findByName(columnLabel)
      value <- getBoolean(index)
    yield value

  override def getByte(columnLabel: String): Fx[Byte] =
    for
      index <- findByName(columnLabel)
      value <- getByte(index)
    yield value

  override def getShort(columnLabel: String): Fx[Short] =
    for
      index <- findByName(columnLabel)
      value <- getShort(index)
    yield value

  override def getInt(columnLabel: String): Fx[Int] =
    for
      index <- findByName(columnLabel)
      value <- getInt(index)
    yield value

  override def getLong(columnLabel: String): Fx[Long] =
    for
      index <- findByName(columnLabel)
      value <- getLong(index)
    yield value

  override def getFloat(columnLabel: String): Fx[Float] =
    for
      index <- findByName(columnLabel)
      value <- getFloat(index)
    yield value

  override def getDouble(columnLabel: String): Fx[Double] =
    for
      index <- findByName(columnLabel)
      value <- getDouble(index)
    yield value

  override def getBytes(columnLabel: String): Fx[Array[Byte]] =
    for
      index <- findByName(columnLabel)
      value <- getBytes(index)
    yield value

  override def getDate(columnLabel: String): Fx[LocalDate] =
    for
      index <- findByName(columnLabel)
      value <- getDate(index)
    yield value

  override def getTime(columnLabel: String): Fx[LocalTime] =
    for
      index <- findByName(columnLabel)
      value <- getTime(index)
    yield value

  override def getTimestamp(columnLabel: String): Fx[LocalDateTime] =
    for
      index <- findByName(columnLabel)
      value <- getTimestamp(index)
    yield value

  override def getMetaData(): Fx[ResultSetMetaData] =
    checkClosed().map { _ =>
      ResultSetMetaDataImpl(columns, serverVariables, version)
    }

  override def getBigDecimal(columnIndex: Int): Fx[BigDecimal] =
    checkClosed() *> rowDecode[BigDecimal](columnIndex, _.decodeBigDecimal, null)

  override def getBigDecimal(columnLabel: String): Fx[BigDecimal] =
    for
      index <- findByName(columnLabel)
      value <- getBigDecimal(index)
    yield value

  override def isBeforeFirst(): Fx[Boolean] =
    Fx.pure(currentCursor <= 0 && records.nonEmpty)

  override def isAfterLast(): Fx[Boolean] =
    Fx.pure(currentCursor > records.length && records.nonEmpty)

  override def isFirst(): Fx[Boolean] =
    Fx.pure(currentCursor == 1 && records.nonEmpty)

  override def isLast(): Fx[Boolean] =
    Fx.pure(currentCursor == records.length)

  override def beforeFirst(): Fx[Unit] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      Fx.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else
      currentCursor = 0
      Fx.unit

  override def afterLast(): Fx[Unit] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      Fx.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else
      currentCursor = records.length + 1
      Fx.unit

  override def first(): Fx[Boolean] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      Fx.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else
      currentCursor = 1
      currentRow    = records.headOption
      Fx.pure(currentRow.isDefined && records.nonEmpty)

  override def last(): Fx[Boolean] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      Fx.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else
      currentCursor = records.length
      currentRow    = records.lastOption
      Fx.pure(currentRow.isDefined && records.nonEmpty)

  override def getRow(): Fx[Int] =
    Fx.pure(
      if currentCursor > records.length then 0
      else currentCursor
    )

  override def absolute(row: Int): Fx[Boolean] =
    val recordSize = records.length
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      Fx.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else if row > 0 then
      currentCursor = row
      currentRow    = records.lift(row - 1)
      Fx.pure(row >= 1 && row <= recordSize)
    else if row < 0 then
      val position = recordSize + row + 1
      currentCursor = position
      currentRow    = records.lift(recordSize + row)
      Fx.pure(position >= 1 && position <= recordSize)
    else
      currentCursor = 0
      currentRow    = None
      Fx.pure(false)

  override def relative(rows: Int): Fx[Boolean] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      Fx.raiseError(
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
        Fx.pure(true)
      else
        currentCursor = 0
        currentRow    = records.lift(currentCursor)
        Fx.pure(false)

  override def previous(): Fx[Boolean] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      Fx.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else if currentCursor > 0 then
      currentCursor = currentCursor - 1
      currentRow    = records.lift(currentCursor - 1)
      Fx.pure(currentRow.isDefined)
    else
      currentCursor = 0
      currentRow    = None
      Fx.pure(false)

  override def getType(): Fx[Int] =
    checkClosed() *> Fx.pure(resultSetType)

  override def getConcurrency(): Fx[Int] =
    checkClosed() *> Fx.pure(resultSetConcurrency)

  /**
   * Does the result set contain rows, or is it the result of a DDL or DML statement?
   *
   * @return true if result set contains rows
   */
  def hasRows(): Fx[Boolean] =
    checkClosed() *>
      Fx.pure(records.nonEmpty)

  /**
   * Returns the number of rows in this <code>ResultSet</code> object.
   *
   * @return
   *   the number of rows
   */
  def rowLength(): Fx[Int] =
    checkClosed() *>
      Fx.pure(records.length)

  /**
   * Decodes a value from the current row at the specified column index.
   * Extracts the raw field bytes from rawBytes and applies the appropriate decoder
   * based on the row's protocol type (text or binary).
   *
   * Column index validation is performed eagerly before decoding.
   * Decode errors are captured via `Fx.delay` and translated
   * to `SQLException` with appropriate SQL states:
   *   - `S1009`: column index out of range
   *   - `22018`: value conversion / parse failure
   *
   * @param index the 1-based column index
   * @param extract a function selecting the decode method from ColumnValueDecoder
   * @param defaultValue the default value to return for null columns
   * @return the decoded value or default value for null columns
   */
  private def rowDecode[T](
    index:        Int,
    extract:      ColumnValueDecoder => (Array[Byte], String, ColumnDataType, Boolean) => T,
    defaultValue: T
  ): Fx[T] =
    if index < 1 || index > columns.length then
      Fx.raiseError(
        new SQLException(
          s"Column index $index is out of range. Number of columns: ${ columns.length }.",
          sqlState = Some("S1009"),
          sql      = statement
        )
      )
    else
      val col        = columns(index - 1)
      val charset    = charsets(index - 1)
      val isUnsigned = unsignedFlags(index - 1)
      val fieldBytes = currentRow.flatMap(row => decoder.extractColumn(row.rawBytes, index - 1, columnTypes))

      fieldBytes match
        case None =>
          lastColumnReadNullable = true
          Fx.pure(defaultValue)
        case Some(bytes) =>
          lastColumnReadNullable = false
          Fx.delay(Option(extract(decoder)(bytes, charset, col.columnType, isUnsigned)))
            .flatMap {
              case None        => Fx.pure(defaultValue)
              case Some(value) => Fx.pure(value)
            }
            .handleErrorWith { e =>
              Fx.raiseError(
                new SQLException(
                  s"Cannot convert column $index value to the requested type: ${ e.getMessage }",
                  sqlState = Some("22018"),
                  sql      = statement
                )
              )
            }

  /**
   * Finds the column index by column name or alias.
   * Performs case-insensitive matching against both name and full name.
   *
   * @param columnLabel the column name or alias to search for
   * @return the 1-based column index if found
   * @throws SQLException if the column name is not found
   */
  private def findByName(columnLabel: String): Fx[Int] =
    val column = columns.zipWithIndex
      .find { (column: ColumnDefinitionPacket, _) =>
        column.name.equalsIgnoreCase(columnLabel) || column.fullName.equalsIgnoreCase(columnLabel)
      }
    column match
      case None =>
        Fx.raiseError(
          new SQLException(
            s"Column name '$columnLabel' does not exist in the ResultSet.",
            sql = statement
          )
        )
      case Some((_, index)) => Fx.pure(index + 1)

  /**
   * Checks if the result set is closed and throws an exception if it is.
   * Should be called at the beginning of most public methods.
   *
   * @return unit if the result set is open
   * @throws SQLException if the result set is closed
   */
  protected def checkClosed(): Fx[Unit] =
    isClosed.get.flatMap { closed =>
      if closed then Fx.raiseError(new SQLException(ResultSetImpl.CLOSED_MESSAGE, sql = statement))
      else Fx.pure(())
    }
