/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

/**
 * The subclass of [[SQLException]] thrown in situations where a previously failed operation might be
 * able to succeed if the application performs some recovery steps and retries the entire transaction.
 */
class SQLRecoverableException(
  message:    String,
  sqlState:   Option[String]    = None,
  vendorCode: Option[Int]       = None,
  sql:        Option[String]    = None,
  detail:     Option[String]    = None,
  hint:       Option[String]    = None,
  vendor:     String            = "SQL",
  cause:      Option[Throwable] = None
) extends SQLException(message, sqlState, vendorCode, sql, detail, hint, vendor, cause)
