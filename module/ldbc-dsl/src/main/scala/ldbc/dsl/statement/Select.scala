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
 * A model for constructing SELECT statements in MySQL.
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
 * @tparam P
 *   Base trait for all products
 * @tparam T
 *   Union type of column
 */
private[ldbc] case class Select[P <: Product, T](
  table:     Table[P],
  statement: String,
  columns:   T,
  params:    List[Parameter.DynamicBinder]
) extends SQL,
          OrderByProvider[P],
          LimitProvider:

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    Select[P, T](table, statement ++ sql.statement, columns, params ++ sql.params)

  /**
   * A method for setting the WHERE condition in a SELECT statement.
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def where(func: Table[P] => Expression): Where[P, T] =
    val expression = func(table)
    Where[P, T](
      table     = table,
      statement = statement ++ s" WHERE ${ expression.statement }",
      columns   = columns,
      params    = params ++ expression.parameter
    )

  def groupBy[A](func: T => Column[A]): GroupBy[P, T, A] =
    GroupBy(
      table   = table,
      query   = statement,
      columns = columns,
      column  = func(columns),
      params  = params
    )
