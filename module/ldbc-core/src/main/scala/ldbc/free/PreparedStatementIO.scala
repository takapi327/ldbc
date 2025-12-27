/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.free

import java.time.*

import scala.concurrent.duration.FiniteDuration

import cats.{ ~>, Applicative }
import cats.free.Free

import cats.effect.kernel.{ CancelScope, Poll, Sync }

import ldbc.sql.*

sealed trait PreparedStatementOp[A]:
  def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[A]

object PreparedStatementOp:
  final case class Embed[A](e: Embedded[A]) extends PreparedStatementOp[A]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[A] = v.embed(e)
  final case class RaiseError[A](e: Throwable) extends PreparedStatementOp[A]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[A] = v.raiseError(e)
  final case class HandleErrorWith[A](fa: PreparedStatementIO[A], f: Throwable => PreparedStatementIO[A])
    extends PreparedStatementOp[A]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[A] = v.handleErrorWith(fa)(f)
  case object Monotonic extends PreparedStatementOp[FiniteDuration]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[FiniteDuration] = v.monotonic
  case object Realtime extends PreparedStatementOp[FiniteDuration]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[FiniteDuration] = v.realTime
  final case class Suspend[A](hint: Sync.Type, thunk: () => A) extends PreparedStatementOp[A]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[A] = v.suspend(hint)(thunk())
  final case class ForceR[A, B](fa: PreparedStatementIO[A], fb: PreparedStatementIO[B]) extends PreparedStatementOp[B]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[B] = v.forceR(fa)(fb)
  final case class Uncancelable[A](body: Poll[PreparedStatementIO] => PreparedStatementIO[A])
    extends PreparedStatementOp[A]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[A] = v.uncancelable(body)
  final case class Poll1[A](poll: Any, fa: PreparedStatementIO[A]) extends PreparedStatementOp[A]:
    override def visit[F[_]](v: Visitor[F]): F[A] = v.poll(poll, fa)
  case object Canceled extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.canceled
  final case class OnCancel[A](fa: PreparedStatementIO[A], fin: PreparedStatementIO[Unit])
    extends PreparedStatementOp[A]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[A] = v.onCancel(fa, fin)

  final case class SetNull(index: Int, sqlType: Int) extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.setNull(index, sqlType)
  final case class SetBoolean(index: Int, value: Boolean) extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.setBoolean(index, value)
  final case class SetByte(index: Int, value: Byte) extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.setByte(index, value)
  final case class SetShort(index: Int, value: Short) extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.setShort(index, value)
  final case class SetInt(index: Int, value: Int) extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.setInt(index, value)
  final case class SetLong(index: Int, value: Long) extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.setLong(index, value)
  final case class SetFloat(index: Int, value: Float) extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.setFloat(index, value)
  final case class SetDouble(index: Int, value: Double) extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.setDouble(index, value)
  final case class SetBigDecimal(index: Int, value: BigDecimal) extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.setBigDecimal(index, value)
  final case class SetString(index: Int, value: String) extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.setString(index, value)
  final case class SetBytes(index: Int, value: Array[Byte]) extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.setBytes(index, value)
  final case class SetTime(index: Int, value: LocalTime) extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.setTime(index, value)
  final case class SetDate(index: Int, value: LocalDate) extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.setDate(index, value)
  final case class SetTimestamp(index: Int, value: LocalDateTime) extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.setTimestamp(index, value)
  final case class SetFetchSize(size: Int) extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.setFetchSize(size)
  final case class AddBatch(sql: String) extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.addBatch(sql)
  final case class ExecuteBatch() extends PreparedStatementOp[Array[Int]]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Array[Int]] = v.executeBatch()
  final case class GetGeneratedKeys() extends PreparedStatementOp[ResultSet[?]]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[ResultSet[?]] = v.getGeneratedKeys()
  final case class ExecuteQuery() extends PreparedStatementOp[ResultSet[?]]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[ResultSet[?]] = v.executeQuery()
  final case class ExecuteUpdate() extends PreparedStatementOp[Int]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Int] = v.executeUpdate()
  final case class SetObject(index: Int, value: Object) extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.setObject(index, value)
  final case class Execute() extends PreparedStatementOp[Boolean]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Boolean] = v.execute()
  case object AddBatch extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.addBatch()
  final case class ExecuteLargeUpdate() extends PreparedStatementOp[Long]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Long] = v.executeLargeUpdate()
  final case class Close() extends PreparedStatementOp[Unit]:
    override def visit[F[_]](v: PreparedStatementOp.Visitor[F]): F[Unit] = v.close()

  given Embeddable[PreparedStatementOp, PreparedStatement[?]] =
    new Embeddable[PreparedStatementOp, PreparedStatement[?]]:
      override def embed[A](
        j:  PreparedStatement[?],
        fa: Free[PreparedStatementOp, A]
      ): Embedded.PreparedStatement[?, A] = Embedded.PreparedStatement(j, fa)

  trait Visitor[F[_]] extends (PreparedStatementOp ~> F):
    final def apply[A](fa: PreparedStatementOp[A]): F[A] = fa.visit(this)

    def embed[A](e:            Embedded[A]):                                                      F[A]
    def handleErrorWith[A](fa: PreparedStatementIO[A])(f:   Throwable => PreparedStatementIO[A]): F[A]
    def raiseError[A](err:     Throwable):                                                        F[A]
    def monotonic:                                                                                F[FiniteDuration]
    def realTime:                                                                                 F[FiniteDuration]
    def suspend[A](hint:       Sync.Type)(thunk:            => A):                                F[A]
    def forceR[A, B](fa:       PreparedStatementIO[A])(fb:  PreparedStatementIO[B]):              F[B]
    def uncancelable[A](body:  Poll[PreparedStatementIO] => PreparedStatementIO[A]):              F[A]
    def poll[A](poll:          Any, fa:                     PreparedStatementIO[A]):              F[A]
    def canceled:                                                                                 F[Unit]
    def onCancel[A](fa:        PreparedStatementIO[A], fin: PreparedStatementIO[Unit]):           F[A]

    def setNull(index:       Int, sqlType: Int):           F[Unit]
    def setBoolean(index:    Int, value:   Boolean):       F[Unit]
    def setByte(index:       Int, value:   Byte):          F[Unit]
    def setShort(index:      Int, value:   Short):         F[Unit]
    def setInt(index:        Int, value:   Int):           F[Unit]
    def setLong(index:       Int, value:   Long):          F[Unit]
    def setFloat(index:      Int, value:   Float):         F[Unit]
    def setDouble(index:     Int, value:   Double):        F[Unit]
    def setBigDecimal(index: Int, value:   BigDecimal):    F[Unit]
    def setString(index:     Int, value:   String):        F[Unit]
    def setBytes(index:      Int, value:   Array[Byte]):   F[Unit]
    def setTime(index:       Int, value:   LocalTime):     F[Unit]
    def setDate(index:       Int, value:   LocalDate):     F[Unit]
    def setTimestamp(index:  Int, value:   LocalDateTime): F[Unit]
    def setFetchSize(size:   Int):                         F[Unit]
    def addBatch(sql:        String):                      F[Unit]
    def executeBatch():                                    F[Array[Int]]
    def getGeneratedKeys():                                F[ResultSet[?]]
    def executeQuery():                                    F[ResultSet[?]]
    def executeUpdate():                                   F[Int]
    def setObject(index:     Int, value:   Object):        F[Unit]
    def execute():                                         F[Boolean]
    def addBatch():                                        F[Unit]
    def executeLargeUpdate():                              F[Long]
    def close():                                           F[Unit]

type PreparedStatementIO[A] = Free[PreparedStatementOp, A]

object PreparedStatementIO:
  module =>

  given Sync[PreparedStatementIO] =
    new Sync[PreparedStatementIO]:
      val monad = Free.catsFreeMonadForFree[PreparedStatementOp]
      override val applicative:     Applicative[PreparedStatementIO] = monad
      override val rootCancelScope: CancelScope                      = CancelScope.Cancelable
      override def pure[A](x: A):   PreparedStatementIO[A]           = monad.pure(x)
      override def flatMap[A, B](fa: PreparedStatementIO[A])(f: A => PreparedStatementIO[B]): PreparedStatementIO[B] =
        monad.flatMap(fa)(f)
      override def tailRecM[A, B](a: A)(f: A => PreparedStatementIO[Either[A, B]]): PreparedStatementIO[B] =
        monad.tailRecM(a)(f)
      override def raiseError[A](e: Throwable): PreparedStatementIO[A] = module.raiseError(e)
      override def handleErrorWith[A](fa: PreparedStatementIO[A])(
        f: Throwable => PreparedStatementIO[A]
      ):                      PreparedStatementIO[A]              = module.handleErrorWith(fa)(f)
      override def monotonic: PreparedStatementIO[FiniteDuration] = module.monotonic
      override def realTime:  PreparedStatementIO[FiniteDuration] = module.realtime
      override def suspend[A](hint: Sync.Type)(thunk: => A): PreparedStatementIO[A] = module.suspend(hint)(thunk)
      override def forceR[A, B](fa: PreparedStatementIO[A])(fb: PreparedStatementIO[B]): PreparedStatementIO[B] =
        module.forceR(fa)(fb)
      override def uncancelable[A](body: Poll[PreparedStatementIO] => PreparedStatementIO[A]): PreparedStatementIO[A] =
        module.uncancelable(body)
      override def canceled: PreparedStatementIO[Unit] = module.canceled
      override def onCancel[A](fa: PreparedStatementIO[A], fin: PreparedStatementIO[Unit]): PreparedStatementIO[A] =
        module.onCancel(fa, fin)

  def embed[F[_], J, A](j: J, fa: Free[F, A])(using ev: Embeddable[F, J]): Free[PreparedStatementOp, A] =
    Free.liftF(PreparedStatementOp.Embed(ev.embed(j, fa)))
  def pure[A](a:         A):         PreparedStatementIO[A] = Free.pure(a)
  def raiseError[A](err: Throwable): PreparedStatementIO[A] = Free.liftF(PreparedStatementOp.RaiseError(err))
  def handleErrorWith[A](fa: PreparedStatementIO[A])(f: Throwable => PreparedStatementIO[A]): PreparedStatementIO[A] =
    Free.liftF[PreparedStatementOp, A](PreparedStatementOp.HandleErrorWith(fa, f))
  val monotonic: PreparedStatementIO[FiniteDuration] =
    Free.liftF[PreparedStatementOp, FiniteDuration](PreparedStatementOp.Monotonic)
  val realtime: PreparedStatementIO[FiniteDuration] =
    Free.liftF[PreparedStatementOp, FiniteDuration](PreparedStatementOp.Realtime)
  def suspend[A](hint: Sync.Type)(thunk: => A): PreparedStatementIO[A] =
    Free.liftF[PreparedStatementOp, A](PreparedStatementOp.Suspend(hint, () => thunk))
  def forceR[A, B](fa: PreparedStatementIO[A])(fb: PreparedStatementIO[B]): PreparedStatementIO[B] =
    Free.liftF[PreparedStatementOp, B](PreparedStatementOp.ForceR(fa, fb))
  def uncancelable[A](body: Poll[PreparedStatementIO] => PreparedStatementIO[A]): PreparedStatementIO[A] =
    Free.liftF[PreparedStatementOp, A](PreparedStatementOp.Uncancelable(body))
  val canceled: PreparedStatementIO[Unit] = Free.liftF[PreparedStatementOp, Unit](PreparedStatementOp.Canceled)
  def onCancel[A](fa: PreparedStatementIO[A], fin: PreparedStatementIO[Unit]): PreparedStatementIO[A] =
    Free.liftF[PreparedStatementOp, A](PreparedStatementOp.OnCancel(fa, fin))
  def capturePoll[M[_]](mpoll: Poll[M]): Poll[PreparedStatementIO] = new Poll[PreparedStatementIO]:
    override def apply[A](fa: PreparedStatementIO[A]): PreparedStatementIO[A] =
      Free.liftF[PreparedStatementOp, A](PreparedStatementOp.Poll1(mpoll, fa))

  def setNull(index: Int, sqlType: Int): PreparedStatementIO[Unit] =
    Free.liftF(PreparedStatementOp.SetNull(index, sqlType))
  def setBoolean(index: Int, value: Boolean): PreparedStatementIO[Unit] =
    Free.liftF(PreparedStatementOp.SetBoolean(index, value))
  def setByte(index: Int, value: Byte): PreparedStatementIO[Unit] =
    Free.liftF(PreparedStatementOp.SetByte(index, value))
  def setShort(index: Int, value: Short): PreparedStatementIO[Unit] =
    Free.liftF(PreparedStatementOp.SetShort(index, value))
  def setInt(index: Int, value: Int):   PreparedStatementIO[Unit] = Free.liftF(PreparedStatementOp.SetInt(index, value))
  def setLong(index: Int, value: Long): PreparedStatementIO[Unit] =
    Free.liftF(PreparedStatementOp.SetLong(index, value))
  def setFloat(index: Int, value: Float): PreparedStatementIO[Unit] =
    Free.liftF(PreparedStatementOp.SetFloat(index, value))
  def setDouble(index: Int, value: Double): PreparedStatementIO[Unit] =
    Free.liftF(PreparedStatementOp.SetDouble(index, value))
  def setBigDecimal(index: Int, value: BigDecimal): PreparedStatementIO[Unit] =
    Free.liftF(PreparedStatementOp.SetBigDecimal(index, value))
  def setString(index: Int, value: String): PreparedStatementIO[Unit] =
    Free.liftF(PreparedStatementOp.SetString(index, value))
  def setBytes(index: Int, value: Array[Byte]): PreparedStatementIO[Unit] =
    Free.liftF(PreparedStatementOp.SetBytes(index, value))
  def setTime(index: Int, value: LocalTime): PreparedStatementIO[Unit] =
    Free.liftF(PreparedStatementOp.SetTime(index, value))
  def setDate(index: Int, value: LocalDate): PreparedStatementIO[Unit] =
    Free.liftF(PreparedStatementOp.SetDate(index, value))
  def setTimestamp(index: Int, value: LocalDateTime): PreparedStatementIO[Unit] =
    Free.liftF(PreparedStatementOp.SetTimestamp(index, value))
  def setFetchSize(size: Int):    PreparedStatementIO[Unit]         = Free.liftF(PreparedStatementOp.SetFetchSize(size))
  def addBatch(sql:      String): PreparedStatementIO[Unit]         = Free.liftF(PreparedStatementOp.AddBatch(sql))
  def executeBatch():             PreparedStatementIO[Array[Int]]   = Free.liftF(PreparedStatementOp.ExecuteBatch())
  def getGeneratedKeys():         PreparedStatementIO[ResultSet[?]] = Free.liftF(PreparedStatementOp.GetGeneratedKeys())
  def executeQuery():             PreparedStatementIO[ResultSet[?]] =
    Free.liftF(PreparedStatementOp.ExecuteQuery())
  def executeUpdate():                      PreparedStatementIO[Int]  = Free.liftF(PreparedStatementOp.ExecuteUpdate())
  def setObject(index: Int, value: Object): PreparedStatementIO[Unit] =
    Free.liftF(PreparedStatementOp.SetObject(index, value))
  def execute():            PreparedStatementIO[Boolean] = Free.liftF(PreparedStatementOp.Execute())
  def addBatch():           PreparedStatementIO[Unit]    = Free.liftF(PreparedStatementOp.AddBatch)
  def executeLargeUpdate(): PreparedStatementIO[Long]    = Free.liftF(PreparedStatementOp.ExecuteLargeUpdate())
  def close():              PreparedStatementIO[Unit]    = Free.liftF(PreparedStatementOp.Close())
