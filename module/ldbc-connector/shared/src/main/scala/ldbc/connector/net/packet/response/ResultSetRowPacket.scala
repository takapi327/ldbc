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

  def apply(_values: Array[Option[String]]): ResultSetRowPacket =
    new ResultSetRowPacket:
      override val values: Array[Option[String]] = _values

  private def decodeToString(remainder: BitVector, size: Int): (BitVector, Option[String]) =
    val (fieldSizeBits, postFieldSize) = remainder.splitAt(size)
    val fieldSizeNumBytes              = fieldSizeBits.toLong(false, ByteOrdering.LittleEndian)
    if fieldSizeNumBytes == NULL then (postFieldSize, None)
    else
      val (fieldBits, postFieldBits) = postFieldSize.splitAt(fieldSizeNumBytes * 8L)
      (postFieldBits, Some(new String(fieldBits.toByteArray, UTF_8)))

  /**
   * Decoder of result set acquisition
   *
   * A foolproof implementation using splitAt is faster than the helper functions provided by scodec.
   *
   * @param columnLength
   *   The number of columns in the result set.
   */
  private def decodeResultSetRow(columnLength: Int): Decoder[ResultSetRowPacket] =
    new Decoder[ResultSetRowPacket]:
      override def decode(bits: BitVector): Attempt[DecodeResult[ResultSetRowPacket]] =
        val buffer                                 = new Array[Option[String]](columnLength)
        var remainingFields                        = columnLength
        var remainder                              = bits
        val (fieldLengthBits, fieldLengthReminded) = remainder.splitAt(8)
        val fieldLength                            = fieldLengthBits.toInt(false)
        remainder = fieldLengthReminded
        while remainingFields >= 1 do
          val index = columnLength - remainingFields
          if fieldLength == NULL && index == 0 then buffer.update(index, None)
          else if index == 0 then
            val (fieldBits, postFieldBits) = remainder.splitAt(fieldLength * 8L)
            buffer.update(index, Some(new String(fieldBits.toByteArray, UTF_8)))
            remainder = postFieldBits
          else
            val (lengthBits, postLengthBits) = remainder.splitAt(8)
            val length                       = lengthBits.toInt(false)
            remainder = postLengthBits
            if length == NULL then buffer.update(index, None)
            else if length <= 251 then
              val (fieldBits, postFieldBits) = remainder.splitAt(length * 8L)
              buffer.update(index, Some(new String(fieldBits.toByteArray, UTF_8)))
              remainder = postFieldBits
            else if length == 252 then
              val (postFieldSize, decodedValue) = decodeToString(remainder, 16)
              buffer.update(index, decodedValue)
              remainder = postFieldSize
            else if length == 253 then
              val (postFieldSize, decodedValue) = decodeToString(remainder, 24)
              buffer.update(index, decodedValue)
              remainder = postFieldSize
            else if length == 254 then
              val (postFieldSize, decodedValue) = decodeToString(remainder, 32)
              buffer.update(index, decodedValue)
              remainder = postFieldSize
            else return Attempt.Failure(Err("Invalid length encoded integer: " + fieldLength))
          end if
          remainingFields -= 1
        end while

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
        case _                => decodeResultSetRow(columnLength).decode(bits)
