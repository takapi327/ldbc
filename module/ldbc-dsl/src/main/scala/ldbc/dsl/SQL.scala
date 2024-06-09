/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import scala.annotation.targetName

import cats.kernel.Semigroup

import ldbc.sql.*

/**
 * A model with a query string and parameters to be bound to the query string that is executed by PreparedStatement,
 * etc.
 */
trait SQL:

  /**
   * an SQL statement that may contain one or more '?' IN parameter placeholders
   */
  def statement: String

  /**
   * statement has '?' that the statement has.
   */
  def params: List[Parameter.DynamicBinder]

  @targetName("combine")
  def ++(sql: SQL): SQL

object SQL:

  given Semigroup[SQL] with
    override def combine(x: SQL, y: SQL): SQL = x ++ y
