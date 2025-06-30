/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.free

import java.time.*

import cats.~>
import cats.free.Free
import cats.MonadThrow

import ldbc.sql.{ ResultSet, ResultSetMetaData }

sealed trait ResultSetOp[A]
object ResultSetOp:
  final case class Next()                                    extends ResultSetOp[Boolean]
  final case class Close()                                   extends ResultSetOp[Unit]
  final case class WasNull()                                 extends ResultSetOp[Boolean]
  final case class GetString(columnIndex: Int)               extends ResultSetOp[String]
  final case class GetBoolean(columnIndex: Int)              extends ResultSetOp[Boolean]
  final case class GetByte(columnIndex: Int)                 extends ResultSetOp[Byte]
  final case class GetShort(columnIndex: Int)                extends ResultSetOp[Short]
  final case class GetInt(columnIndex: Int)                  extends ResultSetOp[Int]
  final case class GetLong(columnIndex: Int)                 extends ResultSetOp[Long]
  final case class GetFloat(columnIndex: Int)                extends ResultSetOp[Float]
  final case class GetDouble(columnIndex: Int)               extends ResultSetOp[Double]
  final case class GetBytes(columnIndex: Int)                extends ResultSetOp[Array[Byte]]
  final case class GetDate(columnIndex: Int)                 extends ResultSetOp[LocalDate]
  final case class GetTime(columnIndex: Int)                 extends ResultSetOp[LocalTime]
  final case class GetTimestamp(columnIndex: Int)            extends ResultSetOp[LocalDateTime]
  final case class GetStringByLabel(columnLabel: String)     extends ResultSetOp[String]
  final case class GetBooleanByLabel(columnLabel: String)    extends ResultSetOp[Boolean]
  final case class GetByteByLabel(columnLabel: String)       extends ResultSetOp[Byte]
  final case class GetShortByLabel(columnLabel: String)      extends ResultSetOp[Short]
  final case class GetIntByLabel(columnLabel: String)        extends ResultSetOp[Int]
  final case class GetLongByLabel(columnLabel: String)       extends ResultSetOp[Long]
  final case class GetFloatByLabel(columnLabel: String)      extends ResultSetOp[Float]
  final case class GetDoubleByLabel(columnLabel: String)     extends ResultSetOp[Double]
  final case class GetBytesByLabel(columnLabel: String)      extends ResultSetOp[Array[Byte]]
  final case class GetDateByLabel(columnLabel: String)       extends ResultSetOp[LocalDate]
  final case class GetTimeByLabel(columnLabel: String)       extends ResultSetOp[LocalTime]
  final case class GetTimestampByLabel(columnLabel: String)  extends ResultSetOp[LocalDateTime]
  final case class GetMetaData()                             extends ResultSetOp[ResultSetMetaData]
  final case class GetBigDecimal(columnIndex: Int)           extends ResultSetOp[BigDecimal]
  final case class GetBigDecimalByLabel(columnLabel: String) extends ResultSetOp[BigDecimal]
  final case class IsBeforeFirst()                           extends ResultSetOp[Boolean]
  final case class IsFirst()                                 extends ResultSetOp[Boolean]
  final case class IsAfterLast()                             extends ResultSetOp[Boolean]
  final case class IsLast()                                  extends ResultSetOp[Boolean]
  final case class BeforeFirst()                             extends ResultSetOp[Unit]
  final case class AfterLast()                               extends ResultSetOp[Unit]
  final case class First()                                   extends ResultSetOp[Boolean]
  final case class Last()                                    extends ResultSetOp[Boolean]
  final case class GetRow()                                  extends ResultSetOp[Int]
  final case class Absolute(row: Int)                        extends ResultSetOp[Boolean]
  final case class Relative(rows: Int)                       extends ResultSetOp[Boolean]
  final case class Previous()                                extends ResultSetOp[Boolean]
  final case class GetType()                                 extends ResultSetOp[Int]
  final case class GetConcurrency()                          extends ResultSetOp[Int]
  final case class RaiseError[A](e: Throwable)               extends ResultSetOp[A]

type ResultSetIO[A] = Free[ResultSetOp, A]

object ResultSetIO:

  def pure[A](a:         A):         ResultSetIO[A] = Free.pure(a)
  def raiseError[A](err: Throwable): ResultSetIO[A] = Free.liftF(ResultSetOp.RaiseError(err))

  def next():                         ResultSetIO[Boolean]       = Free.liftF(ResultSetOp.Next())
  def close():                        ResultSetIO[Unit]          = Free.liftF(ResultSetOp.Close())
  def wasNull():                      ResultSetIO[Boolean]       = Free.liftF(ResultSetOp.WasNull())
  def getString(columnIndex:    Int): ResultSetIO[String]        = Free.liftF(ResultSetOp.GetString(columnIndex))
  def getBoolean(columnIndex:   Int): ResultSetIO[Boolean]       = Free.liftF(ResultSetOp.GetBoolean(columnIndex))
  def getByte(columnIndex:      Int): ResultSetIO[Byte]          = Free.liftF(ResultSetOp.GetByte(columnIndex))
  def getShort(columnIndex:     Int): ResultSetIO[Short]         = Free.liftF(ResultSetOp.GetShort(columnIndex))
  def getInt(columnIndex:       Int): ResultSetIO[Int]           = Free.liftF(ResultSetOp.GetInt(columnIndex))
  def getLong(columnIndex:      Int): ResultSetIO[Long]          = Free.liftF(ResultSetOp.GetLong(columnIndex))
  def getFloat(columnIndex:     Int): ResultSetIO[Float]         = Free.liftF(ResultSetOp.GetFloat(columnIndex))
  def getDouble(columnIndex:    Int): ResultSetIO[Double]        = Free.liftF(ResultSetOp.GetDouble(columnIndex))
  def getBytes(columnIndex:     Int): ResultSetIO[Array[Byte]]   = Free.liftF(ResultSetOp.GetBytes(columnIndex))
  def getDate(columnIndex:      Int): ResultSetIO[LocalDate]     = Free.liftF(ResultSetOp.GetDate(columnIndex))
  def getTime(columnIndex:      Int): ResultSetIO[LocalTime]     = Free.liftF(ResultSetOp.GetTime(columnIndex))
  def getTimestamp(columnIndex: Int): ResultSetIO[LocalDateTime] = Free.liftF(ResultSetOp.GetTimestamp(columnIndex))
  def getString(columnLabel:  String): ResultSetIO[String]      = Free.liftF(ResultSetOp.GetStringByLabel(columnLabel))
  def getBoolean(columnLabel: String): ResultSetIO[Boolean]     = Free.liftF(ResultSetOp.GetBooleanByLabel(columnLabel))
  def getByte(columnLabel:    String): ResultSetIO[Byte]        = Free.liftF(ResultSetOp.GetByteByLabel(columnLabel))
  def getShort(columnLabel:   String): ResultSetIO[Short]       = Free.liftF(ResultSetOp.GetShortByLabel(columnLabel))
  def getInt(columnLabel:     String): ResultSetIO[Int]         = Free.liftF(ResultSetOp.GetIntByLabel(columnLabel))
  def getLong(columnLabel:    String): ResultSetIO[Long]        = Free.liftF(ResultSetOp.GetLongByLabel(columnLabel))
  def getFloat(columnLabel:   String): ResultSetIO[Float]       = Free.liftF(ResultSetOp.GetFloatByLabel(columnLabel))
  def getDouble(columnLabel:  String): ResultSetIO[Double]      = Free.liftF(ResultSetOp.GetDoubleByLabel(columnLabel))
  def getBytes(columnLabel:   String): ResultSetIO[Array[Byte]] = Free.liftF(ResultSetOp.GetBytesByLabel(columnLabel))
  def getDate(columnLabel:    String): ResultSetIO[LocalDate]   = Free.liftF(ResultSetOp.GetDateByLabel(columnLabel))
  def getTime(columnLabel:    String): ResultSetIO[LocalTime]   = Free.liftF(ResultSetOp.GetTimeByLabel(columnLabel))
  def getTimestamp(columnLabel: String): ResultSetIO[LocalDateTime] =
    Free.liftF(ResultSetOp.GetTimestampByLabel(columnLabel))
  def getMetaData(): ResultSetIO[ResultSetMetaData] = Free.liftF(ResultSetOp.GetMetaData())
  def getBigDecimal(columnIndex: Int):    ResultSetIO[BigDecimal] = Free.liftF(ResultSetOp.GetBigDecimal(columnIndex))
  def getBigDecimal(columnLabel: String): ResultSetIO[BigDecimal] =
    Free.liftF(ResultSetOp.GetBigDecimalByLabel(columnLabel))
  def isBeforeFirst():     ResultSetIO[Boolean] = Free.liftF(ResultSetOp.IsBeforeFirst())
  def isFirst():           ResultSetIO[Boolean] = Free.liftF(ResultSetOp.IsFirst())
  def isAfterLast():       ResultSetIO[Boolean] = Free.liftF(ResultSetOp.IsAfterLast())
  def isLast():            ResultSetIO[Boolean] = Free.liftF(ResultSetOp.IsLast())
  def beforeFirst():       ResultSetIO[Unit]    = Free.liftF(ResultSetOp.BeforeFirst())
  def afterLast():         ResultSetIO[Unit]    = Free.liftF(ResultSetOp.AfterLast())
  def first():             ResultSetIO[Boolean] = Free.liftF(ResultSetOp.First())
  def last():              ResultSetIO[Boolean] = Free.liftF(ResultSetOp.Last())
  def getRow():            ResultSetIO[Int]     = Free.liftF(ResultSetOp.GetRow())
  def absolute(row:  Int): ResultSetIO[Boolean] = Free.liftF(ResultSetOp.Absolute(row))
  def relative(rows: Int): ResultSetIO[Boolean] = Free.liftF(ResultSetOp.Relative(rows))
  def previous():          ResultSetIO[Boolean] = Free.liftF(ResultSetOp.Previous())
  def getType():           ResultSetIO[Int]     = Free.liftF(ResultSetOp.GetType())
  def getConcurrency():    ResultSetIO[Int]     = Free.liftF(ResultSetOp.GetConcurrency())

  extension [F[_]: MonadThrow](resultSet: ResultSet[F])
    def interpreter: ResultSetOp ~> F =
      new (ResultSetOp ~> F):
        override def apply[A](fa: ResultSetOp[A]): F[A] = fa match
          case ResultSetOp.Next()                            => resultSet.next()
          case ResultSetOp.Close()                           => resultSet.close()
          case ResultSetOp.WasNull()                         => resultSet.wasNull()
          case ResultSetOp.GetString(columnIndex)            => resultSet.getString(columnIndex)
          case ResultSetOp.GetBoolean(columnIndex)           => resultSet.getBoolean(columnIndex)
          case ResultSetOp.GetByte(columnIndex)              => resultSet.getByte(columnIndex)
          case ResultSetOp.GetShort(columnIndex)             => resultSet.getShort(columnIndex)
          case ResultSetOp.GetInt(columnIndex)               => resultSet.getInt(columnIndex)
          case ResultSetOp.GetLong(columnIndex)              => resultSet.getLong(columnIndex)
          case ResultSetOp.GetFloat(columnIndex)             => resultSet.getFloat(columnIndex)
          case ResultSetOp.GetDouble(columnIndex)            => resultSet.getDouble(columnIndex)
          case ResultSetOp.GetBytes(columnIndex)             => resultSet.getBytes(columnIndex)
          case ResultSetOp.GetDate(columnIndex)              => resultSet.getDate(columnIndex)
          case ResultSetOp.GetTime(columnIndex)              => resultSet.getTime(columnIndex)
          case ResultSetOp.GetTimestamp(columnIndex)         => resultSet.getTimestamp(columnIndex)
          case ResultSetOp.GetStringByLabel(columnLabel)     => resultSet.getString(columnLabel)
          case ResultSetOp.GetBooleanByLabel(columnLabel)    => resultSet.getBoolean(columnLabel)
          case ResultSetOp.GetByteByLabel(columnLabel)       => resultSet.getByte(columnLabel)
          case ResultSetOp.GetShortByLabel(columnLabel)      => resultSet.getShort(columnLabel)
          case ResultSetOp.GetIntByLabel(columnLabel)        => resultSet.getInt(columnLabel)
          case ResultSetOp.GetLongByLabel(columnLabel)       => resultSet.getLong(columnLabel)
          case ResultSetOp.GetFloatByLabel(columnLabel)      => resultSet.getFloat(columnLabel)
          case ResultSetOp.GetDoubleByLabel(columnLabel)     => resultSet.getDouble(columnLabel)
          case ResultSetOp.GetBytesByLabel(columnLabel)      => resultSet.getBytes(columnLabel)
          case ResultSetOp.GetDateByLabel(columnLabel)       => resultSet.getDate(columnLabel)
          case ResultSetOp.GetTimeByLabel(columnLabel)       => resultSet.getTime(columnLabel)
          case ResultSetOp.GetTimestampByLabel(columnLabel)  => resultSet.getTimestamp(columnLabel)
          case ResultSetOp.GetMetaData()                     => resultSet.getMetaData()
          case ResultSetOp.GetBigDecimal(columnIndex)        => resultSet.getBigDecimal(columnIndex)
          case ResultSetOp.GetBigDecimalByLabel(columnLabel) => resultSet.getBigDecimal(columnLabel)
          case ResultSetOp.IsBeforeFirst()                   => resultSet.isBeforeFirst()
          case ResultSetOp.IsFirst()                         => resultSet.isFirst()
          case ResultSetOp.IsAfterLast()                     => resultSet.isAfterLast()
          case ResultSetOp.IsLast()                          => resultSet.isLast()
          case ResultSetOp.BeforeFirst()                     => resultSet.beforeFirst()
          case ResultSetOp.AfterLast()                       => resultSet.afterLast()
          case ResultSetOp.First()                           => resultSet.first()
          case ResultSetOp.Last()                            => resultSet.last()
          case ResultSetOp.GetRow()                          => resultSet.getRow()
          case ResultSetOp.Absolute(row)                     => resultSet.absolute(row)
          case ResultSetOp.Relative(rows)                    => resultSet.relative(rows)
          case ResultSetOp.Previous()                        => resultSet.previous()
          case ResultSetOp.GetType()                         => resultSet.getType()
          case ResultSetOp.GetConcurrency()                  => resultSet.getConcurrency()
          case ResultSetOp.RaiseError(e)                     => MonadThrow[F].raiseError(e)
