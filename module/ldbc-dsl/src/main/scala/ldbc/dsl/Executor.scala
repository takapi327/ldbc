/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import cats.Monad
import cats.syntax.all.*

import cats.effect.*
import cats.effect.kernel.Resource.ExitCase

import ldbc.sql.Connection

import ldbc.dsl.logging.*

/**
 * A trait that represents the execution of a query.
 *
 * @tparam F
 *   The effect type
 * @tparam T
 *   The result type of the query
 */
trait Executor[F[_]: Temporal, T]:

  private[ldbc] def execute(connection: Connection[F])(using logHandler: LogHandler[F]): F[T]

  /**
   * Functions for managing the processing of connections in a read-only manner.
   */
  def readOnly(connection: Connection[F])(using LogHandler[F]): F[T] =
    connection.setReadOnly(true) *> execute(connection)

  /**
   * Functions to manage the processing of connections for writing.
   */
  def commit(connection: Connection[F])(using LogHandler[F]): F[T] =
    connection.setReadOnly(false) *> connection.setAutoCommit(true) *> execute(connection)

  /**
   * Functions to manage the processing of connections, always rolling back.
   */
  def rollback(connection: Connection[F])(using LogHandler[F]): F[T] =
    connection.setReadOnly(false) *> connection.setAutoCommit(false) *> execute(connection) <* connection.rollback()

  /**
   * Functions to manage the processing of connections in a transaction.
   */
  def transaction(connection: Connection[F])(using LogHandler[F]): F[T] =
    val acquire = connection.setReadOnly(false) *> connection.setAutoCommit(false) *> Temporal[F].pure(connection)

    val release = (connection: Connection[F], exitCase: ExitCase) =>
      exitCase match
        case ExitCase.Errored(_) | ExitCase.Canceled => connection.rollback()
        case _ => connection.commit()

    Resource
      .makeCase(acquire)(release)
      .use(execute)

object Executor:
  
  private[ldbc] case class Impl[F[_]: Temporal, T](
    statement: String,
    params:    List[ParameterBinder[F]],
    run:       Connection[F] => F[T]
  ) extends Executor[F, T]:

    private[ldbc] def execute(connection: Connection[F])(using logHandler: LogHandler[F]): F[T] =
      run(connection)
        .onError(ex => logHandler.run(LogEvent.ProcessingFailure(statement, params.map(_.parameter), ex)))
        <* logHandler.run(LogEvent.Success(statement, params.map(_.parameter)))

  def pure[F[_]: Temporal, T](value: T): Executor[F, T] =
    new Executor[F, T]:
      override private[ldbc] def execute(connection: Connection[F])(using LogHandler[F]): F[T] = Monad[F].pure(value)
      override def readOnly(connection:              Connection[F])(using LogHandler[F]): F[T] = Monad[F].pure(value)
      override def commit(connection:                Connection[F])(using LogHandler[F]): F[T] = Monad[F].pure(value)
      override def rollback(connection:              Connection[F])(using LogHandler[F]): F[T] = Monad[F].pure(value)
      override def transaction(connection:           Connection[F])(using LogHandler[F]): F[T] = Monad[F].pure(value)

  given [F[_]: Temporal]: Monad[[T] =>> Executor[F, T]] with
    override def pure[A](x: A): Executor[F, A] = Executor.pure(x)

    override def flatMap[A, B](fa: Executor[F, A])(f: A => Executor[F, B]): Executor[F, B] =
      new Executor[F, B]:
        override private[ldbc] def execute(connection: Connection[F])(using LogHandler[F]): F[B] =
          fa.execute(connection).flatMap(a => f(a).execute(connection))

    override def tailRecM[A, B](a: A)(f: A => Executor[F, Either[A, B]]): Executor[F, B] = ???
