/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

/**
 * The subclass of {@link SQLException} is thrown in situations where a
 * previously failed operation might be able to succeed when the operation is
 * retried without any intervention by application-level functionality.
 */
class SQLTransientException(
  sqlState:         String,
  vendorCode:       Int,
  message:          String,
  sql:              Option[String] = None,
  detail:           Option[String] = None,
  hint:             Option[String] = None,
  originatedPacket: Option[String] = None
) extends SQLException(sqlState, vendorCode, message, sql, detail, hint, originatedPacket)
