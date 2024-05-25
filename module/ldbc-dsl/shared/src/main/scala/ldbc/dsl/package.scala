/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc

import cats.data.Kleisli
import cats.implicits.*

import cats.effect.{ IO, Resource, Sync }
import cats.effect.kernel.Resource.ExitCase

import ldbc.sql.*
import ldbc.dsl.syntax.*

package object dsl:

  private trait SyncSyntax[F[_]: Sync]
    extends StringContextSyntax[F],
            ConnectionSyntax[F],
            QuerySyntax[F],
            CommandSyntax[F]:

    private def buildConnectionResource(acquire: F[Connection[F]]): Resource[F, Connection[F]] =
      val release: Connection[F] => F[Unit] = connection => connection.close()
      Resource.make(acquire)(release)

    extension [T](connectionKleisli: Kleisli[F, Connection[F], T])

      def readOnly(dataSource: DataSource[F]): F[T] =
        buildConnectionResource {
          for
            connection <- dataSource.getConnection
            _          <- connection.setReadOnly(true)
          yield connection
        }
          .use(connectionKleisli.run)

      def autoCommit(dataSource: DataSource[F]): F[T] =
        buildConnectionResource {
          for
            connection <- dataSource.getConnection
            _          <- connection.setReadOnly(false) >> connection.setAutoCommit(true)
          yield connection
        }
          .use(connectionKleisli.run)

      def transaction(dataSource: DataSource[F]): F[T] =
        (for
          connection <- buildConnectionResource {
                          for
                            connection <- dataSource.getConnection
                            _          <- connection.setReadOnly(false) >> connection.setAutoCommit(false)
                          yield connection
                        }
          transact <- Resource.makeCase(Sync[F].pure(connection)) {
                        case (conn, ExitCase.Errored(e)) => conn.rollback() >> Sync[F].raiseError(e)
                        case (conn, _)                   => conn.commit()
                      }
        yield transact).use(connectionKleisli.run)

      def rollback(dataSource: DataSource[F]): F[T] =
        val connectionResource = buildConnectionResource {
          for
            connection <- dataSource.getConnection
            _          <- connection.setAutoCommit(false)
          yield connection
        }
        connectionResource.use { connection =>
          connectionKleisli.run(connection) <* connection.rollback()
        }

  /**
   * Top-level imports provide aliases for the most commonly used types and modules. A typical starting set of imports
   * might look something like this.
   *
   * example:
   * {{{
   *   import ldbc.dsl.io.*
   * }}}
   */
  val io: SyncSyntax[IO] = new SyncSyntax[IO] {}
