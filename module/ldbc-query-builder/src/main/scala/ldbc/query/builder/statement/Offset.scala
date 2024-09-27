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
 * A model for constructing OFFSET statements in MySQL.
 *
 * @param statement
 *   SQL statement string
 * @param params
 *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
 *   only.
 * @param decoder
 *   Decoder for converting SQL data to Scala data
 * @tparam T
 *   Scala types to be converted by Decoder
 */
private[ldbc] case class Offset[T](
  statement: String,
  params:    List[Parameter.Dynamic],
  decoder:   Decoder[T]
) extends Query[T]:

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    Offset(statement ++ sql.statement, params ++ sql.params, decoder)
