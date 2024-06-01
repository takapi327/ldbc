/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.logging

import cats.Applicative
import cats.syntax.all.*

import cats.effect.Sync
import cats.effect.std.Console

/**
 * copied from doobie:
 * https://github.com/tpolecat/doobie/blob/main/modules/free/src/main/scala/doobie/util/log.scala#L42
 *
 * Provides additional processing for Doobie `LogEvent`s.
 */
trait LogHandler[F[_]]:

  def run(logEvent: LogEvent): F[Unit]

object LogHandler:

  def noop[F[_]: Applicative]: LogHandler[F] = (logEvent: LogEvent) => Applicative[F].unit

  def console[F[_]: Console: Sync]: LogHandler[F] =
    case LogEvent.Success(sql, args) =>
      Console[F].println(
        s"""Successful Statement Execution:
           |  $sql
           |
           | arguments = [${args.mkString(",")}]
           |""".stripMargin
      )
    case LogEvent.ProcessingFailure(sql, args, failure) =>
      Console[F].errorln(
        s"""Failed ResultSet Processing:
           |  $sql
           |
           | arguments = [${args.mkString(",")}]
           |""".stripMargin
      ) >> Console[F].printStackTrace(failure)
    case LogEvent.ExecFailure(sql, args, failure) =>
      Console[F].errorln(
        s"""Failed Statement Execution:
           |  $sql
           |
           | arguments = [${args.mkString(",")}]
           |""".stripMargin
      ) >> Console[F].printStackTrace(failure)
