/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.telemetry

import ldbc.sql.Attribute

/**
 * MySQL-specific telemetry attribute keys and factories.
 *
 * These are not part of the OpenTelemetry database semantic conventions (which the DB-agnostic
 * `ldbc.telemetry.TelemetryAttribute` carries); they describe MySQL-only server metadata, so they live with
 * the MySQL driver rather than in the shared telemetry SPI.
 */
object MysqlTelemetryAttribute:

  val DB_MYSQL_VERSION:     String = "db.mysql.version"
  val DB_MYSQL_THREAD_ID:   String = "db.mysql.thread_id"
  val DB_MYSQL_AUTH_PLUGIN: String = "db.mysql.auth_plugin"

  def dbMysqlVersion(version: String): Attribute[String] =
    Attribute(DB_MYSQL_VERSION, version)

  def dbMysqlThreadId(threadId: Int): Attribute[Long] =
    Attribute(DB_MYSQL_THREAD_ID, threadId.toLong)

  def dbMysqlAuthPlugin(plugin: String): Attribute[String] =
    Attribute(DB_MYSQL_AUTH_PLUGIN, plugin)
