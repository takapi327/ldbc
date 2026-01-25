/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.telemetry

import ldbc.connector.*

class TelemetryConfigTest extends FTestPlatform:

  // ============================================================
  // Default configuration tests
  // ============================================================

  test("default config should have extractMetadataFromQueryText disabled") {
    assertEquals(TelemetryConfig.default.extractMetadataFromQueryText, false)
  }

  test("default config should have sanitization enabled") {
    assertEquals(TelemetryConfig.default.sanitizeNonParameterizedQueries, true)
  }

  test("default config should have IN clause collapsing enabled") {
    assertEquals(TelemetryConfig.default.collapseInClauses, true)
  }

  // ============================================================
  // withQueryTextExtraction preset tests
  // ============================================================

  test("withQueryTextExtraction preset should have extraction enabled") {
    assertEquals(TelemetryConfig.withQueryTextExtraction.extractMetadataFromQueryText, true)
  }

  test("withQueryTextExtraction preset should keep sanitization enabled") {
    assertEquals(TelemetryConfig.withQueryTextExtraction.sanitizeNonParameterizedQueries, true)
  }

  // ============================================================
  // Fluent API tests
  // ============================================================

  test("withQueryTextExtraction method should enable extraction") {
    val config = TelemetryConfig.default.withQueryTextExtraction
    assertEquals(config.extractMetadataFromQueryText, true)
  }

  test("withoutQueryTextExtraction method should disable extraction") {
    val config = TelemetryConfig.withQueryTextExtraction.withoutQueryTextExtraction
    assertEquals(config.extractMetadataFromQueryText, false)
  }

  test("withSanitization method should enable sanitization") {
    val config = TelemetryConfig.default.withoutSanitization.withSanitization
    assertEquals(config.sanitizeNonParameterizedQueries, true)
  }

  test("withoutSanitization method should disable sanitization") {
    val config = TelemetryConfig.default.withoutSanitization
    assertEquals(config.sanitizeNonParameterizedQueries, false)
  }

  test("withInClauseCollapsing method should enable collapsing") {
    val config = TelemetryConfig.default.withoutInClauseCollapsing.withInClauseCollapsing
    assertEquals(config.collapseInClauses, true)
  }

  test("withoutInClauseCollapsing method should disable collapsing") {
    val config = TelemetryConfig.default.withoutInClauseCollapsing
    assertEquals(config.collapseInClauses, false)
  }

  // ============================================================
  // processQueryText tests
  // ============================================================

  test("processQueryText should sanitize non-parameterized queries when enabled") {
    val config = TelemetryConfig.default
    val sql    = "SELECT * FROM users WHERE id = 123"
    val result = config.processQueryText(sql)
    assertEquals(result, "SELECT * FROM users WHERE id = ?")
  }

  test("processQueryText should not sanitize parameterized queries") {
    val config = TelemetryConfig.default
    val sql    = "SELECT * FROM users WHERE id = ?"
    val result = config.processQueryText(sql)
    assertEquals(result, "SELECT * FROM users WHERE id = ?")
  }

  test("processQueryText should not sanitize when disabled") {
    val config = TelemetryConfig.default.withoutSanitization
    val sql    = "SELECT * FROM users WHERE id = 123"
    val result = config.processQueryText(sql)
    assertEquals(result, "SELECT * FROM users WHERE id = 123")
  }

  test("processQueryText should collapse IN clauses when enabled") {
    val config = TelemetryConfig.default
    val sql    = "SELECT * FROM users WHERE id IN (?, ?, ?)"
    val result = config.processQueryText(sql)
    assertEquals(result, "SELECT * FROM users WHERE id IN (?)")
  }

  test("processQueryText should not collapse IN clauses when disabled") {
    val config = TelemetryConfig.default.withoutInClauseCollapsing
    val sql    = "SELECT * FROM users WHERE id IN (?, ?, ?)"
    val result = config.processQueryText(sql)
    assertEquals(result, "SELECT * FROM users WHERE id IN (?, ?, ?)")
  }

  // ============================================================
  // getOperationName tests
  // ============================================================

  test("getOperationName should prefer API metadata when available") {
    val config = TelemetryConfig.withQueryTextExtraction
    val result = config.getOperationName("SELECT * FROM users", Some("INSERT"))
    assertEquals(result, Some("INSERT"))
  }

  test("getOperationName should extract from query when extraction enabled and no API metadata") {
    val config = TelemetryConfig.withQueryTextExtraction
    val result = config.getOperationName("SELECT * FROM users")
    assertEquals(result, Some("SELECT"))
  }

  test("getOperationName should return None when extraction disabled and no API metadata") {
    val config = TelemetryConfig.default
    val result = config.getOperationName("SELECT * FROM users")
    assertEquals(result, None)
  }

  // ============================================================
  // getCollectionName tests
  // ============================================================

  test("getCollectionName should prefer API metadata when available") {
    val config = TelemetryConfig.withQueryTextExtraction
    val result = config.getCollectionName("SELECT * FROM users", Some("orders"))
    assertEquals(result, Some("orders"))
  }

  test("getCollectionName should extract from query when extraction enabled and no API metadata") {
    val config = TelemetryConfig.withQueryTextExtraction
    val result = config.getCollectionName("SELECT * FROM users")
    assertEquals(result, Some("users"))
  }

  test("getCollectionName should return None when extraction disabled and no API metadata") {
    val config = TelemetryConfig.default
    val result = config.getCollectionName("SELECT * FROM users")
    assertEquals(result, None)
  }

  // ============================================================
  // getQuerySummary tests
  // ============================================================

  test("getQuerySummary should return operation + collection when both available") {
    val config = TelemetryConfig.withQueryTextExtraction
    val result = config.getQuerySummary("SELECT * FROM users")
    assertEquals(result, Some("SELECT users"))
  }

  test("getQuerySummary should use API metadata when available") {
    val config = TelemetryConfig.default
    val result = config.getQuerySummary("SELECT * FROM users", Some("INSERT"), Some("orders"))
    assertEquals(result, Some("INSERT orders"))
  }

  test("getQuerySummary should return None when extraction disabled and no API metadata") {
    val config = TelemetryConfig.default
    val result = config.getQuerySummary("SELECT * FROM users")
    assertEquals(result, None)
  }

  test("getQuerySummary should return only operation when collection unavailable") {
    val config = TelemetryConfig.default
    val result = config.getQuerySummary("SELECT 1", Some("SELECT"))
    assertEquals(result, Some("SELECT"))
  }

  // ============================================================
  // generateSpanName tests
  // ============================================================

  test("generateSpanName should use query summary when extraction enabled") {
    val config   = TelemetryConfig.withQueryTextExtraction
    val spanName = config.generateSpanName("SELECT * FROM users WHERE id = 1")
    assertEquals(spanName, "SELECT users")
  }

  test("generateSpanName should use API metadata when available") {
    val config   = TelemetryConfig.default
    val spanName = config.generateSpanName(
      sql              = "SELECT * FROM users",
      apiOperationName = Some("SELECT"),
      apiCollectionName = Some("users")
    )
    assertEquals(spanName, "SELECT users")
  }

  test("generateSpanName should fallback to mysql when extraction disabled and no metadata") {
    val config   = TelemetryConfig.default
    val spanName = config.generateSpanName("SELECT * FROM users")
    assertEquals(spanName, "mysql")
  }

  test("generateSpanName should use namespace when no collection available") {
    val config   = TelemetryConfig.default
    val spanName = config.generateSpanName(
      sql              = "SELECT 1",
      apiOperationName = Some("SELECT"),
      namespace        = Some("test_db")
    )
    assertEquals(spanName, "SELECT test_db")
  }

  test("generateSpanName should use server:port as last resort target") {
    val config   = TelemetryConfig.default
    val spanName = config.generateSpanName(
      sql              = "SELECT 1",
      apiOperationName = Some("PING"),
      serverAddress    = Some("localhost"),
      serverPort       = Some(3306)
    )
    assertEquals(spanName, "PING localhost:3306")
  }
