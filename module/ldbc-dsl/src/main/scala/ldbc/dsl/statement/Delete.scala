/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.statement

import scala.annotation.targetName

import ldbc.dsl.*

/**
 * A model for constructing UPDATE statements in MySQL.
 *
 * @param table
 *   Trait for generating SQL table information.
 * @tparam P
 *   Base trait for all products
 * @tparam T
 *   Union type of column
 */
case class Delete[P <: Product, T](
  table:   Table[P],
  columns: T,
  other:   Option[String]                = None,
  params:  List[Parameter.DynamicBinder] = List.empty
) extends SQL,
          LimitProvider:

  @targetName("combine")
  override def ++(sql: SQL): SQL = Delete[P, T](table, columns, Some(sql.statement), params ++ sql.params)

  override def statement: String = s"DELETE FROM ${ table._name }" ++ other.fold("")(s => s" $s")

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
