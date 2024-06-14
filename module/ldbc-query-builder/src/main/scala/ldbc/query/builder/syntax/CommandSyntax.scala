/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.syntax

import cats.data.Kleisli
import cats.implicits.*

import cats.effect.Temporal

import ldbc.sql.{ Statement, ResultSet }
import ldbc.dsl.*
import ldbc.query.builder.statement.Command

trait CommandSyntax[F[_]: Temporal]:

  extension (command: Command)
    def update: Executor[F, Int] =
      Executor.Impl[F, Int](
        command.statement,
        command.params,
        connection =>
          for
            prepareStatement <- connection.prepareStatement(command.statement)
            result <- command.params.zipWithIndex.traverse {
                        case (param, index) => param.bind[F](prepareStatement, index + 1)
                      } >> prepareStatement.executeUpdate() <* prepareStatement.close()
          yield result
      )

    def returning[T <: String | Int | Long](using reader: ResultSetReader[F, T]): Executor[F, T] =
      given Kleisli[F, ResultSet[F], T] = Kleisli(resultSet => reader.read(resultSet, 1))

      Executor.Impl[F, T](
        command.statement,
        command.params,
        connection =>
          for
            prepareStatement <- connection.prepareStatement(command.statement, Statement.RETURN_GENERATED_KEYS)
            resultSet <- command.params.zipWithIndex.traverse {
                           case (param, index) => param.bind[F](prepareStatement, index + 1)
                         } >> prepareStatement.executeUpdate() >> prepareStatement.getGeneratedKeys()
            result <- summon[ResultSetConsumer[F, T]].consume(resultSet) <* prepareStatement.close()
          yield result
      )
