/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

import cats.Monad
import cats.syntax.all.*

import ldbc.sql.logging.*

/**
 * A trait that represents the execution of a query.
 *
 * @tparam F
 *   The effect type
 * @tparam T
 *   The result type of the query
 */
trait Executor[F[_]: Monad, T]:

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
  def transaction(connection: Connection[F])(using LogHandler[F]): F[T]

object Executor:
  def pure[F[_]: Monad, T](value: T): Executor[F, T] =
    new Executor[F, T]:
      override private[ldbc] def execute(connection: Connection[F])(using LogHandler[F]): F[T] = Monad[F].pure(value)
      override def readOnly(connection:              Connection[F])(using LogHandler[F]): F[T] = Monad[F].pure(value)
      override def commit(connection:                Connection[F])(using LogHandler[F]): F[T] = Monad[F].pure(value)
      override def rollback(connection:              Connection[F])(using LogHandler[F]): F[T] = Monad[F].pure(value)
      override def transaction(connection:           Connection[F])(using LogHandler[F]): F[T] = Monad[F].pure(value)
