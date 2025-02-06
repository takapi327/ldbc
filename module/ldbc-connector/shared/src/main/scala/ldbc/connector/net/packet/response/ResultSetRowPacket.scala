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
   */
  private def decodeResultSetRow(fieldLength: Int, columnLength: Int): Decoder[ResultSetRowPacket] =
    (bits: BitVector) =>
      val buffer    = new Array[Option[String]](columnLength)
      var remainder = bits
      for index <- 0 until columnLength do
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
          else
            val (postFieldSize, decodedValue) = decodeToString(remainder, 32)
            buffer.update(index, decodedValue)
            remainder = postFieldSize
        end if

      Attempt.Successful(DecodeResult(ResultSetRowPacket(buffer), bits))

  private def decodeChunkToString(chunk: fs2.Chunk[Byte], size: Int): (fs2.Chunk[Byte], Option[String]) =
    val (fieldSizeChunk, postFieldSizeChunk) = chunk.splitAt(size)
    val fieldSizeNumBytes = fieldSizeChunk.toArray.foldLeft(0L)((acc, byte) => (acc << 8) | (byte & 0xFF))
    if fieldSizeNumBytes == NULL then (postFieldSizeChunk, None)
    else
      val (fieldChunk, postFieldChunk) = postFieldSizeChunk.splitAt(fieldSizeNumBytes.toInt)
      (postFieldChunk, Some(new String(fieldChunk.toArray, UTF_8)))

  private def decodeChunkResultSetRow(fieldLength: Int, columnLength: Int): fs2.Chunk[Byte] => ResultSetRowPacket =
    (chunk: fs2.Chunk[Byte]) =>
      val buffer = new Array[Option[String]](columnLength)
      var remainder = chunk
      var remainedLength = columnLength
      while remainedLength > 0 do
        val index = columnLength - remainedLength
        if fieldLength == NULL && index == 0 then buffer.update(index, None)
        else if index == 0 then
          val (fieldChunk, postFieldChunk) = remainder.splitAt(fieldLength)
          buffer.update(index, Some(new String(fieldChunk.toArray, UTF_8)))
          remainder = postFieldChunk
        else
          val (lengthChunk, postLengthChunk) = remainder.splitAt(1)
          val length = lengthChunk(0).toInt & 0xFF
          remainder = postLengthChunk
          if length == NULL then buffer.update(index, None)
          else if length <= 251 then
            val (fieldChunk, postFieldChunk) = remainder.splitAt(length)
            buffer.update(index, Some(new String(fieldChunk.toArray, UTF_8)))
            remainder = postFieldChunk
          else if length == 252 then
            val (postFieldSizeChunk, decodedValue) = decodeChunkToString(remainder, 2)
            buffer.update(index, decodedValue)
            remainder = postFieldSizeChunk
          else if length == 253 then
            val (postFieldSizeChunk, decodedValue) = decodeChunkToString(remainder, 3)
            buffer.update(index, decodedValue)
            remainder = postFieldSizeChunk
          else
            val (postFieldSizeChunk, decodedValue) = decodeChunkToString(remainder, 4)
            buffer.update(index, decodedValue)
            remainder = postFieldSizeChunk
        end if
        remainedLength -= 1
      
      ResultSetRowPacket(buffer)

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

  def chunkDecoder(
    capabilityFlags: Set[CapabilitiesFlags],
    columnLength:    Int
  ): fs2.Chunk[Byte] => ResultSetRowPacket | EOFPacket | ERRPacket =
    (chunk: fs2.Chunk[Byte]) =>
      val (statusChunk, postLengthChunk) = chunk.splitAt(1)
      val status = statusChunk(0).toInt & 0xFF
      status match
        case EOFPacket.STATUS => EOFPacket.decoder(capabilityFlags).decode(postLengthChunk.toBitVector).require.value
        case ERRPacket.STATUS => ERRPacket.decoder(capabilityFlags).decode(postLengthChunk.toBitVector).require.value
        case fieldLength      => decodeChunkResultSetRow(fieldLength, columnLength)(postLengthChunk)
