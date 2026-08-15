/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

/**
 * The subclass of [[SQLException]] thrown when the driver does not support an optional JDBC feature
 * that the application requested (SQLSTATE class `0A`).
 */
class SQLFeatureNotSupportedException(
  message:    String,
  sqlState:   Option[String]    = None,
  vendorCode: Option[Int]       = None,
  sql:        Option[String]    = None,
  detail:     Option[String]    = None,
  hint:       Option[String]    = None,
  vendor: String = "SQL",
  cause:      Option[Throwable] = None
) extends SQLNonTransientException(message, sqlState, vendorCode, sql, detail, hint, vendor, cause)
