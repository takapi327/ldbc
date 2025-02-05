/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import scodec.*
import scodec.bits.BitVector

import ldbc.connector.data.CapabilitiesFlags

/**
 * AuthenticationPacket is a trait for all authentication packets.
 */
trait AuthenticationPacket extends ResponsePacket

object AuthenticationPacket:

  def decoder(
    capabilityFlags: Set[CapabilitiesFlags]
  ): Decoder[AuthenticationPacket | GenericResponsePackets | UnknownPacket] =
    (bits: BitVector) =>
      var remainder  = bits
      val statusBits = remainder.take(8)
      remainder = remainder.drop(8)
      val status = statusBits.toInt(signed = false)
      status match
        case OKPacket.STATUS                => OKPacket.decoder(capabilityFlags).decode(remainder)
        case ERRPacket.STATUS               => ERRPacket.decoder(capabilityFlags).decode(remainder)
        case AuthMoreDataPacket.STATUS      => AuthMoreDataPacket.decoder.decode(remainder)
        case AuthSwitchRequestPacket.STATUS => AuthSwitchRequestPacket.decoder.decode(remainder)
        case unknown => Decoder.pure(UnknownPacket(unknown, Some(s"Unknown status: $unknown"))).decode(remainder)
