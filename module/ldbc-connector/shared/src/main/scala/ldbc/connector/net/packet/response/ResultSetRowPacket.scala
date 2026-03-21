/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import ldbc.connector.data.*
import ldbc.connector.data.ColumnDataType.*

/**
 * Represents a row in a result set.
 *
 * @see https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_query_response_text_resultset_row.html
 *
 * A row with data for each column.
 *   - NULL is sent as 0xFB
 *   - everything else is converted to a string and is sent as string<lenenc>
 *
 * The entire row is stored as a raw byte array without any conversion.
 * Column values are extracted lazily on get*() calls.
 */
trait ResultSetRowPacket extends ResponsePacket:

  /** The raw bytes of the entire row packet (without protocol framing). */
  def rawBytes: Array[Byte]

  /** True if this row was received via the text protocol; false for binary protocol. */
  def isTextProtocol: Boolean

  override def toString: String = "ProtocolText::ResultSetRow"

object ResultSetRowPacket:

  private val NULL = 0xfb

  private[ldbc] case class TextImpl(rawBytes: Array[Byte]) extends ResultSetRowPacket:
    override def isTextProtocol: Boolean = true

  /**
   * Reads a length-encoded integer at the given offset and returns
   * (dataLength, totalFieldWidth) where totalFieldWidth = prefix bytes + dataLength.
   */
  def readLengthEncoded(bytes: Array[Byte], offset: Int): (Int, Int) =
    val lenByte = bytes(offset) & 0xff
    if lenByte <= 250 then
      (lenByte, 1 + lenByte)
    else if lenByte == 252 then
      val len = (bytes(offset + 1) & 0xff) | ((bytes(offset + 2) & 0xff) << 8)
      (len, 3 + len)
    else if lenByte == 253 then
      val len =
        (bytes(offset + 1) & 0xff) | ((bytes(offset + 2) & 0xff) << 8) | ((bytes(offset + 3) & 0xff) << 16)
      (len, 4 + len)
    else // 254: 8-byte length (practically unused for row data)
      val len = (0 until 8).foldLeft(0L)((acc, i) => acc | ((bytes(offset + 1 + i) & 0xffL) << (i * 8))).toInt
      (len, 9 + len)

  /**
   * Extracts the raw data bytes for the column at columnIndex (0-based) from a text protocol row.
   *
   * rawBytes layout: [len1][field1_data][len2][field2_data][0xfb=NULL][len4][field4_data]...
   *
   * @return None for NULL, Some(bytes) for non-NULL (data only, without length prefix)
   */
  def extractTextColumn(bytes: Array[Byte], columnIndex: Int): Option[Array[Byte]] =
    var offset = 0
    var col    = 0
    while col < columnIndex do
      val lenByte = bytes(offset) & 0xff
      if lenByte == NULL then
        offset += 1
      else
        val (_, totalWidth) = readLengthEncoded(bytes, offset)
        offset += totalWidth
      col += 1

    val lenByte = bytes(offset) & 0xff
    if lenByte == NULL then None
    else
      val (dataLen, _) = readLengthEncoded(bytes, offset)
      val headerSize   = if lenByte <= 250 then 1 else if lenByte == 252 then 3 else if lenByte == 253 then 4 else 9
      Some(bytes.slice(offset + headerSize, offset + headerSize + dataLen))

  /**
   * Extracts the raw data bytes for the column at columnIndex (0-based) from a binary protocol row.
   *
   * rawBytes layout (0x00 header byte already consumed by decoder):
   *   [null_bitmap (N bytes)][field0_data][field1_data]...
   *   Fixed-width types have no length prefix; variable-length types use length-encoded integers.
   *   NULL columns have no data (indicated by null bitmap only).
   *
   * @return None for NULL, Some(bytes) for non-NULL (data only, without length prefix)
   */
  def extractBinaryColumn(
    bytes:       Array[Byte],
    columnIndex: Int,
    columns:     Vector[ColumnDefinitionPacket]
  ): Option[Array[Byte]] =
    val nullBitmapSize = (columns.length + 7 + 2) / 8

    // Check null bitmap (+2 bit offset per MySQL binary protocol spec)
    val isNull = (bytes((columnIndex + 2) / 8) & (1 << ((columnIndex + 2) % 8))) != 0

    if isNull then None
    else
      var offset = nullBitmapSize
      var col    = 0
      while col < columnIndex do
        val nullBit = (bytes((col + 2) / 8) & (1 << ((col + 2) % 8))) != 0
        if !nullBit then
          offset += binaryFieldTotalWidth(bytes, offset, columns(col).columnType)
        col += 1

      Some(extractBinaryFieldData(bytes, offset, columns(columnIndex).columnType))

  /**
   * Returns the total number of bytes a binary protocol field occupies in rawBytes (for skipping).
   * Fixed-width types return their fixed size; variable-length types return prefix + data size.
   */
  private[connector] def binaryFieldTotalWidth(
    bytes:      Array[Byte],
    offset:     Int,
    columnType: ColumnDataType
  ): Int =
    columnType match
      case MYSQL_TYPE_TINY                                       => 1
      case MYSQL_TYPE_SHORT | MYSQL_TYPE_YEAR                    => 2
      case MYSQL_TYPE_LONG | MYSQL_TYPE_INT24 | MYSQL_TYPE_FLOAT => 4
      case MYSQL_TYPE_LONGLONG | MYSQL_TYPE_DOUBLE               => 8
      case _ =>
        val lenByte = bytes(offset) & 0xff
        if lenByte <= 250 then 1 + lenByte
        else if lenByte == 252 then
          3 + ((bytes(offset + 1) & 0xff) | ((bytes(offset + 2) & 0xff) << 8))
        else if lenByte == 253 then
          4 + ((bytes(offset + 1) & 0xff) | ((bytes(offset + 2) & 0xff) << 8) |
            ((bytes(offset + 3) & 0xff) << 16))
        else // 254
          9 + (0 until 8).foldLeft(0L)((acc, i) => acc | ((bytes(offset + 1 + i) & 0xffL) << (i * 8))).toInt

  /**
   * Returns the data bytes for a binary protocol field (without length prefix).
   * Fixed-width types: the fixed number of bytes.
   * Variable-length types: data only (length prefix consumed).
   */
  private[connector] def extractBinaryFieldData(
    bytes:      Array[Byte],
    offset:     Int,
    columnType: ColumnDataType
  ): Array[Byte] =
    columnType match
      case MYSQL_TYPE_TINY                                       => bytes.slice(offset, offset + 1)
      case MYSQL_TYPE_SHORT | MYSQL_TYPE_YEAR                    => bytes.slice(offset, offset + 2)
      case MYSQL_TYPE_LONG | MYSQL_TYPE_INT24 | MYSQL_TYPE_FLOAT => bytes.slice(offset, offset + 4)
      case MYSQL_TYPE_LONGLONG | MYSQL_TYPE_DOUBLE               => bytes.slice(offset, offset + 8)
      case _ =>
        val lenByte = bytes(offset) & 0xff
        if lenByte <= 250 then
          bytes.slice(offset + 1, offset + 1 + lenByte)
        else if lenByte == 252 then
          val len = (bytes(offset + 1) & 0xff) | ((bytes(offset + 2) & 0xff) << 8)
          bytes.slice(offset + 3, offset + 3 + len)
        else if lenByte == 253 then
          val len = (bytes(offset + 1) & 0xff) | ((bytes(offset + 2) & 0xff) << 8) |
            ((bytes(offset + 3) & 0xff) << 16)
          bytes.slice(offset + 4, offset + 4 + len)
        else // 254
          val len =
            (0 until 8).foldLeft(0L)((acc, i) => acc | ((bytes(offset + 1 + i) & 0xffL) << (i * 8))).toInt
          bytes.slice(offset + 9, offset + 9 + len)
