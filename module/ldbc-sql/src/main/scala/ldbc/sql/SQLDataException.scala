/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

/**
 * The subclass of [[SQLException]] thrown for data errors such as an invalid conversion, a value out
 * of range, or a value not permitted for a column (SQLSTATE class `22`).
 */
class SQLDataException(
  message:    String,
  sqlState:   Option[String]    = None,
  vendorCode: Option[Int]       = None,
  sql:        Option[String]    = None,
  detail:     Option[String]    = None,
  hint:       Option[String]    = None,
  vendor: String = "SQL",
  cause:      Option[Throwable] = None
) extends SQLNonTransientException(message, sqlState, vendorCode, sql, detail, hint, vendor, cause)
