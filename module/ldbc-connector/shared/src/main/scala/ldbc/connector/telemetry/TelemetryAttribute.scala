/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.telemetry

import org.typelevel.otel4s.semconv.attributes.DbAttributes
import org.typelevel.otel4s.Attribute

/**
 * ldbc-specific telemetry attributes.
 *
 * Standard OpenTelemetry semantic convention attributes are available via:
 * - `org.typelevel.otel4s.semconv.attributes.DbAttributes`
 * - `org.typelevel.otel4s.semconv.attributes.ServerAttributes`
 * - `org.typelevel.otel4s.semconv.attributes.ErrorAttributes`
 * - `org.typelevel.otel4s.semconv.attributes.NetworkAttributes`
 * - `org.typelevel.otel4s.semconv.experimental.attributes.DbExperimentalAttributes`
 *
 * @see [[https://opentelemetry.io/docs/specs/semconv/db/ Database Semantic Conventions]]
 */
object TelemetryAttribute:

  // ============================================================
  // MySQL-specific attribute keys (not in OTel semantic conventions)
  // ============================================================

  val DB_MYSQL_VERSION:     String = "db.mysql.version"
  val DB_MYSQL_THREAD_ID:   String = "db.mysql.thread_id"
  val DB_MYSQL_AUTH_PLUGIN: String = "db.mysql.auth_plugin"

  // Prefixed parameter attributes (non-standard parameterized attrs)
  val DB_OPERATION_PARAMETER_PREFIX: String = "db.operation.parameter."
  val DB_QUERY_PARAMETER_PREFIX:     String = "db.query.parameter."

  // Schema URL for semantic convention version tracking
  val OTEL_SCHEMA_URL:  String = "otel.schema_url"
  val SCHEMA_URL_VALUE: String = "https://opentelemetry.io/schemas/1.39.0"

  // ============================================================
  // SQL operation name constants
  // ============================================================

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
  // MySQL-specific attribute factory methods
  // ============================================================

  def dbMysqlVersion(version: String): Attribute[String] =
    Attribute(DB_MYSQL_VERSION, version)

  def dbMysqlThreadId(threadId: Int): Attribute[Long] =
    Attribute(DB_MYSQL_THREAD_ID, threadId.toLong)

  def dbMysqlAuthPlugin(plugin: String): Attribute[String] =
    Attribute(DB_MYSQL_AUTH_PLUGIN, plugin)

  // ============================================================
  // Helper methods wrapping semconv attributes with ldbc-specific logic
  // ============================================================

  /**
   * Batch operation size (only set when size >= 2)
   * Per OpenTelemetry spec: db.operation.batch.size should NOT be set to 1
   *
   * @see [[https://opentelemetry.io/docs/specs/semconv/attributes-registry/db/#db-operation-batch-size]]
   */
  def dbOperationBatchSize(size: Int): Option[Attribute[Long]] =
    if size >= 2 then Some(DbAttributes.DbOperationBatchSize(size.toLong))
    else None

  /**
   * Operation parameter (opt-in, Development status)
   *
   * @see [[https://opentelemetry.io/docs/specs/semconv/attributes-registry/db/#db-operation-parameter]]
   */
  def dbOperationParameter[A](key: String, value: A): Attribute[String] =
    Attribute(s"$DB_OPERATION_PARAMETER_PREFIX$key", value.toString)

  /**
   * Query parameter (opt-in, Development status)
   *
   * @see [[https://opentelemetry.io/docs/specs/semconv/attributes-registry/db/#db-query-parameter]]
   */
  def dbQueryParameter[A](key: String, value: A): Attribute[String] =
    Attribute(s"$DB_QUERY_PARAMETER_PREFIX$key", value.toString)

  /**
   * Schema URL attribute for semantic convention version tracking.
   */
  def schemaUrl: Attribute[String] =
    Attribute(OTEL_SCHEMA_URL, SCHEMA_URL_VALUE)
