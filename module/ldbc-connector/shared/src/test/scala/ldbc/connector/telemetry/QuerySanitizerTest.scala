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
  // hasCommaInFromClause boundary tests (B-8)
  // Verify ON DUPLICATE KEY UPDATE does not break table extraction
  // ============================================================

  test("extractTableName should handle INSERT ... ON DUPLICATE KEY UPDATE") {
    val sql = "INSERT INTO users (id, name) VALUES (1, 'test') ON DUPLICATE KEY UPDATE name = 'test'"
    assertEquals(QuerySanitizer.extractTableName(sql), Some("users"))
  }

  test("extractTableName should handle SELECT with ON in column alias") {
    val sql = "SELECT created_on FROM users"
    assertEquals(QuerySanitizer.extractTableName(sql), Some("users"))
  }

  test("extractTableName should handle SELECT FROM with comma and ON keyword after") {
    // " ON " in clauseKeywords could truncate the FROM clause range,
    // but this is a JOIN query so containsMultipleTables catches it via " JOIN " check first
    val sql = "SELECT * FROM users JOIN orders ON users.id = orders.user_id"
    assertEquals(QuerySanitizer.extractTableName(sql), None)
  }

  test("extractTableName should handle subquery with ON DUPLICATE") {
    val sql = "INSERT INTO users SELECT id, name FROM source ON DUPLICATE KEY UPDATE name = source.name"
    assertEquals(QuerySanitizer.extractTableName(sql), None) // contains (SELECT → multi-table
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

  // ============================================================
  // NumericPattern false-positive verification tests
  // Verify that \b\d+\.?\d*\b does NOT match digits within
  // column names, table names, or aliases
  // ============================================================

  test("sanitize should NOT corrupt column names containing digits") {
    val sql      = "SELECT col1, col2 FROM users WHERE id = 123"
    val expected = "SELECT col1, col2 FROM users WHERE id = ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should NOT corrupt table names containing digits") {
    val sql      = "SELECT * FROM table1 WHERE status = 'active'"
    val expected = "SELECT * FROM table1 WHERE status = ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should NOT corrupt aliases containing digits") {
    val sql      = "SELECT id AS id1, name AS name2 FROM users WHERE age = 30"
    val expected = "SELECT id AS id1, name AS name2 FROM users WHERE age = ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should NOT corrupt schema-qualified names with digits") {
    val sql      = "SELECT * FROM schema1.users WHERE id = 456"
    val expected = "SELECT * FROM schema1.users WHERE id = ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should NOT corrupt underscore-digit column names") {
    val sql      = "SELECT user_id1, order_id2 FROM orders WHERE total = 99.99"
    val expected = "SELECT user_id1, order_id2 FROM orders WHERE total = ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should preserve numeric literals in LIMIT/OFFSET") {
    val sql      = "SELECT * FROM users LIMIT 10 OFFSET 20"
    val expected = "SELECT * FROM users LIMIT 10 OFFSET 20"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should replace numeric literals in ORDER BY ordinal") {
    val sql      = "SELECT id, name FROM users ORDER BY 1"
    val expected = "SELECT id, name FROM users ORDER BY ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should NOT corrupt mixed identifiers like md5hash, v2") {
    val sql      = "SELECT v2, md5hash FROM data WHERE value = 42"
    val expected = "SELECT v2, md5hash FROM data WHERE value = ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  // ============================================================
  // BooleanPattern context-awareness tests
  // Verify that IS TRUE / IS FALSE / IS NOT TRUE / IS NOT FALSE
  // are preserved as SQL keywords, not replaced with ?
  // ============================================================

  test("sanitize should preserve IS TRUE as SQL keyword") {
    val sql      = "SELECT * FROM users WHERE active IS TRUE"
    val expected = "SELECT * FROM users WHERE active IS TRUE"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should preserve IS FALSE as SQL keyword") {
    val sql      = "SELECT * FROM users WHERE deleted IS FALSE"
    val expected = "SELECT * FROM users WHERE deleted IS FALSE"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should preserve IS NOT TRUE as SQL keyword") {
    val sql      = "SELECT * FROM users WHERE active IS NOT TRUE"
    val expected = "SELECT * FROM users WHERE active IS NOT TRUE"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should preserve IS NOT FALSE as SQL keyword") {
    val sql      = "SELECT * FROM users WHERE deleted IS NOT FALSE"
    val expected = "SELECT * FROM users WHERE deleted IS NOT FALSE"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should replace standalone TRUE/FALSE boolean literals") {
    val sql      = "INSERT INTO flags (col) VALUES (TRUE)"
    val expected = "INSERT INTO flags (col) VALUES (?)"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("sanitize should replace TRUE/FALSE in WHERE equality") {
    val sql      = "SELECT * FROM users WHERE active = TRUE AND deleted = FALSE"
    val expected = "SELECT * FROM users WHERE active = ? AND deleted = ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  // ============================================================
  // Multi-line SQL tests
  // ============================================================

  test("sanitize should handle multi-line SQL") {
    val sql      = "SELECT * FROM users\nWHERE id = 123\nAND name = 'John'"
    val expected = "SELECT * FROM users\nWHERE id = ?\nAND name = ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  test("extractOperationName should handle multi-line SQL") {
    assertEquals(QuerySanitizer.extractOperationName("SELECT *\nFROM users"), "SELECT")
  }

  test("extractTableName should handle FROM on different line") {
    assertEquals(
      QuerySanitizer.extractTableName("SELECT *\nFROM users\nWHERE id = 1"),
      Some("users")
    )
  }

  // ============================================================
  // SQL comment tests
  // ============================================================

  test("extractOperationName should return UNKNOWN for line comment at start") {
    assertEquals(QuerySanitizer.extractOperationName("-- comment\nSELECT * FROM users"), "UNKNOWN")
  }

  test("extractOperationName should return UNKNOWN for block comment at start") {
    assertEquals(QuerySanitizer.extractOperationName("/* comment */ SELECT * FROM users"), "UNKNOWN")
  }

  test("extractOperationName should handle trailing line comment") {
    assertEquals(QuerySanitizer.extractOperationName("SELECT * FROM users -- get all users"), "SELECT")
  }

  test("extractTableName should work with trailing line comment") {
    assertEquals(
      QuerySanitizer.extractTableName("SELECT * FROM users -- get all users"),
      Some("users")
    )
  }

  test("sanitize should replace literals inside SQL comments") {
    val sql      = "SELECT * FROM users WHERE id = 123 -- filter by user 456"
    val expected = "SELECT * FROM users WHERE id = ? -- filter by user ?"
    assertEquals(QuerySanitizer.sanitize(sql), expected)
  }

  // ============================================================
  // CTE (WITH ... AS) tests
  // ============================================================

  test("extractOperationName should return WITH for CTE") {
    assertEquals(
      QuerySanitizer.extractOperationName(
        "WITH active_users AS (SELECT * FROM users WHERE active = true) SELECT * FROM active_users"
      ),
      "WITH"
    )
  }

  test("extractTableName should return None for CTE (contains subquery)") {
    assertEquals(
      QuerySanitizer.extractTableName(
        "WITH active_users AS (SELECT * FROM users WHERE active = true) SELECT * FROM active_users"
      ),
      None
    )
  }

  test("generateSummary should return only operation for CTE") {
    assertEquals(
      QuerySanitizer.generateSummary(
        "WITH active_users AS (SELECT * FROM users WHERE active = true) SELECT * FROM active_users"
      ),
      "WITH"
    )
  }
