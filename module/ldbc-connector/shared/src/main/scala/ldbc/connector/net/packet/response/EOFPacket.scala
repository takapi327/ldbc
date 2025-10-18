/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import scodec.*
import scodec.bits.*

import org.typelevel.otel4s.Attribute

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

  /**
   * Attribute for EOFException
   */
  def attribute: Attribute[String] = Attribute("error.type", "EOFException")

  override def toString: String = "EOF_Packet"

object EOFPacket:

  val STATUS = 0xfe

  /**
   * Decoder of EOF
   *
   * A foolproof implementation using splitAt is faster than the helper functions provided by scodec.
   *
   * @param capabilityFlags
   *   Values for the capabilities flag bitmask used by the MySQL protocol.
   */
  def decoder(capabilityFlags: Set[CapabilitiesFlags]): Decoder[EOFPacket] =
    (bits: BitVector) =>
      val hasClientProtocol41Flag      = capabilityFlags.contains(CapabilitiesFlags.CLIENT_PROTOCOL_41)
      val (statusBits, postStatusBits) = bits.splitAt(4)
      val status                       = statusBits.toInt(false)
      val packet                       = if hasClientProtocol41Flag then
        val (warningsBits, postWarningsBits) = postStatusBits.splitAt(4)
        val statusFlagsBits                  = postWarningsBits.take(4)
        val warnings                         = warningsBits.toInt(false)
        val statusFlags                      = statusFlagsBits.toInt(false)
        EOFPacket(status, warnings, statusFlags)
      else EOFPacket(status, 0, 0)
      Attempt.successful(DecodeResult(packet, bits))
