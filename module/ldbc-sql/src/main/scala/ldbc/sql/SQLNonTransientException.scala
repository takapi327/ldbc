/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

/**
 * The subclass of [[SQLException]] thrown when a retry of the same operation would fail unless the
 * cause of the exception is corrected.
 */
class SQLNonTransientException(
  message:    String,
  sqlState:   Option[String]    = None,
  vendorCode: Option[Int]       = None,
  sql:        Option[String]    = None,
  detail:     Option[String]    = None,
  hint:       Option[String]    = None,
  vendor: String = "SQL",
  cause:      Option[Throwable] = None
) extends SQLException(message, sqlState, vendorCode, sql, detail, hint, vendor, cause)
