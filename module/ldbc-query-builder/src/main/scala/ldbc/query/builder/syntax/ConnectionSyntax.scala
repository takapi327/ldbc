/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.syntax

import cats.data.Kleisli
import cats.syntax.all.*

import cats.effect.*
import cats.effect.kernel.Resource.ExitCase

import ldbc.sql.Connection

trait ConnectionSyntax[F[_]: Sync]:

  extension [T](connectionKleisli: Kleisli[F, Connection[F], T])

    /**
     * Functions for managing the processing of connections in a read-only manner.
     */
    def readOnly(connection: Connection[F]): F[T] =
      connection.setReadOnly(true) *> connectionKleisli.run(connection)

    /**
     * Functions to manage the processing of connections for writing.
     */
    def autoCommit(connection: Connection[F]): F[T] =
      connection.setReadOnly(false) *> connection.setAutoCommit(true) *> connectionKleisli.run(connection)

    /**
     * Functions to manage the processing of connections in a transaction.
     */
    def transaction(connection: Connection[F]): F[T] =
      val acquire = connection.setReadOnly(false) *> connection.setAutoCommit(false) *> Sync[F].pure(connection)

      val release = (connection: Connection[F], exitCase: ExitCase) =>
        exitCase match
          case ExitCase.Errored(_) | ExitCase.Canceled => connection.rollback()
          case _                                       => connection.commit()

      Resource
        .makeCase(acquire)(release)
        .use(connectionKleisli.run)

    /**
     * Functions to manage the processing of connections, always rolling back.
     */
    def rollback(connection: Connection[F]): F[T] =
      connection.setReadOnly(false) *> connection.setAutoCommit(false) *> connectionKleisli.run(
        connection
      ) <* connection.rollback()
