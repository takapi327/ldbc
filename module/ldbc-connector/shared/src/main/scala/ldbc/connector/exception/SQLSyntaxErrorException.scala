/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

/**
 * The subclass of {@link SQLException} thrown when the SQLState class value
 * is '<i>42</i>', or under vendor-specified conditions. This indicates that the
 * in-progress query has violated SQL syntax rules.
 */
class SQLSyntaxErrorException(
  message:    String,
  sqlState:   Option[String] = None,
  vendorCode: Option[Int]    = None,
  sql:        Option[String] = None,
  detail:     Option[String] = None,
  hint:       Option[String] = None
) extends SQLNonTransientException(message, sqlState, vendorCode, sql, detail, hint)
