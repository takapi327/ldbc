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
 */
case class OKPacket(
  status:           Int,
  affectedRows:     Int,
  lastInsertId:     Int,
  statusFlags:      Int,
  warnings:         Option[Int],
  info:             Int,
  sessionStateInfo: Option[Int]
) extends GenericResponsePackets:

  override def toString: String = "OK_Packet"

object OKPacket:

  val STATUS = 0x00

  val decoder: Decoder[OKPacket] =
    for
      status           <- uint4
      affectedRows     <- uint4
      lastInsertId     <- uint4
      statusFlags      <- uint4
      warnings         <- uint4
      info             <- uint4
      sessionStateInfo <- uint4
    yield OKPacket(status, affectedRows, lastInsertId, statusFlags, Some(warnings), info, Some(sessionStateInfo))
