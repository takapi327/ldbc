/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

case class UnknownPacketException(
  message:          String,
  detail:           Option[String] = None
) extends SQLException(message = message, detail = detail)
