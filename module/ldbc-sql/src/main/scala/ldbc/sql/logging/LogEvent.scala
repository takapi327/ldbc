/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql.logging

/**
 * copied from doobie:
 * https://github.com/tpolecat/doobie/blob/main/modules/free/src/main/scala/doobie/util/log.scala#L22
 *
 * Algebraic type of events that can be passed to a `LogHandler`, both parameterized by the argument type of the SQL
 * input parameters (this is typically an `HList`).
 */
sealed trait LogEvent:

  /** The complete SQL string as seen by JDBC. */
  def sql: String

  /** The query arguments. */
  def args: List[Any]

object LogEvent:

  final case class Success(sql: String, args: List[Any])                               extends LogEvent
  final case class ProcessingFailure(sql: String, args: List[Any], failure: Throwable) extends LogEvent
  final case class ExecFailure(sql: String, args: List[Any], failure: Throwable)       extends LogEvent
