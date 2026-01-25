/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.telemetry

/**
 * Generates span names according to OpenTelemetry database semantic conventions v1.39.0.
 *
 * == Span Name Format ==
 * Priority order:
 * 1. `db.query.summary` (if available)
 * 2. `{db.operation.name} {target}`
 * 3. `{target}` alone
 * 4. `{db.system.name}` as fallback
 *
 * == Target Hierarchy ==
 * - `db.collection.name` for collection operations
 * - `db.stored_procedure.name` for stored procedures
 * - `db.namespace` for namespace operations
 * - `server.address:server.port` for others
 *
 * == Best Practices ==
 * Per OpenTelemetry spec v1.39.0:
 * - Prefer using `generate(Context)` with metadata from higher-level APIs
 * - Use `fromQuery(sql)` only as a FALLBACK when API metadata is unavailable
 * - Operation and collection names should ideally come from API metadata,
 *   not from parsing query text
 *
 * @see [[https://opentelemetry.io/docs/specs/semconv/database/database-spans/#span-name]]
 */
object SpanNameGenerator:

  /**
   * Context for generating span names.
   *
   * @param querySummary Low-cardinality query summary
   * @param operationName SQL operation name
   * @param collectionName Table/collection name
   * @param storedProcedureName Stored procedure name
   * @param namespace Database/schema name
   * @param serverAddress Server hostname
   * @param serverPort Server port number
   */
  case class Context(
    querySummary:        Option[String] = None,
    operationName:       Option[String] = None,
    collectionName:      Option[String] = None,
    storedProcedureName: Option[String] = None,
    namespace:           Option[String] = None,
    serverAddress:       Option[String] = None,
    serverPort:          Option[Int] = None
  )

  /**
   * Generates a span name from the given context.
   *
   * @param context The span name context
   * @return Generated span name
   */
  def generate(context: Context): String =
    // Priority 1: query summary
    context.querySummary match
      case Some(summary) => summary
      case None =>
        val target = resolveTarget(context)

        // Priority 2: {operation} {target}
        context.operationName match
          case Some(op) =>
            target match
              case Some(t) => s"$op $t"
              case None    => op
          case None =>
            // Priority 3: {target} alone
            target match
              case Some(t) => t
              // Priority 4: fallback to system name
              case None => TelemetryAttribute.DB_SYSTEM_MYSQL

  /**
   * Resolves the target according to the hierarchy.
   */
  private def resolveTarget(context: Context): Option[String] =
    context.collectionName
      .orElse(context.storedProcedureName)
      .orElse(context.namespace)
      .orElse(
        for
          addr <- context.serverAddress
          port <- context.serverPort
        yield s"$addr:$port"
      )

  /**
   * Generates a span name from SQL query (FALLBACK method).
   *
   * '''WARNING''': Per OpenTelemetry spec v1.39.0, operation and collection names
   * SHOULD NOT be extracted from query text. Use this method only when the
   * higher-level API does not provide operation metadata directly.
   *
   * Prefer using `generate(Context)` with metadata from:
   * - Prepared statement metadata
   * - API-level operation type
   * - Framework-provided context
   *
   * @param sql The SQL query
   * @param namespace Optional database namespace
   * @param serverAddress Optional server address
   * @param serverPort Optional server port
   * @return Generated span name
   */
  def fromQuery(
    sql:           String,
    namespace:     Option[String] = None,
    serverAddress: Option[String] = None,
    serverPort:    Option[Int] = None
  ): String =
    val summary = QuerySanitizer.generateSummary(sql)
    val context = Context(
      querySummary   = Some(summary),
      operationName  = Some(QuerySanitizer.extractOperationName(sql)),
      collectionName = QuerySanitizer.extractTableName(sql),
      namespace      = namespace,
      serverAddress  = serverAddress,
      serverPort     = serverPort
    )
    generate(context)
