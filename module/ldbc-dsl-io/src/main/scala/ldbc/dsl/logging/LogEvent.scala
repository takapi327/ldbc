/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.dsl.logging

/** copied from doobie:
 * https://github.com/tpolecat/doobie/blob/main/modules/free/src/main/scala/doobie/util/log.scala#L22
 * 
 * Algebraic type of events that can be passed to a `LogHandler`,
 * both parameterized by the argument type of the SQL input parameters (this is typically an `HList`).
 */
sealed trait LogEvent:

  /** The complete SQL string as seen by JDBC. */
  def sql: String

  /** The query arguments. */
  def args: List[Any]

object LogEvent:

  final case class Success(sql: String, args: List[Any]) extends LogEvent
  final case class ExecFailure(sql: String, args: List[Any], failure: Throwable) extends LogEvent
