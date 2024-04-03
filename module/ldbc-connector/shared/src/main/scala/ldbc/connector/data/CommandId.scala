/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

object CommandId:

  val COM_QUIT             = 0x01
  val COM_INIT_DB          = 0x02
  val COM_QUERY            = 0x03
  val COM_STATISTICS       = 0x09
  val COM_PING             = 0x0e
  val COM_STMT_PREPARE     = 0x16
  val COM_STMT_EXECUTE     = 0x17
  val COM_STMT_CLOSE       = 0x19
  val COM_SET_OPTION       = 0x1b
  val COM_RESET_CONNECTION = 0x1f
