/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import com.mysql.cj.jdbc.MysqlDataSource

import cats.effect.*

import jdbc.connector.*

class JdbcSQLStringContextUpdateTest extends SQLStringContextUpdateTest:

  val ds = new MysqlDataSource()
  ds.setServerName(MySQLTestConfig.host)
  ds.setPortNumber(MySQLTestConfig.port)
  ds.setDatabaseName("world")
  ds.setUser(MySQLTestConfig.user)
  ds.setPassword(MySQLTestConfig.password)
  ds.setDatabaseName("connector_test")

  override def prefix: "jdbc" | "ldbc" = "jdbc"

  override def connection: ConnectionFixture =
    JdbcConnectionFixture(
      "connection",
      MySQLDataSource.fromDataSource(ds, ExecutionContexts.synchronous)
    )
