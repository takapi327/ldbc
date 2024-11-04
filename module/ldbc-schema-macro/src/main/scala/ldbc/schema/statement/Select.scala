/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.statement

import scala.annotation.targetName

import ldbc.dsl.{Parameter, SQL}
import ldbc.query.builder.Column
import ldbc.query.builder.statement.Expression

case class Select[A, B](
  table: A,
  columns: Column[B],
  statement: String,
  params: List[Parameter.Dynamic]
) extends Query[A, B]:

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

  def where(func: A => Expression): Where.Q[A, B] =
    val expression = func(table)
    Where.Q[A, B](
      table = table,
      columns = columns,
      statement = statement ++ s" WHERE ${ expression.statement }",
      params = params ++ expression.parameter
   )
