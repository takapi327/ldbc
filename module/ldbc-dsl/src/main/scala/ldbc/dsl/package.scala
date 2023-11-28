/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc

import javax.sql.DataSource

import cats.data.Kleisli
import cats.implicits.*

import cats.effect.{ IO, Resource, Sync }
import cats.effect.kernel.Resource.ExitCase

import ldbc.core.Database as CoreDatabase
import ldbc.sql.*
import ldbc.dsl.syntax.*

package object dsl:

  private trait SyncSyntax[F[_]: Sync]
    extends StringContextSyntax[F],
            SQLSyntax[F],
            ConnectionSyntax[F],
            QuerySyntax[F],
            CommandSyntax[F],
            DatabaseSyntax[F]:

    private def buildConnectionResource(acquire: F[Connection[F]]): Resource[F, Connection[F]] =
      val release: Connection[F] => F[Unit] = connection => connection.close()
      Resource.make(acquire)(release)

    extension [T](connectionKleisli: Kleisli[F, Connection[F], T])

      def readOnly(dataSource: DataSource): F[T] =
        buildConnectionResource {
          for
            connection <- Sync[F].blocking(dataSource.getConnection).map(ConnectionIO[F])
            _          <- connection.setReadOnly(true)
          yield connection
        }
          .use(connectionKleisli.run)

      def autoCommit(dataSource: DataSource): F[T] =
        buildConnectionResource {
          for
            connection <- Sync[F].blocking(dataSource.getConnection).map(ConnectionIO[F])
            _          <- connection.setReadOnly(false) >> connection.setAutoCommit(true)
          yield connection
        }
          .use(connectionKleisli.run)

      def transaction(dataSource: DataSource): F[T] =
        (for
          connection <- buildConnectionResource {
                          for
                            connection <- Sync[F].blocking(dataSource.getConnection).map(ConnectionIO[F])
                            _          <- connection.setReadOnly(false) >> connection.setAutoCommit(false)
                          yield connection
                        }
          transact <- Resource.makeCase(Sync[F].pure(connection)) {
                        case (conn, ExitCase.Errored(e)) => conn.rollback() >> Sync[F].raiseError(e)
                        case (conn, _)                   => conn.commit()
                      }
        yield transact).use(connectionKleisli.run)

      def rollback(dataSource: DataSource): F[T] =
        val connectionResource = buildConnectionResource {
          for
            connection <- Sync[F].blocking(dataSource.getConnection).map(ConnectionIO[F])
            _ <- connection.setAutoCommit(false)
          yield connection
        }
        connectionResource.use { connection =>
          connectionKleisli.run(connection) <* connection.rollback()
        }

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

      def fromDataSource(dataSource: DataSource): Database[F] =
        Database.fromDataSource[F](database.databaseType, database.name, database.host, database.port, dataSource)

  val io: SyncSyntax[IO] = new SyncSyntax[IO] {}
