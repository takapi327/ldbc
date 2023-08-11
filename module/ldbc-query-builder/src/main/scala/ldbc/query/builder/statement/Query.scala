/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder.statement

import ldbc.sql.ParameterBinder

/** Trait for constructing Statements that set conditions.
  *
  * @tparam F
  *   The effect type
  * @tparam T
  *   Column Tuples
  */
private[ldbc] trait Query[F[_], T <: Tuple]:

  /** SQL statement string
    */
  def statement: String

  /** Union-type column list
    */
  def columns: T

  /** A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
    * only.
    */
  def params: Seq[ParameterBinder[F]]
