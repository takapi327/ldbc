/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package jdbc.connector

import java.sql

import ldbc.sql.Savepoint

case class MysqlSavepoint(name: String) extends Savepoint:
  override def getSavepointId():   Int    = throw new java.sql.SQLException("Only named savepoints are supported.")
  override def getSavepointName(): String = name

object MysqlSavepoint:
  def apply(savepoint: java.sql.Savepoint): Savepoint = MysqlSavepoint(savepoint.getSavepointName)

  def toJdbc(savepoint: Savepoint): java.sql.Savepoint = new sql.Savepoint:
    override def getSavepointId:   Int    = savepoint.getSavepointId()
    override def getSavepointName: String = savepoint.getSavepointName()
