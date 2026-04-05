/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scodec.*
import scodec.bits.BitVector
import scodec.codecs.*

import ldbc.connector.data.CapabilitiesFlags

package object response:

  /**
   * Decoder for a text protocol result set row.
   *
   * Reads the first byte to determine packet type:
   *   - EOFPacket.STATUS (0xFE): delegates to EOFPacket decoder
   *   - ERRPacket.STATUS (0xFF): delegates to ERRPacket decoder
   *   - anything else: stores the entire raw bytes as a TextImpl row
   *
   * @param capabilityFlags the server capability flags used for EOF/ERR decoding
   */
  def textResultSetRowDecoder(
    capabilityFlags: Set[CapabilitiesFlags]
  ): Decoder[ResultSetRowPacket | EOFPacket | ERRPacket] =
    (bits: BitVector) =>
      val firstByte = bits.getByte(0) & 0xff
      firstByte match
        case EOFPacket.STATUS => EOFPacket.decoder(capabilityFlags).decode(bits.drop(8))
        case ERRPacket.STATUS => ERRPacket.decoder(capabilityFlags).decode(bits.drop(8))
        case _                =>
          Attempt.Successful(DecodeResult(ResultSetRowPacket.TextImpl(bits.toByteArray), BitVector.empty))

  /**
   * Decoder for a binary protocol result set row.
   *
   * Consumes the leading status byte (0x00 = OK) to determine packet type:
   *   - EOFPacket.STATUS (0xFE): delegates to EOFPacket decoder
   *   - ERRPacket.STATUS (0xFF): delegates to ERRPacket decoder
   *   - OKPacket.STATUS  (0x00): stores remaining bytes (null bitmap + field data) as a BinaryProtocolResultSetRowPacket
   *
   * @param capabilityFlags the server capability flags used for EOF/ERR decoding
   */
  def binaryResultSetRowDecoder(
    capabilityFlags: Set[CapabilitiesFlags]
  ): Decoder[ResultSetRowPacket | EOFPacket | ERRPacket] =
    uint8L.flatMap {
      case EOFPacket.STATUS => EOFPacket.decoder(capabilityFlags)
      case ERRPacket.STATUS => ERRPacket.decoder(capabilityFlags)
      case OKPacket.STATUS  =>
        (bits: BitVector) =>
          Attempt.Successful(DecodeResult(BinaryProtocolResultSetRowPacket(bits.toByteArray), BitVector.empty))
    }
