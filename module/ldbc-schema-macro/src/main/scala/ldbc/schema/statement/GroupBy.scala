/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.statement

import scala.annotation.targetName

import ldbc.dsl.{Parameter, SQL}
import ldbc.schema.Column

case class GroupBy[A, B](
                          table: A,
                          columns: Column[B],
                          statement: String,
                          params: List[Parameter.Dynamic]
                        ) extends Query[A, B], OrderBy.Provider[A, B], Limit.QueryProvider[A, B]:
  
  @targetName("combine")
  override def ++(sql: SQL): SQL =
    this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)
    
  def having(func: A => Expression): Having[A, B] =
    val expression = func(table)
    Having[A, B](
      table = table,
      columns = columns,
      statement = statement ++ s" HAVING ${ expression.statement }",
      params = params ++ expression.parameter
    )
