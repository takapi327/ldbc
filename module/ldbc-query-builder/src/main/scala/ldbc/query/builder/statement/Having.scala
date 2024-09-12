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
 * A model for constructing HAVING statements in MySQL.
 *
 * @param table
 *   Trait for generating SQL table information.
 * @param statement
 *   SQL statement string
 * @param params
 *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
 *   only.
 * @tparam P
 *   Base trait for all products
 * @tparam T
 *   Scala types to be converted by Decoder
 */
private[ldbc] case class Having[P <: Product, T](
  table:     Table[P],
  statement: String,
  params:    List[Parameter.Dynamic],
  decoder: Decoder[T]
) extends Query[T],
          OrderByProvider[P, T],
          Limit.QueryProvider[T]:

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    Having[P, T](table, statement ++ sql.statement, params ++ sql.params, decoder)
