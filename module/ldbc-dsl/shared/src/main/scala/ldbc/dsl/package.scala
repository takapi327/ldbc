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

import ldbc.core.Database as CoreDatabase
import ldbc.core.model.Enum
import ldbc.sql.*
import ldbc.dsl.syntax.*

package object dsl:

  private trait SyncSyntax[F[_]: Sync]
    extends StringContextSyntax[F],
            SQLSyntax[F],
            ConnectionSyntax[F],
            QuerySyntax[F],
            CommandSyntax[F],
            DatabaseSyntax[F],
            internalSyntax,
            Alias:

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

      def readOnly(database: Database[F]): F[T] =
        database.readOnly(connectionKleisli)

      def autoCommit(dataSource: DataSource[F]): F[T] =
        buildConnectionResource {
          for
            connection <- dataSource.getConnection
            _          <- connection.setReadOnly(false) >> connection.setAutoCommit(true)
          yield connection
        }
          .use(connectionKleisli.run)

      def autoCommit(database: Database[F]): F[T] =
        database.autoCommit(connectionKleisli)

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

      def transaction(database: Database[F]): F[T] =
        database.transaction(connectionKleisli)

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

      def rollback(database: Database[F]): F[T] =
        database.rollback(connectionKleisli)

    extension (database: CoreDatabase)

      def fromDriverManager(): Database[F] =
        Database.fromDriverManager[F](database.databaseType, database.name, database.host, database.port, None, None)

      def fromDriverManager(
        user:     String,
        password: String
      ): Database[F] =
        Database.fromDriverManager[F](
          database.databaseType,
          database.name,
          database.host,
          database.port,
          Some(user),
          Some(password)
        )

      def fromDataSource(dataSource: DataSource[F]): Database[F] =
        Database.fromDataSource[F](database.databaseType, database.name, database.host, database.port, dataSource)

  /**
   * Top-level imports provide aliases for the most commonly used types and modules. A typical starting set of imports
   * might look something like this.
   *
   * example:
   * {{{
   *   import ldbc.dsl.io.*
   * }}}
   */
  val io: SyncSyntax[IO] = new SyncSyntax[IO]:
    given [F[_]]: Parameter[F, Enum] with
      override def bind(statement: PreparedStatement[F], index: Int, value: Enum): F[Unit] =
        statement.setString(index, value.toString)
