/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.syntax

import cats.data.Kleisli
import cats.implicits.*
import cats.effect.Sync

import ldbc.core.attribute.AutoInc
import ldbc.sql.*
import ldbc.query.builder.ColumnReader
import ldbc.query.builder.statement.{ Command, Insert }
import ldbc.dsl.logging.{ LogEvent, LogHandler }

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

    def update[T](func: Table[P] => ColumnReader[F, T])(using logHandler: LogHandler[F]): Kleisli[F, Connection[F], T] =
      Kleisli { connection =>
        val column = func(insert.table)
        require(column.attributes.contains(AutoInc()), "Auto Increment is not set for the specified column.")
        given Kleisli[F, ResultSet[F], T] = column.read(1)
        (for
          statement <- connection.prepareStatement(insert.statement, Statement.Generated.RETURN_GENERATED_KEYS)
          resultSet <- insert.params.zipWithIndex.traverse {
                         case (param, index) => param.bind(statement, index + 1)
                       } >> statement.executeUpdate() >> statement.getGeneratedKeys()
          result <- summon[ResultSetConsumer[F, T]].consume(resultSet) <* statement.close()
        yield result)
          .onError(ex =>
            logHandler.run(LogEvent.ExecFailure(insert.statement, insert.params.map(_.parameter).toList, ex))
          )
          <* logHandler.run(LogEvent.Success(insert.statement, insert.params.map(_.parameter).toList))
      }
