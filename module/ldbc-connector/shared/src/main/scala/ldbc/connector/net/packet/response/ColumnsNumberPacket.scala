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
 * A packet that contains the number of columns in the result set.
 * 
 * @param size
 *   the number of columns in the result set
 */
case class ColumnsNumberPacket(size: Int) extends ResponsePacket:

  override def toString: String = "ColumnsNumber Packet"

object ColumnsNumberPacket:

  def decoder(capabilityFlags: Set[CapabilitiesFlags]): Decoder[ColumnsNumberPacket | OKPacket | ERRPacket] =
    uint8.flatMap { status =>
      (status: @switch) match
        case OKPacket.STATUS  => OKPacket.decoder(capabilityFlags)
        case ERRPacket.STATUS => ERRPacket.decoder(capabilityFlags)
        case value            => Decoder.pure(ColumnsNumberPacket(value))
    }
