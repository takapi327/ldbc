/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.syntax

import scala.deriving.Mirror

import cats.data.Kleisli
import cats.implicits.*
import cats.effect.Sync

import ldbc.core.attribute.AutoInc
import ldbc.core.interpreter.Tuples as CoreTuples
import ldbc.sql.*
import ldbc.sql.logging.LogEvent
import ldbc.query.builder.statement.{ Command, Insert }
import ldbc.dsl.logging.LogHandler

trait CommandSyntax[F[_]: Sync]:

  extension (command: Command[F])
    def update(using logHandler: LogHandler[F]): Kleisli[F, Connection[F], Int] = Kleisli { connection =>
      (for
        statement <- connection.prepareStatement(command.statement)
        result <- command.params.zipWithIndex.traverse {
                    case (param, index) => param.bind(statement, index + 1)
                  } >> statement.executeUpdate() <* statement.close()
      yield result)
        .onError(ex =>
          logHandler.run(LogEvent.ExecFailure(command.statement, command.params.map(_.parameter).toList, ex))
        )
        <* logHandler.run(LogEvent.Success(command.statement, command.params.map(_.parameter).toList))
    }

  implicit class InsertOps[P <: Product](insert: Insert[F, P]):
    def update(using logHandler: LogHandler[F]): Kleisli[F, Connection[F], Int] = Kleisli { connection =>
      (for
        statement <- connection.prepareStatement(insert.statement)
        result <- insert.params.zipWithIndex.traverse {
                    case (param, index) => param.bind(statement, index + 1)
                  } >> statement.executeUpdate() <* statement.close()
      yield result)
        .onError(ex =>
          logHandler.run(LogEvent.ExecFailure(insert.statement, insert.params.map(_.parameter).toList, ex))
        )
        <* logHandler.run(LogEvent.Success(insert.statement, insert.params.map(_.parameter).toList))
    }

    def returning[Tag <: Singleton](tag: Tag)(using
      mirror: Mirror.ProductOf[P],
      index:  ValueOf[CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]],
      reader: ResultSetReader[
        F,
        Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]
      ],
      logHandler: LogHandler[F]
    ): Kleisli[F, Connection[F], Tuple.Elem[
      mirror.MirroredElemTypes,
      CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]
    ]] =
      Kleisli { connection =>
        val column = insert.tableQuery.selectDynamic[Tag](tag)
        require(
          column.attributes.contains(AutoInc()),
          s"Auto Increment is not set on the ${ column.label } column of the ${ insert.tableQuery.table._name } table."
        )
        given Kleisli[
          F,
          ResultSet[F],
          Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]
        ] = Kleisli { resultSet => reader.read(resultSet, 1) }
        (for
          statement <- connection.prepareStatement(insert.statement, Statement.RETURN_GENERATED_KEYS)
          resultSet <- insert.params.zipWithIndex.traverse {
                         case (param, index) => param.bind(statement, index + 1)
                       } >> statement.executeUpdate() >> statement.getGeneratedKeys()
          result <- summon[ResultSetConsumer[
                      F,
                      Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]
                    ]].consume(resultSet) <* statement.close()
        yield result)
          .onError(ex =>
            logHandler.run(LogEvent.ExecFailure(insert.statement, insert.params.map(_.parameter).toList, ex))
          )
          <* logHandler.run(LogEvent.Success(insert.statement, insert.params.map(_.parameter).toList))
      }
