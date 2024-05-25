/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.logging

import cats.implicits.*

import cats.effect.Sync
import cats.effect.std.Console

import ldbc.sql.logging.*

object ConsoleLogHandler:

  /**
   * LogHandler for simple log output using Console.
   *
   * In a production environment, it is recommended to use a customized LogHandler using log4j, etc. instead of this
   * one.
   *
   * @tparam F
   *   The effect type
   */
  def apply[F[_]: Console: Sync]: LogHandler[F] =
    case LogEvent.Success(sql, args) =>
      Console[F].println(
        s"""Successful Statement Execution:
           |  $sql
           |
           | arguments = [${ args.mkString(",") }]
           |""".stripMargin
      )
    case LogEvent.ProcessingFailure(sql, args, failure) =>
      Console[F].errorln(
        s"""Failed ResultSet Processing:
           |  $sql
           |
           | arguments = [${ args.mkString(",") }]
           |""".stripMargin
      ) >> Console[F].printStackTrace(failure)
    case LogEvent.ExecFailure(sql, args, failure) =>
      Console[F].errorln(
        s"""Failed Statement Execution:
           |  $sql
           |
           | arguments = [${ args.mkString(",") }]
           |""".stripMargin
      ) >> Console[F].printStackTrace(failure)