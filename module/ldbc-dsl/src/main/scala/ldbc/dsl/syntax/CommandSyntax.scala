/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.dsl.syntax

import cats.data.Kleisli
import cats.implicits.*
import cats.effect.Sync

import ldbc.sql.Connection
import ldbc.query.builder.statement.Command
import ldbc.dsl.logging.{ LogEvent, LogHandler }

trait CommandSyntax[F[_]: Sync]:

  extension (command: Command[F])

    def update(using logHandler: LogHandler[F]): Kleisli[F, Connection[F], Int] = Kleisli { connection =>
      (for
        statement <- connection.prepareStatement(command.statement)
        result <- command.params.zipWithIndex.traverse {
          case (param, index) => param.bind(statement, index + 1)
        } >> statement.executeUpdate()
      yield result)
        .onError(ex => logHandler.run(LogEvent.ExecFailure(command.statement, command.params.map(_.parameter).toList, ex)))
        <* logHandler.run(LogEvent.Success(command.statement, command.params.map(_.parameter).toList))
    }
