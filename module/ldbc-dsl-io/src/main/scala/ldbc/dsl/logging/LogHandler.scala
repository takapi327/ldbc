/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.dsl.logging

/** copied from doobie:
 * https://github.com/tpolecat/doobie/blob/main/modules/free/src/main/scala/doobie/util/log.scala#L42
 *
 * Provides additional processing for Doobie `LogEvent`s.
 */
trait LogHandler[F[_]]:

  def run(logEvent: LogEvent): F[Unit]
