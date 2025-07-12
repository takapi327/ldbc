/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.free

import scala.concurrent.duration.FiniteDuration

import cats.~>
import cats.free.Free
import cats.syntax.all.*

import cats.effect.kernel.{Poll, Sync}

import ldbc.sql.*

sealed trait StatementOp[A]:
  def visit[F[_]](v: StatementOp.Visitor[F]): F[A]

object StatementOp:
  final case class Embed[A](e: Embedded[A]) extends StatementOp[A]:
    override def visit[F[_]](v: StatementOp.Visitor[F]): F[A] = v.embed(e)
  final case class RaiseError[A](e: Throwable) extends StatementOp[A]:
    override def visit[F[_]](v: StatementOp.Visitor[F]): F[A] = v.raiseError(e)
  final case class HandleErrorWith[A](fa: StatementIO[A], f: Throwable => StatementIO[A]) extends StatementOp[A]:
    override def visit[F[_]](v: StatementOp.Visitor[F]): F[A] = v.handleErrorWith(fa)(f)
  case object Monotonic extends StatementOp[FiniteDuration]:
    override def visit[F[_]](v: StatementOp.Visitor[F]): F[FiniteDuration] = v.monotonic
  case object Realtime extends StatementOp[FiniteDuration]:
    override def visit[F[_]](v: StatementOp.Visitor[F]): F[FiniteDuration] = v.realTime
  final case class Suspend[A](hint: Sync.Type, thunk: () => A) extends StatementOp[A]:
    override def visit[F[_]](v: StatementOp.Visitor[F]): F[A] = v.suspend(hint)(thunk())
  final case class ForceR[A, B](fa: StatementIO[A], fb: StatementIO[B]) extends StatementOp[B]:
    override def visit[F[_]](v: StatementOp.Visitor[F]): F[B] = v.forceR(fa)(fb)
  final case class Uncancelable[A](body: Poll[StatementIO] => StatementIO[A]) extends StatementOp[A]:
    override def visit[F[_]](v: StatementOp.Visitor[F]): F[A] = v.uncancelable(body)
  final case class Poll1[A](poll: Any, fa: StatementIO[A]) extends StatementOp[A]:
    override def visit[F[_]](v: Visitor[F]): F[A] = v.poll(poll, fa)
  case object Canceled extends StatementOp[Unit]:
    override def visit[F[_]](v: StatementOp.Visitor[F]): F[Unit] = v.canceled
  final case class OnCancel[A](fa: StatementIO[A], fin: StatementIO[Unit]) extends StatementOp[A]:
    override def visit[F[_]](v: StatementOp.Visitor[F]): F[A] = v.onCancel(fa, fin)

  final case class AddBatch[A](sql: String) extends StatementOp[Unit]:
    override def visit[F[_]](v: StatementOp.Visitor[F]): F[Unit] = v.addBatch(sql)
  final case class ExecuteBatch[A]() extends StatementOp[Array[Int]]:
    override def visit[F[_]](v: StatementOp.Visitor[F]): F[Array[Int]] = v.executeBatch()

  given Embeddable[StatementOp, Statement[?]] =
    new Embeddable[StatementOp, Statement[?]]:
      override def embed[A](j: Statement[?], fa: Free[StatementOp, A]): Embedded.Statement[?, A] = Embedded.Statement(j, fa)

  trait Visitor[F[_]] extends (StatementOp ~> F):
    final def apply[A](fa: StatementOp[A]): F[A] = fa.visit(this)

    def embed[A](e: Embedded[A]): F[A]
    def handleErrorWith[A](fa: StatementIO[A])(f: Throwable => StatementIO[A]): F[A]
    def raiseError[A](err: Throwable): F[A]
    def monotonic: F[FiniteDuration]
    def realTime: F[FiniteDuration]
    def suspend[A](hint: Sync.Type)(thunk: => A): F[A]
    def forceR[A, B](fa: StatementIO[A])(fb: StatementIO[B]): F[B]
    def uncancelable[A](body: Poll[StatementIO] => StatementIO[A]): F[A]
    def poll[A](poll: Any, fa: StatementIO[A]): F[A]
    def canceled: F[Unit]
    def onCancel[A](fa: StatementIO[A], fin: StatementIO[Unit]): F[A]

    def addBatch(sql: String): F[Unit]
    def executeBatch(): F[Array[Int]]

type StatementIO[A] = Free[StatementOp, A]

object StatementIO:
  module =>

  def embed[F[_], J, A](j: J, fa: Free[F, A])(using ev: Embeddable[F, J]): Free[StatementOp, A] =
    Free.liftF(StatementOp.Embed(ev.embed(j, fa)))
  def pure[A](a: A): StatementIO[A] = Free.pure(a)
  def raiseError[A](err: Throwable): StatementIO[A] = Free.liftF(StatementOp.RaiseError(err))
  def handleErrorWith[A](fa: StatementIO[A])(f: Throwable => StatementIO[A]): StatementIO[A] =
    Free.liftF[StatementOp, A](StatementOp.HandleErrorWith(fa, f))
  val monotonic: StatementIO[FiniteDuration] = Free.liftF[StatementOp, FiniteDuration](StatementOp.Monotonic)
  val realtime: StatementIO[FiniteDuration] = Free.liftF[StatementOp, FiniteDuration](StatementOp.Realtime)
  def suspend[A](hint: Sync.Type)(thunk: => A): StatementIO[A] = Free.liftF[StatementOp, A](StatementOp.Suspend(hint, () => thunk))
  def forceR[A, B](fa: StatementIO[A])(fb: StatementIO[B]): StatementIO[B] = Free.liftF[StatementOp, B](StatementOp.ForceR(fa, fb))
  def uncancelable[A](body: Poll[StatementIO] => StatementIO[A]): StatementIO[A] = Free.liftF[StatementOp, A](StatementOp.Uncancelable(body))
  val canceled: StatementIO[Unit] = Free.liftF[StatementOp, Unit](StatementOp.Canceled)
  def onCancel[A](fa: StatementIO[A], fin: StatementIO[Unit]): StatementIO[A] = Free.liftF[StatementOp, A](StatementOp.OnCancel(fa, fin))
  def capturePoll[M[_]](mpoll: Poll[M]): Poll[StatementIO] = new Poll[StatementIO]:
    override def apply[A](fa: StatementIO[A]): StatementIO[A] = Free.liftF[StatementOp, A](StatementOp.Poll1(mpoll, fa))
  
  def addBatch(sql: String): StatementIO[Unit] = Free.liftF[StatementOp, Unit](StatementOp.AddBatch(sql))
  def executeBatch(): StatementIO[Array[Int]] = Free.liftF[StatementOp, Array[Int]](StatementOp.ExecuteBatch())
  