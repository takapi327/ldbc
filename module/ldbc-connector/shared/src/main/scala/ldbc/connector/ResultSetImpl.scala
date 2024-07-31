/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.time.*

import cats.*
import cats.syntax.all.*

import cats.effect.Ref

import ldbc.sql.{ ResultSet, ResultSetMetaData }

import ldbc.connector.util.Version
import ldbc.connector.exception.SQLException
import ldbc.connector.net.packet.response.*
import ldbc.connector.codec.all.*
import ldbc.connector.codec.Codec

/**
 * A table of data representing a database result set, which is usually generated by executing a statement that queries the database.
 */
private[ldbc] case class ResultSetImpl[F[_]](
  columns:                Vector[ColumnDefinitionPacket],
  records:                Vector[ResultSetRowPacket],
  serverVariables:        Map[String, String],
  version:                Version,
  isClosed:               Ref[F, Boolean],
  lastColumnReadNullable: Ref[F, Boolean],
  currentCursor:          Ref[F, Int],
  currentRow:             Ref[F, Option[ResultSetRowPacket]],
  resultSetType:          Int = ResultSet.TYPE_FORWARD_ONLY,
  resultSetConcurrency:   Int = ResultSet.CONCUR_READ_ONLY
)(using ev: MonadError[F, Throwable])
  extends ResultSet[F]:

  def next(): F[Boolean] =
    checkClose {
      currentCursor.get.flatMap { cursor =>
        if cursor <= records.size then
          currentRow.set(records.lift(cursor)) *>
            currentCursor.update(_ + 1) *>
            currentRow.get.map(_.isDefined)
        else currentCursor.update(_ + 1).as(false)
      }
    }

  override def close(): F[Unit] = isClosed.set(true)

  override def wasNull(): F[Boolean] = lastColumnReadNullable.get

  override def getString(columnIndex: Int): F[String] =
    checkClose {
      rowDecode(row => text.decode(columnIndex, List(row.values(columnIndex - 1))).toOption)
        .flatMap {
          case Some(value) => lastColumnReadNullable.set(false).as(value)
          case None        => lastColumnReadNullable.set(true).as(null)
        }
    }

  override def getBoolean(columnIndex: Int): F[Boolean] =
    checkClose {
      rowDecode(row => boolean.decode(columnIndex, List(row.values(columnIndex - 1))).toOption)
        .flatMap {
          case Some(value) => lastColumnReadNullable.set(false).as(value)
          case None        => lastColumnReadNullable.set(true).as(false)
        }
    }

  override def getByte(columnIndex: Int): F[Byte] =
    checkClose {
      rowDecode(row => bit.decode(columnIndex, List(row.values(columnIndex - 1))).toOption)
        .flatMap {
          case Some(value) => lastColumnReadNullable.set(false).as(value)
          case None        => lastColumnReadNullable.set(true).as(0)
        }
    }

  override def getShort(columnIndex: Int): F[Short] =
    checkClose {
      rowDecode(row => smallint.decode(columnIndex, List(row.values(columnIndex - 1))).toOption)
        .flatMap {
          case Some(value) => lastColumnReadNullable.set(false).as(value)
          case None        => lastColumnReadNullable.set(true).as(0)
        }
    }

  override def getInt(columnIndex: Int): F[Int] =
    checkClose {
      rowDecode(row => int.decode(columnIndex, List(row.values(columnIndex - 1))).toOption)
        .flatMap {
          case Some(value) => lastColumnReadNullable.set(false).as(value)
          case None        => lastColumnReadNullable.set(true).as(0)
        }
    }

  override def getLong(columnIndex: Int): F[Long] =
    checkClose {
      rowDecode(row => bigint.decode(columnIndex, List(row.values(columnIndex - 1))).toOption)
        .flatMap {
          case Some(value) => lastColumnReadNullable.set(false).as(value)
          case None        => lastColumnReadNullable.set(true).as(0L)
        }
    }

  override def getFloat(columnIndex: Int): F[Float] =
    checkClose {
      rowDecode(row => float.decode(columnIndex, List(row.values(columnIndex - 1))).toOption)
        .flatMap {
          case Some(value) => lastColumnReadNullable.set(false).as(value)
          case None        => lastColumnReadNullable.set(true).as(0f)
        }
    }

  override def getDouble(columnIndex: Int): F[Double] =
    checkClose {
      rowDecode(row => double.decode(columnIndex, List(row.values(columnIndex - 1))).toOption)
        .flatMap {
          case Some(value) => lastColumnReadNullable.set(false).as(value)
          case None        => lastColumnReadNullable.set(true).as(0.toDouble)
        }
    }

  override def getBytes(columnIndex: Int): F[Array[Byte]] =
    checkClose {
      rowDecode(row => binary(255).decode(columnIndex, List(row.values(columnIndex - 1))).toOption)
        .flatMap {
          case Some(value) => lastColumnReadNullable.set(false).as(value)
          case None        => lastColumnReadNullable.set(true).as(null)
        }
    }

  override def getDate(columnIndex: Int): F[LocalDate] =
    checkClose {
      rowDecode(row => date.decode(columnIndex, List(row.values(columnIndex - 1))).toOption)
        .flatMap {
          case Some(value) => lastColumnReadNullable.set(false).as(value)
          case None        => lastColumnReadNullable.set(true).as(null)
        }
    }

  override def getTime(columnIndex: Int): F[LocalTime] =
    checkClose {
      rowDecode(row => time.decode(columnIndex, List(row.values(columnIndex - 1))).toOption)
        .flatMap {
          case Some(value) => lastColumnReadNullable.set(false).as(value)
          case None        => lastColumnReadNullable.set(true).as(null)
        }
    }

  override def getTimestamp(columnIndex: Int): F[LocalDateTime] =
    checkClose {
      rowDecode(row => timestamp.decode(columnIndex, List(row.values(columnIndex - 1))).toOption)
        .flatMap {
          case Some(value) => lastColumnReadNullable.set(false).as(value)
          case None        => lastColumnReadNullable.set(true).as(null)
        }
    }

  override def getString(columnLabel: String): F[String] =
    checkClose {
      columns.zipWithIndex.find(_._1.name == columnLabel) match
        case Some((_, index)) => getString(index + 1)
        case None             => lastColumnReadNullable.set(true).as(null)
    }

  override def getBoolean(columnLabel: String): F[Boolean] =
    checkClose {
      columns.zipWithIndex.find(_._1.name == columnLabel) match
        case Some((_, index)) => getBoolean(index + 1)
        case None             => lastColumnReadNullable.set(true).as(false)
    }

  override def getByte(columnLabel: String): F[Byte] =
    checkClose {
      columns.zipWithIndex.find(_._1.name == columnLabel) match
        case Some((_, index)) => getByte(index + 1)
        case None             => lastColumnReadNullable.set(true).as(0)
    }

  override def getShort(columnLabel: String): F[Short] =
    checkClose {
      columns.zipWithIndex.find(_._1.name == columnLabel) match
        case Some((_, index)) => getShort(index + 1)
        case None             => lastColumnReadNullable.set(true).as(0)
    }

  override def getInt(columnLabel: String): F[Int] =
    checkClose {
      columns.zipWithIndex.find(_._1.name == columnLabel) match
        case Some((_, index)) => getInt(index + 1)
        case None             => lastColumnReadNullable.set(true).as(0)
    }

  override def getLong(columnLabel: String): F[Long] =
    checkClose {
      columns.zipWithIndex.find(_._1.name == columnLabel) match
        case Some((_, index)) => getLong(index + 1)
        case None             => lastColumnReadNullable.set(true).as(0L)
    }

  override def getFloat(columnLabel: String): F[Float] =
    checkClose {
      columns.zipWithIndex.find(_._1.name == columnLabel) match
        case Some((_, index)) => getFloat(index + 1)
        case None             => lastColumnReadNullable.set(true).as(0f)
    }

  override def getDouble(columnLabel: String): F[Double] =
    checkClose {
      columns.zipWithIndex.find(_._1.name == columnLabel) match
        case Some((_, index)) => getDouble(index + 1)
        case None             => lastColumnReadNullable.set(true).as(0.toDouble)
    }

  override def getBytes(columnLabel: String): F[Array[Byte]] =
    checkClose {
      columns.zipWithIndex.find(_._1.name == columnLabel) match
        case Some((_, index)) => getBytes(index + 1)
        case None             => lastColumnReadNullable.set(true).as(null)
    }

  override def getDate(columnLabel: String): F[LocalDate] =
    checkClose {
      columns.zipWithIndex.find(_._1.name == columnLabel) match
        case Some((_, index)) => getDate(index + 1)
        case None             => lastColumnReadNullable.set(true).as(null)
    }

  override def getTime(columnLabel: String): F[LocalTime] =
    checkClose {
      columns.zipWithIndex.find(_._1.name == columnLabel) match
        case Some((_, index)) => getTime(index + 1)
        case None             => lastColumnReadNullable.set(true).as(null)
    }

  override def getTimestamp(columnLabel: String): F[LocalDateTime] =
    checkClose {
      columns.zipWithIndex.find(_._1.name == columnLabel) match
        case Some((_, index)) => getTimestamp(index + 1)
        case None             => lastColumnReadNullable.set(true).as(null)
    }

  override def getMetaData(): F[ResultSetMetaData] =
    checkClose {
      ev.pure(ResultSetMetaDataImpl(columns, serverVariables, version))
    }

  override def getBigDecimal(columnIndex: Int): F[BigDecimal] =
    checkClose {
      rowDecode(row => decimal().decode(columnIndex, List(row.values(columnIndex - 1))).toOption)
        .flatMap {
          case Some(value) => lastColumnReadNullable.set(false).as(value)
          case None        => lastColumnReadNullable.set(true).as(null)
        }
    }

  override def getBigDecimal(columnLabel: String): F[BigDecimal] =
    checkClose {
      columns.zipWithIndex.find(_._1.name == columnLabel) match
        case Some((_, index)) => getBigDecimal(index + 1)
        case None             => lastColumnReadNullable.set(true).as(null)
    }

  override def isBeforeFirst(): F[Boolean] =
    currentCursor.get.map { cursor =>
      cursor <= 0 && records.nonEmpty
    }

  override def isAfterLast(): F[Boolean] =
    currentCursor.get.map { cursor =>
      cursor > records.size && records.nonEmpty
    }

  override def isFirst(): F[Boolean] =
    currentCursor.get.map { cursor =>
      cursor > 0
    }

  override def isLast(): F[Boolean] =
    currentCursor.get.map { cursor =>
      cursor == records.size
    }

  override def beforeFirst(): F[Unit] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      raiseError("Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")
    else currentCursor.set(0)

  override def afterLast(): F[Unit] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      raiseError("Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")
    else currentCursor.set(records.size + 1)

  override def first(): F[Boolean] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      raiseError("Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")
    else
      currentCursor.set(1) *>
        currentRow.set(records.headOption) *>
        currentRow.get.map(_.isDefined && records.nonEmpty)

  override def last(): F[Boolean] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      raiseError("Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")
    else
      currentCursor.set(records.size) *>
        currentRow.set(records.lastOption) *>
        currentRow.get.map(_.isDefined && records.nonEmpty)

  override def getRow(): F[Int] =
    currentCursor.get.map { cursor =>
      if cursor > records.size then 0
      else cursor
    }

  override def absolute(row: Int): F[Boolean] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      raiseError("Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")
    else if row > 0 then
      currentCursor.set(row) *>
        currentRow.set(records.lift(row - 1)).map(_ => row >= 1 && row <= records.size)
    else if row < 0 then
      val position = records.size + row + 1
      currentCursor.set(position) *>
        currentRow.set(records.lift(records.size + row)).map(_ => position >= 1 && position <= records.size)
    else
      currentCursor.set(0) *>
        currentRow.set(None).map(_ => false)

  override def relative(rows: Int): F[Boolean] =
    if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
      raiseError("Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")
    else
      currentCursor.get.flatMap { cursor =>
        val position = cursor + rows
        if position >= 1 && position <= records.size then
          currentCursor.set(position) *>
            currentRow.set(records.lift(position - 1)).map(_ => true)
        else
          currentCursor.updateAndGet(_ => 0).flatMap { updated =>
            currentRow.set(records.lift(updated)).map(_ => false)
          }
      }

  override def previous(): F[Boolean] =
    currentCursor.get.flatMap { cursor =>
      if resultSetType == ResultSet.TYPE_FORWARD_ONLY then
        raiseError("Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY.")
      else if cursor > 0 then
        currentCursor.updateAndGet(_ - 1).flatMap { updated =>
          currentRow.updateAndGet(_ => records.lift(updated - 1)).map(_.isDefined)
        }
      else
        currentCursor.set(0) *>
          currentRow.set(None).map(_ => false)
    }

  override def getType(): F[Int] =
    checkClose {
      ev.pure(resultSetType)
    }

  override def getConcurrency(): F[Int] =
    checkClose {
      ev.pure(resultSetConcurrency)
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
  def decode[T](codec: Codec[T]): F[List[T]] =
    checkClose {
      ev.point(
        records.flatMap(row => codec.decode(0, row.values.toList).toOption).toList
      )
    }

  /**
   * Does the result set contain rows, or is it the result of a DDL or DML statement?
   *
   * @return true if result set contains rows
   */
  def hasRows(): F[Boolean] =
    checkClose {
      ev.pure(records.nonEmpty)
    }

  /**
   * Returns the number of rows in this <code>ResultSet</code> object.
   *
   * @return
   *   the number of rows
   */
  def rowLength(): F[Int] =
    checkClose {
      ev.pure(records.size)
    }

  private def checkClose[T](f: => F[T]): F[T] =
    isClosed.get.flatMap { isClosed =>
      if isClosed then raiseError("Operation not allowed after ResultSet closed")
      else f
    }

  private def rowDecode[T](decode: ResultSetRowPacket => Option[T]): F[Option[T]] =
    currentRow.get.map(_.flatMap(decode))

  private def raiseError[T](message: String): F[T] =
    ev.raiseError(new SQLException(message))

private[ldbc] object ResultSetImpl:

  def apply[F[_]](
    columns:                Vector[ColumnDefinitionPacket],
    records:                Vector[ResultSetRowPacket],
    serverVariables:        Map[String, String],
    version:                Version,
    isClosed:               Ref[F, Boolean],
    lastColumnReadNullable: Ref[F, Boolean],
    currentCursor:          Ref[F, Int],
    currentRow:             Ref[F, Option[ResultSetRowPacket]]
  )(using MonadError[F, Throwable]): ResultSetImpl[F] =
    ResultSetImpl[F](
      columns,
      records,
      serverVariables,
      version,
      isClosed,
      lastColumnReadNullable,
      currentCursor,
      currentRow,
      ResultSet.TYPE_FORWARD_ONLY
    )

  def empty[F[_]](
    serverVariables:        Map[String, String],
    version:                Version,
    isClosed:               Ref[F, Boolean],
    lastColumnReadNullable: Ref[F, Boolean],
    currentCursor:          Ref[F, Int],
    currentRow:             Ref[F, Option[ResultSetRowPacket]]
  )(using MonadError[F, Throwable]): ResultSetImpl[F] =
    this.apply(
      Vector.empty,
      Vector.empty,
      serverVariables,
      version,
      isClosed,
      lastColumnReadNullable,
      currentCursor,
      currentRow
    )
