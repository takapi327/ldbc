/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import scala.annotation.targetName

import ldbc.dsl.{ Parameter, SQL }
import ldbc.dsl.codec.{Encoder, Decoder}
import ldbc.query.builder.*

trait Limit:
  
  /** SQL statement string */
  def statement: String
  
  /** A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index only. */
  def params:    List[Parameter.Dynamic]
  
object Limit:

  /**
   * A model for constructing read-only query LIMIT statements in MySQL.
   * 
   * @param statement
   *   SQL statement string
   * @param params
   *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index only.
   * @param decoder
   *   Function to convert values from ResultSet to type T.
   * @tparam T
   *   Scala types to be converted by Decoder
   */
  case class Q[T](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder: Decoder[T]
  ) extends Limit, Query[T]:
    
    @targetName("combine")
    override def ++(sql: SQL): SQL = Q[T](statement ++ sql.statement, params ++ sql.params, decoder)

    def offset(length: Long): Encoder[Long] ?=> Offset[T] =
      Offset(
        statement = statement ++ " OFFSET ?",
        params    = params :+ Parameter.Dynamic(length),
        decoder = this.decoder
      )

  transparent trait QueryProvider[T]:
    self: Query[T] =>

    def limit(length: Long): Encoder[Long] ?=> Limit.Q[T] =
      Limit.Q(
        statement = statement ++ " LIMIT ?",
        params    = params :+ Parameter.Dynamic(length),
        decoder = self.decoder
      )

  /**
   * A model for constructing write-only query LIMIT statements in MySQL.
   * 
   * @param statement
   *   SQL statement string
   * @param params
   *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index only.
   */
  case class C(
    statement: String,
    params:    List[Parameter.Dynamic]
  ) extends Limit, Command:
    
    @targetName("combine")
    override def ++(sql: SQL): SQL = C(statement ++ sql.statement, params ++ sql.params)
    
  transparent trait CommandProvider:
    self: Command =>

    def limit(length: Long): Encoder[Long] ?=> Limit.C =
      Limit.C(
        statement = statement ++ " LIMIT ?",
        params    = params :+ Parameter.Dynamic(length)
      )
