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

  test("default config should have extractMetadataFromQueryText enabled") {
    assertEquals(TelemetryConfig.default.extractMetadataFromQueryText, true)
  }

  test("default config should have sanitization enabled") {
    assertEquals(TelemetryConfig.default.sanitizeNonParameterizedQueries, true)
  }

  test("default config should have IN clause collapsing enabled") {
    assertEquals(TelemetryConfig.default.collapseInClauses, true)
  }

  // ============================================================
  // withoutQueryTextExtraction preset tests
  // ============================================================

  test("withoutQueryTextExtraction preset should have extraction disabled") {
    assertEquals(TelemetryConfig.withoutQueryTextExtraction.extractMetadataFromQueryText, false)
  }

  test("withoutQueryTextExtraction preset should keep sanitization enabled") {
    assertEquals(TelemetryConfig.withoutQueryTextExtraction.sanitizeNonParameterizedQueries, true)
  }

  // ============================================================
  // Fluent API tests
  // ============================================================

  test("withQueryTextExtraction method should enable extraction") {
    val config = TelemetryConfig.withoutQueryTextExtraction.withQueryTextExtraction
    assertEquals(config.extractMetadataFromQueryText, true)
  }

  test("withoutQueryTextExtraction method should disable extraction") {
    val config = TelemetryConfig.default.withoutQueryTextExtraction
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
  // getQuerySummary tests
  // Per OTel spec v1.39.0: instrumentations that support query
  // parsing SHOULD generate a query summary based on db.query.text
  // ============================================================

  test("getQuerySummary should generate summary from query text when extraction enabled") {
    val config = TelemetryConfig.default
    val result = config.getQuerySummary("SELECT * FROM users")
    assertEquals(result, Some("SELECT users"))
  }

  test("getQuerySummary should use API metadata when available (takes priority over query text)") {
    val config = TelemetryConfig.default
    val result = config.getQuerySummary("SELECT * FROM users", Some("INSERT"), Some("orders"))
    assertEquals(result, Some("INSERT orders"))
  }

  test("getQuerySummary should use API metadata even when extraction disabled") {
    val config = TelemetryConfig.withoutQueryTextExtraction
    val result = config.getQuerySummary("SELECT * FROM users", Some("INSERT"), Some("orders"))
    assertEquals(result, Some("INSERT orders"))
  }

  test("getQuerySummary should return None when extraction disabled and no API metadata") {
    val config = TelemetryConfig.withoutQueryTextExtraction
    val result = config.getQuerySummary("SELECT * FROM users")
    assertEquals(result, None)
  }

  test("getQuerySummary should return only operation when collection unavailable via API metadata") {
    val config = TelemetryConfig.withoutQueryTextExtraction
    val result = config.getQuerySummary("SELECT 1", Some("SELECT"))
    assertEquals(result, Some("SELECT"))
  }

  // ============================================================
  // generateSpanName tests
  // ============================================================

  test("generateSpanName should use query summary when extraction enabled") {
    val config   = TelemetryConfig.default
    val spanName = config.generateSpanName("SELECT * FROM users WHERE id = 1")
    assertEquals(spanName, "SELECT users")
  }

  test("generateSpanName should use API metadata when available") {
    val config   = TelemetryConfig.withoutQueryTextExtraction
    val spanName = config.generateSpanName(
      sql               = "SELECT * FROM users",
      apiOperationName  = Some("SELECT"),
      apiCollectionName = Some("users")
    )
    assertEquals(spanName, "SELECT users")
  }

  test("generateSpanName should fallback to mysql when extraction disabled and no metadata") {
    val config   = TelemetryConfig.withoutQueryTextExtraction
    val spanName = config.generateSpanName("SELECT * FROM users")
    assertEquals(spanName, "mysql")
  }

  test("generateSpanName should use namespace when no collection available") {
    val config   = TelemetryConfig.withoutQueryTextExtraction
    val spanName = config.generateSpanName(
      sql              = "SELECT 1",
      apiOperationName = Some("SELECT"),
      namespace        = Some("test_db")
    )
    assertEquals(spanName, "SELECT test_db")
  }

  test("generateSpanName should use server:port as last resort target") {
    val config   = TelemetryConfig.withoutQueryTextExtraction
    val spanName = config.generateSpanName(
      sql              = "SELECT 1",
      apiOperationName = Some("PING"),
      serverAddress    = Some("localhost"),
      serverPort       = Some(3306)
    )
    assertEquals(spanName, "PING localhost:3306")
  }

  // ============================================================
  // resolveSpanName tests
  // ============================================================

  test("resolveSpanName should use default name when extraction is disabled") {
    val config   = TelemetryConfig.withoutQueryTextExtraction
    val spanName = config.resolveSpanName("SELECT * FROM users", TelemetrySpanName.STMT_EXECUTE)
    assertEquals(spanName, "Execute Statement")
  }

  test("resolveSpanName should generate dynamic name when extraction is enabled") {
    val config   = TelemetryConfig.default
    val spanName = config.resolveSpanName("SELECT * FROM users", TelemetrySpanName.STMT_EXECUTE)
    assertEquals(spanName, "SELECT users")
  }

  test("resolveSpanName should use namespace when extraction enabled but no table") {
    val config   = TelemetryConfig.default
    val spanName = config.resolveSpanName("SELECT 1", TelemetrySpanName.STMT_EXECUTE, namespace = Some("mydb"))
    assertEquals(spanName, "SELECT mydb")
  }

  // ============================================================
  // getQuerySummary vs generateSpanName behavior difference tests
  //
  // These methods intentionally differ for the "operation only" case:
  // - getQuerySummary returns the db.query.summary ATTRIBUTE value
  // - generateSpanName generates a SPAN NAME using SpanNameGenerator's
  //   priority hierarchy, leveraging namespace/server targets
  // ============================================================

  test("getQuerySummary should return operation only for query without table") {
    val config = TelemetryConfig.default
    val result = config.getQuerySummary("SELECT 1")
    assertEquals(result, Some("SELECT"))
  }

  test("generateSpanName should use namespace fallback for query without table") {
    val config   = TelemetryConfig.default
    val spanName = config.generateSpanName("SELECT 1", namespace = Some("mydb"))
    // generateSpanName intentionally does NOT set querySummary when only operation is available,
    // so it falls through to Priority 2: {operation} {target} = "SELECT mydb"
    assertEquals(spanName, "SELECT mydb")
  }

  test("getQuerySummary and generateSpanName should agree when both operation and collection available") {
    val config  = TelemetryConfig.default
    val summary = config.getQuerySummary("SELECT * FROM users")
    val span    = config.generateSpanName("SELECT * FROM users")
    assertEquals(summary, Some("SELECT users"))
    assertEquals(span, "SELECT users")
  }
