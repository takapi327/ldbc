/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.annotation.targetName

import ldbc.dsl.{ SQL, Parameter }

/**
 * Trait represents a command in MySQL.
 */
trait Command extends SQL

object Command:
  case class Pure(statement: String, params: List[Parameter.Dynamic]) extends Command:
    @targetName("combine")
    override def ++(sql: SQL): SQL = this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)
