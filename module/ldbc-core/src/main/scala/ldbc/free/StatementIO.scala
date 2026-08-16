/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.free

import cats.~>
import cats.free.Free

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
  final case class ExecuteQuery(sql: String) extends StatementOp[ResultSet[?]]:
    override def visit[F[_]](v: StatementOp.Visitor[F]): F[ResultSet[?]] = v.executeQuery(sql)
  final case class ExecuteUpdate(sql: String) extends StatementOp[Int]:
    override def visit[F[_]](v: StatementOp.Visitor[F]): F[Int] = v.executeUpdate(sql)
  final case class AddBatch[A](sql: String) extends StatementOp[Unit]:
    override def visit[F[_]](v: StatementOp.Visitor[F]): F[Unit] = v.addBatch(sql)
  final case class ExecuteBatch[A]() extends StatementOp[Array[Int]]:
    override def visit[F[_]](v: StatementOp.Visitor[F]): F[Array[Int]] = v.executeBatch()
  final case class Close() extends StatementOp[Unit]:
    override def visit[F[_]](v: StatementOp.Visitor[F]): F[Unit] = v.close()

  given Embeddable[StatementOp, Statement[?]] =
    new Embeddable[StatementOp, Statement[?]]:
      override def embed[A](j: Statement[?], fa: Free[StatementOp, A]): Embedded.Statement[?, A] =
        Embedded.Statement(j, fa)

  trait Visitor[F[_]] extends (StatementOp ~> F):
    final def apply[A](fa: StatementOp[A]): F[A] = fa.visit(this)

    def embed[A](e:            Embedded[A]):                                      F[A]
    def handleErrorWith[A](fa: StatementIO[A])(f:   Throwable => StatementIO[A]): F[A]
    def raiseError[A](err:     Throwable):                                        F[A]

    def executeQuery(sql:  String): F[ResultSet[?]]
    def executeUpdate(sql: String): F[Int]
    def addBatch(sql:      String): F[Unit]
    def executeBatch():             F[Array[Int]]
    def close():                    F[Unit]

type StatementIO[A] = Free[StatementOp, A]

object StatementIO:
  module =>

  def embed[F[_], J, A](j: J, fa: Free[F, A])(using ev: Embeddable[F, J]): Free[StatementOp, A] =
    Free.liftF(StatementOp.Embed(ev.embed(j, fa)))
  def pure[A](a:         A):         StatementIO[A] = Free.pure(a)
  def raiseError[A](err: Throwable): StatementIO[A] = Free.liftF(StatementOp.RaiseError(err))
  def handleErrorWith[A](fa: StatementIO[A])(f: Throwable => StatementIO[A]): StatementIO[A] =
    Free.liftF[StatementOp, A](StatementOp.HandleErrorWith(fa, f))

  def executeQuery(sql: String): StatementIO[ResultSet[?]] =
    Free.liftF[StatementOp, ResultSet[?]](StatementOp.ExecuteQuery(sql))
  def executeUpdate(sql: String): StatementIO[Int]  = Free.liftF[StatementOp, Int](StatementOp.ExecuteUpdate(sql))
  def addBatch(sql:      String): StatementIO[Unit] = Free.liftF[StatementOp, Unit](StatementOp.AddBatch(sql))
  def executeBatch(): StatementIO[Array[Int]] = Free.liftF[StatementOp, Array[Int]](StatementOp.ExecuteBatch())
  def close():        StatementIO[Unit]       = Free.liftF[StatementOp, Unit](StatementOp.Close())
