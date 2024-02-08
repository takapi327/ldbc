/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import scodec.*
import scodec.codecs.*

/**
 * This packet signals that an error occurred.
 *
 * It contains a SQL state value if CLIENT_PROTOCOL_41 is enabled.
 *
 * Error texts cannot exceed MYSQL_ERRMSG_SIZE
 * 
 * @param status
 *   Type: int<1>
 *   Name: header
 *   Description: 0xFF ERR packet header
 * @param errorCode
 *   Type: int<2>
 *   Name: error_code
 *   Description: error-code
 * @param sqlStateMarker
 *   Type: string<1>
 *   Name: sql_state_marker
 *   Description: # marker of the SQL state
 * @param sqlState
 *   Type: string<5>
 *   Name: sql_state
 *   Description: SQL state
 * @param errorMessage
 *   Type: string<EOF>
 *   Name: error_message
 *   Description: human readable error message
 */
case class ERRPacket(
  status:         Int,
  errorCode:      Int,
  sqlStateMarker: Int,
  sqlState:       String,
  errorMessage:   String
) extends GenericResponsePackets:

  override def toString: String = "ERR_Packet"

object ERRPacket:

  val STATUS = 0xff

  val decoder: Decoder[ERRPacket] =
    for
      errorCode      <- uint16L
      sqlStateMarker <- uint8
      sqlState       <- bytes(5)
      errorMessage   <- bytes
    yield ERRPacket(
      STATUS,
      errorCode,
      sqlStateMarker,
      sqlState.decodeUtf8Lenient,
      errorMessage.decodeUtf8Lenient
    )
