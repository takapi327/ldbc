/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.exception

import ldbc.sql.SQLException

/** Thrown when the driver receives a MySQL protocol packet it does not recognise. */
case class UnknownPacketException(
  message:             String,
  override val detail: Option[String] = None
) extends SQLException(message, vendor = "MySQL")
