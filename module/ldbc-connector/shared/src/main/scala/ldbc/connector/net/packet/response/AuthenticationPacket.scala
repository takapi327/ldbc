/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import scodec.*
import scodec.codecs.*

import ldbc.connector.data.CapabilitiesFlags
import ldbc.connector.exception.MySQLException

trait AuthenticationPacket extends ResponsePacket

object AuthenticationPacket:

  def decoder(capabilityFlags: Seq[CapabilitiesFlags]): Decoder[AuthenticationPacket | GenericResponsePackets] =
    int8.flatMap {
      case OKPacket.STATUS           => OKPacket.decoder(capabilityFlags)
      case ERRPacket.STATUS          => ERRPacket.decoder(capabilityFlags)
      case AuthMoreDataPacket.STATUS => AuthMoreDataPacket.decoder
      case unknown                   => throw new MySQLException(None, "Error during database operation", Some(s"Unknown status: $unknown"))
    }
