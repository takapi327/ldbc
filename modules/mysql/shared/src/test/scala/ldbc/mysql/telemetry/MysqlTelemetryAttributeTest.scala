/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.telemetry

import ldbc.sql.Attribute

import ldbc.mysql.FTestPlatform

class MysqlTelemetryAttributeTest extends FTestPlatform:

  test("dbMysqlVersion should return correct attribute") {
    val version = "8.0.33"
    assertEquals(MysqlTelemetryAttribute.dbMysqlVersion(version), Attribute("db.mysql.version", version))
  }

  test("dbMysqlThreadId should return correct attribute") {
    val threadId = 12345
    assertEquals(MysqlTelemetryAttribute.dbMysqlThreadId(threadId), Attribute("db.mysql.thread_id", 12345L))
  }

  test("dbMysqlAuthPlugin should return correct attribute") {
    val plugin = "mysql_native_password"
    assertEquals(MysqlTelemetryAttribute.dbMysqlAuthPlugin(plugin), Attribute("db.mysql.auth_plugin", plugin))
  }
