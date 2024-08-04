/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import scodec.*
import scodec.codecs.*

import cats.syntax.option.*

import ldbc.connector.data.CapabilitiesFlags

/**
 * If CLIENT_PROTOCOL_41 is enabled, the EOF packet contains a warning count and status flags.
 * 
 * The EOF_Packet packet may appear in places where a Protocol::LengthEncodedInteger may appear. You must check whether the packet length is less than 9 to make sure that it is a EOF_Packet packet.
 *
 * @param status
 *   Type: int<1>
 *   Name: header
 *   Description: 0xFE EOF packet header
 * @param warnings
 *   Type: int<2>
 *   Name: warnings
 *   Description: number of warnings
 * @param statusFlags
 *   Type: int<2>
 *   Name: status_flags
 *   Description: SERVER_STATUS_flags_enum
 */
case class EOFPacket(
  status:      Int,
  warnings:    Int,
  statusFlags: Int
) extends GenericResponsePackets:

  override def toString: String = "EOF_Packet"

object EOFPacket:

  val STATUS = 0xfe

  def decoder(capabilityFlags: Set[CapabilitiesFlags]): Decoder[EOFPacket] =
    val hasClientProtocol41Flag = capabilityFlags.contains(CapabilitiesFlags.CLIENT_PROTOCOL_41)
    uint4.flatMap { status =>
      if hasClientProtocol41Flag then
        for
          warnings    <- uint4
          statusFlags <- uint4
        yield EOFPacket(status, warnings, statusFlags)
      else
        Decoder.pure(EOFPacket(status, 0, 0))
        //provide(EOFPacket(status, None, None))
    }
    //for
    //  status      <- uint4
    //  warnings    <- if hasClientProtocol41Flag then uint4.map(_.some) else provide(None)
    //  statusFlags <- if hasClientProtocol41Flag then uint4.map(_.some) else provide(None)
    //yield EOFPacket(status, warnings, statusFlags)
