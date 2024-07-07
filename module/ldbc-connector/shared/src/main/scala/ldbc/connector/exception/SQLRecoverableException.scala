/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

/**
 * The subclass of {@link SQLException} thrown in situations where a
 * previously failed operation might be able to succeed if the application performs
 *  some recovery steps and retries the entire transaction or in the case of a
 * distributed transaction, the transaction branch.  At a minimum,
 * the recovery operation must include closing the current connection and getting
 * a new connection.
 */
class SQLRecoverableException(
  message:    String,
  sqlState:   Option[String] = None,
  vendorCode: Option[Int]    = None,
  sql:        Option[String] = None,
  detail:     Option[String] = None,
  hint:       Option[String] = None
) extends SQLException(message, sqlState, vendorCode, sql, detail, hint)
