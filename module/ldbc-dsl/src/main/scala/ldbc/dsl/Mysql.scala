/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

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
    Mysql[F](statement ++ sql.statement, params ++ sql.params)

  override def to[T](using consumer: ResultSetConsumer[F, T]): Executor[F, T] =
    ExecutorImpl[F, T](
      statement,
      params,
      connection =>
        for
          prepareStatement <- connection.prepareStatement(statement)
          resultSet <- params.zipWithIndex.traverse {
                         case (param, index) => param.bind(prepareStatement, index + 1)
                       } >> prepareStatement.executeQuery()
          result <- consumer.consume(resultSet) <* prepareStatement.close()
        yield result
    )

  override def update: Executor[F, Int] =
    ExecutorImpl[F, Int](
      statement,
      params,
      connection =>
        for
          prepareStatement <- connection.prepareStatement(statement)
          result <- params.zipWithIndex.traverse {
                      case (param, index) => param.bind(prepareStatement, index + 1)
                    } >> prepareStatement.executeUpdate() <* prepareStatement.close()
        yield result
    )

  override def returning[T <: String | Int | Long](using reader: ResultSetReader[F, T]): Executor[F, T] =
    given Kleisli[F, ResultSet[F], T] = Kleisli(resultSet => reader.read(resultSet, 1))

    ExecutorImpl[F, T](
      statement,
      params,
      connection =>
        for
          prepareStatement <- connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS)
          resultSet <- params.zipWithIndex.traverse {
                         case (param, index) => param.bind(prepareStatement, index + 1)
                       } >> prepareStatement.executeUpdate() >> prepareStatement.getGeneratedKeys()
          result <- summon[ResultSetConsumer[F, T]].consume(resultSet) <* prepareStatement.close()
        yield result
    )
