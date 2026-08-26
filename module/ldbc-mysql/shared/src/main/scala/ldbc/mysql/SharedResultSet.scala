/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import java.time.*

import ldbc.sql.{ ResultSet, ResultSetMetaData, SyncRow }
import ldbc.sql.SQLException

import ldbc.effect.{ Concurrent, Ref }
import ldbc.effect.syntax.*
import ldbc.mysql.data.*
import ldbc.mysql.net.packet.response.*
import ldbc.mysql.net.Protocol
import ldbc.mysql.util.Version

private[ldbc] trait SharedResultSet[F[_]] extends ResultSet[F]:

  /** The effect instance used by the shared result-set logic. Concrete result sets supply it. */
  protected def F: Concurrent[F]
  private given Concurrent[F] = F

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
  def decoder:              ColumnValueDecoder

  protected final var lastColumnReadNullable: Boolean                    = false
  protected final var currentCursor:          Int                        = 0
  protected final var currentRow:             Option[ResultSetRowPacket] = records.headOption

  private lazy val charsets:      Vector[String]         = columns.map(_.charset)
  private lazy val columnTypes:   Vector[ColumnDataType] = columns.map(_.columnType)
  private lazy val unsignedFlags: Vector[Boolean]        =
    columns.map(_.flags.contains(ColumnDefinitionFlags.UNSIGNED_FLAG))

  override def close(): F[Unit] = isClosed.set(true)

  override def wasNull(): F[Boolean] = F.pure(lastColumnReadNullable)

  override def getString(columnIndex: Int): F[String] =
    checkClosed() *> rowDecode[String](columnIndex, _.decodeString, null)

  override def getBoolean(columnIndex: Int): F[Boolean] =
    checkClosed() *> rowDecode[Boolean](columnIndex, _.decodeBoolean, false)

  override def getByte(columnIndex: Int): F[Byte] =
    checkClosed() *> rowDecode[Byte](columnIndex, _.decodeByte, 0)

  override def getShort(columnIndex: Int): F[Short] =
    checkClosed() *> rowDecode[Short](columnIndex, _.decodeShort, 0)

  override def getInt(columnIndex: Int): F[Int] =
    checkClosed() *> rowDecode[Int](columnIndex, _.decodeInt, 0)

  override def getLong(columnIndex: Int): F[Long] =
    checkClosed() *> rowDecode[Long](columnIndex, _.decodeLong, 0L)

  override def getFloat(columnIndex: Int): F[Float] =
    checkClosed() *> rowDecode[Float](columnIndex, _.decodeFloat, 0f)

  override def getDouble(columnIndex: Int): F[Double] =
    checkClosed() *> rowDecode[Double](columnIndex, _.decodeDouble, 0.0)

  override def getBytes(columnIndex: Int): F[Array[Byte]] =
    checkClosed() *> rowDecode[Array[Byte]](columnIndex, _.decodeBytes, null)

  override def getDate(columnIndex: Int): F[LocalDate] =
    checkClosed() *> rowDecode[LocalDate](columnIndex, _.decodeDate, null)

  override def getTime(columnIndex: Int): F[LocalTime] =
    checkClosed() *> rowDecode[LocalTime](columnIndex, _.decodeTime, null)

  override def getTimestamp(columnIndex: Int): F[LocalDateTime] =
    checkClosed() *> rowDecode[LocalDateTime](columnIndex, _.decodeTimestamp, null)

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
    checkClosed() *> rowDecode[BigDecimal](columnIndex, _.decodeBigDecimal, null)

  override def getBigDecimal(columnLabel: String): F[BigDecimal] =
    for
      index <- findByName(columnLabel)
      value <- getBigDecimal(index)
    yield value

  override def isBeforeFirst(): F[Boolean] =
    F.pure(currentCursor <= 0 && records.nonEmpty)

  override def isAfterLast(): F[Boolean] =
    F.pure(currentCursor > records.length && records.nonEmpty)

  override def isFirst(): F[Boolean] =
    F.pure(currentCursor == 1 && records.nonEmpty)

  override def isLast(): F[Boolean] =
    F.pure(currentCursor == records.length)

  override def beforeFirst(): F[Unit] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      F.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else
      currentCursor = 0
      F.unit

  override def afterLast(): F[Unit] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      F.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else
      currentCursor = records.length + 1
      F.unit

  override def first(): F[Boolean] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      F.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else
      currentCursor = 1
      currentRow    = records.headOption
      F.pure(currentRow.isDefined && records.nonEmpty)

  override def last(): F[Boolean] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      F.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else
      currentCursor = records.length
      currentRow    = records.lastOption
      F.pure(currentRow.isDefined && records.nonEmpty)

  override def getRow(): F[Int] =
    F.pure(
      if currentCursor > records.length then 0
      else currentCursor
    )

  override def absolute(row: Int): F[Boolean] =
    val recordSize = records.length
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      F.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else if row > 0 then
      currentCursor = row
      currentRow    = records.lift(row - 1)
      F.pure(row >= 1 && row <= recordSize)
    else if row < 0 then
      val position = recordSize + row + 1
      currentCursor = position
      currentRow    = records.lift(recordSize + row)
      F.pure(position >= 1 && position <= recordSize)
    else
      currentCursor = 0
      currentRow    = None
      F.pure(false)

  override def relative(rows: Int): F[Boolean] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      F.raiseError(
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
        F.pure(true)
      else
        currentCursor = 0
        currentRow    = records.lift(currentCursor)
        F.pure(false)

  override def previous(): F[Boolean] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      F.raiseError(
        new SQLException(
          "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.",
          sql = statement
        )
      )
    else if currentCursor > 0 then
      currentCursor = currentCursor - 1
      currentRow    = records.lift(currentCursor - 1)
      F.pure(currentRow.isDefined)
    else
      currentCursor = 0
      currentRow    = None
      F.pure(false)

  override def getType(): F[Int] =
    checkClosed() *> F.pure(resultSetType)

  override def getConcurrency(): F[Int] =
    checkClosed() *> F.pure(resultSetConcurrency)

  /**
   * Does the result set contain rows, or is it the result of a DDL or DML statement?
   *
   * @return true if result set contains rows
   */
  def hasRows(): F[Boolean] =
    checkClosed() *>
      F.pure(records.nonEmpty)

  /**
   * Returns the number of rows in this <code>ResultSet</code> object.
   *
   * @return
   *   the number of rows
   */
  def rowLength(): F[Int] =
    checkClosed() *>
      F.pure(records.length)

  /**
   * Decodes a value from the current row at the specified column index.
   * Extracts the raw field bytes from rawBytes and applies the appropriate decoder
   * based on the row's protocol type (text or binary).
   *
   * Column index validation is performed eagerly before decoding.
   * Decode errors are captured via `F.delay` and translated
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
  ): F[T] =
    if index < 1 || index > columns.length then
      F.raiseError(
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
          F.pure(defaultValue)
        case Some(bytes) =>
          lastColumnReadNullable = false
          F.delay(Option(extract(decoder)(bytes, charset, col.columnType, isUnsigned)))
            .flatMap {
              case None        => F.pure(defaultValue)
              case Some(value) => F.pure(value)
            }
            .handleErrorWith { e =>
              F.raiseError(
                new SQLException(
                  s"Cannot convert column $index value to the requested type: ${ e.getMessage }",
                  sqlState = Some("22018"),
                  sql      = statement
                )
              )
            }

  /**
   * Synchronous twin of [[rowDecode]]: decodes column `index` from the **given row** (not `currentRow`),
   * sharing the exact same range check, byte extraction, `lastColumnReadNullable` update, and value conversion.
   * Differs only in effect handling — it returns `T` directly and **throws** `SQLException` on out-of-range or
   * conversion errors. Intended to run inside a single enclosing effect (see [[BufferedSyncRow]] /
   * `foldRowsSync`), which turns any throwable into `F.raiseError`.
   */
  private[ldbc] def rowDecodeSync[T](
    row:          ResultSetRowPacket,
    index:        Int,
    extract:      ColumnValueDecoder => (Array[Byte], String, ColumnDataType, Boolean) => T,
    defaultValue: T
  ): T =
    if index < 1 || index > columns.length then
      throw new SQLException(
        s"Column index $index is out of range. Number of columns: ${ columns.length }.",
        sqlState = Some("S1009"),
        sql      = statement
      )
    else
      val col        = columns(index - 1)
      val charset    = charsets(index - 1)
      val isUnsigned = unsignedFlags(index - 1)

      decoder.extractColumn(row.rawBytes, index - 1, columnTypes) match
        case None =>
          lastColumnReadNullable = true
          defaultValue
        case Some(bytes) =>
          lastColumnReadNullable = false
          try Option(extract(decoder)(bytes, charset, col.columnType, isUnsigned)).getOrElse(defaultValue)
          catch
            case e: Throwable =>
              throw new SQLException(
                s"Cannot convert column $index value to the requested type: ${ e.getMessage }",
                sqlState = Some("22018"),
                sql      = statement
              )

  /**
   * A reusable [[SyncRow]] view over a single buffered row. Each accessor delegates to [[rowDecodeSync]] so the
   * conversion rules and `lastColumnReadNullable` bookkeeping stay identical to the effectful `getX` path.
   * `row` is repointed per iteration by `foldRowsSync` to avoid per-row allocation.
   */
  protected final class BufferedSyncRow extends SyncRow:
    var row: ResultSetRowPacket = null
    override def getString(i:     Int): String        = rowDecodeSync(row, i, _.decodeString, null)
    override def getBoolean(i:    Int): Boolean        = rowDecodeSync(row, i, _.decodeBoolean, false)
    override def getByte(i:       Int): Byte           = rowDecodeSync(row, i, _.decodeByte, 0)
    override def getShort(i:      Int): Short          = rowDecodeSync(row, i, _.decodeShort, 0)
    override def getInt(i:        Int): Int            = rowDecodeSync(row, i, _.decodeInt, 0)
    override def getLong(i:       Int): Long           = rowDecodeSync(row, i, _.decodeLong, 0L)
    override def getFloat(i:      Int): Float          = rowDecodeSync(row, i, _.decodeFloat, 0f)
    override def getDouble(i:     Int): Double         = rowDecodeSync(row, i, _.decodeDouble, 0.0)
    override def getBytes(i:      Int): Array[Byte]    = rowDecodeSync(row, i, _.decodeBytes, null)
    override def getDate(i:       Int): LocalDate      = rowDecodeSync(row, i, _.decodeDate, null)
    override def getTime(i:       Int): LocalTime      = rowDecodeSync(row, i, _.decodeTime, null)
    override def getTimestamp(i:  Int): LocalDateTime  = rowDecodeSync(row, i, _.decodeTimestamp, null)
    override def getBigDecimal(i: Int): BigDecimal     = rowDecodeSync(row, i, _.decodeBigDecimal, null)
    override def wasNull(): Boolean = lastColumnReadNullable

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
        F.raiseError(
          new SQLException(
            s"Column name '$columnLabel' does not exist in the ResultSet.",
            sql = statement
          )
        )
      case Some((_, index)) => F.pure(index + 1)

  /**
   * Checks if the result set is closed and throws an exception if it is.
   * Should be called at the beginning of most public methods.
   *
   * @return unit if the result set is open
   * @throws SQLException if the result set is closed
   */
  protected def checkClosed(): F[Unit] =
    isClosed.get.flatMap { closed =>
      if closed then F.raiseError(new SQLException(ResultSetImpl.CLOSED_MESSAGE, sql = statement))
      else F.pure(())
    }
