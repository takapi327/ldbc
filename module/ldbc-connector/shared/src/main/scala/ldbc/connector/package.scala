/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc

import cats.data.Kleisli
import cats.syntax.all.*

import cats.effect.*
import cats.effect.kernel.Resource.ExitCase

import ldbc.sql.*

package object connector:

  private trait StringContextSyntax[F[_]: Temporal]:

    extension (sc: StringContext)
      inline def sql(inline args: ParameterBinder[F]*): SQL[F] =
        val strings     = sc.parts.iterator
        val expressions = args.iterator
        Mysql[F](strings.mkString("?"), expressions.toList)

  private trait ConnectionSyntax[F[_]: Temporal]:

    extension [T](connectionKleisli: Kleisli[F, Connection[F], T])

      def readOnly(connection: Connection[F]): F[T] =
        connection.setReadOnly(true) *> connectionKleisli.run(connection)

      def autoCommit(connection: Connection[F]): F[T] =
        connection.setReadOnly(false) *> connection.setAutoCommit(true) *> connectionKleisli.run(connection)

      def transaction(connection: Connection[F]): F[T] =
        val acquire = connection.setReadOnly(false) *> connection.setAutoCommit(false) *> Temporal[F].pure(connection)

        val release = (connection: Connection[F], exitCase: ExitCase) =>
          exitCase match
            case ExitCase.Errored(_) | ExitCase.Canceled => connection.rollback()
            case _                                       => connection.commit()

        Resource
          .makeCase(acquire)(release)
          .use(connectionKleisli.run)

      def rollback(connection: Connection[F]): F[T] =
        connection.setReadOnly(false) *> connection.setAutoCommit(false) *> connectionKleisli.run(
          connection
        ) <* connection.rollback()

  /**
   * Top-level imports provide aliases for the most commonly used types and modules. A typical starting set of imports
   * might look something like this.
   *
   * example:
   * {{{
   *   import ldbc.connector.io.*
   * }}}
   */
  val io: StringContextSyntax[IO] & ConnectionSyntax[IO] = new StringContextSyntax[IO] with ConnectionSyntax[IO] {}
