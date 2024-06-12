/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import ldbc.dsl.Parameter

/**
 * Trait for constructing Statements that set conditions.
 *
 * @tparam T
 *   Column Tuples
 */
private[ldbc] trait Query[T]:

  /**
   * SQL statement string
   */
  def statement: String

  /**
   * Union-type column list
   */
  def columns: T

  /**
   * A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
   * only.
   */
  def params: Seq[Parameter.DynamicBinder]
