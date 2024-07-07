/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import ldbc.sql.Savepoint

import ldbc.connector.exception.SQLException

/**
 * Represents SQL SAVEPOINTS in MySQL.
 */
class MysqlSavepoint(name: String) extends Savepoint:
  override def getSavepointId(): Int = throw SQLFeatureNotSupportedException("Only named savepoints are supported.")
  override def getSavepointName(): String = name
