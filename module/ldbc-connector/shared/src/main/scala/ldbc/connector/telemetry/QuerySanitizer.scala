/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.telemetry

import scala.util.matching.Regex

/**
 * SQL query sanitizer for OpenTelemetry compliance.
 *
 * Replaces sensitive literals in SQL queries with placeholders
 * according to OpenTelemetry semantic conventions v1.39.0.
 *
 * == Query Text Recording Rules ==
 * Per OpenTelemetry spec:
 * - Parameterized queries: Record by default WITHOUT sanitization
 * - Non-parameterized queries: Record ONLY with sanitization (replace literals with `?`)
 * - Batch operations: Concatenate individual texts with `;` separator if different
 * - IN-clauses: May be collapsed (e.g., `IN (?)`) to manage cardinality
 * - Truncation: Permitted for performance considerations
 *
 * == Operation/Collection Name Extraction ==
 * Per OpenTelemetry spec v1.39.0:
 * - Operation names SHOULD NOT be extracted from `db.query.text`
 * - Collection names SHOULD NOT be extracted from `db.query.text`
 * - Prefer using higher-level API metadata when available
 *
 * The extraction methods in this object are provided as a FALLBACK for cases
 * where the instrumentation's higher-level API does not provide operation or
 * collection metadata directly (e.g., raw query execution).
 *
 * @see [[https://opentelemetry.io/docs/specs/semconv/database/database-spans/#recording-database-query-text]]
 * @see [[https://opentelemetry.io/docs/specs/semconv/database/sql/]]
 */
object QuerySanitizer:

  /**
   * Placeholder character for sanitized values.
   * Per OpenTelemetry spec: "The placeholder value SHOULD be ?"
   */
  val PLACEHOLDER: String = "?"

  /**
   * Maximum query length for regex-based processing.
   * Queries exceeding this limit are returned as-is (or truncated for summaries)
   * to prevent ReDoS attacks from pathological input patterns.
   */
  val MAX_QUERY_LENGTH: Int = 10000

  // Regex patterns for different literal types
  private val STRING_LITERAL_PATTERN:  Regex = """'(?:[^'\\]|\\.)*'""".r
  private val DOUBLE_QUOTED_PATTERN:   Regex = """"(?:[^"\\]|\\.)*"""".r
  private val NUMERIC_PATTERN:         Regex = """\b\d+\.?\d*\b""".r
  private val HEX_PATTERN:             Regex = """0[xX][0-9a-fA-F]+""".r
  private val BINARY_PATTERN:          Regex = """0[bB][01]+""".r
  private val NULL_PATTERN:            Regex = """(?i)(?:IS\s+NOT\s+|IS\s+)?\bNULL\b""".r
  private val BOOLEAN_PATTERN:         Regex = """(?i)\b(?:TRUE|FALSE)\b""".r
  private val LIMIT_OFFSET_CONTEXT:    Regex = """(?i)\b(?:LIMIT|OFFSET)\s*$""".r

  // Pattern to extract operation name (preserves original case)
  private val OPERATION_PATTERN: Regex = """^\s*(\w+)""".r

  // Patterns for parameterized query detection
  private val POSITIONAL_PLACEHOLDER_PATTERN: Regex = """\?""".r
  private val NUMERIC_PLACEHOLDER_PATTERN:    Regex = """\$\d+""".r
  private val NAMED_PLACEHOLDER_PATTERN:      Regex = """:\w+""".r

  // Pattern to extract table name from common SQL statements (case-insensitive)
  private val SELECT_FROM_PATTERN: Regex = """(?i)\bFROM\s+`?(\w+)`?""".r
  private val INSERT_INTO_PATTERN: Regex = """(?i)\bINTO\s+`?(\w+)`?""".r
  private val UPDATE_PATTERN:      Regex = """(?i)\bUPDATE\s+`?(\w+)`?""".r

  /**
   * Checks if a query is parameterized (contains placeholders).
   * Parameterized queries should NOT be sanitized per OpenTelemetry spec.
   *
   * @param sql The SQL query
   * @return true if the query contains parameterized placeholders
   */
  def isParameterizedQuery(sql: String): Boolean =
    if sql.length > MAX_QUERY_LENGTH then false
    else
      // Strip string literals first to avoid false positives from
      // placeholders inside literal values (e.g., 'What?' or "value:name")
      val stripped = STRING_LITERAL_PATTERN.replaceAllIn(
        DOUBLE_QUOTED_PATTERN.replaceAllIn(sql, ""),
        ""
      )
      POSITIONAL_PLACEHOLDER_PATTERN.findFirstIn(stripped).isDefined ||
      NUMERIC_PLACEHOLDER_PATTERN.findFirstIn(stripped).isDefined ||
      NAMED_PLACEHOLDER_PATTERN.findFirstIn(stripped).isDefined

  /**
   * Sanitizes SQL query by replacing all literal values with placeholders.
   * This should only be called for non-parameterized queries.
   *
   * @param sql The original SQL query
   * @return Sanitized query with literals replaced by "?"
   */
  def sanitize(sql: String): String =
    if sql.length > MAX_QUERY_LENGTH then sql
    else
      // Order matters: process string literals first to avoid partial matches
      val patterns = List(
        STRING_LITERAL_PATTERN,
        DOUBLE_QUOTED_PATTERN,
        HEX_PATTERN,
        BINARY_PATTERN,
        BOOLEAN_PATTERN
      )
      val result = patterns.foldLeft(sql)((result, pattern) => pattern.replaceAllIn(result, PLACEHOLDER))
      // Handle numeric literals with context awareness:
      // Preserve values after LIMIT/OFFSET for better observability
      val resultWithNumerics = NUMERIC_PATTERN.replaceAllIn(
        result,
        m =>
          val prefix = result.substring(0, m.start)
          if LIMIT_OFFSET_CONTEXT.findFirstIn(prefix).isDefined then Regex.quoteReplacement(m.matched)
          else PLACEHOLDER
      )
      // Handle NULL separately: preserve IS NULL / IS NOT NULL, replace standalone NULL with placeholder
      NULL_PATTERN.replaceAllIn(
        resultWithNumerics,
        m =>
          if m.matched.trim.toUpperCase.startsWith("IS") then Regex.quoteReplacement(m.matched)
          else PLACEHOLDER
      )

  /**
   * Conditionally sanitizes SQL query based on whether it's parameterized.
   * Per OpenTelemetry spec: Parameterized queries SHOULD NOT be sanitized.
   *
   * @param sql The original SQL query
   * @return The query, sanitized only if it's not already parameterized
   */
  def sanitizeIfNeeded(sql: String): String =
    if isParameterizedQuery(sql) then sql
    else sanitize(sql)

  /**
   * Collapses multiple placeholders in IN clauses.
   * "IN (?, ?, ?, ?)" becomes "IN (?)"
   *
   * @param sql The sanitized SQL query
   * @return Query with collapsed IN clauses
   */
  def collapseInClauses(sql: String): String =
    if sql.length > MAX_QUERY_LENGTH then sql
    else
      val inClausePattern = """\bIN\s*\(\s*\?(?:\s*,\s*\?)*\s*\)""".r
      inClausePattern.replaceAllIn(sql, "IN (?)")

  /**
   * Extracts the SQL operation name from a query (FALLBACK method).
   *
   * '''WARNING''': Per OpenTelemetry spec v1.39.0, operation names SHOULD NOT be
   * extracted from `db.query.text`. Use this method only when the higher-level API
   * does not provide operation metadata directly.
   *
   * When available, prefer using operation names from:
   * - Prepared statement metadata
   * - API-level operation type (e.g., `executeQuery` vs `executeUpdate`)
   * - Framework-provided operation context
   *
   * @note Case is NOT normalized - original casing is preserved per spec.
   * @param sql The SQL query
   * @return The operation name preserving original case, or "UNKNOWN"
   */
  def extractOperationName(sql: String): String =
    OPERATION_PATTERN.findFirstMatchIn(sql.trim) match
      case Some(m) => m.group(1) // Preserve original case per OpenTelemetry spec
      case None    => "UNKNOWN"

  /**
   * Extracts the primary table name from a query (FALLBACK method).
   *
   * '''WARNING''': Per OpenTelemetry spec v1.39.0, collection names SHOULD NOT be
   * extracted from `db.query.text`. Use this method only when the higher-level API
   * does not provide collection metadata directly.
   *
   * When available, prefer using collection names from:
   * - Prepared statement metadata
   * - ORM/framework entity mappings
   * - API-level table/collection context
   *
   * @note Case is NOT normalized - original casing is preserved per spec.
   * @note Returns None for multi-table operations (JOINs, subqueries) per spec.
   * @param sql The SQL query
   * @return Optional table name (None for multi-table operations)
   */
  def extractTableName(sql: String): Option[String] =
    if sql.length > MAX_QUERY_LENGTH then None
    // Check for multi-table operations - return None per OpenTelemetry spec
    else if containsMultipleTables(sql) then None
    else
      val operationUpper = extractOperationName(sql).toUpperCase
      operationUpper match
        case "SELECT" | "DELETE" =>
          SELECT_FROM_PATTERN.findFirstMatchIn(sql).map(_.group(1)) // Preserves original case
        case "INSERT" =>
          INSERT_INTO_PATTERN.findFirstMatchIn(sql).map(_.group(1))
        case "UPDATE" =>
          UPDATE_PATTERN.findFirstMatchIn(sql).map(_.group(1))
        case _ => None

  /**
   * Checks if query involves multiple tables (JOIN, subquery, etc.)
   * Per OpenTelemetry spec: Don't extract collection names for multi-collection operations
   */
  private def containsMultipleTables(sql: String): Boolean =
    val upperSql = sql.toUpperCase
    upperSql.contains(" JOIN ") ||
    upperSql.contains("(SELECT") ||
    hasCommaInFromClause(upperSql)

  /**
   * Checks if there is a comma within the FROM clause (between FROM and the next SQL keyword).
   * This distinguishes `SELECT a, b FROM users` (comma in column list, single table)
   * from `SELECT * FROM users, orders` (comma in FROM clause, multiple tables).
   */
  private def hasCommaInFromClause(upperSql: String): Boolean =
    val fromIdx = upperSql.indexOf(" FROM ")
    if fromIdx < 0 then false
    else
      val afterFrom      = upperSql.substring(fromIdx + 6)
      val clauseKeywords = List(" WHERE ", " ORDER ", " GROUP ", " HAVING ", " LIMIT ", " UNION ", " SET ", " ON ")
      val endIdx         = clauseKeywords.flatMap { kw =>
        val idx = afterFrom.indexOf(kw)
        if idx >= 0 then Some(idx) else None
      } match
        case Nil     => afterFrom.length
        case indices => indices.min
      afterFrom.substring(0, endIdx).contains(",")

  /**
   * Generates a low-cardinality query summary for span names (FALLBACK method).
   *
   * '''WARNING''': Per OpenTelemetry spec v1.39.0, avoid extracting `db.query.summary`
   * from query text when APIs provide direct operation metadata.
   *
   * Format: "{operation} {table}" or just "{operation}"
   * Per OpenTelemetry spec: Max 255 characters, preserve original casing
   *
   * Prefer using the overloaded `generateSummary(operation, table)` method
   * when operation and table metadata are available from higher-level APIs.
   *
   * @param sql The SQL query
   * @return Query summary string
   */
  def generateSummary(sql: String): String =
    val operation = extractOperationName(sql)
    val table     = extractTableName(sql)

    val summary = table match
      case Some(t) => s"$operation $t"
      case None    => operation

    // Limit to 255 characters per spec
    if summary.length > 255 then summary.take(255)
    else summary

  /**
   * Generates a query summary with explicit operation and table.
   * Preferred over extracting from query text when metadata is available.
   *
   * @param operation The SQL operation
   * @param table Optional table name
   * @return Query summary string
   */
  def generateSummary(operation: String, table: Option[String]): String =
    table match
      case Some(t) => s"$operation $t"
      case None    => operation
