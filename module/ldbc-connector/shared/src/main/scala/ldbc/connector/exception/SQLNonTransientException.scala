/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

/**
 * The subclass of {@link SQLException} thrown when an instance where a retry
 * of the same operation would fail unless the cause of the <code>SQLException</code>
 * is corrected.
 */
class SQLNonTransientException(
  message:          String,
  sqlState:         Option[String] = None,
  vendorCode:       Option[Int]    = None,
  sql:              Option[String] = None,
  detail:           Option[String] = None,
  hint:             Option[String] = None,
  originatedPacket: Option[String] = None
) extends SQLException(message, sqlState, vendorCode, sql, detail, hint, originatedPacket)
