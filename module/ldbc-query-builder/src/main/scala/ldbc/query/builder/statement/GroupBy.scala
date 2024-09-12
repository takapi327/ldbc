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
 * @tparam C
 *   Union type of column
 * @tparam D
 *   Scala types to be converted by Decoder
 */
private[ldbc] case class GroupBy[P <: Product, C, D](
  table:     Table[P],
  columns:   C,
  statement: String,
  params:    List[Parameter.Dynamic],
  decoder: Decoder[D]
) extends Query[D],
          OrderByProvider[P, D],
          Limit.QueryProvider[D]:

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    GroupBy[P, C, D](table, columns, statement ++ sql.statement, params ++ sql.params, decoder)

  def having(func: C => Expression): Having[P, D] =
    val expression = func(columns)
    Having[P, D](
      table     = table,
      statement = statement ++ s" HAVING ${ expression.statement }",
      params    = params ++ expression.parameter,
      decoder   = decoder
    )
