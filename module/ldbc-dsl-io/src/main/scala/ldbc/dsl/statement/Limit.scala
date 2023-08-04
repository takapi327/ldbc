/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.dsl.statement

import ldbc.core.Table
import ldbc.dsl.{ Parameter, ParameterBinder }

/** A model for constructing LIMIT statements in MySQL.
 *
 * @param table
 *   Trait for generating SQL table information.
 * @param statement
 *   SQL statement string
 * @param columns
 *   Union-type column list
 * @param params
 *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
 *   only.
 * @tparam F
 *   The effect type
 * @tparam P
 *   Base trait for all products
 * @tparam T
 *   Union type of column
 */
private[ldbc] case class Limit[F[_], P <: Product, T](
  table:     Table[P],
  statement: String,
  columns:   T,
  params:    Seq[ParameterBinder[F]]
) extends Query[F, T]:

  /** A method for setting the OFFSET condition in a statement.
   */
  def offset(length: Long): Parameter[F, Long] ?=> Limit[F, P, T] =
    Limit(
      table = table,
      statement = statement ++ " OFFSET ?",
      columns = columns,
      params = params :+ ParameterBinder(length)
    )

/** Transparent Trait to provide limit method.
 *
 * @tparam F
 *   The effect type
 * @tparam P
 *   Base trait for all products
 * @tparam T
 *   Union type of column
 */
transparent private[ldbc] trait LimitProvider[F[_], P <: Product, T]:
  self: Query[F, T] =>

  /** Trait for generating SQL table information.
   */
  def table: Table[P]

  /** A method for setting the LIMIT condition in a statement.
   */
  def limit(length: Long): Parameter[F, Long] ?=> Limit[F, P, T] =
    Limit(
      table = table,
      statement = statement ++ " LIMIT ?",
      columns = columns,
      params = params :+ ParameterBinder(length)
    )
