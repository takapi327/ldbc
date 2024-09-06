/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder

import scala.annotation.targetName

import ldbc.dsl.*

package object statement:

  trait Command extends SQL
  object Command:
    case class Pure(statement: String, params: List[Parameter.Dynamic]) extends Command:
      @targetName("combine")
      override def ++(sql: SQL): SQL = this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

  trait Query[T] extends SQL
