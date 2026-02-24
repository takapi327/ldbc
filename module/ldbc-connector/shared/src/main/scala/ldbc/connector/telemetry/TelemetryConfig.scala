/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.telemetry

/**
 * Configuration for OpenTelemetry telemetry behavior.
 *
 * This configuration allows fine-grained control over how telemetry data is collected
 * and processed, particularly regarding the extraction of metadata from SQL query text.
 *
 * == OpenTelemetry Spec Compliance ==
 * Per OpenTelemetry Semantic Conventions v1.39.0:
 * - Operation names SHOULD NOT be extracted from `db.query.text`
 * - Collection names SHOULD NOT be extracted from `db.query.text`
 * - Non-parameterized queries SHOULD be sanitized before recording
 *
 * The default configuration is spec-compliant. Users can opt-in to query text
 * extraction when higher-level API metadata is not available.
 *
 * @param extractMetadataFromQueryText
 *   Whether to generate `db.query.summary` from query text for span naming.
 *   Per OpenTelemetry spec v1.39.0, instrumentations that support query parsing
 *   SHOULD generate a query summary based on `db.query.text`.
 *   Default is `true` (spec-compliant: generates dynamic span names, e.g., "SELECT users").
 *   Set to `false` to use fixed span names from TelemetrySpanName enum.
 *
 *   '''Note''': Per OpenTelemetry spec v1.39.0, `db.operation.name` and
 *   `db.collection.name` attributes SHOULD NOT be extracted from `db.query.text`.
 *   This flag does NOT affect individual attribute extraction — only span name
 *   generation via `db.query.summary`.
 *
 * @param sanitizeNonParameterizedQueries
 *   Whether to sanitize non-parameterized queries by replacing literals with `?`.
 *   This is REQUIRED by OpenTelemetry spec for non-parameterized queries.
 *   Default is `true`.
 *
 * @param collapseInClauses
 *   Whether to collapse IN clauses (e.g., `IN (?, ?, ?)` becomes `IN (?)`).
 *   This helps manage cardinality for metrics and span names.
 *   Default is `true`.
 *
 * @see [[https://opentelemetry.io/docs/specs/semconv/database/database-spans/]]
 * @see [[https://opentelemetry.io/docs/specs/semconv/database/sql/]]
 */
final case class TelemetryConfig(
  extractMetadataFromQueryText:    Boolean = true,
  sanitizeNonParameterizedQueries: Boolean = true,
  collapseInClauses:               Boolean = true
):

  /**
   * Enables generation of `db.query.summary` from query text for dynamic span naming.
   *
   * Per OpenTelemetry spec v1.39.0, instrumentations that support query parsing
   * SHOULD generate a query summary based on `db.query.text`.
   *
   * '''Note''': This does NOT extract `db.operation.name` or `db.collection.name`
   * from query text (which is SHOULD NOT per spec). It only affects span name
   * generation via `db.query.summary`.
   *
   * @return A new config with query summary generation enabled
   */
  def withQueryTextExtraction: TelemetryConfig =
    copy(extractMetadataFromQueryText = true)

  /**
   * Disables generation of `db.query.summary` from query text.
   * Span names will use fixed names from TelemetrySpanName enum.
   *
   * @return A new config with query summary generation disabled
   */
  def withoutQueryTextExtraction: TelemetryConfig =
    copy(extractMetadataFromQueryText = false)

  /**
   * Enables sanitization of non-parameterized queries.
   *
   * @return A new config with sanitization enabled
   */
  def withSanitization: TelemetryConfig =
    copy(sanitizeNonParameterizedQueries = true)

  /**
   * Disables sanitization of non-parameterized queries.
   *
   * '''Warning''': This may expose sensitive data in telemetry.
   * Only disable if you have alternative sanitization in place.
   *
   * @return A new config with sanitization disabled
   */
  def withoutSanitization: TelemetryConfig =
    copy(sanitizeNonParameterizedQueries = false)

  /**
   * Enables collapsing of IN clauses.
   *
   * @return A new config with IN clause collapsing enabled
   */
  def withInClauseCollapsing: TelemetryConfig =
    copy(collapseInClauses = true)

  /**
   * Disables collapsing of IN clauses.
   *
   * @return A new config with IN clause collapsing disabled
   */
  def withoutInClauseCollapsing: TelemetryConfig =
    copy(collapseInClauses = false)

  // ============================================================
  // Config-aware query processing methods
  // ============================================================

  /**
   * Processes query text according to this configuration.
   *
   * - Sanitizes non-parameterized queries if `sanitizeNonParameterizedQueries` is true
   * - Collapses IN clauses if `collapseInClauses` is true
   *
   * @param sql The SQL query
   * @return Processed query text
   */
  def processQueryText(sql: String): String =
    val sanitized =
      if sanitizeNonParameterizedQueries then QuerySanitizer.sanitizeIfNeeded(sql)
      else sql

    if collapseInClauses then QuerySanitizer.collapseInClauses(sanitized)
    else sanitized

  /**
   * Generates a query summary for `db.query.summary` and span naming.
   *
   * Per OpenTelemetry spec v1.39.0, instrumentations that support query parsing
   * SHOULD generate a query summary based on `db.query.text`. This is distinct
   * from `db.operation.name` / `db.collection.name` attributes which SHOULD NOT
   * be extracted from query text.
   *
   * Priority:
   * 1. API-provided metadata (operation + collection)
   * 2. Query text parsing via QuerySanitizer (when `extractMetadataFromQueryText` is enabled)
   *
   * @param sql The SQL query
   * @param apiOperationName Operation name from higher-level API (preferred)
   * @param apiCollectionName Collection name from higher-level API (preferred)
   * @return Query summary string, or None if generation is disabled and no API metadata
   */
  def getQuerySummary(
    sql:               String,
    apiOperationName:  Option[String] = None,
    apiCollectionName: Option[String] = None
  ): Option[String] =
    (apiOperationName, apiCollectionName) match
      case (Some(op), Some(col)) => Some(s"$op $col")
      case (Some(op), None)      => Some(op)
      case (None, Some(_))       => None
      case (None, None)          =>
        // Per OTel spec: instrumentations that support query parsing SHOULD
        // generate a query summary based on db.query.text
        if extractMetadataFromQueryText then Some(QuerySanitizer.generateSummary(sql))
        else None

  /**
   * Generates a span name according to this configuration.
   *
   * Uses SpanNameGenerator with context built from available metadata.
   * Follows OpenTelemetry priority order:
   * 1. `{db.query.summary}` from API metadata or query text parsing (SHOULD per spec)
   * 2. `{db.operation.name} {target}` if API-provided operation and target available
   * 3. `{target}` alone
   * 4. `{db.system.name}` as fallback
   *
   * '''Note''': Query text parsing for span names (`db.query.summary`) is recommended
   * by the spec. Individual attributes (`db.operation.name`, `db.collection.name`)
   * are NOT extracted from query text (SHOULD NOT per spec).
   *
   * @param sql The SQL query
   * @param apiOperationName Operation name from higher-level API
   * @param apiCollectionName Collection name from higher-level API
   * @param namespace Database namespace
   * @param serverAddress Server address
   * @param serverPort Server port
   * @return Generated span name
   */
  def generateSpanName(
    sql:               String,
    apiOperationName:  Option[String] = None,
    apiCollectionName: Option[String] = None,
    namespace:         Option[String] = None,
    serverAddress:     Option[String] = None,
    serverPort:        Option[Int] = None
  ): String =
    // For span name context, extract operation/collection from query text when enabled.
    // This is part of db.query.summary generation (SHOULD per spec).
    // Note: this is for span naming only, NOT for db.operation.name/db.collection.name attributes.
    val operationForSpan = apiOperationName.orElse(
      if extractMetadataFromQueryText then Some(QuerySanitizer.extractOperationName(sql))
      else None
    )
    val collectionForSpan = apiCollectionName.orElse(
      if extractMetadataFromQueryText then QuerySanitizer.extractTableName(sql)
      else None
    )

    // Use querySummary (Priority 1) only when both operation and collection are available,
    // so that partial information (operation only) can be combined with namespace/server targets
    // via SpanNameGenerator's Priority 2 hierarchy.
    val querySummary = (operationForSpan, collectionForSpan) match
      case (Some(op), Some(col)) => Some(s"$op $col")
      case _                     => None

    val context = SpanNameGenerator.Context(
      querySummary   = querySummary,
      operationName  = operationForSpan,
      collectionName = collectionForSpan,
      namespace      = namespace,
      serverAddress  = serverAddress,
      serverPort     = serverPort
    )
    SpanNameGenerator.generate(context)

  /**
   * Resolves the span name for a SQL operation.
   *
   * When `extractMetadataFromQueryText` is enabled, generates a dynamic span name
   * from the SQL query (e.g., "SELECT users"). Otherwise, falls back to the provided
   * default span name (e.g., "Execute Statement").
   *
   * @param sql The SQL query
   * @param defaultName Fallback span name when metadata extraction is disabled
   * @param namespace Database namespace
   * @param serverAddress Server address
   * @param serverPort Server port
   * @return Resolved span name
   */
  def resolveSpanName(
    sql:           String,
    defaultName:   TelemetrySpanName,
    namespace:     Option[String] = None,
    serverAddress: Option[String] = None,
    serverPort:    Option[Int] = None
  ): String =
    if extractMetadataFromQueryText then
      generateSpanName(sql, namespace = namespace, serverAddress = serverAddress, serverPort = serverPort)
    else defaultName.name

object TelemetryConfig:

  /**
   * Default configuration that is compliant with OpenTelemetry spec v1.39.0.
   *
   * - Query summary generation: enabled (SHOULD per spec)
   * - Sanitization: enabled (required by spec)
   * - IN clause collapsing: enabled (recommended)
   */
  val default: TelemetryConfig = TelemetryConfig()

  /**
   * Configuration that disables `db.query.summary` generation from query text.
   *
   * Span names will use fixed names from TelemetrySpanName enum
   * (e.g., "Execute Statement" instead of "SELECT users").
   *
   * - Query summary generation: disabled
   * - Sanitization: enabled (required by spec)
   * - IN clause collapsing: enabled (recommended)
   */
  val withoutQueryTextExtraction: TelemetryConfig =
    TelemetryConfig(extractMetadataFromQueryText = false)
