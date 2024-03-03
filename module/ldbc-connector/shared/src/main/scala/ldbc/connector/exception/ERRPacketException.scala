/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

case class ERRPacketException(
  message: String,
  sql:     Option[String] = None,
  detail:  Option[String] = None,
  hint:    Option[String] = None
) extends MySQLException(message, sql, detail, hint)
