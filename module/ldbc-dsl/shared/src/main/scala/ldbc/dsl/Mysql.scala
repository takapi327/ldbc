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
import ldbc.sql.logging.*

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
case class Mysql[F[_]: Temporal](statement: String, params: Seq[ParameterBinder[F]]) extends SQL[F]:

  @targetName("combine")
  override def ++(sql: SQL[F]): SQL[F] =
    Mysql[F](statement ++ " " ++ sql.statement, params ++ sql.params)

  override def update(using logHandler: LogHandler[F]): Query[F, Int] = QueryImpl[F, Int] { connection =>
    (for
      statement <- connection.prepareStatement(statement)
      result <- params.zipWithIndex.traverse {
                  case (param, index) => param.bind(statement, index + 1)
                } >> statement.executeUpdate() <* statement.close()
    yield result)
      .onError(ex => logHandler.run(LogEvent.ExecFailure(statement, params.map(_.parameter).toList, ex)))
      <* logHandler.run(LogEvent.Success(statement, params.map(_.parameter).toList))
  }

  override def returning[T <: String | Int | Long](using
    reader:     ResultSetReader[F, T],
    logHandler: LogHandler[F]
  ): Query[F, T] = QueryImpl[F, T] { connection =>
    given Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
      reader.read(resultSet, 1)
    }

    (for
      statement <- connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS)
      resultSet <- params.zipWithIndex.traverse {
                     case (param, index) => param.bind(statement, index + 1)
                   } >> statement.executeUpdate() >> statement.getGeneratedKeys()
      result <- summon[ResultSetConsumer[F, T]].consume(resultSet) <* statement.close()
    yield result)
      .onError(ex => logHandler.run(LogEvent.ExecFailure(statement, params.map(_.parameter).toList, ex)))
      <* logHandler.run(LogEvent.Success(statement, params.map(_.parameter).toList))
  }

  private[ldbc] override def connection[T](
    statement: String,
    params:    Seq[ParameterBinder[F]],
    consumer:  ResultSetConsumer[F, T]
  )(using logHandler: LogHandler[F]): Query[F, T] =
    QueryImpl[F, T] { connection =>
      for
        prepareStatement <- connection.prepareStatement(statement)
        resultSet <- params.zipWithIndex.traverse {
                       case (param, index) => param.bind(prepareStatement, index + 1)
                     } >> prepareStatement
                       .executeQuery()
        result <-
          consumer
            .consume(resultSet)
            .onError(ex => logHandler.run(LogEvent.ProcessingFailure(statement, params.map(_.parameter).toList, ex)))
            <* prepareStatement.close()
            <* logHandler.run(LogEvent.Success(statement, params.map(_.parameter).toList))
      yield result
    }
