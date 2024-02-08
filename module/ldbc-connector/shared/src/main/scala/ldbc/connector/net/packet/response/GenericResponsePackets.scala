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

trait GenericResponsePackets extends ResponsePacket

object GenericResponsePackets:

  val decoder: Decoder[GenericResponsePackets] =
    int8.flatMap { status =>
      (status: @switch) match
        case OKPacket.STATUS  => OKPacket.decoder
        case EOFPacket.STATUS => EOFPacket.decoder
        case ERRPacket.STATUS => ERRPacket.decoder
    }
