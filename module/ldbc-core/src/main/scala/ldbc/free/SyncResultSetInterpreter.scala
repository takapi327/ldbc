/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.free

import java.time.*

import cats.free.Free
import cats.Id

import ldbc.sql.{ ResultSetMetaData, SyncRow }

/**
 * Marker exception signalling that a decoder's [[ResultSetIO]] program contains an operation that cannot be
 * interpreted synchronously against a single buffered row (anything other than a by-index column read or
 * `wasNull`: `next`, `close`, by-label reads, cursor navigation, metadata access, `embed`, …).
 *
 * When [[syncResultSetInterpreter]] hits such an op it throws this, and the batch drain interpreter falls back
 * to the ordinary per-row effectful path. It never escapes to user code.
 */
final class NonSyncDecoderException(op: String)
  extends RuntimeException(s"ResultSet operation '$op' cannot be decoded synchronously; falling back")

/**
 * Interprets a **row decoder's** [[ResultSetOp]] program synchronously against one [[SyncRow]], with no effect.
 *
 * Only the by-index value reads (`getX(i)`) and `wasNull` — the ops a value `Decoder` actually emits — are
 * supported; every other op throws [[NonSyncDecoderException]] so the caller can retreat to the effectful path.
 * Because the result functor is `cats.Id`, `decode(...).foldMap(syncResultSetInterpreter(row))` runs the whole
 * row decode allocation-light and without a single `F` bind.
 */
def syncResultSetInterpreter(row: SyncRow): ResultSetOp.Visitor[Id] = new ResultSetOp.Visitor[Id]:

  override def getString(columnIndex:     Int): Id[String]        = row.getString(columnIndex)
  override def getBoolean(columnIndex:    Int): Id[Boolean]       = row.getBoolean(columnIndex)
  override def getByte(columnIndex:       Int): Id[Byte]          = row.getByte(columnIndex)
  override def getShort(columnIndex:      Int): Id[Short]         = row.getShort(columnIndex)
  override def getInt(columnIndex:        Int): Id[Int]           = row.getInt(columnIndex)
  override def getLong(columnIndex:       Int): Id[Long]          = row.getLong(columnIndex)
  override def getFloat(columnIndex:      Int): Id[Float]         = row.getFloat(columnIndex)
  override def getDouble(columnIndex:     Int): Id[Double]        = row.getDouble(columnIndex)
  override def getBytes(columnIndex:      Int): Id[Array[Byte]]   = row.getBytes(columnIndex)
  override def getDate(columnIndex:       Int): Id[LocalDate]     = row.getDate(columnIndex)
  override def getTime(columnIndex:       Int): Id[LocalTime]     = row.getTime(columnIndex)
  override def getTimestamp(columnIndex:  Int): Id[LocalDateTime] = row.getTimestamp(columnIndex)
  override def getBigDecimal(columnIndex: Int): Id[BigDecimal]    = row.getBigDecimal(columnIndex)
  override def wasNull():                       Id[Boolean]       = row.wasNull()

  override def embed[A](e:        Embedded[A]): Id[A] = throw new NonSyncDecoderException("embed")
  override def raiseError[A](err: Throwable):   Id[A] = throw err

  override def next():  Id[Boolean] = throw new NonSyncDecoderException("next")
  override def close(): Id[Unit]    = throw new NonSyncDecoderException("close")

  override def getString(columnLabel:  String): Id[String]      = throw new NonSyncDecoderException("getString(label)")
  override def getBoolean(columnLabel: String): Id[Boolean]     = throw new NonSyncDecoderException("getBoolean(label)")
  override def getByte(columnLabel:    String): Id[Byte]        = throw new NonSyncDecoderException("getByte(label)")
  override def getShort(columnLabel:   String): Id[Short]       = throw new NonSyncDecoderException("getShort(label)")
  override def getInt(columnLabel:     String): Id[Int]         = throw new NonSyncDecoderException("getInt(label)")
  override def getLong(columnLabel:    String): Id[Long]        = throw new NonSyncDecoderException("getLong(label)")
  override def getFloat(columnLabel:   String): Id[Float]       = throw new NonSyncDecoderException("getFloat(label)")
  override def getDouble(columnLabel:  String): Id[Double]      = throw new NonSyncDecoderException("getDouble(label)")
  override def getBytes(columnLabel:   String): Id[Array[Byte]] = throw new NonSyncDecoderException("getBytes(label)")
  override def getDate(columnLabel:    String): Id[LocalDate]   = throw new NonSyncDecoderException("getDate(label)")
  override def getTime(columnLabel:    String): Id[LocalTime]   = throw new NonSyncDecoderException("getTime(label)")
  override def getTimestamp(columnLabel: String): Id[LocalDateTime] = throw new NonSyncDecoderException(
    "getTimestamp(label)"
  )
  override def getBigDecimal(columnLabel: String): Id[BigDecimal] = throw new NonSyncDecoderException(
    "getBigDecimal(label)"
  )

  override def getMetaData():       Id[ResultSetMetaData] = throw new NonSyncDecoderException("getMetaData")
  override def isBeforeFirst():     Id[Boolean]           = throw new NonSyncDecoderException("isBeforeFirst")
  override def isFirst():           Id[Boolean]           = throw new NonSyncDecoderException("isFirst")
  override def isAfterLast():       Id[Boolean]           = throw new NonSyncDecoderException("isAfterLast")
  override def isLast():            Id[Boolean]           = throw new NonSyncDecoderException("isLast")
  override def beforeFirst():       Id[Unit]              = throw new NonSyncDecoderException("beforeFirst")
  override def afterLast():         Id[Unit]              = throw new NonSyncDecoderException("afterLast")
  override def first():             Id[Boolean]           = throw new NonSyncDecoderException("first")
  override def last():              Id[Boolean]           = throw new NonSyncDecoderException("last")
  override def getRow():            Id[Int]               = throw new NonSyncDecoderException("getRow")
  override def absolute(row:  Int): Id[Boolean]           = throw new NonSyncDecoderException("absolute")
  override def relative(rows: Int): Id[Boolean]           = throw new NonSyncDecoderException("relative")
  override def previous():          Id[Boolean]           = throw new NonSyncDecoderException("previous")
  override def getType():           Id[Int]               = throw new NonSyncDecoderException("getType")
  override def getConcurrency():    Id[Int]               = throw new NonSyncDecoderException("getConcurrency")
  override def drainRows[B](fallback: Free[ResultSetOp, B], zero: B, step: (B, SyncRow) => B): Id[B] =
    throw new NonSyncDecoderException("drainRows")
