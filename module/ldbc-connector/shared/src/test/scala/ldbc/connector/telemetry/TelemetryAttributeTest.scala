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

  test("dbOperationBatchSize should return Some for size >= 2") {
    assertEquals(
      TelemetryAttribute.dbOperationBatchSize(2),
      Some(Attribute("db.operation.batch.size", 2L))
    )
    assertEquals(
      TelemetryAttribute.dbOperationBatchSize(100),
      Some(Attribute("db.operation.batch.size", 100L))
    )
  }

  test("dbOperationBatchSize should return None for size < 2") {
    // Per OpenTelemetry spec: db.operation.batch.size should NOT be set to 1
    // Operations are only considered batches when they contain two or more operations
    assertEquals(TelemetryAttribute.dbOperationBatchSize(1), None)
    assertEquals(TelemetryAttribute.dbOperationBatchSize(0), None)
  }

  test("SqlOperation should have correct operation names") {
    assertEquals(TelemetryAttribute.SqlOperation.SELECT, "SELECT")
    assertEquals(TelemetryAttribute.SqlOperation.INSERT, "INSERT")
    assertEquals(TelemetryAttribute.SqlOperation.UPDATE, "UPDATE")
    assertEquals(TelemetryAttribute.SqlOperation.DELETE, "DELETE")
    assertEquals(TelemetryAttribute.SqlOperation.BATCH, "BATCH")
  }

  test("dbOperationParameter should return correct attribute with key") {
    assertEquals(
      TelemetryAttribute.dbOperationParameter("limit", 100),
      Attribute("db.operation.parameter.limit", "100")
    )
    assertEquals(
      TelemetryAttribute.dbOperationParameter("offset", 50),
      Attribute("db.operation.parameter.offset", "50")
    )
  }

  test("dbQueryParameter should return correct attribute with key") {
    assertEquals(
      TelemetryAttribute.dbQueryParameter("0", "John"),
      Attribute("db.query.parameter.0", "John")
    )
    assertEquals(
      TelemetryAttribute.dbQueryParameter("userId", 123),
      Attribute("db.query.parameter.userId", "123")
    )
  }

  test("dbResponseStatusCode should return correct attribute for string code") {
    assertEquals(
      TelemetryAttribute.dbResponseStatusCode("1045"),
      Attribute("db.response.status_code", "1045")
    )
  }

  test("dbResponseStatusCode should return correct attribute for int code") {
    assertEquals(
      TelemetryAttribute.dbResponseStatusCode(1045),
      Attribute("db.response.status_code", "1045")
    )
  }

  test("errorType should return correct attribute from string") {
    assertEquals(
      TelemetryAttribute.errorType("CONNECTION_TIMEOUT"),
      Attribute("error.type", "CONNECTION_TIMEOUT")
    )
  }

  test("errorType should return correct attribute from exception") {
    val exception = new java.sql.SQLException("Test error")
    assertEquals(
      TelemetryAttribute.errorType(exception),
      Attribute("error.type", "java.sql.SQLException")
    )
  }

  test("dbStoredProcedureName should return correct attribute") {
    assertEquals(
      TelemetryAttribute.dbStoredProcedureName("get_user_by_id"),
      Attribute("db.stored_procedure.name", "get_user_by_id")
    )
  }

  test("dbResponseReturnedRows should return correct attribute") {
    assertEquals(
      TelemetryAttribute.dbResponseReturnedRows(42L),
      Attribute("db.response.returned_rows", 42L)
    )
  }

  test("schemaUrl should return correct attribute with v1.39.0") {
    assertEquals(
      TelemetryAttribute.schemaUrl,
      Attribute("otel.schema_url", "https://opentelemetry.io/schemas/1.39.0")
    )
  }
