/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.statement

import scala.annotation.targetName

import ldbc.dsl.{ Parameter, SQL }
import ldbc.dsl.codec.Encoder
import ldbc.schema.Column

case class Update[A](
  table:     A,
  statement: String,
  params:    List[Parameter.Dynamic]
) extends Command:

  @targetName("combine")
  override def ++(sql: SQL): SQL = this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

  def set[B](column: A => Column[B], value: B)(using Encoder[B]): Update[A] =
    this.copy(
      statement = statement ++ s" SET ${ column(table).name } = ?",
      params    = params :+ Parameter.Dynamic(value)
    )

  def set[B](column: A => Column[B], value: Option[B])(using Encoder[B]): Update[A] =
    value.fold(this)(v => set(column, v))

  def set[B](column: A => Column[B], value: B, bool: Boolean)(using Encoder[B]): Update[A] =
    if bool then set(column, value) else this

  def where(func: A => Expression): Where.C[A] =
    val expression = func(table)
    Where.C[A](
      table     = table,
      statement = statement ++ s" WHERE ${ expression.statement }",
      params    = params ++ expression.parameter
    )
