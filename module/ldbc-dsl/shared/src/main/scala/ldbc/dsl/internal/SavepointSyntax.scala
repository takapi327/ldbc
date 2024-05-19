/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.internal

import ldbc.sql.*

trait SavepointSyntax:

  class MysqlSavepoint(name: String) extends java.sql.Savepoint:
    override def getSavepointId():   Int    = throw new java.sql.SQLException("Only named savepoints are supported.")
    override def getSavepointName(): String = name

  object Savepoint:

    def apply(savepoint: java.sql.Savepoint): Savepoint = new Savepoint:
      override def getSavepointId():   Int    = savepoint.getSavepointId
      override def getSavepointName(): String = savepoint.getSavepointName
