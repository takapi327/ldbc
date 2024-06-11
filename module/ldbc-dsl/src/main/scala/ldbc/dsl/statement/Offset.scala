/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.statement

import scala.annotation.targetName

import ldbc.sql.Parameter

import ldbc.dsl.SQL

/**
 * A model for constructing OFFSET statements in MySQL.
 *
 * @param query
 *   Query string
 * @param params
 *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
 *   only.
 */
private[ldbc] case class Offset(
  query:  String,
  params: List[Parameter.DynamicBinder]
) extends SQL:

  override def statement: String = query ++ " OFFSET ?"

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    Offset(statement ++ sql.statement, params ++ sql.params)
