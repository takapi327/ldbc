/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.util

import org.typelevel.otel4s.Attribute

/**
 * OpenTelemetry semantic conventions for MySQL client operations.
 * Based on: https://opentelemetry.io/docs/specs/semconv/database/mysql/
 */
object OpenTelemetryAttributes:

  /**
   * Database system name (required to be "mysql")
   */
  val dbSystemName: Attribute[String] = Attribute("db.system.name", "mysql")

  /**
   * The name of the database (schema) being accessed
   */
  def dbNamespace(database: String): Attribute[String] =
    Attribute("db.namespace", database)

  /**
   * The name of the primary table/collection
   */
  def dbCollectionName(table: String): Attribute[String] =
    Attribute("db.collection.name", table)

  /**
   * The name of the operation being executed
   */
  def dbOperationName(operation: String): Attribute[String] =
    Attribute("db.operation.name", operation)

  /**
   * The database query text
   */
  def dbQueryText(query: String): Attribute[String] =
    Attribute("db.query.text", query)

  /**
   * Low cardinality representation of the query
   */
  def dbQuerySummary(summary: String): Attribute[String] =
    Attribute("db.query.summary", summary)

  /**
   * Database server address
   */
  def serverAddress(host: String): Attribute[String] =
    Attribute("server.address", host)

  /**
   * Database server port
   */
  def serverPort(port: Int): Attribute[Long] =
    Attribute("server.port", port.toLong)

  /**
   * MySQL server version
   */
  def dbMysqlVersion(version: String): Attribute[String] =
    Attribute("db.mysql.version", version)

  /**
   * MySQL thread ID
   */
  def dbMysqlThreadId(threadId: Int): Attribute[Long] =
    Attribute("db.mysql.thread_id", threadId.toLong)

  /**
   * MySQL authentication plugin
   */
  def dbMysqlAuthPlugin(plugin: String): Attribute[String] =
    Attribute("db.mysql.auth_plugin", plugin)

  /**
   * Statement type (custom attribute)
   */
  def statementType(sType: String): Attribute[String] =
    Attribute("db.statement.type", sType)

  /**
   * Batch size for batch operations
   * Operations are only considered batches when they contain two or more operations
   */
  def batchSize(size: Long): Option[Attribute[Long]] =
    if (size >= 2) Some(Attribute("db.operation.batch.size", size))
    else None

  /**
   * Sanitize SQL query to remove sensitive data
   * This is a simple implementation that replaces string literals and numbers
   */
  def sanitizeSql(sql: String): String =
    sql
      .replaceAll("'[^']*'", "'?'")            // Replace string literals with '?'
      .replaceAll("\"[^\"]*\"", "\"?\"")       // Replace quoted identifiers with "?"
      .replaceAll("\\b\\d+(\\.\\d+)?\\b", "?") // Replace numbers (including decimals) with ?

  /**
   * Stored procedure name attribute
   */
  def dbStoredProcedureName(name: String): Attribute[String] =
    Attribute("db.stored_procedure.name", name)

  /**
   * Attribute for EOFException
   */
  def eofException: Attribute[String] =
    Attribute("error.type", "EOFException")

  /**
   * Extract operation name from SQL statement
   * Note: According to OpenTelemetry spec, db.operation.name SHOULD NOT be extracted from db.query.text
   * This is provided for cases where operation name is not available through other means
   */
  def extractOperationName(sql: String): String =
    val trimmed   = sql.trim.toUpperCase
    val firstWord = trimmed.split("\\s+").headOption.getOrElse("UNKNOWN")
    firstWord match
      case "SELECT"   => "SELECT"
      case "INSERT"   => "INSERT"
      case "UPDATE"   => "UPDATE"
      case "DELETE"   => "DELETE"
      case "CREATE"   => "CREATE"
      case "DROP"     => "DROP"
      case "ALTER"    => "ALTER"
      case "TRUNCATE" => "TRUNCATE"
      case "SHOW"     => "SHOW"
      case "SET"      => "SET"
      case "BEGIN"    => "BEGIN"
      case "COMMIT"   => "COMMIT"
      case "ROLLBACK" => "ROLLBACK"
      case "CALL"     => "CALL"
      case _          => "OTHER"

  /**
   * Extract table name from simple SQL statements
   * Note: According to OpenTelemetry spec, db.collection.name SHOULD NOT be extracted from db.query.text
   * This is provided for cases where collection name is not available through other means
   */
  def extractTableName(sql: String): Option[String] =
    val trimmed = sql.trim.toUpperCase
    val pattern = """(?:FROM|INTO|UPDATE)\s+([^\s,;]+)""".r
    pattern.findFirstMatchIn(trimmed).map(_.group(1))
  
  /**
   * Extract stored procedure name from CALL statements
   */
  def extractStoredProcedureName(sql: String): Option[String] =
    val trimmed = sql.trim.toUpperCase
    val pattern = """CALL\s+([^\s(]+)""".r
    pattern.findFirstMatchIn(trimmed).map(_.group(1))

  /**
   * Create span name based on operation and table
   */
  def createSpanName(operation: String, table: Option[String]): String =
    table match
      case Some(t) => s"$operation $t"
      case None    => operation
