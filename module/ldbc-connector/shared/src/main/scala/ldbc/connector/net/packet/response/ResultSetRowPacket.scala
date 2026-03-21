/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

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

