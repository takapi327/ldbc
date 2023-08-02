/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.syntax

import cats.data.Kleisli
import cats.implicits.*
import cats.effect.Sync

import ldbc.sql.*
import ldbc.dsl.statement.Query
import ldbc.dsl.logging.{ LogEvent, LogHandler }

trait QuerySyntax[F[_]: Sync]:

  extension [T](query: Query[F, T])

    private def connection[A](consumer: ResultSetConsumer[F, A])(using
      logHandler:                       LogHandler[F]
    ): Kleisli[F, Connection[F], A] =
      Kleisli { connection =>
        for
          statement <- connection.prepareStatement(query.statement)
          resultSet <- query.params.zipWithIndex.traverse {
                         case (param, index) => param.bind(statement, index + 1)
                       } >> statement
                         .executeQuery()
                         .onError(ex =>
                           logHandler.run(
                             LogEvent.ExecFailure(query.statement, query.params.map(_.parameter).toList, ex)
                           )
                         )
          result <-
            consumer
              .consume(resultSet)
              .onError(ex =>
                logHandler.run(LogEvent.ProcessingFailure(query.statement, query.params.map(_.parameter).toList, ex))
              )
              <* statement.close()
              <* logHandler.run(LogEvent.Success(query.statement, query.params.map(_.parameter).toList))
        yield result
      }

    def headOption[A](func: T => Kleisli[F, ResultSet[F], A])(using
      LogHandler[F]
    ): Kleisli[F, Connection[F], Option[A]] =
      given Kleisli[F, ResultSet[F], A] = func(query.columns)
      connection[Option[A]](summon[ResultSetConsumer[F, Option[A]]])

    def toList[A](func: T => Kleisli[F, ResultSet[F], A])(using LogHandler[F]): Kleisli[F, Connection[F], List[A]] =
      given Kleisli[F, ResultSet[F], A] = func(query.columns)
      connection[List[A]](summon[ResultSetConsumer[F, List[A]]])

    def unsafe[A](func: T => Kleisli[F, ResultSet[F], A])(using LogHandler[F]): Kleisli[F, Connection[F], A] =
      given Kleisli[F, ResultSet[F], A] = func(query.columns)
      connection[A](summon[ResultSetConsumer[F, A]])
