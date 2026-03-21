/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

/**
 * A row packet received via the MySQL binary protocol.
 *
 * Stores the entire row as raw bytes starting after the 0x00 status byte
 * (i.e., beginning with the null bitmap).
 * Column values are decoded lazily by extractBinaryColumn on get*() calls.
 */
case class BinaryProtocolResultSetRowPacket(rawBytes: Array[Byte]) extends ResultSetRowPacket:
  override def isTextProtocol: Boolean = false
  override def toString: String        = "Binary Protocol ResultSet Row"
