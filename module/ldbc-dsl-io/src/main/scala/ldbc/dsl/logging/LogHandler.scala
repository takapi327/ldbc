/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.logging

import cats.implicits.*

import cats.effect.Sync
import cats.effect.std.Console

/** copied from doobie:
  * https://github.com/tpolecat/doobie/blob/main/modules/free/src/main/scala/doobie/util/log.scala#L42
  *
  * Provides additional processing for Doobie `LogEvent`s.
  */
trait LogHandler[F[_]]:

  def run(logEvent: LogEvent): F[Unit]

object LogHandler:

  /** LogHandler for simple log output using Console.
    *
    * In a production environment, it is recommended to use a customized LogHandler using log4j, etc. instead of this
    * one.
    *
    * @tparam F
    *   The effect type
    */
  def consoleLogger[F[_]: Console: Sync]: LogHandler[F] =
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
