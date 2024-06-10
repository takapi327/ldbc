/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.statement

import scala.annotation.targetName

import ldbc.sql.Parameter

import ldbc.dsl.*

/**
 * A model for constructing GROUP BY statements in MySQL.
 *
 * @param table
 *   Trait for generating SQL table information.
 * @param columns
 *   Union-type column list
 * @param column
 *   Trait for representing SQL Column
 * @param query
 *   Query string
 * @param params
 *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
 *   only.
 * @tparam P
 *   Base trait for all products
 * @tparam A
 *   Union type of column
 */
private[ldbc] case class GroupBy[P, A, B](
                                                   table: Table[P],
                                                   columns:    A,
                                                   column:    Column[B],
                                                   query:  String,
                                                   params:     List[Parameter.DynamicBinder]
                                                 ) extends SQL, OrderByProvider[P], LimitProvider:

  override def statement: String = query ++ s" GROUP BY ${ column.name }"

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    GroupBy[P, A, B](table, columns, column, query ++ sql.statement, params ++ sql.params)

  def having(func: A => Expression): Having[P] =
    val expression = func(columns)
    Having(
      table = table,
      query = query,
      params = params ++ expression.parameter,
      expression = expression
   )