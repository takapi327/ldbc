/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.syntax.all.*
import cats.effect.{Temporal, Resource}
import cats.effect.kernel.Resource.ExitCase

import ldbc.sql.*

private[ldbc] case class QueryImpl[F[_]: Temporal, T](run: Connection[F] => F[T]) extends Query[F, T]:

  override def readOnly(connection: Connection[F]): F[T] =
    for
      _          <- connection.setReadOnly(true)
      result <- run(connection)
    yield result

  override def autoCommit(connection: Connection[F]): F[T] =
    for
      _ <- connection.setReadOnly(false)
      _ <- connection.setAutoCommit(true)
      result <- run(connection)
    yield result

  override def transaction(connection: Connection[F]): F[T] =
    val acquire = connection.setReadOnly(false) >> connection.setAutoCommit(false) >> Temporal[F].pure(connection)

    val release = (connection: Connection[F], exitCase: ExitCase) => exitCase match {
      case ExitCase.Errored(ex) => connection.rollback() *> Temporal[F].raiseError(ex)
      case _ => connection.commit()
    }

    Resource.makeCase(acquire)(release).use(run)

  override def rollback(connection: Connection[F]): F[T] =
    for
      _ <- connection.setReadOnly(false)
      _ <- connection.setAutoCommit(false)
      result <- run(connection)
      _ <- connection.rollback()
    yield result
