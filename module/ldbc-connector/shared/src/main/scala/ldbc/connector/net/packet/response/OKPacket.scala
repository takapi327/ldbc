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

import ldbc.connector.data.*

/**
 * An OK packet is sent from the server to the client to signal successful completion of a command.
 *
 * if CLIENT_PROTOCOL_41 is set, the packet contains a warning count.
 *
 * @param status
 *   Type: int<1>
 *   Name: header
 *   Description: 0x00 or 0xFE the OK packet header
 * @param affectedRows
 *   Type: int<lenenc>
 *   Name: affected_rows
 *   Description: affected rows
 * @param lastInsertId
 *   Type: int<lenenc>
 *   Name: last_insert_id
 *   Description: last insert-id
 * @param statusFlags if capabilities & CLIENT_PROTOCOL_41
 *   Type: int<2>
 *   Name: status_flags
 *   Description: SERVER_STATUS_flags_enum
 * @param warnings if capabilities & CLIENT_PROTOCOL_41
 *   Type: int<2>
 *   Name: warnings
 *   Description: number of warnings
 * @param info
 *   Type: string<lenenc>
 *   Name: info
 *   Description: human readable status information
 * @param sessionStateInfo
 *   Type: string<lenenc>
 *   Name: session state info
 *   Description: Session State Information
 * @param msg
 *   Type: string<EOF>
 *   Name: info
 *   Description: human readable status information
 */
case class OKPacket(
  status:           Int,
  affectedRows:     Int,
  lastInsertId:     Int,
  statusFlags:      Seq[ServerStatusFlags],
  warnings:         Option[Int],
  info:             Option[String],
  sessionStateInfo: Option[String],
  msg:              Option[String]
) extends GenericResponsePackets:

  override def toString: String = "OK_Packet"

object OKPacket:

  val STATUS = 0x00

  def decoder(capabilityFlags: Seq[CapabilitiesFlags]): Decoder[OKPacket] =
    val hasClientProtocol41Flag   = capabilityFlags.contains(CapabilitiesFlags.CLIENT_PROTOCOL_41)
    val hasClientTransactionsFlag = capabilityFlags.contains(CapabilitiesFlags.CLIENT_TRANSACTIONS)
    val hasClientSessionTrackFlag = capabilityFlags.contains(CapabilitiesFlags.CLIENT_SESSION_TRACK)
    for
      status       <- uint8
      affectedRows <- uint8
      lastInsertId <- uint8
      statusFlags <-
        (if hasClientProtocol41Flag || hasClientTransactionsFlag then uint16L.map(int => ServerStatusFlags(int.toLong))
         else provide(Nil))
      warnings <- if hasClientProtocol41Flag then uint8L.map(_.some) else provide(None)
      info     <- if hasClientSessionTrackFlag then bytes.map(_.decodeUtf8Lenient.some) else provide(None)
      sessionStateInfo <- (if statusFlags.contains(ServerStatusFlags.SERVER_SESSION_STATE_CHANGED) then
                             bytes.map(_.decodeUtf8Lenient.some)
                           else provide(None))
      msg <- if !hasClientSessionTrackFlag then bytes.map(_.decodeUtf8Lenient.some) else provide(None)
    yield OKPacket(status, affectedRows, lastInsertId, statusFlags, warnings, info, sessionStateInfo, msg)
