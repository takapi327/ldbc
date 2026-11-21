/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net.packet

import scodec.*
import scodec.bits.BitVector
import scodec.codecs.*

import ldbc.mysql.data.CapabilitiesFlags

package object response:

  /**
   * The payload size at or above which a `0xFE` marker belongs to a row rather than to the end of a
   * result set.
   *
   * The marker on its own does not say which it is. `0xFE` ends a result set — as an end-of-file
   * packet, or as the OK packet that replaces it when `CLIENT_DEPRECATE_EOF` is negotiated — and it
   * also introduces a length-encoded integer in its eight-byte form, which is how a row states the
   * length of a column value of 16MB or more. Only the payload size separates them: either
   * terminator is a handful of bytes, while a row that opens with the eight-byte form carries a value
   * of at least 16,777,216 and so cannot be shorter than this. MySQL Connector/J draws the line in
   * the same place (`NativePacketPayload.isEOFPacket` and `isResultSetOKPacket`).
   */
  private val ROW_MARKER_MIN_PAYLOAD_SIZE = 16777215L

  /**
   * Decoder for a text protocol result set row.
   *
   * Reads the first byte to determine packet type:
   *   - EOFPacket.STATUS (0xFE) in a payload short enough to be a terminator: delegates to EOFPacket decoder
   *   - ERRPacket.STATUS (0xFF): delegates to ERRPacket decoder
   *   - anything else: stores the entire raw bytes as a TextImpl row
   *
   * Taking `0xFE` as the end of the result set regardless of length would end it at the first row
   * whose column value reaches 16MB, reporting no rows at all and leaving that row's bytes to be read
   * as the reply to whatever is asked next.
   *
   * @param capabilityFlags the server capability flags used for EOF/ERR decoding
   */
  def textResultSetRowDecoder(
    capabilityFlags: Set[CapabilitiesFlags]
  ): Decoder[ResultSetRowPacket | EOFPacket | ERRPacket] =
    (bits: BitVector) =>
      val firstByte = bits.getByte(0) & 0xff
      firstByte match
        case EOFPacket.STATUS if bits.size / 8 < ROW_MARKER_MIN_PAYLOAD_SIZE =>
          EOFPacket.decoder(capabilityFlags).decode(bits.drop(8))
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
