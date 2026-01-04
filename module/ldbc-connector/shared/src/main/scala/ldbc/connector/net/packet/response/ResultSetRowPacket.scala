/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import scodec.*
import scodec.bits.BitVector

import ldbc.connector.data.*

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
  private def decodeResultSetRow(
    fieldLength:       Int,
    columnCharsets: Vector[String]
  ): Decoder[ResultSetRowPacket] =
    (bits: BitVector) =>
      val bytes        = bits.toByteArray
      val columnLength = columnCharsets.length
      val buffer       = new Array[Option[String]](columnLength)
      var offset       = 0
      var index        = 0

      while index < columnLength do {
        val charset = columnCharsets(index)
        if fieldLength == NULL && index == 0 then buffer(index) = None
        else if index == 0 then
          buffer(index) = Some(new String(bytes, offset, fieldLength, charset))
          offset += fieldLength
        else
          val length = bytes(offset) & 0xff
          offset += 1

          if length == NULL then buffer(index) = None
          else if length <= 251 then
            buffer(index) = Some(new String(bytes, offset, length, charset))
            offset += length
          else
            val actualLength = length match
              case 252 => (bytes(offset) & 0xff) | ((bytes(offset + 1) & 0xff) << 8)
              case 253 =>
                (bytes(offset) & 0xff) |
                  ((bytes(offset + 1) & 0xff) << 8) |
                  ((bytes(offset + 2) & 0xff) << 16)
              case _ =>
                (bytes(offset) & 0xff) |
                  ((bytes(offset + 1) & 0xff) << 8) |
                  ((bytes(offset + 2) & 0xff) << 16) |
                  ((bytes(offset + 3) & 0xff) << 24)

            val headerSize = if length == 252 then 2 else if length == 253 then 3 else 4
            offset += headerSize
            buffer(index) = Some(new String(bytes, offset, actualLength, charset))
            offset += actualLength

        index += 1
      }

      Attempt.Successful(DecodeResult(ResultSetRowPacket(buffer), bits))

  def decoder(
    capabilityFlags:   Set[CapabilitiesFlags],
    columnDefinitions: Vector[ColumnDefinitionPacket]
  ): Decoder[ResultSetRowPacket | EOFPacket | ERRPacket] =
    (bits: BitVector) =>
      val (statusBits, postLengthBits) = bits.splitAt(8)
      val status                       = statusBits.toInt(false)
      status match
        case EOFPacket.STATUS => EOFPacket.decoder(capabilityFlags).decode(postLengthBits)
        case ERRPacket.STATUS => ERRPacket.decoder(capabilityFlags).decode(postLengthBits)
        case fieldLength      =>
          val columnCharsets = columnDefinitions.map {
            case _: ColumnDefinition320Packet => "UTF-8"
            case column: ColumnDefinition41Packet => CharsetMapping.getJavaCharsetFromCollationIndex(column.characterSet)
          }
          decodeResultSetRow(fieldLength, columnCharsets).decode(postLengthBits)
