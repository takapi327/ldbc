/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import ldbc.connector.exception.UnknownPacketException

/**
 * Represents a response packet that is not recognized by the client.
 *
 * @param status
 *   the status code of the packet
 * @param detail
 *   the detail message of the packet
 * @param originatedPacket
 *   the packet that originated the unknown packet
 */
case class UnknownPacket(
  status:           Int,
  detail:           Option[String] = None,
) extends ResponsePacket:
  override def toString: String = "Unknown Packet"

  def toException(message: String): UnknownPacketException =
    UnknownPacketException(
      message          = message,
      detail           = detail,
    )
