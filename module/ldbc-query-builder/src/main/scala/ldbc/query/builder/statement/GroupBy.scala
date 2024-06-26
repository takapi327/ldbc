/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import scala.annotation.targetName

import ldbc.dsl.{ Parameter, SQL }
import ldbc.query.builder.*

/**
 * A model for constructing GROUP BY statements in MySQL.
 *
 * @param table
 *   Trait for generating SQL table information.
 * @param columns
 *   Union-type column list
 * @param statement
 *   SQL statement string
 * @param params
 *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
 *   only.
 * @tparam P
 *   Base trait for all products
 * @tparam T
 *   Union type of column
 */
private[ldbc] case class GroupBy[P <: Product, T](
  table:     Table[P],
  columns:   T,
  statement: String,
  params:    List[Parameter.DynamicBinder]
) extends Query[T],
          OrderByProvider[P, T],
          LimitProvider[T]:

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    GroupBy[P, T](table, columns, statement ++ sql.statement, params ++ sql.params)

  def having(func: T => Expression): Having[P, T] =
    val expression = func(columns)
    Having[P, T](
      table     = table,
      statement = statement ++ s" HAVING ${ expression.statement }",
      params    = params ++ expression.parameter
    )
