/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import ldbc.core.*
import ldbc.sql.Parameter
import ldbc.query.builder.TableQuery

/**
 * A model for constructing WHERE statements in MySQL.
 *
 * @param tableQuery
 *   Trait for generating SQL table information.
 * @param statement
 *   SQL statement string
 * @param columns
 *   Union-type column list
 * @param params
 *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
 *   only.
 * @tparam P
 *   Base trait for all products
 * @tparam T
 *   Union type of column
 */
private[ldbc] case class Where[P <: Product, T](
  tableQuery: TableQuery[P],
  statement:  String,
  columns:    T,
  params:     Seq[Parameter.DynamicBinder]
) extends Query[T],
          OrderByProvider[P, T],
          LimitProvider[T]:

  /**
   * A method for combining WHERE statements.
   *
   * @param label
   *   A conjunctive expression to join WHERE statements together.
   * @param expressionSyntax
   *   Trait for the syntax of expressions available in MySQL.
   */
  private def union(label: String, expressionSyntax: ExpressionSyntax): Where[P, T] =
    Where[P, T](
      tableQuery = tableQuery,
      statement  = statement ++ s" $label ${ expressionSyntax.statement }",
      columns    = columns,
      params     = params ++ expressionSyntax.parameter
    )

  def and(func: TableQuery[P] => ExpressionSyntax): Where[P, T] = union("AND", func(tableQuery))

  def or(func: TableQuery[P] => ExpressionSyntax): Where[P, T] = union("OR", func(tableQuery))

  def ||(func: TableQuery[P] => ExpressionSyntax): Where[P, T] = union("||", func(tableQuery))

  def xor(func: TableQuery[P] => ExpressionSyntax): Where[P, T] = union("XOR", func(tableQuery))

  def &&(func: TableQuery[P] => ExpressionSyntax): Where[P, T] = union("&&", func(tableQuery))

  def groupBy[A](func: T => Column[A]): GroupBy[P, T] =
    GroupBy(
      tableQuery = tableQuery,
      statement  = statement ++ s" GROUP BY ${ func(columns).label }",
      columns    = columns,
      params     = params
    )
