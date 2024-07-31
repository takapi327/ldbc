/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import scala.annotation.switch

import scodec.*
import scodec.codecs.*

import ldbc.connector.data.CapabilitiesFlags

/**
 * A generic response packet that can be either an OK packet, an EOF packet, or an ERR packet.
 *
 * For most commands the client sends to the server, the server returns one of these packets in response:
 * - An [[OKPacket]] indicates that the command was successful.
 * - An [[EOFPacket]] indicates that the command was successful, but that the server has no more data to send in response to the command.
 * - An [[ERRPacket]] indicates that the command was unsuccessful.
 */
trait GenericResponsePackets extends ResponsePacket

object GenericResponsePackets:

  def decoder(capabilityFlags: Set[CapabilitiesFlags]): Decoder[GenericResponsePackets] =
    uint8L.flatMap { status =>
      (status: @switch) match
        case OKPacket.STATUS  => OKPacket.decoder(capabilityFlags)
        case EOFPacket.STATUS => EOFPacket.decoder(capabilityFlags)
        case ERRPacket.STATUS => ERRPacket.decoder(capabilityFlags)
    }
