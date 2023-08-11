/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder.statement

import ldbc.core.*
import ldbc.sql.ParameterBinder

/** A model for constructing WHERE statements in MySQL.
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
private[ldbc] case class Where[F[_], P <: Product, T <: Tuple](
  table:     Table[P],
  statement: String,
  columns:   T,
  params:    Seq[ParameterBinder[F]]
) extends Query[F, T],
          OrderByProvider[F, P, T],
          LimitProvider[F, T]:

  /** A method for combining WHERE statements.
    *
    * @param label
    *   A conjunctive expression to join WHERE statements together.
    * @param expressionSyntax
    *   Trait for the syntax of expressions available in MySQL.
    */
  private def union(label: String, expressionSyntax: ExpressionSyntax[F]): Where[F, P, T] =
    Where[F, P, T](
      table     = table,
      statement = statement ++ s" $label ${ expressionSyntax.statement }",
      columns   = columns,
      params    = params ++ expressionSyntax.parameter
    )

  def and(func: Table[P] => ExpressionSyntax[F]): Where[F, P, T] = union("AND", func(table))
  def or(func: Table[P] => ExpressionSyntax[F]):  Where[F, P, T] = union("OR", func(table))
  def ||(func: Table[P] => ExpressionSyntax[F]):  Where[F, P, T] = union("||", func(table))
  def xor(func: Table[P] => ExpressionSyntax[F]): Where[F, P, T] = union("XOR", func(table))
  def &&(func: Table[P] => ExpressionSyntax[F]):  Where[F, P, T] = union("&&", func(table))

  def groupBy[A](func: T => Column[A]): GroupBy[F, P, T] =
    GroupBy(
      table     = table,
      statement = statement ++ s" GROUP BY ${ func(columns).label }",
      columns   = columns,
      params    = params
    )
