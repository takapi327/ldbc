/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.telemetry

import org.typelevel.otel4s.Attribute

object TelemetryAttribute:

  /**
   * OpenTelemetry semantic conventions for MySQL client operations.
   * Based on: https://opentelemetry.io/docs/specs/semconv/database/mysql/
   */
  val DB_OPERATION:     String = "db.operation.name"
  val DB_NAMESPACE:     String = "db.namespace"
  val DB_COLLECTION:    String = "db.collection.name"
  val DB_QUERY:         String = "db.query.text"
  val DB_QUERY_SUMMARY: String = "db.query.summary"
  val DB_SYSTEM:        String = "db.system.name"
  val SERVER_ADDRESS:   String = "server.address"
  val SERVER_PORT:      String = "server.port"
  val VERSION:          String = "db.mysql.version"
  val THREAD_ID:        String = "db.mysql.thread_id"
  val AUTH_PLUGIN:      String = "db.mysql.auth_plugin"
  val BATCH_SIZE:       String = "db.operation.batch.size"
  val STORED_PROCEDURE: String = "db.stored_procedure.name"
  val ERROR_TYPE:       String = "error.type"
  val STATUS_CODE:      String = "db.response.status_code"

  /**
   * Most common operation values.
   */
  val DB_SYSTEM_DEFAULT:  String = "mysql"
  val OPERATION_BATCH:    String = "(SQL batch)"
  val OPERATION_COMMIT:   String = "COMMIT"
  val OPERATION_CREATE:   String = "CREATE"
  val OPERATION_EXPLAIN:  String = "EXPLAIN"
  val OPERATION_INIT_DB:  String = "INIT_DB"
  val OPERATION_KILL:     String = "KILL"
  val OPERATION_PING:     String = "PING"
  val OPERATION_ROLLBACK: String = "ROLLBACK"
  val OPERATION_SELECT:   String = "SELECT"
  val OPERATION_SET:      String = "SET"
  val OPERATION_SHOW:     String = "SHOW"
  val OPERATION_SHUTDOWN: String = "SHUTDOWN"
  val OPERATION_USE:      String = "USE"

  /**
   * Database system name (required to be "mysql")
   */
  val dbSystemName: Attribute[String] = Attribute(DB_SYSTEM, DB_SYSTEM_DEFAULT)

  /**
   * The name of the database (schema) being accessed
   */
  def dbNamespace(database: String): Attribute[String] =
    Attribute(DB_NAMESPACE, database)

  /**
   * The name of the primary table/collection
   */
  def dbCollectionName(table: String): Attribute[String] =
    Attribute(DB_COLLECTION, table)

  /**
   * The name of the operation being executed
   */
  def dbOperationName(operation: String): Attribute[String] =
    Attribute(DB_OPERATION, operation)

  /**
   * The database query text
   */
  def dbQueryText(query: String): Attribute[String] =
    Attribute(DB_QUERY, query)

  /**
   * Low cardinality representation of the query
   */
  def dbQuerySummary(summary: String): Attribute[String] =
    Attribute(DB_QUERY_SUMMARY, summary)

  /**
   * Database server address
   */
  def serverAddress(host: String): Attribute[String] =
    Attribute(SERVER_ADDRESS, host)

  /**
   * Database server port
   */
  def serverPort(port: Int): Attribute[Long] =
    Attribute(SERVER_PORT, port.toLong)

  /**
   * MySQL server version
   */
  def dbMysqlVersion(version: String): Attribute[String] =
    Attribute(VERSION, version)

  /**
   * MySQL thread ID
   */
  def dbMysqlThreadId(threadId: Int): Attribute[Long] =
    Attribute(THREAD_ID, threadId.toLong)

  /**
   * MySQL authentication plugin
   */
  def dbMysqlAuthPlugin(plugin: String): Attribute[String] =
    Attribute(AUTH_PLUGIN, plugin)

  /**
   * Batch size for batch operations
   * Operations are only considered batches when they contain two or more operations
   */
  def batchSize(size: Long): List[Attribute[?]] =
    if size >= 2 then
      List(
        dbOperationName("BATCH"),
        Attribute(BATCH_SIZE, size)
      )
    else List(dbOperationName("BATCH"))

  /**
   * Stored procedure name attribute
   */
  def dbStoredProcedureName(name: String): Attribute[String] =
    Attribute(STORED_PROCEDURE, name)
