/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.annotation.targetName

import ldbc.dsl.{ Parameter, SQL }
import ldbc.dsl.codec.Encoder

trait Limit:

  /** SQL statement string */
  def statement: String

  /** A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index only. */
  def params: List[Parameter.Dynamic]

object Limit:

  case class Q[A, B](
                      table:     A,
                      columns:   Column[B],
                      statement: String,
                      params:    List[Parameter.Dynamic]
                    ) extends Limit,
    Query[A, B]:

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

    def offset(length: Long): Encoder[Long] ?=> Offset[A, B] =
      Offset(
        table     = table,
        columns   = columns,
        statement = statement ++ " OFFSET ?",
        params    = params :+ Parameter.Dynamic(length)
      )

  transparent trait QueryProvider[A, B]:
    self: Query[A, B] =>

    def limit(length: Long): Encoder[Long] ?=> Limit.Q[A, B] =
      Limit.Q(
        table     = self.table,
        columns   = self.columns,
        statement = self.statement ++ " LIMIT ?",
        params    = self.params :+ Parameter.Dynamic(length)
      )

  case class C(
                statement: String,
                params:    List[Parameter.Dynamic]
              ) extends Limit,
    Command:

    @targetName("combine")
    override def ++(sql: SQL): SQL = this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

  transparent trait CommandProvider:
    self: Command =>

    def limit(length: Long): Encoder[Long] ?=> Limit.C =
      Limit.C(
        statement = statement ++ " LIMIT ?",
        params    = params :+ Parameter.Dynamic(length)
      )
