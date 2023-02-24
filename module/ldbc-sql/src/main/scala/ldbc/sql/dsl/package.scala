/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.sql

import javax.sql.DataSource

import cats.data.Kleisli
import cats.implicits.*

import cats.effect.{ IO, Resource }

import ldbc.sql.ResultSetConsumer

package object dsl:

  val io: SQLSyntax[IO] = new SQLSyntax[IO]:

    extension (sql: SQL[IO])
      def query[T](using consumer: ResultSetConsumer[IO, T]): Kleisli[IO, DataSource, T] = Kleisli { dataSource =>
        val acquire: IO[Connection[IO]] = IO.blocking(dataSource.getConnection).map(Connection(_))
        val release: Connection[IO] => IO[Unit] = connection => connection.close()
        val resource: Resource[IO, Connection[IO]] = Resource.make(acquire)(release)
        resource.use(connection =>
          for
            statement <- connection.prepareStatement(sql.statement)
            resultSet <- sql.params.zipWithIndex.traverse {
              case (param, index) => param.bind(statement, index + 1)
            } >> statement.executeQuery()
            result <- consumer.consume(resultSet) <* statement.close()
          yield result
        )
      }
