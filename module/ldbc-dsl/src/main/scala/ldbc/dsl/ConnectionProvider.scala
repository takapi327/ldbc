/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import cats.data.Kleisli
import cats.implicits.*
import cats.effect.Sync

import ldbc.sql.*
import ldbc.sql.util.FactoryCompat
import ldbc.sql.logging.*

/**
 * Trait provides a connection method to the database.
 *
 * @tparam F
 *   The effect type
 */
trait ConnectionProvider[F[_]: Sync]:

  private def connection[T](
    statement: String,
    params:    Seq[ParameterBinder[F]],
    consumer:  ResultSetConsumer[F, T]
  )(using logHandler: LogHandler[F]): Kleisli[F, Connection[F], T] =
    Kleisli { connection =>
      for
        prepareStatement <- connection.prepareStatement(statement)
        resultSet <- params.zipWithIndex.traverse {
                       case (param, index) => param.bind(prepareStatement, index + 1)
                     } >> prepareStatement
                       .executeQuery()
                       .onError(ex =>
                         logHandler.run(
                           LogEvent.ExecFailure(statement, params.map(_.parameter).toList, ex)
                         )
                       )
        result <-
          consumer
            .consume(resultSet)
            .onError(ex => logHandler.run(LogEvent.ProcessingFailure(statement, params.map(_.parameter).toList, ex)))
            <* prepareStatement.close()
            <* logHandler.run(LogEvent.Success(statement, params.map(_.parameter).toList))
      yield result
    }

  /**
   * Methods for returning an array of data to be retrieved from the database.
   */
  protected def connectionToList[T](
    statement: String,
    params:    Seq[ParameterBinder[F]]
  )(using Kleisli[F, ResultSet[F], T], LogHandler[F], FactoryCompat[T, List[T]]): Kleisli[F, Connection[F], List[T]] =
    connection[List[T]](statement, params, summon[ResultSetConsumer[F, List[T]]])

  /**
   * A method to return the data to be retrieved from the database as Option type. If there are multiple data, the first
   * one is retrieved.
   */
  protected def connectionToHeadOption[T](
    statement: String,
    params:    Seq[ParameterBinder[F]]
  )(using Kleisli[F, ResultSet[F], T], LogHandler[F]): Kleisli[F, Connection[F], Option[T]] =
    connection[Option[T]](statement, params, summon[ResultSetConsumer[F, Option[T]]])

  /**
   * A method to return the data to be retrieved from the database as is. If the data does not exist, an exception is
   * raised. Use the [[connectionToHeadOption]] method if you want to retrieve individual data.
   */
  protected def connectionToUnsafe[T](
    statement: String,
    params:    Seq[ParameterBinder[F]]
  )(using Kleisli[F, ResultSet[F], T], LogHandler[F]): Kleisli[F, Connection[F], T] =
    connection[T](statement, params, summon[ResultSetConsumer[F, T]])
