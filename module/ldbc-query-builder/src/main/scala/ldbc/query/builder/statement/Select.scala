/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import scala.annotation.targetName

import ldbc.dsl.{ Parameter, SQL }
import ldbc.dsl.codec.Decoder
import ldbc.query.builder.*

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
 * @tparam C
 *   Union type of column
 * @tparam D
 *   Scala types to be converted by Decoder
 */
private[ldbc] case class Select[P <: Product, C, D](
  table:     Table[P],
  statement: String,
  columns:   C,
  params:    List[Parameter.Dynamic],
  decoder:   Decoder[D]
) extends Query[D],
          OrderByProvider[P, D],
          Limit.QueryProvider[D]:

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    Select[P, C, D](table, statement ++ sql.statement, columns, params ++ sql.params, decoder)

  /**
   * A method for setting the WHERE condition in a SELECT statement.
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def where(func: Table[P] => Expression): Where.Q[P, C, D] =
    val expression = func(table)
    Where.Q[P, C, D](
      table     = table,
      statement = statement ++ s" WHERE ${ expression.statement }",
      columns   = columns,
      params    = params ++ expression.parameter,
      decoder   = decoder
    )

  def groupBy[A](func: C => Column[A]): GroupBy[P, C, D] =
    GroupBy(
      table     = table,
      statement = statement ++ s" GROUP BY ${ func(columns).name }",
      columns   = columns,
      params    = params,
      decoder   = decoder
    )
