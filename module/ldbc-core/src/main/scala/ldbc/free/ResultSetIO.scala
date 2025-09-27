/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.free

import java.time.*

import cats.~>
import cats.free.Free

import ldbc.sql.{ ResultSet, ResultSetMetaData }

sealed trait ResultSetOp[A]:
  def visit[F[_]](v: ResultSetOp.Visitor[F]): F[A]

object ResultSetOp:
  final case class Embed[A](e: Embedded[A]) extends ResultSetOp[A]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[A] = v.embed(e)
  final case class Next() extends ResultSetOp[Boolean]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Boolean] = v.next()
  final case class Close() extends ResultSetOp[Unit]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Unit] = v.close()
  final case class WasNull() extends ResultSetOp[Boolean]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Boolean] = v.wasNull()
  final case class GetString(columnIndex: Int) extends ResultSetOp[String]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[String] = v.getString(columnIndex)
  final case class GetBoolean(columnIndex: Int) extends ResultSetOp[Boolean]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Boolean] = v.getBoolean(columnIndex)
  final case class GetByte(columnIndex: Int) extends ResultSetOp[Byte]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Byte] = v.getByte(columnIndex)
  final case class GetShort(columnIndex: Int) extends ResultSetOp[Short]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Short] = v.getShort(columnIndex)
  final case class GetInt(columnIndex: Int) extends ResultSetOp[Int]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Int] = v.getInt(columnIndex)
  final case class GetLong(columnIndex: Int) extends ResultSetOp[Long]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Long] = v.getLong(columnIndex)
  final case class GetFloat(columnIndex: Int) extends ResultSetOp[Float]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Float] = v.getFloat(columnIndex)
  final case class GetDouble(columnIndex: Int) extends ResultSetOp[Double]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Double] = v.getDouble(columnIndex)
  final case class GetBytes(columnIndex: Int) extends ResultSetOp[Array[Byte]]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Array[Byte]] = v.getBytes(columnIndex)
  final case class GetDate(columnIndex: Int) extends ResultSetOp[LocalDate]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[LocalDate] = v.getDate(columnIndex)
  final case class GetTime(columnIndex: Int) extends ResultSetOp[LocalTime]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[LocalTime] = v.getTime(columnIndex)
  final case class GetTimestamp(columnIndex: Int) extends ResultSetOp[LocalDateTime]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[LocalDateTime] = v.getTimestamp(columnIndex)
  final case class GetStringByLabel(columnLabel: String) extends ResultSetOp[String]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[String] = v.getString(columnLabel)
  final case class GetBooleanByLabel(columnLabel: String) extends ResultSetOp[Boolean]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Boolean] = v.getBoolean(columnLabel)
  final case class GetByteByLabel(columnLabel: String) extends ResultSetOp[Byte]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Byte] = v.getByte(columnLabel)
  final case class GetShortByLabel(columnLabel: String) extends ResultSetOp[Short]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Short] = v.getShort(columnLabel)
  final case class GetIntByLabel(columnLabel: String) extends ResultSetOp[Int]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Int] = v.getInt(columnLabel)
  final case class GetLongByLabel(columnLabel: String) extends ResultSetOp[Long]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Long] = v.getLong(columnLabel)
  final case class GetFloatByLabel(columnLabel: String) extends ResultSetOp[Float]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Float] = v.getFloat(columnLabel)
  final case class GetDoubleByLabel(columnLabel: String) extends ResultSetOp[Double]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Double] = v.getDouble(columnLabel)
  final case class GetBytesByLabel(columnLabel: String) extends ResultSetOp[Array[Byte]]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Array[Byte]] = v.getBytes(columnLabel)
  final case class GetDateByLabel(columnLabel: String) extends ResultSetOp[LocalDate]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[LocalDate] = v.getDate(columnLabel)
  final case class GetTimeByLabel(columnLabel: String) extends ResultSetOp[LocalTime]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[LocalTime] = v.getTime(columnLabel)
  final case class GetTimestampByLabel(columnLabel: String) extends ResultSetOp[LocalDateTime]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[LocalDateTime] = v.getTimestamp(columnLabel)
  final case class GetMetaData() extends ResultSetOp[ResultSetMetaData]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[ResultSetMetaData] = v.getMetaData()
  final case class GetBigDecimal(columnIndex: Int) extends ResultSetOp[BigDecimal]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[BigDecimal] = v.getBigDecimal(columnIndex)
  final case class GetBigDecimalByLabel(columnLabel: String) extends ResultSetOp[BigDecimal]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[BigDecimal] = v.getBigDecimal(columnLabel)
  final case class IsBeforeFirst() extends ResultSetOp[Boolean]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Boolean] = v.isBeforeFirst()
  final case class IsFirst() extends ResultSetOp[Boolean]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Boolean] = v.isFirst()
  final case class IsAfterLast() extends ResultSetOp[Boolean]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Boolean] = v.isAfterLast()
  final case class IsLast() extends ResultSetOp[Boolean]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Boolean] = v.isLast()
  final case class BeforeFirst() extends ResultSetOp[Unit]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Unit] = v.beforeFirst()
  final case class AfterLast() extends ResultSetOp[Unit]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Unit] = v.afterLast()
  final case class First() extends ResultSetOp[Boolean]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Boolean] = v.first()
  final case class Last() extends ResultSetOp[Boolean]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Boolean] = v.last()
  final case class GetRow() extends ResultSetOp[Int]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Int] = v.getRow()
  final case class Absolute(row: Int) extends ResultSetOp[Boolean]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Boolean] = v.absolute(row)
  final case class Relative(rows: Int) extends ResultSetOp[Boolean]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Boolean] = v.relative(rows)
  final case class Previous() extends ResultSetOp[Boolean]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Boolean] = v.previous()
  final case class GetType() extends ResultSetOp[Int]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Int] = v.getType()
  final case class GetConcurrency() extends ResultSetOp[Int]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[Int] = v.getConcurrency()
  final case class RaiseError[A](e: Throwable) extends ResultSetOp[A]:
    override def visit[F[_]](v: ResultSetOp.Visitor[F]): F[A] = v.raiseError(e)

  given Embeddable[ResultSetOp, ResultSet[?]] =
    new Embeddable[ResultSetOp, ResultSet[?]]:
      override def embed[A](j: ResultSet[?], fa: Free[ResultSetOp, A]): Embedded.ResultSet[?, A] =
        Embedded.ResultSet(j, fa)

  trait Visitor[F[_]] extends (ResultSetOp ~> F):
    final def apply[A](fa: ResultSetOp[A]): F[A] = fa.visit(this)

    def embed[A](e:        Embedded[A]): F[A]
    def raiseError[A](err: Throwable):   F[A]

    def next():                             F[Boolean]
    def close():                            F[Unit]
    def wasNull():                          F[Boolean]
    def getString(columnIndex:     Int):    F[String]
    def getBoolean(columnIndex:    Int):    F[Boolean]
    def getByte(columnIndex:       Int):    F[Byte]
    def getShort(columnIndex:      Int):    F[Short]
    def getInt(columnIndex:        Int):    F[Int]
    def getLong(columnIndex:       Int):    F[Long]
    def getFloat(columnIndex:      Int):    F[Float]
    def getDouble(columnIndex:     Int):    F[Double]
    def getBytes(columnIndex:      Int):    F[Array[Byte]]
    def getDate(columnIndex:       Int):    F[LocalDate]
    def getTime(columnIndex:       Int):    F[LocalTime]
    def getTimestamp(columnIndex:  Int):    F[LocalDateTime]
    def getString(columnLabel:     String): F[String]
    def getBoolean(columnLabel:    String): F[Boolean]
    def getByte(columnLabel:       String): F[Byte]
    def getShort(columnLabel:      String): F[Short]
    def getInt(columnLabel:        String): F[Int]
    def getLong(columnLabel:       String): F[Long]
    def getFloat(columnLabel:      String): F[Float]
    def getDouble(columnLabel:     String): F[Double]
    def getBytes(columnLabel:      String): F[Array[Byte]]
    def getDate(columnLabel:       String): F[LocalDate]
    def getTime(columnLabel:       String): F[LocalTime]
    def getTimestamp(columnLabel:  String): F[LocalDateTime]
    def getMetaData():                      F[ResultSetMetaData]
    def getBigDecimal(columnIndex: Int):    F[BigDecimal]
    def getBigDecimal(columnLabel: String): F[BigDecimal]
    def isBeforeFirst():                    F[Boolean]
    def isFirst():                          F[Boolean]
    def isAfterLast():                      F[Boolean]
    def isLast():                           F[Boolean]
    def beforeFirst():                      F[Unit]
    def afterLast():                        F[Unit]
    def first():                            F[Boolean]
    def last():                             F[Boolean]
    def getRow():                           F[Int]
    def absolute(row:              Int):    F[Boolean]
    def relative(rows:             Int):    F[Boolean]
    def previous():                         F[Boolean]
    def getType():                          F[Int]
    def getConcurrency():                   F[Int]

type ResultSetIO[A] = Free[ResultSetOp, A]

object ResultSetIO:
  module =>

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
