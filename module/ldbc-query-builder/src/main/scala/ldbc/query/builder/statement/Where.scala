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
 * A model for constructing WHERE statements in MySQL.
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
private[ldbc] case class Where[P <: Product, T](
  table:     Table[P],
  statement: String,
  columns:   T,
  params:    List[Parameter.DynamicBinder]
) extends Query[T],
          Command,
          OrderByProvider[P, T],
          LimitProvider[T]:

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    Where[P, T](table, statement ++ sql.statement, columns, params ++ sql.params)

  /**
   * A method for combining WHERE statements.
   *
   * @param label
   *   A conjunctive expression to join WHERE statements together.
   * @param expression
   *   Trait for the syntax of expressions available in MySQL.
   */
  private def union(label: String, expression: Expression): Where[P, T] =
    Where[P, T](
      table     = table,
      statement = statement ++ s" $label ${ expression.statement }",
      columns   = columns,
      params    = params ++ expression.parameter
    )

  def and(func: Table[P] => Expression): Where[P, T] = union("AND", func(table))

  def or(func: Table[P] => Expression): Where[P, T] = union("OR", func(table))

  @targetName("OR")
  def ||(func: Table[P] => Expression): Where[P, T] = union("||", func(table))

  def xor(func: Table[P] => Expression): Where[P, T] = union("XOR", func(table))

  @targetName("AND")
  def &&(func: Table[P] => Expression): Where[P, T] = union("&&", func(table))

  def groupBy[A](func: T => Column[A]): GroupBy[P, T] =
    GroupBy(
      table   = table,
      statement  = statement ++ s" GROUP BY ${ func(columns).name }",
      columns = columns,
      params  = params
    )
