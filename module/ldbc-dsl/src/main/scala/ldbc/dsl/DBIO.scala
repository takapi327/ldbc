/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import scala.concurrent.duration.*

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.kernel.Resource.ExitCase

import ldbc.sql.*
import ldbc.sql.logging.LogEvent

import ldbc.dsl.codec.Encoder

/**
 * A trait that represents the execution of a query.
 *
 * @tparam F
 *   The effect type
 * @tparam T
 *   The result type of the query
 */
trait DBIO[F[_], T]:

  /**
   * The function that actually executes the query.
   *
   * @param connection
   *   The connection to the database
   * @return
   *   The result of the query
   */
  def run(connection: Connection[F]): F[T]

  /**
   * Functions for managing the processing of connections in a read-only manner.
   */
  def readOnly(connection: Connection[F]): F[T]

  /**
   * Functions to manage the processing of connections for writing.
   */
  def commit(connection: Connection[F]): F[T]

  /**
   * Functions to manage the processing of connections, always rolling back.
   */
  def rollback(connection: Connection[F]): F[T]

  /**
   * Functions to manage the processing of connections in a transaction.
   */
  def transaction(connection: Connection[F]): F[T]

object DBIO extends ParamBinder:

  private[ldbc] case class Impl[F[_]: MonadCancelThrow, T](
    statement: String,
    params:    List[Parameter],
    func:      Connection[F] => F[T]
  ) extends DBIO[F, T]:

    override def run(connection: Connection[F]): F[T] =
      func(connection)
        .onError(ex => connection.logHandler.run(LogEvent.ProcessingFailure(statement, params.map(_.value), ex)))
        <* connection.logHandler.run(LogEvent.Success(statement, params.map(_.value)))

    override def readOnly(connection: Connection[F]): F[T] =
      connection.setReadOnly(true) *> run(connection) <* connection.setReadOnly(false)

    override def commit(connection: Connection[F]): F[T] =
      connection.setReadOnly(false) *> connection.setAutoCommit(true) *> run(connection)

    override def rollback(connection: Connection[F]): F[T] =
      connection.setReadOnly(false) *> connection.setAutoCommit(false) *> run(connection) <* connection
        .rollback() <* connection.setAutoCommit(true)

    override def transaction(connection: Connection[F]): F[T] =
      val acquire = connection.setReadOnly(false) *> connection.setAutoCommit(false) *> MonadThrow[F].pure(connection)

      val release = (connection: Connection[F], exitCase: ExitCase) =>
        (exitCase match
          case ExitCase.Errored(_) | ExitCase.Canceled => connection.rollback()
          case _                                       => connection.commit()
        )
          *> connection.setAutoCommit(true)

      Resource
        .makeCase(acquire)(release)
        .use(run)

  def pure[F[_]: Monad, A](value: A): DBIO[F, A] =
    new DBIO[F, A]:
      override def run(connection:         Connection[F]): F[A] = Monad[F].pure(value)
      override def readOnly(connection:    Connection[F]): F[A] = Monad[F].pure(value)
      override def commit(connection:      Connection[F]): F[A] = Monad[F].pure(value)
      override def rollback(connection:    Connection[F]): F[A] = Monad[F].pure(value)
      override def transaction(connection: Connection[F]): F[A] = Monad[F].pure(value)

  def raiseError[F[_], A](e: Throwable)(using ev: MonadThrow[F]): DBIO[F, A] =
    new DBIO[F, A]:
      override def run(connection:         Connection[F]): F[A] = ev.raiseError(e)
      override def readOnly(connection:    Connection[F]): F[A] = ev.raiseError(e)
      override def commit(connection:      Connection[F]): F[A] = ev.raiseError(e)
      override def rollback(connection:    Connection[F]): F[A] = ev.raiseError(e)
      override def transaction(connection: Connection[F]): F[A] = ev.raiseError(e)

  def sleep[F[_]: Temporal](duration: FiniteDuration): DBIO[F, Unit] =
    new DBIO[F, Unit]:
      override def run(connection:         Connection[F]): F[Unit] = Temporal[F].sleep(duration)
      override def readOnly(connection:    Connection[F]): F[Unit] = Temporal[F].sleep(duration)
      override def commit(connection:      Connection[F]): F[Unit] = Temporal[F].sleep(duration)
      override def rollback(connection:    Connection[F]): F[Unit] = Temporal[F].sleep(duration)
      override def transaction(connection: Connection[F]): F[Unit] = Temporal[F].sleep(duration)

  /**
   * A method that performs a single operation on the database server.
   *
   * The execution result returns only whether the process succeeded or failed.
   *
   * {{{
   *   DBIO.single("SELECT 1")
   * }}}
   *
   * @param statement
   *   The SQL statement to be executed
   * @tparam F
   *   the effect type
   * @return
   *   The result of the operation
   */
  def single[F[_]: MonadCancelThrow](statement: String): DBIO[F, Boolean] =
    val func: Connection[F] => F[Boolean] = connection =>
      for
        stmt   <- connection.createStatement()
        result <- stmt.execute(statement)
      yield result
    Impl(statement, List.empty, func)

  /**
   * A method that performs a single operation on the database server.
   *
   * {{{
   *   DBIO.single("SELECT ?", 1)
   * }}}
   *
   * @param statement
   *   The SQL statement to be executed
   * @param head
   *   The first parameter
   * @param tails
   *   The rest of the parameters
   * @tparam F
   *   the effect type
   * @return
   *   The result of the operation
   */
  def single[F[_]: MonadCancelThrow](
    statement: String,
    head:      Encoder.Supported,
    tails:     Encoder.Supported*
  ): DBIO[F, ResultSet] =
    val params = (head :: tails.toList).map(Parameter.Dynamic.Success(_))
    val func: Connection[F] => F[ResultSet] = connection =>
      for
        prepareStatement <- connection.prepareStatement(statement)
        _                <- paramBind(prepareStatement, params)
        result           <- prepareStatement.executeQuery()
      yield result
    Impl(statement, params, func)

  /**
   * A method that performs multiple operations on the database server.
   *
   * {{{
   *   DBIO.sequence(
   *     DBIO.single("SELECT 1"),
   *     DBIO.single("SELECT 2"),
   *   )
   * }}}
   *
   * @param dbios
   *   The operations to be executed
   * @tparam F
   *   the effect type
   * @tparam A
   *   The result type of the operation
   * @return
   *   List of Execution Results
   */
  def sequence[F[_]: Monad, A](dbios: DBIO[F, A]*): DBIO[F, List[A]] =
    new DBIO[F, List[A]]:
      override def run(connection: Connection[F]): F[List[A]] =
        dbios.toList.traverse(_.run(connection))

      override def readOnly(connection: Connection[F]): F[List[A]] =
        dbios.toList.traverse(_.readOnly(connection))

      override def commit(connection: Connection[F]): F[List[A]] =
        dbios.toList.traverse(_.commit(connection))

      override def rollback(connection: Connection[F]): F[List[A]] =
        dbios.toList.traverse(_.rollback(connection))

      override def transaction(connection: Connection[F]): F[List[A]] =
        dbios.toList.traverse(_.transaction(connection))

  given [F[_]: MonadCancelThrow]: MonadThrow[[T] =>> DBIO[F, T]] with
    override def pure[A](x: A): DBIO[F, A] = DBIO.pure(x)

    override def flatMap[A, B](fa: DBIO[F, A])(f: A => DBIO[F, B]): DBIO[F, B] =
      new DBIO[F, B]:
        override def run(connection: Connection[F]): F[B] =
          fa.run(connection).flatMap(a => f(a).run(connection))
        override def readOnly(connection: Connection[F]): F[B] =
          connection.setReadOnly(true) *> run(connection) <* connection.setReadOnly(false)
        override def commit(connection: Connection[F]): F[B] =
          connection.setReadOnly(false) *> connection.setAutoCommit(true) *> run(connection)
        override def rollback(connection: Connection[F]): F[B] =
          connection.setReadOnly(false) *> connection.setAutoCommit(false) *> run(connection) <* connection
            .rollback() <* connection.setAutoCommit(true)
        override def transaction(connection: Connection[F]): F[B] =
          val acquire =
            connection.setReadOnly(false) *> connection.setAutoCommit(false) *> MonadCancelThrow[F].pure(connection)
          val release = (connection: Connection[F], exitCase: ExitCase) =>
            (exitCase match
              case ExitCase.Errored(_) | ExitCase.Canceled => connection.rollback()
              case _                                       => connection.commit()
            )
              *> connection.setAutoCommit(true)
          Resource
            .makeCase(acquire)(release)
            .use(run)

    override def tailRecM[A, B](a: A)(f: A => DBIO[F, Either[A, B]]): DBIO[F, B] =
      new DBIO[F, B]:
        override def run(connection: Connection[F]): F[B] =
          MonadCancelThrow[F].tailRecM(a)(a => f(a).run(connection))

        override def readOnly(connection: Connection[F]): F[B] =
          connection.setReadOnly(true) *> run(connection) <* connection.setReadOnly(false)

        override def commit(connection: Connection[F]): F[B] =
          connection.setReadOnly(false) *> connection.setAutoCommit(true) *> run(connection)

        override def rollback(connection: Connection[F]): F[B] =
          connection.setReadOnly(false) *> connection.setAutoCommit(false) *> run(connection) <* connection
            .rollback() <* connection.setAutoCommit(true)

        override def transaction(connection: Connection[F]): F[B] =
          val acquire =
            connection.setReadOnly(false) *> connection.setAutoCommit(false) *> MonadCancelThrow[F].pure(connection)

          val release = (connection: Connection[F], exitCase: ExitCase) =>
            (exitCase match
              case ExitCase.Errored(_) | ExitCase.Canceled => connection.rollback()
              case _                                       => connection.commit()
            )
              *> connection.setAutoCommit(true)

          Resource
            .makeCase(acquire)(release)
            .use(run)

    override def ap[A, B](ff: DBIO[F, A => B])(fa: DBIO[F, A]): DBIO[F, B] =
      new DBIO[F, B]:
        override def run(connection: Connection[F]): F[B] =
          (ff.run(connection), fa.run(connection)).mapN(_(_))

        override def readOnly(connection: Connection[F]): F[B] =
          connection.setReadOnly(true) *> run(connection) <* connection.setReadOnly(false)

        override def commit(connection: Connection[F]): F[B] =
          connection.setReadOnly(false) *> connection.setAutoCommit(true) *> run(connection)

        override def rollback(connection: Connection[F]): F[B] =
          connection.setReadOnly(false) *> connection.setAutoCommit(false) *> run(connection) <* connection
            .rollback() <* connection.setAutoCommit(true)

        override def transaction(connection: Connection[F]): F[B] =
          val acquire =
            connection.setReadOnly(false) *> connection.setAutoCommit(false) *> MonadCancelThrow[F].pure(connection)

          val release = (connection: Connection[F], exitCase: ExitCase) =>
            (exitCase match
              case ExitCase.Errored(_) | ExitCase.Canceled => connection.rollback()
              case _                                       => connection.commit()
            )
              *> connection.setAutoCommit(true)

          Resource
            .makeCase(acquire)(release)
            .use(run)

    override def raiseError[A](e: Throwable): DBIO[F, A] =
      DBIO.raiseError(e)

    override def handleErrorWith[A](fa: DBIO[F, A])(f: Throwable => DBIO[F, A]): DBIO[F, A] =
      new DBIO[F, A]:
        override def run(connection: Connection[F]): F[A] =
          fa.run(connection).handleErrorWith(e => f(e).run(connection))

        override def readOnly(connection: Connection[F]): F[A] =
          connection.setReadOnly(true) *> run(connection) <* connection.setReadOnly(false)

        override def commit(connection: Connection[F]): F[A] =
          connection.setReadOnly(false) *> connection.setAutoCommit(true) *> run(connection)

        override def rollback(connection: Connection[F]): F[A] =
          connection.setReadOnly(false) *> connection.setAutoCommit(false) *> run(connection) <* connection
            .rollback() <* connection.setAutoCommit(true)

        override def transaction(connection: Connection[F]): F[A] =
          val acquire =
            connection.setReadOnly(false) *> connection.setAutoCommit(false) *> MonadCancelThrow[F].pure(connection)

          val release = (connection: Connection[F], exitCase: ExitCase) =>
            (exitCase match
              case ExitCase.Errored(_) | ExitCase.Canceled => connection.rollback()
              case _                                       => connection.commit()
            )
              *> connection.setAutoCommit(true)

          Resource
            .makeCase(acquire)(release)
            .use(run)
