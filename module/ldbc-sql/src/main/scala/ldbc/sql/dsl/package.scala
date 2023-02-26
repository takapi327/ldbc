/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.sql

import cats.data.Kleisli
import cats.implicits.*

import cats.effect.{ IO, Resource }

import ldbc.sql.{ Connection, ResultSetConsumer }

package object dsl:

  val io: SQLSyntax[IO] = new SQLSyntax[IO]:

    extension (sql: SQL[IO])
      def query[T](using consumer: ResultSetConsumer[IO, T]): Kleisli[IO, Connection[IO], T] = Kleisli { connection =>
        for
          statement <- connection.prepareStatement(sql.statement)
          resultSet <- sql.params.zipWithIndex.traverse {
            case (param, index) => param.bind(statement, index + 1)
          } >> statement.executeQuery()
          result <- consumer.consume(resultSet) <* statement.close()
        yield result
      }

      def update(): Kleisli[IO, Connection[IO], Int] = Kleisli { connection =>
        for
          statement <- connection.prepareStatement(sql.statement)
          result <- sql.params.zipWithIndex.traverse {
            case (param, index) => param.bind(statement, index + 1)
          } >> statement.executeUpdate()
        yield result
      }
