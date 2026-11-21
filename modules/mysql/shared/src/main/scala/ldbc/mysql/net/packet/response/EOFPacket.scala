/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net.packet
package response

import scodec.*
import scodec.bits.*

import ldbc.sql.Attribute

import ldbc.mysql.data.{ CapabilitiesFlags, ServerStatusFlags }

/**
 * The end of a result set, as the server reports it.
 *
 * Two different packets end a result set and both are represented here. Which one arrives depends on
 * `CLIENT_DEPRECATE_EOF`: without it the server sends an EOF packet, with it an OK packet whose
 * header byte is `0xFE` instead of the usual `0x00`. The two carry the same two pieces of
 * information — how many warnings the statement raised and what the session status is now — behind
 * different layouts, so callers are given the information rather than the layout.
 *
 * @param statusFlags
 *   the server status as of this statement
 * @param warnings
 *   the number of warnings, when the connection negotiated `CLIENT_PROTOCOL_41`
 */
case class EOFPacket(
  statusFlags: Set[ServerStatusFlags],
  warnings:    Option[Int]
) extends GenericResponsePackets:

  /**
   * Attribute for EOFException
   */
  def attribute: Attribute[String] = Attribute("error.type", "EOFException")

  override def toString: String = "EOF_Packet"

object EOFPacket:

  val STATUS = 0xfe

  /**
   * The body of an EOF packet once its header byte is gone: two bytes of warning count and two of
   * status flags.
   */
  private val EOF_BODY_SIZE = 4L

  /**
   * Decoder of the packet that ends a result set.
   *
   * The caller has already consumed the `0xFE` header byte, so what arrives here is the body. Its
   * size is what says which of the two layouts it is: an EOF packet's body is exactly four bytes,
   * while the OK packet that replaces it under `CLIENT_DEPRECATE_EOF` is longer because it opens
   * with two length-encoded integers. Reading one as the other would take the status flags from
   * wherever `affected_rows` happens to sit.
   *
   * MySQL Connector/J splits on the same boundary, in `NativeProtocol.readServerStatusForResultSets`
   * via `NativePacketPayload.isEOFPacket` (`0xFE` and no more than five bytes including the header).
   *
   * The longer layout is handed to [[OKPacket.decoder]] rather than parsed again here: it is the
   * same packet, and the length-encoded prefixes it has to step over are already handled there.
   *
   * @param capabilityFlags
   *   Values for the capabilities flag bitmask used by the MySQL protocol.
   */
  def decoder(capabilityFlags: Set[CapabilitiesFlags]): Decoder[EOFPacket] =
    (bits: BitVector) =>
      if !capabilityFlags.contains(CapabilitiesFlags.CLIENT_PROTOCOL_41) then
        Attempt.successful(DecodeResult(EOFPacket(Set.empty, None), BitVector.empty))
      else if bits.size / 8 == EOF_BODY_SIZE then
        val bytes       = bits.toByteArray
        val warnings    = (bytes(0) & 0xff) | ((bytes(1) & 0xff) << 8)
        val statusFlags = (bytes(2) & 0xff) | ((bytes(3) & 0xff) << 8)
        Attempt.successful(
          DecodeResult(EOFPacket(ServerStatusFlags(statusFlags.toLong), Some(warnings)), BitVector.empty)
        )
      else if bits.size / 8 > EOF_BODY_SIZE then
        OKPacket.decoder(capabilityFlags).decode(bits).map(_.map(ok => EOFPacket(ok.statusFlags, ok.warnings)))
      else
        Attempt.failure(
          Err(
            s"An end-of-result-set packet with CLIENT_PROTOCOL_41 carries four bytes after its header, " +
              s"but this one carries ${ bits.size / 8 }."
          )
        )
