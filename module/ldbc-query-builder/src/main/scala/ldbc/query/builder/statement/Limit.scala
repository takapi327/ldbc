/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import ldbc.dsl.Parameter

/**
 * A model for constructing LIMIT statements in MySQL.
 *
 * @param statement
 *   SQL statement string
 * @param columns
 *   Union-type column list
 * @param params
 *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
 *   only.
 * @tparam T
 *   Union type of column
 */
private[ldbc] case class Limit[T](
  statement: String,
  columns:   T,
  params:    Seq[Parameter.DynamicBinder]
) extends Query[T]:

  /**
   * A method for setting the OFFSET condition in a statement.
   */
  def offset(length: Long): Parameter[Long] ?=> Limit[T] =
    Limit(
      statement = statement ++ " OFFSET ?",
      columns   = columns,
      params    = params :+ Parameter.DynamicBinder(length)
    )

/**
 * Transparent Trait to provide limit method.
 *
 * @tparam T
 *   Union type of column
 */
private[ldbc] transparent trait LimitProvider[T]:
  self: Query[T] =>

  /**
   * A method for setting the LIMIT condition in a statement.
   */
  def limit(length: Long): Parameter[Long] ?=> Limit[T] =
    Limit(
      statement = statement ++ " LIMIT ?",
      columns   = columns,
      params    = params :+ Parameter.DynamicBinder(length)
    )
