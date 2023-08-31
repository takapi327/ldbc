/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.dsl

import cats.data.Kleisli
import cats.implicits.*

import cats.effect.Sync

import ldbc.sql.*
import ldbc.dsl.logging.{ LogEvent, LogHandler }

case class Command[F[_]: Sync, T](
  statement: String,
  params:    Seq[ParameterBinder[F]],
  resultSet: Kleisli[F, ResultSet[F], T]
):

  private def connection[A](consumer: ResultSetConsumer[F, A])(using
    logHandler: LogHandler[F]
  ): Kleisli[F, Connection[F], A] =
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
            .onError(ex =>
              logHandler.run(LogEvent.ProcessingFailure(statement, params.map(_.parameter).toList, ex))
            )
            <* prepareStatement.close()
            <* logHandler.run(LogEvent.Success(statement, params.map(_.parameter).toList))
      yield result
    }

  def toList: LogHandler[F] ?=> Kleisli[F, Connection[F], List[T]] =
    given Kleisli[F, ResultSet[F], T] = resultSet
    connection[List[T]](
      summon[ResultSetConsumer[F, List[T]]]
    )

  def headOption: LogHandler[F] ?=> Kleisli[F, Connection[F], Option[T]] =
    given Kleisli[F, ResultSet[F], T] = resultSet
    connection[Option[T]](
      summon[ResultSetConsumer[F, Option[T]]]
    )

  def unsafe: LogHandler[F] ?=> Kleisli[F, Connection[F], T] =
    given Kleisli[F, ResultSet[F], T] = resultSet
    connection[T](
      summon[ResultSetConsumer[F, T]]
    )
