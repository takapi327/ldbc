/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.telemetry

import ldbc.connector.*

class SpanNameGeneratorTest extends FTestPlatform:

  // ============================================================
  // generate Tests - Priority 1: querySummary
  // ============================================================

  test("generate should use querySummary when available") {
    val ctx = SpanNameGenerator.Context(querySummary = Some("SELECT users"))
    assertEquals(SpanNameGenerator.generate(ctx), "SELECT users")
  }

  test("generate should prefer querySummary over other fields") {
    val ctx = SpanNameGenerator.Context(
      querySummary   = Some("SELECT users"),
      operationName  = Some("INSERT"),
      collectionName = Some("orders")
    )
    assertEquals(SpanNameGenerator.generate(ctx), "SELECT users")
  }

  // ============================================================
  // generate Tests - Priority 2: operation + target
  // ============================================================

  test("generate should use operation + collection when querySummary is absent") {
    val ctx = SpanNameGenerator.Context(
      operationName  = Some("SELECT"),
      collectionName = Some("users")
    )
    assertEquals(SpanNameGenerator.generate(ctx), "SELECT users")
  }

  test("generate should use operation + storedProcedureName when collection is absent") {
    val ctx = SpanNameGenerator.Context(
      operationName       = Some("CALL"),
      storedProcedureName = Some("get_user")
    )
    assertEquals(SpanNameGenerator.generate(ctx), "CALL get_user")
  }

  test("generate should use operation + namespace when others are absent") {
    val ctx = SpanNameGenerator.Context(
      operationName = Some("USE"),
      namespace     = Some("test_db")
    )
    assertEquals(SpanNameGenerator.generate(ctx), "USE test_db")
  }

  test("generate should use operation + server:port as fallback target") {
    val ctx = SpanNameGenerator.Context(
      operationName = Some("PING"),
      serverAddress = Some("localhost"),
      serverPort    = Some(3306)
    )
    assertEquals(SpanNameGenerator.generate(ctx), "PING localhost:3306")
  }

  test("generate should use operation only when no target is available") {
    val ctx = SpanNameGenerator.Context(operationName = Some("PING"))
    assertEquals(SpanNameGenerator.generate(ctx), "PING")
  }

  // ============================================================
  // generate Tests - Priority 3: target alone
  // ============================================================

  test("generate should use collection alone when no operation") {
    val ctx = SpanNameGenerator.Context(collectionName = Some("users"))
    assertEquals(SpanNameGenerator.generate(ctx), "users")
  }

  test("generate should use storedProcedureName alone when no operation") {
    val ctx = SpanNameGenerator.Context(storedProcedureName = Some("get_user"))
    assertEquals(SpanNameGenerator.generate(ctx), "get_user")
  }

  test("generate should use namespace alone when no operation") {
    val ctx = SpanNameGenerator.Context(namespace = Some("test_db"))
    assertEquals(SpanNameGenerator.generate(ctx), "test_db")
  }

  test("generate should use server:port alone when no operation") {
    val ctx = SpanNameGenerator.Context(
      serverAddress = Some("localhost"),
      serverPort    = Some(3306)
    )
    assertEquals(SpanNameGenerator.generate(ctx), "localhost:3306")
  }

  // ============================================================
  // generate Tests - Priority 4: fallback
  // ============================================================

  test("generate should fallback to mysql when no context") {
    val ctx = SpanNameGenerator.Context()
    assertEquals(SpanNameGenerator.generate(ctx), "mysql")
  }

  test("generate should fallback when only partial server info") {
    // Only serverAddress without port should not form a target
    val ctx = SpanNameGenerator.Context(serverAddress = Some("localhost"))
    assertEquals(SpanNameGenerator.generate(ctx), "mysql")
  }

  test("generate should fallback when only port without address") {
    // Only serverPort without address should not form a target
    val ctx = SpanNameGenerator.Context(serverPort = Some(3306))
    assertEquals(SpanNameGenerator.generate(ctx), "mysql")
  }

  // ============================================================
  // Target resolution hierarchy Tests
  // ============================================================

  test("target resolution should prefer collection over storedProcedure") {
    val ctx = SpanNameGenerator.Context(
      operationName       = Some("SELECT"),
      collectionName      = Some("users"),
      storedProcedureName = Some("get_user")
    )
    assertEquals(SpanNameGenerator.generate(ctx), "SELECT users")
  }

  test("target resolution should prefer storedProcedure over namespace") {
    val ctx = SpanNameGenerator.Context(
      operationName       = Some("CALL"),
      storedProcedureName = Some("get_user"),
      namespace           = Some("test_db")
    )
    assertEquals(SpanNameGenerator.generate(ctx), "CALL get_user")
  }

  test("target resolution should prefer namespace over server:port") {
    val ctx = SpanNameGenerator.Context(
      operationName = Some("USE"),
      namespace     = Some("test_db"),
      serverAddress = Some("localhost"),
      serverPort    = Some(3306)
    )
    assertEquals(SpanNameGenerator.generate(ctx), "USE test_db")
  }
