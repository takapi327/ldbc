/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import java.nio.charset.StandardCharsets.UTF_8

import scodec.*
import scodec.bits.{ BitVector, ByteOrdering }

import ldbc.connector.data.CapabilitiesFlags

/**
 * Represents a row in a result set.
 *
 * @see https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_query_response_text_resultset_row.html
 *
 * A row with data for each column.
 *   - NULL is sent as 0xFB
 *   - everything else is converted to a string and is sent as string<lenenc>
 */
trait ResultSetRowPacket extends ResponsePacket:

  /**
   * The values of the row.
   */
  def values: Array[Option[String]]

  override def toString: String = "ProtocolText::ResultSetRow"

object ResultSetRowPacket:

  private val NULL = 0xfb

  private[ldbc] case class Impl(values: Array[Option[String]]) extends ResultSetRowPacket

  def apply(values: Array[Option[String]]): ResultSetRowPacket = Impl(values)

  /**
   * Decoder of result set acquisition
   *
   * A foolproof implementation using splitAt is faster than the helper functions provided by scodec.
   */
  private def decodeResultSetRow(fieldLength: Int, columnLength: Int): Decoder[ResultSetRowPacket] =
    (bits: BitVector) =>
      val bytes = bits.toByteArray
      val buffer = new Array[Option[String]](columnLength)
      var remainder = bytes
      var index = 0

      while (index < columnLength) {
        if (fieldLength == NULL && index == 0) {
          buffer(index) = None
        } else if (index == 0) {
          val (fieldBits, postFieldBits) = remainder.splitAt(fieldLength)
          buffer(index) = Some(new String(fieldBits, UTF_8))
          remainder = postFieldBits
        } else {
          val length = remainder(0).toInt & 0xFF
          remainder = remainder.drop(1)

          if (length == NULL) {
            buffer(index) = None
          } else if (length <= 251) {
            val (fieldBits, postFieldBits) = remainder.splitAt(length)
            buffer(index) = Some(new String(fieldBits, UTF_8))
            remainder = postFieldBits
          } else {
            val actualLength = length match {
              case 252 => ((remainder(0).toInt & 0xFF) | ((remainder(1).toInt & 0xFF) << 8))
              case 253 => ((remainder(0).toInt & 0xFF) | ((remainder(1).toInt & 0xFF) << 8) | ((remainder(2).toInt & 0xFF) << 16))
              case _ => ((remainder(0).toInt & 0xFF) | ((remainder(1).toInt & 0xFF) << 8) |
                ((remainder(2).toInt & 0xFF) << 16) | ((remainder(3).toInt & 0xFF) << 24))
            }
            val headerSize = if (length == 252) 2 else if (length == 253) 3 else 8
            remainder = remainder.drop(headerSize)
            val (fieldBits, postFieldBits) = remainder.splitAt(actualLength)
            buffer(index) = Some(new String(fieldBits, UTF_8))
            remainder = postFieldBits
          }
        }
        index += 1
      }

      Attempt.Successful(DecodeResult(ResultSetRowPacket(buffer), bits))

  def decoder(
    capabilityFlags: Set[CapabilitiesFlags],
    columnLength:    Int
  ): Decoder[ResultSetRowPacket | EOFPacket | ERRPacket] =
    (bits: BitVector) =>
      val (statusBits, postLengthBits) = bits.splitAt(8)
      val status                       = statusBits.toInt(false)
      status match
        case EOFPacket.STATUS => EOFPacket.decoder(capabilityFlags).decode(postLengthBits)
        case ERRPacket.STATUS => ERRPacket.decoder(capabilityFlags).decode(postLengthBits)
        case fieldLength      => decodeResultSetRow(fieldLength, columnLength).decode(postLengthBits)
