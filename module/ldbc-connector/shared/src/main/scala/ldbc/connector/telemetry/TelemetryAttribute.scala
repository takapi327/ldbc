/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.telemetry

import org.typelevel.otel4s.Attribute

/**
 * OpenTelemetry semantic conventions for database client operations.
 *
 * Based on:
 * - https://opentelemetry.io/docs/specs/semconv/db/database-spans/
 * - https://opentelemetry.io/docs/specs/semconv/db/sql/
 *
 * @see [[https://opentelemetry.io/docs/specs/semconv/db/ Database Semantic Conventions]]
 */
object TelemetryAttribute:

  // ============================================================
  // Attribute Keys (Semantic Conventions v1.39.0)
  // ============================================================

  // Required attributes
  val DB_SYSTEM_NAME: String = "db.system.name"

  // Conditionally required attributes
  val DB_COLLECTION_NAME:      String = "db.collection.name"
  val DB_NAMESPACE:            String = "db.namespace"
  val DB_OPERATION_NAME:       String = "db.operation.name"
  val DB_RESPONSE_STATUS_CODE: String = "db.response.status_code"
  val SERVER_ADDRESS:          String = "server.address"
  val SERVER_PORT:             String = "server.port"
  val ERROR_TYPE:              String = "error.type"

  // Recommended attributes
  val DB_OPERATION_BATCH_SIZE:  String = "db.operation.batch.size"
  val DB_QUERY_SUMMARY:         String = "db.query.summary"
  val DB_QUERY_TEXT:            String = "db.query.text"
  val DB_STORED_PROCEDURE_NAME: String = "db.stored_procedure.name"
  val NETWORK_PEER_ADDRESS:     String = "network.peer.address"
  val NETWORK_PEER_PORT:        String = "network.peer.port"

  // Opt-in attributes (Development status)
  val DB_OPERATION_PARAMETER_PREFIX: String = "db.operation.parameter."
  val DB_QUERY_PARAMETER_PREFIX:     String = "db.query.parameter."
  val DB_RESPONSE_RETURNED_ROWS:     String = "db.response.returned_rows"

  // MySQL-specific attributes
  val DB_MYSQL_VERSION:     String = "db.mysql.version"
  val DB_MYSQL_THREAD_ID:   String = "db.mysql.thread_id"
  val DB_MYSQL_AUTH_PLUGIN: String = "db.mysql.auth_plugin"

  // Connection pool attributes (Development status)
  val DB_CLIENT_CONNECTION_POOL_NAME: String = "db.client.connection.pool.name"
  val DB_CLIENT_CONNECTION_STATE:     String = "db.client.connection.state"

  // Schema URL for semantic convention version tracking
  val OTEL_SCHEMA_URL: String = "otel.schema_url"

  // ============================================================
  // Attribute Values
  // ============================================================

  /** Schema URL indicating the semantic convention version used. */
  val SCHEMA_URL_VALUE: String = "https://opentelemetry.io/schemas/1.39.0"

  /** MySQL system identifier */
  val DB_SYSTEM_MYSQL: String = "mysql"

  // Connection states
  val CONNECTION_STATE_IDLE: String = "idle"
  val CONNECTION_STATE_USED: String = "used"

  /**
   * Common SQL operations.
   */
  object SqlOperation:
    val SELECT:   String = "SELECT"
    val INSERT:   String = "INSERT"
    val UPDATE:   String = "UPDATE"
    val DELETE:   String = "DELETE"
    val CREATE:   String = "CREATE"
    val DROP:     String = "DROP"
    val ALTER:    String = "ALTER"
    val TRUNCATE: String = "TRUNCATE"
    val CALL:     String = "CALL"
    val COMMIT:   String = "COMMIT"
    val ROLLBACK: String = "ROLLBACK"
    val SET:      String = "SET"
    val SHOW:     String = "SHOW"
    val USE:      String = "USE"
    val EXPLAIN:  String = "EXPLAIN"
    val BATCH:    String = "BATCH"
    val PING:     String = "PING"
    val INIT_DB:  String = "INIT_DB"
    val KILL:     String = "KILL"
    val SHUTDOWN: String = "SHUTDOWN"

  // ============================================================
  // Attribute Factory Methods
  // ============================================================

  /**
   * Database system identifier (Required)
   * @see [[https://opentelemetry.io/docs/specs/semconv/attributes-registry/db/#db-system-name]]
   */
  val dbSystemName: Attribute[String] = Attribute(DB_SYSTEM_NAME, DB_SYSTEM_MYSQL)

  /**
   * Database namespace (schema/database name)
   * @see [[https://opentelemetry.io/docs/specs/semconv/attributes-registry/db/#db-namespace]]
   */
  def dbNamespace(database: String): Attribute[String] =
    Attribute(DB_NAMESPACE, database)

  /**
   * Collection (table) name within the database
   * @see [[https://opentelemetry.io/docs/specs/semconv/attributes-registry/db/#db-collection-name]]
   */
  def dbCollectionName(table: String): Attribute[String] =
    Attribute(DB_COLLECTION_NAME, table)

  /**
   * Operation name (SELECT, INSERT, UPDATE, DELETE, etc.)
   * @see [[https://opentelemetry.io/docs/specs/semconv/attributes-registry/db/#db-operation-name]]
   */
  def dbOperationName(operation: String): Attribute[String] =
    Attribute(DB_OPERATION_NAME, operation)

  /**
   * Full database query text
   * @note Should be sanitized to remove sensitive data for non-parameterized queries
   * @see [[https://opentelemetry.io/docs/specs/semconv/attributes-registry/db/#db-query-text]]
   */
  def dbQueryText(query: String): Attribute[String] =
    Attribute(DB_QUERY_TEXT, query)

  /**
   * Low-cardinality query summary for span names and metric grouping
   * @see [[https://opentelemetry.io/docs/specs/semconv/attributes-registry/db/#db-query-summary]]
   */
  def dbQuerySummary(summary: String): Attribute[String] =
    Attribute(DB_QUERY_SUMMARY, summary)

  /**
   * Database response status code
   * @see [[https://opentelemetry.io/docs/specs/semconv/attributes-registry/db/#db-response-status-code]]
   */
  def dbResponseStatusCode(code: String): Attribute[String] =
    Attribute(DB_RESPONSE_STATUS_CODE, code)

  /**
   * Database response status code (numeric)
   */
  def dbResponseStatusCode(code: Int): Attribute[String] =
    Attribute(DB_RESPONSE_STATUS_CODE, code.toString)

  /**
   * Number of rows returned by the operation (opt-in)
   * @see [[https://opentelemetry.io/docs/specs/semconv/attributes-registry/db/#db-response-returned-rows]]
   */
  def dbResponseReturnedRows(rows: Long): Attribute[Long] =
    Attribute(DB_RESPONSE_RETURNED_ROWS, rows)

  /**
   * Error type classification
   * @see [[https://opentelemetry.io/docs/specs/semconv/attributes-registry/error/]]
   */
  def errorType(error: String): Attribute[String] =
    Attribute(ERROR_TYPE, error)

  /**
   * Error type from exception class name
   */
  def errorType(exception: Throwable): Attribute[String] =
    Attribute(ERROR_TYPE, exception.getClass.getName)

  /**
   * Server address (hostname)
   */
  def serverAddress(host: String): Attribute[String] =
    Attribute(SERVER_ADDRESS, host)

  /**
   * Server port number
   */
  def serverPort(port: Int): Attribute[Long] =
    Attribute(SERVER_PORT, port.toLong)

  /**
   * Network peer address
   */
  def networkPeerAddress(address: String): Attribute[String] =
    Attribute(NETWORK_PEER_ADDRESS, address)

  /**
   * Network peer port
   */
  def networkPeerPort(port: Int): Attribute[Long] =
    Attribute(NETWORK_PEER_PORT, port.toLong)

  /**
   * MySQL server version
   */
  def dbMysqlVersion(version: String): Attribute[String] =
    Attribute(DB_MYSQL_VERSION, version)

  /**
   * MySQL thread ID
   */
  def dbMysqlThreadId(threadId: Int): Attribute[Long] =
    Attribute(DB_MYSQL_THREAD_ID, threadId.toLong)

  /**
   * MySQL authentication plugin
   */
  def dbMysqlAuthPlugin(plugin: String): Attribute[String] =
    Attribute(DB_MYSQL_AUTH_PLUGIN, plugin)

  /**
   * Batch operation size (only set when size >= 2)
   * Per OpenTelemetry spec: db.operation.batch.size should NOT be set to 1
   *
   * @see [[https://opentelemetry.io/docs/specs/semconv/attributes-registry/db/#db-operation-batch-size]]
   */
  def dbOperationBatchSize(size: Int): Option[Attribute[Long]] =
    if size >= 2 then Some(Attribute(DB_OPERATION_BATCH_SIZE, size.toLong))
    else None

  /**
   * Stored procedure name
   */
  def dbStoredProcedureName(name: String): Attribute[String] =
    Attribute(DB_STORED_PROCEDURE_NAME, name)

  /**
   * Operation parameter (opt-in, Development status)
   *
   * A database operation parameter, with `<key>` being the parameter name,
   * and the attribute value being a string representation of the parameter value.
   *
   * @note Use this for higher-level API operation parameters.
   *       For SQL query bind variables, use `dbQueryParameter` instead.
   * @param key Parameter name
   * @param value Parameter value (will be converted to string)
   * @see [[https://opentelemetry.io/docs/specs/semconv/attributes-registry/db/#db-operation-parameter]]
   */
  def dbOperationParameter[A](key: String, value: A): Attribute[String] =
    Attribute(s"$DB_OPERATION_PARAMETER_PREFIX$key", value.toString)

  /**
   * Query parameter (opt-in, Development status)
   *
   * A query parameter used in `db.query.text`, with `<key>` being the parameter name,
   * and the attribute value being a string representation of the parameter value.
   *
   * @note Use indexed keys (e.g., "0", "1") for positional parameters,
   *       or named keys matching the parameter names in the query.
   * @param key Parameter name or index
   * @param value Parameter value (will be converted to string)
   * @see [[https://opentelemetry.io/docs/specs/semconv/attributes-registry/db/#db-query-parameter]]
   */
  def dbQueryParameter[A](key: String, value: A): Attribute[String] =
    Attribute(s"$DB_QUERY_PARAMETER_PREFIX$key", value.toString)

  /**
   * Connection pool name
   */
  def dbClientConnectionPoolName(name: String): Attribute[String] =
    Attribute(DB_CLIENT_CONNECTION_POOL_NAME, name)

  /**
   * Connection state (idle or used)
   */
  def dbClientConnectionState(state: String): Attribute[String] =
    Attribute(DB_CLIENT_CONNECTION_STATE, state)

  /**
   * Schema URL attribute for semantic convention version tracking.
   * @see [[https://opentelemetry.io/docs/concepts/instrumentation/libraries/]]
   */
  def schemaUrl: Attribute[String] =
    Attribute(OTEL_SCHEMA_URL, SCHEMA_URL_VALUE)

  // ============================================================
  // Legacy compatibility (deprecated)
  // ============================================================

  /**
   * Batch size for batch operations.
   * Per OpenTelemetry spec: db.operation.batch.size should NOT be set to 1.
   * Operations are only considered batches when they contain two or more operations.
   *
   * @param size The number of operations in the batch
   * @return List of attributes for batch operations (empty if size < 2)
   */
  @deprecated("Use dbOperationBatchSize instead", "0.7.0")
  def batchSize(size: Long): List[Attribute[?]] =
    if size >= 2 then
      List(
        dbOperationName(SqlOperation.BATCH),
        Attribute(DB_OPERATION_BATCH_SIZE, size)
      )
    else List.empty
