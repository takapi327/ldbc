/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import scala.annotation.targetName

import cats.syntax.all.*

import ldbc.dsl.codec.Decoder

/**
 * A model with a query string and parameters to be bound to the query string that is executed by PreparedStatement,
 * etc.
 *
 * @param statement
 *   an SQL statement that may contain one or more '?' IN parameter placeholders
 * @param params
 *   statement has '?' that the statement has.
 */
case class Mysql(statement: String, params: List[Parameter.Dynamic]) extends SQL, ParamBinder:

  @targetName("combine")
  override def ++(sql: SQL): Mysql =
    Mysql(statement ++ sql.statement, params ++ sql.params)

  /**
   * A method to convert a query to a [[ldbc.dsl.Query]].
   * 
   * {{{
   *   sql"SELECT `name` FROM user".query[String]
   * }}}
   *
   * @return
   * A [[ldbc.dsl.Query]] instance
   */
  def query[T](using decoder: Decoder[T]): Query[T] =
    Query.Impl[T](statement, params, decoder)

  /**
   * A method to execute an update operation against the MySQL server.
   * 
   * {{{
   *   sql"UPDATE user SET `name` = ${"Alice"} WHERE `id` = ${1L}".update
   * }}}
   *
   * @return
   * The number of rows updated
   */
  def update: DBIO[Int] = DBIO.update(statement, params)

  /**
   * A method to execute an insert operation against the MySQL server.
   * 
   * {{{
   *   sql"INSERT INTO user (`name`, `age`) VALUES (${("Alice", 20)})".returning[Long]
   * }}}
   *
   * @tparam T
   *   The type of the primary key
   * @return
   *   The primary key value
   */
  def returning[T <: String | Int | Long](using decoder: Decoder[T]): DBIO[T] =
    DBIO.returning(statement, params, decoder)
