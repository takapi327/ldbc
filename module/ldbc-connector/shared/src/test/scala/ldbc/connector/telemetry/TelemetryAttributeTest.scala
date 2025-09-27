/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.telemetry

import org.typelevel.otel4s.Attribute

import ldbc.connector.*

class TelemetryAttributeTest extends FTestPlatform:

  test("dbSystemName should return mysql") {
    assertEquals(TelemetryAttribute.dbSystemName, Attribute("db.system.name", "mysql"))
  }

  test("dbNamespace should return correct attribute") {
    val database = "test_db"
    assertEquals(TelemetryAttribute.dbNamespace(database), Attribute("db.namespace", database))
  }

  test("dbCollectionName should return correct attribute") {
    val table = "users"
    assertEquals(TelemetryAttribute.dbCollectionName(table), Attribute("db.collection.name", table))
  }

  test("dbOperationName should return correct attribute") {
    val operation = "SELECT"
    assertEquals(TelemetryAttribute.dbOperationName(operation), Attribute("db.operation.name", operation))
  }

  test("dbQueryText should return correct attribute") {
    val query = "SELECT * FROM users WHERE id = 1"
    assertEquals(TelemetryAttribute.dbQueryText(query), Attribute("db.query.text", query))
  }

  test("dbQuerySummary should return correct attribute") {
    val summary = "SELECT * FROM users"
    assertEquals(TelemetryAttribute.dbQuerySummary(summary), Attribute("db.query.summary", summary))
  }

  test("serverAddress should return correct attribute") {
    val host = "localhost"
    assertEquals(TelemetryAttribute.serverAddress(host), Attribute("server.address", host))
  }

  test("serverPort should return correct attribute") {
    val port = 3306
    assertEquals(TelemetryAttribute.serverPort(port), Attribute("server.port", 3306L))
  }

  test("dbMysqlVersion should return correct attribute") {
    val version = "8.0.33"
    assertEquals(TelemetryAttribute.dbMysqlVersion(version), Attribute("db.mysql.version", version))
  }

  test("dbMysqlThreadId should return correct attribute") {
    val threadId = 12345
    assertEquals(TelemetryAttribute.dbMysqlThreadId(threadId), Attribute("db.mysql.thread_id", 12345L))
  }

  test("dbMysqlAuthPlugin should return correct attribute") {
    val plugin = "mysql_native_password"
    assertEquals(TelemetryAttribute.dbMysqlAuthPlugin(plugin), Attribute("db.mysql.auth_plugin", plugin))
  }

  test("batchSize should return Some for size >= 2") {
    assertEquals(
      TelemetryAttribute.batchSize(2L),
      List(TelemetryAttribute.dbOperationName("BATCH"), Attribute("db.operation.batch.size", 2L))
    )
    assertEquals(
      TelemetryAttribute.batchSize(100L),
      List(TelemetryAttribute.dbOperationName("BATCH"), Attribute("db.operation.batch.size", 100L))
    )
  }

  test("batchSize should return None for size < 2") {
    assertEquals(TelemetryAttribute.batchSize(1L), List(TelemetryAttribute.dbOperationName("BATCH")))
    assertEquals(TelemetryAttribute.batchSize(0L), List(TelemetryAttribute.dbOperationName("BATCH")))
  }
