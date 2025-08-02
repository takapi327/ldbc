/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import com.mysql.cj.jdbc.MysqlDataSource

import cats.effect.*

import jdbc.connector.*
import ldbc.DataSource

class JdbcConnectionTest extends ConnectionTest:

  val ds = new MysqlDataSource()
  ds.setServerName(host)
  ds.setPortNumber(port)
  ds.setDatabaseName(database)
  ds.setUser(user)
  ds.setPassword(password)

  override def prefix: "jdbc" | "ldbc" = "jdbc"

  override def datasource(databaseTerm: "SCHEMA" | "CATALOG" = "CATALOG"): DataSource[IO] =
    ds.setDatabaseTerm(databaseTerm)
    MySQLDataSource.fromDataSource[IO](ds, ExecutionContexts.synchronous)
