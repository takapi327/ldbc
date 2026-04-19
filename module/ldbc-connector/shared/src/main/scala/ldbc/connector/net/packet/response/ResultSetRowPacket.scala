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

  override def toString: String = "ProtocolText::ResultSetRow"

object ResultSetRowPacket:

  private[ldbc] case class TextImpl(rawBytes: Array[Byte]) extends ResultSetRowPacket

  /**
   * Builds a text protocol row from string column values.
   *
   * Each value is encoded as a MySQL length-encoded string:
   *   - `None`  → 0xFB (NULL)
   *   - `Some(s)` → length prefix (1, 3, or 4 bytes) followed by UTF-8 bytes
   *
   * @param values column values in order, None for NULL
   * @return a TextImpl row packet
   */
  private[ldbc] def fromStrings(values: Option[String]*): ResultSetRowPacket =
    val bytes = values.flatMap {
      case None    => Array(0xfb.toByte)
      case Some(s) =>
        val data = s.getBytes("UTF-8")
        if data.length <= 250 then Array((data.length & 0xff).toByte) ++ data
        else if data.length <= 65535 then
          Array(0xfc.toByte, (data.length & 0xff).toByte, ((data.length >> 8) & 0xff).toByte) ++ data
        else
          Array(
            0xfd.toByte,
            (data.length & 0xff).toByte,
            ((data.length >> 8) & 0xff).toByte,
            ((data.length >> 16) & 0xff).toByte
          ) ++ data
    }.toArray
    TextImpl(bytes)
