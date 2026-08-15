/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

/**
 * The subclass of [[SQLException]] thrown when the current statement was automatically rolled back by
 * the database, for example on a deadlock or serialization failure (SQLSTATE class `40`).
 */
class SQLTransactionRollbackException(
  message:    String,
  sqlState:   Option[String]    = None,
  vendorCode: Option[Int]       = None,
  sql:        Option[String]    = None,
  detail:     Option[String]    = None,
  hint:       Option[String]    = None,
  vendor: String = "SQL",
  cause:      Option[Throwable] = None
) extends SQLTransientException(message, sqlState, vendorCode, sql, detail, hint, vendor, cause)
