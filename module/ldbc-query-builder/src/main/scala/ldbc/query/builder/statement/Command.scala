/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.query.builder.statement

import ldbc.sql.ParameterBinder

/** Trait for building Statements to be added, updated, and deleted.
 *
 * @tparam F
 *   The effect type
 */
private[ldbc] trait Command[F[_]]:

  /** SQL statement string
   */
  def statement: String

  /** A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
   * only.
   */
  def params: Seq[ParameterBinder[F]]
