/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

//package ldbc.dsl
//
//import cats.*
//import cats.syntax.all.*
//
//import cats.effect.*
//import cats.effect.kernel.Resource.ExitCase
//
//import ldbc.sql.Connection
//
//import ldbc.dsl.logging.*
//
//case class ExecutorImpl[F[_]: Temporal, T](
//  statement: String,
//  params:    List[ParameterBinder[F]],
//  run:       Connection[F] => F[T]
//) extends Executor[F, T]:
//
//  private[ldbc] def execute(connection: Connection[F])(using logHandler: LogHandler[F]): F[T] =
//    run(connection)
//      .onError(ex => logHandler.run(LogEvent.ProcessingFailure(statement, params.map(_.parameter), ex)))
//      <* logHandler.run(LogEvent.Success(statement, params.map(_.parameter)))
