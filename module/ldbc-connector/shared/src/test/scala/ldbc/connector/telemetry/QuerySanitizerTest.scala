/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.telemetry

import ldbc.connector.*

class QuerySanitizerTest extends FTestPlatform:

  // ============================================================
  // isParameterizedQuery Tests
  // ============================================================

  test("isParameterizedQuery should detect positional placeholders") {
    assert(QuerySanitizer.isParameterizedQuery("SELECT * FROM users WHERE id = ?"))
  }

  test("isParameterizedQuery should detect numeric placeholders") {
    assert(QuerySanitizer.isParameterizedQuery("SELECT * FROM users WHERE id = $1"))
  }

  test("isParameterizedQuery should detect named placeholders") {
    assert(QuerySanitizer.isParameterizedQuery("SELECT * FROM users WHERE id = :id"))
  }

  test("isParameterizedQuery should return false for non-parameterized queries") {
    assert(!QuerySanitizer.isParameterizedQuery("SELECT * FROM users WHERE id = 123"))
  }

  test("isParameterizedQuery should ignore ? inside single-quoted string literals") {
    assert(!QuerySanitizer.isParameterizedQuery("SELECT * FROM users WHERE name = 'What?' AND password = 'secret123'"))
  }

  test("isParameterizedQuery should ignore ? inside double-quoted string literals") {
    assert(!QuerySanitizer.isParameterizedQuery("""SELECT * FROM users WHERE name = "What?" AND id = 123"""))
  }

  test("isParameterizedQuery should detect real placeholder even with string literal containing ?") {
    assert(QuerySanitizer.isParameterizedQuery("SELECT * FROM users WHERE name = 'What?' AND id = ?"))
  }

  test("isParameterizedQuery should ignore named placeholder inside string literal") {
    assert(!QuerySanitizer.isParameterizedQuery("SELECT * FROM users WHERE time = '12:30:00'"))
  }

  test("isParameterizedQuery should ignore named placeholder in DATETIME string literal") {
    assert(!QuerySanitizer.isParameterizedQuery("SELECT * FROM events WHERE created_at = '2024-01-01 12:30:00'"))
  }

  test("isParameterizedQuery should ignore numeric placeholder inside string literal") {
    assert(!QuerySanitizer.isParameterizedQuery("SELECT * FROM users WHERE note = 'cost is $1'"))
  }

  test("isParameterizedQuery should ignore colon in URL string literal") {
    assert(!QuerySanitizer.isParameterizedQuery("INSERT INTO links (url) VALUES ('https://example.com')"))
  }

  // ============================================================
  // sanitize Tests
  // ============================================================

  test("sanitize should replace string literals") {
    val sql      = "SELECT * FROM users WHERE name = 'John'"
    val expected = "SELECT * FROM users WHERE name = ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should replace numeric literals") {
    val sql      = "SELECT * FROM users WHERE id = 123"
    val expected = "SELECT * FROM users WHERE id = ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should replace multiple literals") {
    val sql      = "SELECT * FROM users WHERE id = 123 AND name = 'John' AND active = TRUE"
    val expected = "SELECT * FROM users WHERE id = ? AND name = ? AND active = ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should preserve IS NULL keyword") {
    val sql = "SELECT * FROM users WHERE deleted_at IS NULL"
    assertEquals(QuerySanitizer.sanitize(sql), "SELECT * FROM users WHERE deleted_at IS NULL")
  }

  test("sanitize should preserve IS NOT NULL keyword") {
    val sql = "SELECT * FROM users WHERE deleted_at IS NOT NULL"
    assertEquals(QuerySanitizer.sanitize(sql), "SELECT * FROM users WHERE deleted_at IS NOT NULL")
  }

  test("sanitize should replace NULL value literal") {
    val sql = "INSERT INTO users (name, email) VALUES (NULL, NULL)"
    assertEquals(QuerySanitizer.sanitize(sql), "INSERT INTO users (name, email) VALUES (?, ?)")
  }

  test("sanitize should replace NULL in equality comparison") {
    val sql = "SELECT * FROM users WHERE name = NULL"
    assertEquals(QuerySanitizer.sanitize(sql), "SELECT * FROM users WHERE name = ?")
  }

  test("sanitize should replace double-quoted strings") {
    val sql      = """SELECT * FROM users WHERE name = "John""""
    val expected = "SELECT * FROM users WHERE name = ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should replace hex literals") {
    val sql      = "SELECT * FROM users WHERE flags = 0xFF"
    val expected = "SELECT * FROM users WHERE flags = ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should replace binary literals") {
    val sql      = "SELECT * FROM users WHERE mask = 0b1010"
    val expected = "SELECT * FROM users WHERE mask = ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should replace boolean literals") {
    val sql      = "SELECT * FROM users WHERE active = TRUE AND verified = FALSE"
    val expected = "SELECT * FROM users WHERE active = ? AND verified = ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should replace decimal numbers") {
    val sql      = "SELECT * FROM products WHERE price = 99.99"
    val expected = "SELECT * FROM products WHERE price = ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should handle escaped quotes in strings") {
    val sql      = "SELECT * FROM users WHERE name = 'O\\'Brien'"
    val expected = "SELECT * FROM users WHERE name = ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  // ============================================================
  // sanitizeIfNeeded Tests
  // ============================================================

  test("sanitizeIfNeeded should not sanitize parameterized queries") {
    val sql = "SELECT * FROM users WHERE id = ?"
    assertEquals(QuerySanitizer.sanitizeIfNeeded(sql), sql)
  }

  test("sanitizeIfNeeded should sanitize non-parameterized queries") {
    val sql      = "SELECT * FROM users WHERE id = 123"
    val expected = "SELECT * FROM users WHERE id = ?"
    assertEquals(QuerySanitizer.sanitizeIfNeeded(sql), expected)
  }

  test("sanitizeIfNeeded should sanitize query with ? inside string literal (security: M-2)") {
    val sql      = "SELECT * FROM users WHERE name = 'What?' AND password = 'secret123'"
    val expected = "SELECT * FROM users WHERE name = ? AND password = ?"
    assertEquals(QuerySanitizer.sanitizeIfNeeded(sql), expected)
  }

  // ============================================================
  // collapseInClauses Tests
  // ============================================================

  test("collapseInClauses should collapse IN clauses") {
    val sql      = "SELECT * FROM users WHERE id IN (?, ?, ?, ?)"
    val expected = "SELECT * FROM users WHERE id IN (?)"
    assertEquals(QuerySanitizer.collapseInClauses(sql), expected)
  }

  test("collapseInClauses should handle single placeholder") {
    val sql = "SELECT * FROM users WHERE id IN (?)"
    assertEquals(QuerySanitizer.collapseInClauses(sql), sql)
  }

  test("collapseInClauses should handle multiple IN clauses") {
    val sql      = "SELECT * FROM users WHERE id IN (?, ?, ?) AND status IN (?, ?)"
    val expected = "SELECT * FROM users WHERE id IN (?) AND status IN (?)"
    assertEquals(QuerySanitizer.collapseInClauses(sql), expected)
  }

  // ============================================================
  // extractOperationName Tests
  // ============================================================

  test("extractOperationName should extract SELECT") {
    assertEquals(QuerySanitizer.extractOperationName("SELECT * FROM users"), "SELECT")
  }

  test("extractOperationName should extract INSERT") {
    assertEquals(QuerySanitizer.extractOperationName("INSERT INTO users VALUES (1)"), "INSERT")
  }

  test("extractOperationName should extract UPDATE") {
    assertEquals(QuerySanitizer.extractOperationName("UPDATE users SET name = 'x'"), "UPDATE")
  }

  test("extractOperationName should extract DELETE") {
    assertEquals(QuerySanitizer.extractOperationName("DELETE FROM users WHERE id = 1"), "DELETE")
  }

  test("extractOperationName should preserve original case") {
    assertEquals(QuerySanitizer.extractOperationName("select * from users"), "select")
  }

  test("extractOperationName should handle leading whitespace") {
    assertEquals(QuerySanitizer.extractOperationName("  SELECT * FROM users"), "SELECT")
  }

  test("extractOperationName should return UNKNOWN for empty query") {
    assertEquals(QuerySanitizer.extractOperationName(""), "UNKNOWN")
  }

  // ============================================================
  // extractTableName Tests
  // ============================================================

  test("extractTableName should extract table from SELECT") {
    assertEquals(QuerySanitizer.extractTableName("SELECT * FROM users"), Some("users"))
  }

  test("extractTableName should extract table from INSERT") {
    assertEquals(QuerySanitizer.extractTableName("INSERT INTO orders VALUES (1)"), Some("orders"))
  }

  test("extractTableName should extract table from UPDATE") {
    assertEquals(QuerySanitizer.extractTableName("UPDATE products SET price = 100"), Some("products"))
  }

  test("extractTableName should extract table from DELETE") {
    assertEquals(QuerySanitizer.extractTableName("DELETE FROM users WHERE id = 1"), Some("users"))
  }

  test("extractTableName should return None for JOIN queries") {
    assertEquals(
      QuerySanitizer.extractTableName("SELECT * FROM users JOIN orders ON users.id = orders.user_id"),
      None
    )
  }

  test("extractTableName should return None for subqueries") {
    assertEquals(
      QuerySanitizer.extractTableName("SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)"),
      None
    )
  }

  test("extractTableName should return None for comma-separated tables") {
    assertEquals(QuerySanitizer.extractTableName("SELECT * FROM users, orders WHERE users.id = orders.user_id"), None)
  }

  test("extractTableName should handle backtick-quoted table names") {
    assertEquals(QuerySanitizer.extractTableName("SELECT * FROM `users`"), Some("users"))
  }

  test("extractTableName should preserve original case") {
    assertEquals(QuerySanitizer.extractTableName("SELECT * FROM Users"), Some("Users"))
  }

  test("extractTableName should extract table from multi-column SELECT") {
    assertEquals(
      QuerySanitizer.extractTableName("SELECT id, name, email FROM users WHERE active = true"),
      Some("users")
    )
  }

  test("extractTableName should extract table from SELECT with expressions containing commas") {
    assertEquals(
      QuerySanitizer.extractTableName("SELECT id, CONCAT(first_name, last_name) FROM users"),
      Some("users")
    )
  }

  // ============================================================
  // generateSummary Tests
  // ============================================================

  test("generateSummary should create operation table format") {
    assertEquals(QuerySanitizer.generateSummary("SELECT * FROM users"), "SELECT users")
  }

  test("generateSummary should handle queries without tables") {
    assertEquals(QuerySanitizer.generateSummary("SELECT 1"), "SELECT")
  }

  test("generateSummary should handle INSERT queries") {
    assertEquals(QuerySanitizer.generateSummary("INSERT INTO users VALUES (1, 'John')"), "INSERT users")
  }

  test("generateSummary should handle UPDATE queries") {
    assertEquals(QuerySanitizer.generateSummary("UPDATE products SET price = 100"), "UPDATE products")
  }

  test("generateSummary should handle DELETE queries") {
    assertEquals(QuerySanitizer.generateSummary("DELETE FROM orders WHERE id = 1"), "DELETE orders")
  }

  test("generateSummary should handle multi-column SELECT correctly") {
    assertEquals(
      QuerySanitizer.generateSummary("SELECT id, name, email FROM users WHERE active = true"),
      "SELECT users"
    )
  }

  test("generateSummary with explicit parameters should format correctly") {
    assertEquals(QuerySanitizer.generateSummary("SELECT", Some("users")), "SELECT users")
  }

  test("generateSummary with explicit parameters should handle None table") {
    assertEquals(QuerySanitizer.generateSummary("SELECT", None), "SELECT")
  }
