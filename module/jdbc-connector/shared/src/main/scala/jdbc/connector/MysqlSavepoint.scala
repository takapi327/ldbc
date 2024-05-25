/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package jdbc.connector

import ldbc.sql.Savepoint

import java.sql

case class MysqlSavepoint(name: String) extends Savepoint:
  override def getSavepointId(): Int = throw new java.sql.SQLException("Only named savepoints are supported.")
  override def getSavepointName(): String = name

  def toJdbc: java.sql.Savepoint = new sql.Savepoint:
    override def getSavepointId: Int = MysqlSavepoint.this.getSavepointId()
    override def getSavepointName: String = MysqlSavepoint.this.getSavepointName()

object MysqlSavepoint:
  def apply(savepoint: java.sql.Savepoint): Savepoint = MysqlSavepoint(savepoint.getSavepointName)
