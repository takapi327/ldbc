/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.statement

import scala.annotation.targetName

import ldbc.dsl.{Parameter, SQL}
import ldbc.schema.Column

case class Having[A, B](
                         table: A,
                         columns: Column[B],
                         statement: String,
                         params: List[Parameter.Dynamic]
                       ) extends Query[A, B], OrderBy.Provider[A, B], Limit.QueryProvider[A, B]:
  
  @targetName("combine")
  override def ++(sql: SQL): SQL =
    this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)
