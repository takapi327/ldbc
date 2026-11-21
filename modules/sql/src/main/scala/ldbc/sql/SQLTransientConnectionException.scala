/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

/**
 * The subclass of [[SQLException]] thrown when a connection operation that failed might succeed if
 * retried, for example a transient network or connection-pool error (SQLSTATE class `08`).
 */
class SQLTransientConnectionException(
  message:    String,
  sqlState:   Option[String]    = None,
  vendorCode: Option[Int]       = None,
  sql:        Option[String]    = None,
  detail:     Option[String]    = None,
  hint:       Option[String]    = None,
  vendor:     String            = "SQL",
  cause:      Option[Throwable] = None
) extends SQLTransientException(message, sqlState, vendorCode, sql, detail, hint, vendor, cause)
