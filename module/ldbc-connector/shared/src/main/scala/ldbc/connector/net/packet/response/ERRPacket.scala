/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import scodec.*
import scodec.codecs.*

import cats.syntax.option.*

import ldbc.connector.data.CapabilitiesFlags
import ldbc.connector.exception.ERRPacketException

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
  sqlState:       Option[String],
  errorMessage:   String
) extends GenericResponsePackets:

  override def toString: String = "ERR_Packet"

  def toException(message: String): ERRPacketException = ERRPacketException(
    message = message,
    detail = Some(
      s"Error code: $errorCode,${ sqlState.fold("")(state => s" SQL state: $state,") } Error message: $errorMessage"
    )
  )

  def toException(message: String, sql: String): ERRPacketException = ERRPacketException(
    message = message,
    sql     = Some(sql),
    detail = Some(
      s"Error code: $errorCode,${ sqlState.fold("")(state => s" SQL state: $state,") } Error message: $errorMessage"
    )
  )

object ERRPacket:

  val STATUS = 0xff

  def decoder(capabilityFlags: Seq[CapabilitiesFlags]): Decoder[ERRPacket] =
    val hasClientProtocol41Flag = capabilityFlags.contains(CapabilitiesFlags.CLIENT_PROTOCOL_41)
    for
      errorCode      <- uint16L
      sqlStateMarker <- if hasClientProtocol41Flag then uint8 else provide(0)
      sqlState       <- if hasClientProtocol41Flag then bytes(5).map(_.decodeUtf8Lenient.some) else provide(None)
      errorMessage   <- bytes
    yield ERRPacket(
      STATUS,
      errorCode,
      sqlStateMarker,
      sqlState,
      errorMessage.decodeUtf8Lenient
    )
