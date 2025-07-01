/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.free

import java.time.*

import cats.{~>, MonadThrow}
import cats.free.Free
import cats.syntax.all.*

import ldbc.sql.*

sealed trait PreparedStatementOp[A]
object PreparedStatementOp:
  final case class SetNull(index: Int, sqlType: Int) extends PreparedStatementOp[Unit]
  final case class SetBoolean(index: Int, value: Boolean) extends PreparedStatementOp[Unit]
  final case class SetByte(index: Int, value: Byte) extends PreparedStatementOp[Unit]
  final case class SetShort(index: Int, value: Short) extends PreparedStatementOp[Unit]
  final case class SetInt(index: Int, value: Int) extends PreparedStatementOp[Unit]
  final case class SetLong(index: Int, value: Long) extends PreparedStatementOp[Unit]
  final case class SetFloat(index: Int, value: Float) extends PreparedStatementOp[Unit]
  final case class SetDouble(index: Int, value: Double) extends PreparedStatementOp[Unit]
  final case class SetBigDecimal(index: Int, value: BigDecimal) extends PreparedStatementOp[Unit]
  final case class SetString(index: Int, value: String) extends PreparedStatementOp[Unit]
  final case class SetBytes(index: Int, value: Array[Byte]) extends PreparedStatementOp[Unit]
  final case class SetTime(index: Int, value: LocalTime) extends PreparedStatementOp[Unit]
  final case class SetDate(index: Int, value: LocalDate) extends PreparedStatementOp[Unit]
  final case class SetTimestamp(index: Int, value: LocalDateTime) extends PreparedStatementOp[Unit]
  final case class SetFetchSize(size: Int) extends PreparedStatementOp[Unit]
  //final case class ExecuteQuery[A](statement: String, decoder: Decoder[A]) extends PreparedStatementOp[java.sql.ResultSet]
  //final case class ExecuteQuery() extends PreparedStatementOp[ResultSet[?]]
  final case class ExecuteQuery[F[_]]() extends PreparedStatementOp[ResultSet[F]]
  final case class ExecuteUpdate() extends PreparedStatementOp[Int]
  final case class SetObject(index: Int, value: Object) extends PreparedStatementOp[Unit]
  final case class Execute() extends PreparedStatementOp[Boolean]
  final case class AddBatch() extends PreparedStatementOp[Unit]
  final case class ExecuteLargeUpdate() extends PreparedStatementOp[Long]

type PreparedStatementIO[A] = Free[PreparedStatementOp, A]

object PreparedStatementIO:

  def setNull(index: Int, sqlType: Int): PreparedStatementIO[Unit] = Free.liftF(PreparedStatementOp.SetNull(index, sqlType))
  def setBoolean(index: Int, value: Boolean): PreparedStatementIO[Unit] = Free.liftF(PreparedStatementOp.SetBoolean(index, value))
  def setByte(index: Int, value: Byte): PreparedStatementIO[Unit] = Free.liftF(PreparedStatementOp.SetByte(index, value))
  def setShort(index: Int, value: Short): PreparedStatementIO[Unit] = Free.liftF(PreparedStatementOp.SetShort(index, value))
  def setInt(index: Int, value: Int): PreparedStatementIO[Unit] = Free.liftF(PreparedStatementOp.SetInt(index, value))
  def setLong(index: Int, value: Long): PreparedStatementIO[Unit] = Free.liftF(PreparedStatementOp.SetLong(index, value))
  def setFloat(index: Int, value: Float): PreparedStatementIO[Unit] = Free.liftF(PreparedStatementOp.SetFloat(index, value))
  def setDouble(index: Int, value: Double): PreparedStatementIO[Unit] = Free.liftF(PreparedStatementOp.SetDouble(index, value))
  def setBigDecimal(index: Int, value: BigDecimal): PreparedStatementIO[Unit] = Free.liftF(PreparedStatementOp.SetBigDecimal(index, value))
  def setString(index: Int, value: String): PreparedStatementIO[Unit] = Free.liftF(PreparedStatementOp.SetString(index, value))
  def setBytes(index: Int, value: Array[Byte]): PreparedStatementIO[Unit] = Free.liftF(PreparedStatementOp.SetBytes(index, value))
  def setTime(index: Int, value: LocalTime): PreparedStatementIO[Unit] = Free.liftF(PreparedStatementOp.SetTime(index, value))
  def setDate(index: Int, value: LocalDate): PreparedStatementIO[Unit] = Free.liftF(PreparedStatementOp.SetDate(index, value))
  def setTimestamp(index: Int, value: LocalDateTime): PreparedStatementIO[Unit] = Free.liftF(PreparedStatementOp.SetTimestamp(index, value))
  def setFetchSize(size: Int): PreparedStatementIO[Unit] = Free.liftF(PreparedStatementOp.SetFetchSize(size))
  def executeQuery[F[_]](): PreparedStatementIO[ResultSet[F]] =
    Free.liftF(PreparedStatementOp.ExecuteQuery())
  def executeUpdate(): PreparedStatementIO[Int] = Free.liftF(PreparedStatementOp.ExecuteUpdate())
  def setObject(index: Int, value: Object): PreparedStatementIO[Unit] = Free.liftF(PreparedStatementOp.SetObject(index, value))
  def execute(): PreparedStatementIO[Boolean] = Free.liftF(PreparedStatementOp.Execute())
  def addBatch(): PreparedStatementIO[Unit] = Free.liftF(PreparedStatementOp.AddBatch())
  def executeLargeUpdate(): PreparedStatementIO[Long] = Free.liftF(PreparedStatementOp.ExecuteLargeUpdate())

  extension [F[_]: MonadThrow](preparedStatement: PreparedStatement[F])
    def interpreterT: PreparedStatementOp ~> F =
      new (PreparedStatementOp ~> F):
        def apply[A](op: PreparedStatementOp[A]): F[A] = op match
          case PreparedStatementOp.SetNull(index, sqlType) => preparedStatement.setNull(index, sqlType)
          case PreparedStatementOp.SetBoolean(index, value) => preparedStatement.setBoolean(index, value)
          case PreparedStatementOp.SetByte(index, value) => preparedStatement.setByte(index, value)
          case PreparedStatementOp.SetShort(index, value) => preparedStatement.setShort(index, value)
          case PreparedStatementOp.SetInt(index, value) => preparedStatement.setInt(index, value)
          case PreparedStatementOp.SetLong(index, value) => preparedStatement.setLong(index, value)
          case PreparedStatementOp.SetFloat(index, value) => preparedStatement.setFloat(index, value)
          case PreparedStatementOp.SetDouble(index, value) => preparedStatement.setDouble(index, value)
          case PreparedStatementOp.SetBigDecimal(index, value) => preparedStatement.setBigDecimal(index, value)
          case PreparedStatementOp.SetString(index, value) => preparedStatement.setString(index, value)
          case PreparedStatementOp.SetBytes(index, value) => preparedStatement.setBytes(index, value)
          case PreparedStatementOp.SetTime(index, value) => preparedStatement.setTime(index, value)
          case PreparedStatementOp.SetDate(index, value) => preparedStatement.setDate(index, value)
          case PreparedStatementOp.SetTimestamp(index, value) => preparedStatement.setTimestamp(index, value)
          case PreparedStatementOp.SetFetchSize(size) => preparedStatement.setFetchSize(size)
          //case PreparedStatementOp.ExecuteQuery(statement, decoder) => preparedStatement.executeQuery().flatMap { resultSet =>
          //  (ResultSetIO.next() *> decoder.decode(1, statement)).foldMap(resultSet.interpreter)
          //}
          //case PreparedStatementOp.ExecuteQuery() => preparedStatement.executeQuery().asInstanceOf[F[ResultSet[?]]]
          case PreparedStatementOp.ExecuteQuery() => preparedStatement.executeQuery()
          case PreparedStatementOp.ExecuteUpdate() => preparedStatement.executeUpdate()
          case PreparedStatementOp.SetObject(index, value) => preparedStatement.setObject(index, value)
          case PreparedStatementOp.Execute() => preparedStatement.execute()
          case PreparedStatementOp.AddBatch() => preparedStatement.addBatch()
          case PreparedStatementOp.ExecuteLargeUpdate() => preparedStatement.executeLargeUpdate()
