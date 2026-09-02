/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import com.mysql.cj.jdbc.MysqlDataSource

import cats.effect.*

import ldbc.sql.DataSource

import jdbc.connector.*

class JdbcConnectionTest extends ConnectionTest[IO] with IOAsyncDatabaseSuite:

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
