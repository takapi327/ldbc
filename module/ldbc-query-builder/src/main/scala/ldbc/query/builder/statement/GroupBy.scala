/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import ldbc.dsl.Parameter
import ldbc.query.builder.TableQuery

/**
 * A model for constructing GROUP BY statements in MySQL.
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
private[ldbc] case class GroupBy[P <: Product, T](
  tableQuery: TableQuery[P],
  statement:  String,
  columns:    T,
  params:     Seq[Parameter.DynamicBinder]
) extends Query[T],
          OrderByProvider[P, T],
          LimitProvider[T]:

  def having(func: T => ExpressionSyntax): Having[P, T] =
    val expressionSyntax = func(columns)
    Having(
      tableQuery = tableQuery,
      statement  = statement ++ s" HAVING ${ expressionSyntax.statement }",
      columns    = columns,
      params     = params ++ expressionSyntax.parameter
    )
