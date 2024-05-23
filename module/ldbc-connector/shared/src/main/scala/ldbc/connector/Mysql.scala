/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.annotation.targetName

import cats.data.Kleisli
import cats.syntax.all.*

import cats.effect.Temporal

import ldbc.sql.*

/**
 * A model with a query string and parameters to be bound to the query string that is executed by PreparedStatement,
 * etc.
 *
 * @param statement
 *   an SQL statement that may contain one or more '?' IN parameter placeholders
 * @param params
 *   statement has '?' that the statement has.
 * @tparam F
 *   The effect type
 */
case class Mysql[F[_]: Temporal](statement: String, params: List[ParameterBinder[F]]) extends SQL[F]:

  @targetName("combine")
  override def ++(sql: SQL[F]): SQL[F] =
    Mysql[F](statement ++ " " ++ sql.statement, params ++ sql.params)

  override def update: Query[F, Int] = QueryImpl[F, Int](statement, params.map(_.parameter)) { connection =>
    for
      statement <- connection.prepareStatement(statement)
      result <- params.zipWithIndex.traverse {
                  case (param, index) => param.bind(statement, index + 1)
                } >> statement.executeUpdate() <* statement.close()
    yield result
  }

  override def returning[T <: String | Int | Long](using
    reader: ResultSetReader[F, T]
  ): Query[F, T] = QueryImpl[F, T](statement, params.map(_.parameter)) { connection =>
    given Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
      reader.read(resultSet, 1)
    }

    for
      statement <- connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS)
      resultSet <- params.zipWithIndex.traverse {
                     case (param, index) => param.bind(statement, index + 1)
                   } >> statement.executeUpdate() >> statement.getGeneratedKeys()
      result <- summon[ResultSetConsumer[F, T]].consume(resultSet) <* statement.close()
    yield result
  }

  private[ldbc] override def connection[T](
    statement: String,
    params:    Seq[ParameterBinder[F]],
    consumer:  ResultSetConsumer[F, T]
  ): Query[F, T] =
    QueryImpl[F, T](statement, params.map(_.parameter).toList) { connection =>
      for
        prepareStatement <- connection.prepareStatement(statement)
        resultSet <- params.zipWithIndex.traverse {
                       case (param, index) => param.bind(prepareStatement, index + 1)
                     } >> prepareStatement
                       .executeQuery()
        result <- consumer.consume(resultSet) <* prepareStatement.close()
      yield result
    }
