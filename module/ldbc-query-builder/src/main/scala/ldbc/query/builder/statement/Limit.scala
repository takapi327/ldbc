/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import scala.annotation.targetName

import ldbc.dsl.{Parameter, SQL}
import ldbc.query.builder.*

/**
 * A model for constructing LIMIT statements in MySQL.
 *
 * @param query
 *   Query string
 * @param params
 *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
 *   only.
 * @tparam T
 *   Union type of column
 */
private[ldbc] case class Limit[T](
  query:  String,
  params: List[Parameter.DynamicBinder]
) extends Query[T]:

  override def statement: String = query ++ " LIMIT ?"

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    Limit(statement ++ sql.statement, params ++ sql.params)

  /**
   * A method for setting the OFFSET condition in a statement.
   */
  def offset(length: Long): Parameter[Long] ?=> Offset[T] =
    Offset(
      query  = statement,
      params = params :+ Parameter.DynamicBinder(length)
    )

/**
 * Transparent Trait to provide limit method.
 */
private[ldbc] transparent trait LimitProvider[T]:
  self: SQL =>

  /**
   * A method for setting the LIMIT condition in a statement.
   */
  def limit(length: Long): Parameter[Long] ?=> Limit[T] =
    Limit(
      query  = statement,
      params = params :+ Parameter.DynamicBinder(length)
    )
