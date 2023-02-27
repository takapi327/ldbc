/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc

import javax.sql.DataSource

import cats.data.Kleisli
import cats.implicits.*

import cats.effect.{ Sync, IO, Resource }
import cats.effect.kernel.Resource.ExitCase

import ldbc.sql.{ Connection, ResultSetConsumer }

package object dsl:

  private trait SyncSyntax[F[_]: Sync] extends SQLSyntax[F], ConnectionSyntax[F]:
    private def buildConnectionResource(acquire: F[Connection[F]]): Resource[F, Connection[F]] =
      val release: Connection[F] => F[Unit] = connection => connection.close()
      Resource.make(acquire)(release)

    extension (sql: SQL[F])
      def query[T](using consumer: ResultSetConsumer[F, T]): Kleisli[F, Connection[F], T] = Kleisli { connection =>
        for
          statement <- connection.prepareStatement(sql.statement)
          resultSet <- sql.params.zipWithIndex.traverse {
            case (param, index) => param.bind(statement, index + 1)
          } >> statement.executeQuery()
          result <- consumer.consume(resultSet) <* statement.close()
        yield result
      }

      def update(): Kleisli[F, Connection[F], Int] = Kleisli { connection =>
        for
          statement <- connection.prepareStatement(sql.statement)
          result <- sql.params.zipWithIndex.traverse {
            case (param, index) => param.bind(statement, index + 1)
          } >> statement.executeUpdate()
        yield result
      }

    extension [T](connectionKleisli: Kleisli[F, Connection[F], T])

      def readOnly: Kleisli[F, DataSource, T] = Kleisli { dataSource =>
        buildConnectionResource {
          for
            connection <- Sync[F].blocking(dataSource.getConnection).map(ConnectionIO[F])
            _          <- connection.setReadOnly(true)
          yield connection
        }
          .use(connectionKleisli.run)
      }

      def autoCommit: Kleisli[F, DataSource, T] = Kleisli { dataSource =>
        buildConnectionResource {
          for
            connection <- Sync[F].blocking(dataSource.getConnection).map(ConnectionIO[F])
            _          <- connection.setReadOnly(false) >> connection.setAutoCommit(true)
          yield connection
        }
          .use(connectionKleisli.run)
      }

      def transaction: Kleisli[F, DataSource, T] = Kleisli { dataSource =>
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
      }

  val io: SyncSyntax[IO] = new SyncSyntax[IO] {}
