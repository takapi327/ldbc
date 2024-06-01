/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.kernel.Resource.ExitCase

import ldbc.sql.*
import ldbc.sql.logging.*

case class ExecutorImpl[F[_]: Temporal, T](
  statement: String,
  params:    List[ParameterBinder[F]],
  run:       Connection[F] => F[T]
) extends Executor[F, T]:

  private[ldbc] def execute(connection: Connection[F])(using logHandler: LogHandler[F]): F[T] =
    run(connection)
      .onError(ex => logHandler.run(LogEvent.ProcessingFailure(statement, params.map(_.parameter), ex)))
      <* logHandler.run(LogEvent.Success(statement, params.map(_.parameter)))

  override def transaction(connection: Connection[F])(using logHandler: LogHandler[F]): F[T] =
    val acquire = connection.setReadOnly(false) *> connection.setAutoCommit(false) *> Temporal[F].pure(connection)

    val release = (connection: Connection[F], exitCase: ExitCase) =>
      exitCase match
        case ExitCase.Errored(_) | ExitCase.Canceled => connection.rollback()
        case _                                       => connection.commit()

    Resource
      .makeCase(acquire)(release)
      .use(execute)
