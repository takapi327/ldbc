/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.annotation.targetName

import ldbc.dsl.{ Parameter, SQL }

/**
 * A model for constructing OFFSET statements in MySQL.
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
 * @tparam A
 *   The type of Table. in the case of Join, it is a Tuple of type Table.
 * @tparam B
 *   Scala types to be converted by Decoder
 */
case class Offset[A, B](
  table:     A,
  columns:   Column[B],
  statement: String,
  params:    List[Parameter.Dynamic]
) extends Query[A, B]:

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)
