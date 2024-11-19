/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.annotation.targetName

import ldbc.dsl.{ Parameter, SQL }

case class Select[A, B](
  table:     A,
  columns:   Column[B],
  statement: String,
  params:    List[Parameter.Dynamic]
) extends Query[A, B],
          OrderBy.Provider[A, B],
          Limit.QueryProvider[A, B]:

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

  def where(func: A => Expression): Where.Q[A, B] =
    val expression = func(table)
    Where.Q[A, B](
      table     = table,
      columns   = columns,
      statement = statement ++ s" WHERE ${ expression.statement }",
      params    = params ++ expression.parameter
    )

  def groupBy[C](func: A => Column[C]): GroupBy[A, B] =
    GroupBy[A, B](
      table     = table,
      columns   = columns,
      statement = statement ++ s" GROUP BY ${ func(table).toString }",
      params    = params
    )
