/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import ldbc.core.*
import ldbc.dsl.Parameter
import ldbc.query.builder.TableQuery

/**
 * A model for constructing SELECT statements in MySQL.
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
private[ldbc] case class Select[P <: Product, T](
  tableQuery: TableQuery[P],
  statement:  String,
  columns:    T,
  params:     Seq[Parameter.DynamicBinder]
) extends Query[T],
          OrderByProvider[P, T],
          LimitProvider[T]:

  /**
   * A method for setting the WHERE condition in a SELECT statement.
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def where(func: TableQuery[P] => ExpressionSyntax): Where[P, T] =
    val expressionSyntax = func(tableQuery)
    Where[P, T](
      tableQuery = tableQuery,
      statement  = statement ++ s" WHERE ${ expressionSyntax.statement }",
      columns    = columns,
      params     = params ++ expressionSyntax.parameter
    )

  def groupBy[A](func: T => Column[A]): GroupBy[P, T] =
    GroupBy(
      tableQuery = tableQuery,
      statement  = statement ++ s" GROUP BY ${ func(columns).label }",
      columns    = columns,
      params     = params
    )
