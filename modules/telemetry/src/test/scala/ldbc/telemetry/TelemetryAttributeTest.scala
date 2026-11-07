/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.telemetry

import munit.FunSuite

import ldbc.sql.Attribute

class TelemetryAttributeTest extends FunSuite:

  test("dbOperationBatchSize should return Some with DbAttributes key for size >= 2") {
    assertEquals(
      TelemetryAttribute.dbOperationBatchSize(2),
      Some(DbAttributes.DbOperationBatchSize(2L))
    )
    assertEquals(
      TelemetryAttribute.dbOperationBatchSize(100),
      Some(DbAttributes.DbOperationBatchSize(100L))
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

  test("schemaUrl should return correct attribute with v1.39.0") {
    assertEquals(
      TelemetryAttribute.schemaUrl,
      Attribute("otel.schema_url", "https://opentelemetry.io/schemas/1.39.0")
    )
  }
