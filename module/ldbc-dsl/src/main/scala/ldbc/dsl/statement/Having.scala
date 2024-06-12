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
 * A model for constructing HAVING statements in MySQL.
 *
 * @param table
 *   Trait for generating SQL table information.
 * @param query
 *   Query string
 * @param params
 *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
 *   only.
 * @tparam P
 *   Base trait for all products
 * @tparam T
 *   Union type of column
 */
private[ldbc] case class Having[P <: Product, T](
  table:      Table[P],
  query:      String,
  params:     List[Parameter.DynamicBinder],
  expression: Expression
) extends Query.Provider[T],
          OrderByProvider[P, T],
          LimitProvider[T]:

  override def statement: String = query ++ s" HAVING ${ expression.statement }"

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    Having[P, T](table, query ++ sql.statement, params ++ sql.params, expression)
