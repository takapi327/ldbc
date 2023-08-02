/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.statement

import ldbc.core.Table
import ldbc.dsl.ParameterBinder

/** A model for constructing SELECT statements in MySQL.
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
private[ldbc] case class Select[F[_], P <: Product, T](
  table:     Table[P],
  statement: String,
  columns:   T,
  params:    Seq[ParameterBinder[F]]
) extends Query[F, T]:

  /** A method for setting the WHERE condition in a SELECT statement.
    *
    * @param func
    *   Function to construct an expression using the columns that Table has.
    */
  def where(func: Table[P] => ExpressionSyntax[F]): Where[F, P, T] =
    val expressionSyntax = func(table)
    Where[F, P, T](
      table     = table,
      statement = statement ++ s" WHERE ${ expressionSyntax.statement }",
      columns   = columns,
      params    = params ++ expressionSyntax.parameter
    )
