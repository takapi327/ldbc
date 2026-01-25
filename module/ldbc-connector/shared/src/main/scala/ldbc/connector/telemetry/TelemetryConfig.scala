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
 *   Whether to extract operation name (`db.operation.name`) and collection name
 *   (`db.collection.name`) from query text. Per OpenTelemetry spec v1.39.0,
 *   this SHOULD NOT be done when higher-level API metadata is available.
 *   Default is `false` (spec-compliant).
 *   Set to `true` only when API-level metadata is not available.
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
  extractMetadataFromQueryText:    Boolean = false,
  sanitizeNonParameterizedQueries: Boolean = true,
  collapseInClauses:               Boolean = true
):

  /**
   * Enables extraction of operation and collection names from query text.
   *
   * '''Note''': Per OpenTelemetry spec v1.39.0, this SHOULD NOT be used when
   * higher-level API metadata is available. Use this only as a fallback.
   *
   * @return A new config with query text extraction enabled
   */
  def withQueryTextExtraction: TelemetryConfig =
    copy(extractMetadataFromQueryText = true)

  /**
   * Disables extraction of operation and collection names from query text.
   * This is the spec-compliant default behavior.
   *
   * @return A new config with query text extraction disabled
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
   * Extracts operation name from query if extraction is enabled.
   *
   * @param sql The SQL query
   * @param apiOperationName Operation name from higher-level API (preferred)
   * @return Operation name from API if available, from query if extraction enabled, None otherwise
   */
  def getOperationName(sql: String, apiOperationName: Option[String] = None): Option[String] =
    apiOperationName.orElse {
      if extractMetadataFromQueryText then Some(QuerySanitizer.extractOperationName(sql))
      else None
    }

  /**
   * Extracts collection (table) name from query if extraction is enabled.
   *
   * @param sql The SQL query
   * @param apiCollectionName Collection name from higher-level API (preferred)
   * @return Collection name from API if available, from query if extraction enabled, None otherwise
   */
  def getCollectionName(sql: String, apiCollectionName: Option[String] = None): Option[String] =
    apiCollectionName.orElse {
      if extractMetadataFromQueryText then QuerySanitizer.extractTableName(sql)
      else None
    }

  /**
   * Generates a query summary according to this configuration.
   *
   * @param sql The SQL query
   * @param apiOperationName Operation name from higher-level API (preferred)
   * @param apiCollectionName Collection name from higher-level API (preferred)
   * @return Query summary string, or None if extraction is disabled and no API metadata
   */
  def getQuerySummary(
    sql:               String,
    apiOperationName:  Option[String] = None,
    apiCollectionName: Option[String] = None
  ): Option[String] =
    val operation  = getOperationName(sql, apiOperationName)
    val collection = getCollectionName(sql, apiCollectionName)

    (operation, collection) match
      case (Some(op), Some(col)) => Some(s"$op $col")
      case (Some(op), None)      => Some(op)
      case (None, Some(col))     => Some(col)
      case (None, None)          => None

  /**
   * Generates a span name according to this configuration.
   *
   * Uses SpanNameGenerator with context built from available metadata.
   * Follows OpenTelemetry priority order:
   * 1. `{db.query.summary}` (operation + collection) if both available
   * 2. `{db.operation.name} {target}` if operation and target available
   * 3. `{target}` alone
   * 4. `{db.system.name}` as fallback
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
    val operationName  = getOperationName(sql, apiOperationName)
    val collectionName = getCollectionName(sql, apiCollectionName)

    // Only use querySummary (priority 1) when we have both operation and collection
    // Otherwise, let SpanNameGenerator handle priority 2-4 with namespace/server:port
    val querySummary = (operationName, collectionName) match
      case (Some(op), Some(col)) => Some(s"$op $col")
      case _                     => None

    val context = SpanNameGenerator.Context(
      querySummary   = querySummary,
      operationName  = operationName,
      collectionName = collectionName,
      namespace      = namespace,
      serverAddress  = serverAddress,
      serverPort     = serverPort
    )
    SpanNameGenerator.generate(context)

object TelemetryConfig:

  /**
   * Default configuration that is compliant with OpenTelemetry spec v1.39.0.
   *
   * - Query text extraction: disabled (spec-compliant)
   * - Sanitization: enabled (required by spec)
   * - IN clause collapsing: enabled (recommended)
   */
  val default: TelemetryConfig = TelemetryConfig()

  /**
   * Configuration that enables query text extraction as a fallback.
   *
   * Use this when higher-level API metadata is not available and you need
   * to extract operation/collection names from query text.
   *
   * - Query text extraction: enabled (fallback mode)
   * - Sanitization: enabled (required by spec)
   * - IN clause collapsing: enabled (recommended)
   */
  val withQueryTextExtraction: TelemetryConfig =
    TelemetryConfig(extractMetadataFromQueryText = true)
