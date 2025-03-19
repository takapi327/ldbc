/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.annotation.targetName

import ldbc.dsl.{ DBIO, Parameter, SQL }
import ldbc.dsl.codec.Decoder

/**
 * Trait represents a command in MySQL.
 */
trait Command extends SQL:

  /**
   * A method to execute an update operation against the MySQL server.
   *
   * {{{
   *   TableQuery[User]
   *     .update(user => user.id *: user.name *: user.age)((1L, "Alice", 20))
   *     .where(_.id === 1L)
   *     .update
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
   *   TableQuery[User]
   *     .insertInto(user => user.name *: user.age)
   *     .values(("Alice", 20))
   *     .returning[Long]
   * }}}
   *
   * @tparam T
   * The type of the primary key
   * @return
   * The primary key value
   */
  def returning[T <: String | Int | Long](using decoder: Decoder[T]): DBIO[T] =
    DBIO.returning(statement, params, decoder)

object Command:
  case class Pure(statement: String, params: List[Parameter.Dynamic]) extends Command:
    @targetName("combine")
    override def ++(sql: SQL): SQL = this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)
