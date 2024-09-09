/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import scala.annotation.targetName

import ldbc.dsl.*
import ldbc.query.builder.*

/**
 * A model for constructing UPDATE statements in MySQL.
 *
 * @param table
 *   Trait for generating SQL table information.
 * @param columns
 *   Union-type column list
 * @param statement
 *   SQL statement string
 * @tparam P
 *   Base trait for all products
 * @tparam T
 *   Union type of column
 */
private[ldbc] case class Delete[P <: Product, T](
  table:     Table[P],
  columns:   T,
  statement: String,
  params:    List[Parameter.Dynamic] = List.empty
) extends Command,
          LimitProvider[T]:

  @targetName("combine")
  override def ++(sql: SQL): SQL = Delete[P, T](table, columns, statement ++ sql.statement, params ++ sql.params)

  /**
   * A method for setting the WHERE condition in a DELETE statement.
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def where(func: Table[P] => Expression): Where[P, T] =
    val expression = func(table)
    Where(
      table     = table,
      statement = statement ++ s" WHERE ${ expression.statement }",
      columns   = columns,
      params    = params ++ expression.parameter
    )
