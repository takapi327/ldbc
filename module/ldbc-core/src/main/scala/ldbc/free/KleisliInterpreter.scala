/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.free

import java.time.*

import scala.concurrent.duration.FiniteDuration

import cats.~>
import cats.data.Kleisli
import cats.free.Free

import cats.effect.kernel.{ Poll, Sync }

import ldbc.sql.*

import ldbc.logging.*

/**
 * This code is based on doobie's code.
 * @see https://github.com/typelevel/doobie/blob/main/modules/free/src/main/scala/doobie/free/kleisliinterpreter.scala
 */
class KleisliInterpreter[F[_]: Sync](logHandler: LogHandler[F]) extends Interpreter[F]:
  outer =>

  lazy val ConnectionInterpreter: ConnectionOp ~> ([A] =>> Kleisli[F, Connection[F], A]) = new ConnectionInterpreter {}
  lazy val StatementInterpreter:  StatementOp ~> ([A] =>> Kleisli[F, Statement[F], A])   = new StatementInterpreter {}
  lazy val PreparedStatementInterpreter: PreparedStatementOp ~> ([A] =>> Kleisli[F, PreparedStatement[F], A]) =
    new PreparedStatementInterpreter {}
  lazy val ResultSetInterpreter: ResultSetOp ~> ([A] =>> Kleisli[F, ResultSet[F], A]) = new ResultSetInterpreter {}

  def primitive[J, A](f:  J => F[A]): Kleisli[F, J, A]              = Kleisli(f)
  def raiseError[J, A](e: Throwable): Kleisli[F, J, A]              = Kleisli(_ => Sync[F].raiseError(e))
  def monotonic[J]:                   Kleisli[F, J, FiniteDuration] = Kleisli(_ => Sync[F].monotonic)
  def realTime[J]:                    Kleisli[F, J, FiniteDuration] = Kleisli(_ => Sync[F].realTime)
  def suspend[J, A](hint: Sync.Type)(thunk: => A): Kleisli[F, J, A]    = Kleisli(_ => Sync[F].suspend(hint)(thunk))
  def canceled[J]:                                 Kleisli[F, J, Unit] = Kleisli(_ => Sync[F].canceled)
  def handleErrorWith[G[_], J, A](
    interpreter: G ~> ([T] =>> Kleisli[F, J, T])
  )(fa: Free[G, A])(f: Throwable => Free[G, A]): Kleisli[F, J, A] =
    Kleisli(j => Sync[F].handleErrorWith(fa.foldMap(interpreter).run(j))(f.andThen(_.foldMap(interpreter).run(j))))
  def forceR[G[_], J, A, B](interpreter: G ~> ([T] =>> Kleisli[F, J, T]))(fa: Free[G, A])(
    fb: Free[G, B]
  ): Kleisli[F, J, B] = Kleisli(j => Sync[F].forceR(fa.foldMap(interpreter).run(j))(fb.foldMap(interpreter).run(j)))
  def uncancelable[G[_], J, A](
    interpreter: G ~> ([T] =>> Kleisli[F, J, T]),
    capture:     Poll[F] => Poll[[T] =>> Free[G, T]]
  )(body: Poll[[T] =>> Free[G, T]] => Free[G, A]): Kleisli[F, J, A] =
    Kleisli(j => Sync[F].uncancelable(body.compose(capture).andThen(_.foldMap(interpreter).run(j))))
  def poll[G[_], J, A](interpreter: G ~> ([T] =>> Kleisli[F, J, T]))(mpoll: Any, fa: Free[G, A]): Kleisli[F, J, A] =
    Kleisli(j => mpoll.asInstanceOf[Poll[F]].apply(fa.foldMap(interpreter).run(j)))
  def onCancel[G[_], J, A](
    interpreter: G ~> ([T] =>> Kleisli[F, J, T])
  )(fa: Free[G, A], fin: Free[G, Unit]): Kleisli[F, J, A] =
    Kleisli(j => Sync[F].onCancel(fa.foldMap(interpreter).run(j), fin.foldMap(interpreter).run(j)))

  def embed[J, A](e: Embedded[A]): Kleisli[F, J, A] =
    e match
      case Embedded.Connection(j, fa) =>
        Kleisli(_ => fa.foldMap(ConnectionInterpreter).run(j.asInstanceOf[Connection[F]]))
      case Embedded.Statement(j, fa) => Kleisli(_ => fa.foldMap(StatementInterpreter).run(j.asInstanceOf[Statement[F]]))
      case Embedded.PreparedStatement(j, fa) =>
        Kleisli(_ => fa.foldMap(PreparedStatementInterpreter).run(j.asInstanceOf[PreparedStatement[F]]))
      case Embedded.ResultSet(j, fa) => Kleisli(_ => fa.foldMap(ResultSetInterpreter).run(j.asInstanceOf[ResultSet[F]]))

  trait ConnectionInterpreter extends ConnectionOp.Visitor[[A] =>> Kleisli[F, Connection[F], A]]:

    override def embed[A](e:        Embedded[A]): Kleisli[F, Connection[F], A] = outer.embed(e)
    override def raiseError[A](err: Throwable):   Kleisli[F, Connection[F], A] = outer.raiseError(err)
    override def monotonic: Kleisli[F, Connection[F], FiniteDuration] = outer.monotonic[Connection[F]]
    override def realTime:  Kleisli[F, Connection[F], FiniteDuration] = outer.realTime[Connection[F]]
    override def suspend[A](hint: Sync.Type)(thunk: => A): Kleisli[F, Connection[F], A] = outer.suspend(hint)(thunk)
    override def canceled: Kleisli[F, Connection[F], Unit] = outer.canceled[Connection[F]]
    override def handleErrorWith[A](fa: ConnectionIO[A])(
      f: Throwable => ConnectionIO[A]
    ): Kleisli[F, Connection[F], A] = outer.handleErrorWith(this)(fa)(f)
    override def forceR[A, B](fa: ConnectionIO[A])(fb: ConnectionIO[B]): Kleisli[F, Connection[F], B] =
      outer.forceR(this)(fa)(fb)
    override def uncancelable[A](body: Poll[ConnectionIO] => ConnectionIO[A]): Kleisli[F, Connection[F], A] =
      outer.uncancelable(this, ConnectionIO.capturePoll)(body)
    override def poll[A](poll: Any, fa: ConnectionIO[A]): Kleisli[F, Connection[F], A] = outer.poll(this)(poll, fa)
    override def onCancel[A](fa: ConnectionIO[A], fin: ConnectionIO[Unit]): Kleisli[F, Connection[F], A] =
      outer.onCancel(this)(fa, fin)
    override def performLogging(event: LogEvent): Kleisli[F, Connection[F], Unit] = Kleisli(_ => logHandler.run(event))

    override def createStatement(): Kleisli[F, Connection[F], Statement[?]] =
      primitive[Connection[F], Statement[F]](_.createStatement())
        .asInstanceOf[Kleisli[F, Connection[F], Statement[?]]]
    override def prepareStatement(sql: String): Kleisli[F, Connection[F], PreparedStatement[?]] =
      primitive[Connection[F], PreparedStatement[F]](_.prepareStatement(sql))
        .asInstanceOf[Kleisli[F, Connection[F], PreparedStatement[?]]]
    override def setAutoCommit(autoCommit: Boolean): Kleisli[F, Connection[F], Unit] = primitive(
      _.setAutoCommit(autoCommit)
    )
    override def commit():                         Kleisli[F, Connection[F], Unit] = primitive(_.commit())
    override def rollback():                       Kleisli[F, Connection[F], Unit] = primitive(_.rollback())
    override def close():                          Kleisli[F, Connection[F], Unit] = primitive(_.close())
    override def setReadOnly(isReadOnly: Boolean): Kleisli[F, Connection[F], Unit] = primitive(
      _.setReadOnly(isReadOnly)
    )
    override def prepareStatement(
      sql:                  String,
      resultSetType:        Int,
      resultSetConcurrency: Int
    ): Kleisli[F, Connection[F], PreparedStatement[?]] =
      primitive[Connection[F], PreparedStatement[F]](_.prepareStatement(sql, resultSetType, resultSetConcurrency))
        .asInstanceOf[Kleisli[F, Connection[F], PreparedStatement[?]]]
    override def prepareStatement(
      sql:               String,
      autoGeneratedKeys: Int
    ): Kleisli[F, Connection[F], PreparedStatement[?]] =
      primitive[Connection[F], PreparedStatement[F]](_.prepareStatement(sql, autoGeneratedKeys))
        .asInstanceOf[Kleisli[F, Connection[F], PreparedStatement[?]]]
    override def setSavepoint():                 Kleisli[F, Connection[F], Savepoint] = primitive(_.setSavepoint())
    override def setSavepoint(name:  String):    Kleisli[F, Connection[F], Savepoint] = primitive(_.setSavepoint(name))
    override def rollback(savepoint: Savepoint): Kleisli[F, Connection[F], Unit]      = primitive(_.rollback(savepoint))
    override def releaseSavepoint(savepoint: Savepoint): Kleisli[F, Connection[F], Unit] = primitive(
      _.releaseSavepoint(savepoint)
    )

  trait StatementInterpreter extends StatementOp.Visitor[[A] =>> Kleisli[F, Statement[F], A]]:

    override def embed[A](e:        Embedded[A]): Kleisli[F, Statement[F], A] = outer.embed(e)
    override def raiseError[A](err: Throwable):   Kleisli[F, Statement[F], A] = outer.raiseError(err)
    override def monotonic: Kleisli[F, Statement[F], FiniteDuration] = outer.monotonic[Statement[F]]
    override def realTime:  Kleisli[F, Statement[F], FiniteDuration] = outer.realTime[Statement[F]]
    override def suspend[A](hint: Sync.Type)(thunk: => A): Kleisli[F, Statement[F], A]    = outer.suspend(hint)(thunk)
    override def canceled:                                 Kleisli[F, Statement[F], Unit] = outer.canceled[Statement[F]]
    override def handleErrorWith[A](fa: StatementIO[A])(f: Throwable => StatementIO[A]): Kleisli[F, Statement[F], A] =
      outer.handleErrorWith(this)(fa)(f)
    override def forceR[A, B](fa: StatementIO[A])(fb: StatementIO[B]): Kleisli[F, Statement[F], B] =
      outer.forceR(this)(fa)(fb)
    override def uncancelable[A](body: Poll[StatementIO] => StatementIO[A]): Kleisli[F, Statement[F], A] =
      outer.uncancelable(this, StatementIO.capturePoll)(body)
    override def poll[A](poll: Any, fa: StatementIO[A]): Kleisli[F, Statement[F], A] = outer.poll(this)(poll, fa)
    override def onCancel[A](fa: StatementIO[A], fin: StatementIO[Unit]): Kleisli[F, Statement[F], A] =
      outer.onCancel(this)(fa, fin)

    override def addBatch(sql: String): Kleisli[F, Statement[F], Unit]       = primitive(_.addBatch(sql))
    override def executeBatch():        Kleisli[F, Statement[F], Array[Int]] = primitive(_.executeBatch())

  trait PreparedStatementInterpreter extends PreparedStatementOp.Visitor[[A] =>> Kleisli[F, PreparedStatement[F], A]]:

    override def embed[A](e:        Embedded[A]): Kleisli[F, PreparedStatement[F], A] = outer.embed(e)
    override def raiseError[A](err: Throwable):   Kleisli[F, PreparedStatement[F], A] = outer.raiseError(err)
    override def monotonic: Kleisli[F, PreparedStatement[F], FiniteDuration] = outer.monotonic[PreparedStatement[F]]
    override def realTime:  Kleisli[F, PreparedStatement[F], FiniteDuration] = outer.realTime[PreparedStatement[F]]
    override def suspend[A](hint: Sync.Type)(thunk: => A): Kleisli[F, PreparedStatement[F], A] =
      outer.suspend(hint)(thunk)
    override def canceled: Kleisli[F, PreparedStatement[F], Unit] = outer.canceled[PreparedStatement[F]]
    override def handleErrorWith[A](fa: PreparedStatementIO[A])(
      f: Throwable => PreparedStatementIO[A]
    ): Kleisli[F, PreparedStatement[F], A] = outer.handleErrorWith(this)(fa)(f)
    override def forceR[A, B](fa: PreparedStatementIO[A])(
      fb: PreparedStatementIO[B]
    ): Kleisli[F, PreparedStatement[F], B] = outer.forceR(this)(fa)(fb)
    override def uncancelable[A](
      body: Poll[PreparedStatementIO] => PreparedStatementIO[A]
    ): Kleisli[F, PreparedStatement[F], A] = outer.uncancelable(this, PreparedStatementIO.capturePoll)(body)
    override def poll[A](poll: Any, fa: PreparedStatementIO[A]): Kleisli[F, PreparedStatement[F], A] =
      outer.poll(this)(poll, fa)
    override def onCancel[A](
      fa:  PreparedStatementIO[A],
      fin: PreparedStatementIO[Unit]
    ): Kleisli[F, PreparedStatement[F], A] = outer.onCancel(this)(fa, fin)

    override def setNull(index: Int, sqlType: Int): Kleisli[F, PreparedStatement[F], Unit] = primitive(
      _.setNull(index, sqlType)
    )
    override def setBoolean(index: Int, value: Boolean): Kleisli[F, PreparedStatement[F], Unit] = primitive(
      _.setBoolean(index, value)
    )
    override def setByte(index: Int, value: Byte): Kleisli[F, PreparedStatement[F], Unit] = primitive(
      _.setByte(index, value)
    )
    override def setShort(index: Int, value: Short): Kleisli[F, PreparedStatement[F], Unit] = primitive(
      _.setShort(index, value)
    )
    override def setInt(index: Int, value: Int): Kleisli[F, PreparedStatement[F], Unit] = primitive(
      _.setInt(index, value)
    )
    override def setLong(index: Int, value: Long): Kleisli[F, PreparedStatement[F], Unit] = primitive(
      _.setLong(index, value)
    )
    override def setFloat(index: Int, value: Float): Kleisli[F, PreparedStatement[F], Unit] = primitive(
      _.setFloat(index, value)
    )
    override def setDouble(index: Int, value: Double): Kleisli[F, PreparedStatement[F], Unit] = primitive(
      _.setDouble(index, value)
    )
    override def setBigDecimal(index: Int, value: BigDecimal): Kleisli[F, PreparedStatement[F], Unit] = primitive(
      _.setBigDecimal(index, value)
    )
    override def setString(index: Int, value: String): Kleisli[F, PreparedStatement[F], Unit] = primitive(
      _.setString(index, value)
    )
    override def setBytes(index: Int, value: Array[Byte]): Kleisli[F, PreparedStatement[F], Unit] = primitive(
      _.setBytes(index, value)
    )
    override def setDate(index: Int, value: LocalDate): Kleisli[F, PreparedStatement[F], Unit] = primitive(
      _.setDate(index, value)
    )
    override def setTime(index: Int, value: LocalTime): Kleisli[F, PreparedStatement[F], Unit] = primitive(
      _.setTime(index, value)
    )
    override def setTimestamp(index: Int, value: LocalDateTime): Kleisli[F, PreparedStatement[F], Unit] = primitive(
      _.setTimestamp(index, value)
    )
    override def setFetchSize(size: Int):    Kleisli[F, PreparedStatement[F], Unit] = primitive(_.setFetchSize(size))
    override def addBatch(sql:      String): Kleisli[F, PreparedStatement[F], Unit] = primitive(_.addBatch(sql))
    override def executeBatch():     Kleisli[F, PreparedStatement[F], Array[Int]]   = primitive(_.executeBatch())
    override def getGeneratedKeys(): Kleisli[F, PreparedStatement[F], ResultSet[?]] =
      primitive[PreparedStatement[F], ResultSet[F]](_.getGeneratedKeys())
        .asInstanceOf[Kleisli[F, PreparedStatement[F], ResultSet[?]]]
    override def executeQuery(): Kleisli[F, PreparedStatement[F], ResultSet[?]] =
      primitive[PreparedStatement[F], ResultSet[F]](_.executeQuery())
        .asInstanceOf[Kleisli[F, PreparedStatement[F], ResultSet[?]]]
    override def executeUpdate(): Kleisli[F, PreparedStatement[F], Int] = primitive(_.executeUpdate())
    override def setObject(index: Int, value: Object): Kleisli[F, PreparedStatement[F], Unit] = primitive(
      _.setObject(index, value)
    )
    override def execute():            Kleisli[F, PreparedStatement[F], Boolean] = primitive(_.execute())
    override def addBatch():           Kleisli[F, PreparedStatement[F], Unit]    = primitive(_.addBatch())
    override def executeLargeUpdate(): Kleisli[F, PreparedStatement[F], Long]    =
      primitive[PreparedStatement[F], Long](_.executeLargeUpdate())
    override def close(): Kleisli[F, PreparedStatement[F], Unit] = primitive(_.close())

  trait ResultSetInterpreter extends ResultSetOp.Visitor[[A] =>> Kleisli[F, ResultSet[F], A]]:

    override def embed[A](e:        Embedded[A]): Kleisli[F, ResultSet[F], A] = outer.embed(e)
    override def raiseError[A](err: Throwable):   Kleisli[F, ResultSet[F], A] = outer.raiseError(err)

    override def next():                       Kleisli[F, ResultSet[F], Boolean] = primitive(_.next())
    override def close():                      Kleisli[F, ResultSet[F], Unit]    = primitive(_.close())
    override def wasNull():                    Kleisli[F, ResultSet[F], Boolean] = primitive(_.wasNull())
    override def getString(columnIndex:  Int): Kleisli[F, ResultSet[F], String]  = primitive(_.getString(columnIndex))
    override def getBoolean(columnIndex: Int): Kleisli[F, ResultSet[F], Boolean] = primitive(_.getBoolean(columnIndex))
    override def getByte(columnIndex:    Int): Kleisli[F, ResultSet[F], Byte]    = primitive(_.getByte(columnIndex))
    override def getShort(columnIndex:   Int): Kleisli[F, ResultSet[F], Short]   = primitive(_.getShort(columnIndex))
    override def getInt(columnIndex:     Int): Kleisli[F, ResultSet[F], Int]     = primitive(_.getInt(columnIndex))
    override def getLong(columnIndex:    Int): Kleisli[F, ResultSet[F], Long]    = primitive(_.getLong(columnIndex))
    override def getFloat(columnIndex:   Int): Kleisli[F, ResultSet[F], Float]   = primitive(_.getFloat(columnIndex))
    override def getDouble(columnIndex:  Int): Kleisli[F, ResultSet[F], Double]  = primitive(_.getDouble(columnIndex))
    override def getBytes(columnIndex: Int): Kleisli[F, ResultSet[F], Array[Byte]] = primitive(_.getBytes(columnIndex))
    override def getDate(columnIndex:  Int): Kleisli[F, ResultSet[F], LocalDate]   = primitive(_.getDate(columnIndex))
    override def getTime(columnIndex:  Int): Kleisli[F, ResultSet[F], LocalTime]   = primitive(_.getTime(columnIndex))
    override def getTimestamp(columnIndex: Int): Kleisli[F, ResultSet[F], LocalDateTime] = primitive(
      _.getTimestamp(columnIndex)
    )
    override def getString(columnLabel: String): Kleisli[F, ResultSet[F], String] = primitive(_.getString(columnLabel))
    override def getBoolean(columnLabel: String): Kleisli[F, ResultSet[F], Boolean] = primitive(
      _.getBoolean(columnLabel)
    )
    override def getByte(columnLabel:   String): Kleisli[F, ResultSet[F], Byte]   = primitive(_.getByte(columnLabel))
    override def getShort(columnLabel:  String): Kleisli[F, ResultSet[F], Short]  = primitive(_.getShort(columnLabel))
    override def getInt(columnLabel:    String): Kleisli[F, ResultSet[F], Int]    = primitive(_.getInt(columnLabel))
    override def getLong(columnLabel:   String): Kleisli[F, ResultSet[F], Long]   = primitive(_.getLong(columnLabel))
    override def getFloat(columnLabel:  String): Kleisli[F, ResultSet[F], Float]  = primitive(_.getFloat(columnLabel))
    override def getDouble(columnLabel: String): Kleisli[F, ResultSet[F], Double] = primitive(_.getDouble(columnLabel))
    override def getBytes(columnLabel: String): Kleisli[F, ResultSet[F], Array[Byte]] = primitive(
      _.getBytes(columnLabel)
    )
    override def getDate(columnLabel: String): Kleisli[F, ResultSet[F], LocalDate] = primitive(_.getDate(columnLabel))
    override def getTime(columnLabel: String): Kleisli[F, ResultSet[F], LocalTime] = primitive(_.getTime(columnLabel))
    override def getTimestamp(columnLabel: String): Kleisli[F, ResultSet[F], LocalDateTime] = primitive(
      _.getTimestamp(columnLabel)
    )
    override def getMetaData(): Kleisli[F, ResultSet[F], ResultSetMetaData] = primitive(_.getMetaData())
    override def getBigDecimal(columnIndex: Int): Kleisli[F, ResultSet[F], BigDecimal] = primitive(
      _.getBigDecimal(columnIndex)
    )
    override def getBigDecimal(columnLabel: String): Kleisli[F, ResultSet[F], BigDecimal] = primitive(
      _.getBigDecimal(columnLabel)
    )
    override def isBeforeFirst():     Kleisli[F, ResultSet[F], Boolean] = primitive(_.isBeforeFirst())
    override def isFirst():           Kleisli[F, ResultSet[F], Boolean] = primitive(_.isFirst())
    override def isAfterLast():       Kleisli[F, ResultSet[F], Boolean] = primitive(_.isAfterLast())
    override def isLast():            Kleisli[F, ResultSet[F], Boolean] = primitive(_.isLast())
    override def beforeFirst():       Kleisli[F, ResultSet[F], Unit]    = primitive(_.beforeFirst())
    override def afterLast():         Kleisli[F, ResultSet[F], Unit]    = primitive(_.afterLast())
    override def first():             Kleisli[F, ResultSet[F], Boolean] = primitive(_.first())
    override def last():              Kleisli[F, ResultSet[F], Boolean] = primitive(_.last())
    override def getRow():            Kleisli[F, ResultSet[F], Int]     = primitive(_.getRow())
    override def absolute(row:  Int): Kleisli[F, ResultSet[F], Boolean] = primitive(_.absolute(row))
    override def relative(rows: Int): Kleisli[F, ResultSet[F], Boolean] = primitive(_.relative(rows))
    override def previous():          Kleisli[F, ResultSet[F], Boolean] = primitive(_.previous())
    override def getType():           Kleisli[F, ResultSet[F], Int]     = primitive(_.getType())
    override def getConcurrency():    Kleisli[F, ResultSet[F], Int]     = primitive(_.getConcurrency())
