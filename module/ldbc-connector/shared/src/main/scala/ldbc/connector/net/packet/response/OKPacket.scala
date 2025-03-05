/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import scodec.*
import scodec.bits.BitVector

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
  affectedRows:     Long,
  lastInsertId:     Long,
  statusFlags:      Set[ServerStatusFlags],
  warnings:         Option[Int],
  info:             Option[String],
  sessionStateInfo: Option[String],
  msg:              Option[String]
) extends GenericResponsePackets:

  override def toString: String = "OK_Packet"

object OKPacket:

  val STATUS = 0x00

  def decoder(capabilityFlags: Set[CapabilitiesFlags]): Decoder[OKPacket] =
    (bits: BitVector) =>
      val bytes  = bits.toByteArray
      var offset = 0

      val hasClientProtocol41Flag   = capabilityFlags.contains(CapabilitiesFlags.CLIENT_PROTOCOL_41)
      val hasClientTransactionsFlag = capabilityFlags.contains(CapabilitiesFlags.CLIENT_TRANSACTIONS)
      val hasClientSessionTrackFlag = capabilityFlags.contains(CapabilitiesFlags.CLIENT_SESSION_TRACK)

      val affectedRowsLength = bytes(offset) & 0xff
      offset += 1

      val affectedRows = if affectedRowsLength <= 251 then
        affectedRowsLength
      else if affectedRowsLength == 252 then
        val rows = (bytes(offset) & 0xff) | ((bytes(offset + 1) & 0xff) << 8)
        offset += 2
        rows
      else if affectedRowsLength == 253 then
        val rows = (bytes(offset) & 0xff) |
          ((bytes(offset + 1) & 0xff) << 8) |
          ((bytes(offset + 2) & 0xff) << 16)
        offset += 3
        rows
      else
        val rows = (bytes(offset) & 0xff) |
          ((bytes(offset + 1) & 0xff) << 8) |
          ((bytes(offset + 2) & 0xff) << 16) |
          ((bytes(offset + 3) & 0xff) << 24)
        offset += 4
        rows
      
      val lastInsertIdLength = bytes(offset) & 0xff
      offset += 1

      val lastInsertId = if lastInsertIdLength <= 251 then
        lastInsertIdLength
      else if lastInsertIdLength == 252 then
        val id = (bytes(offset) & 0xff) | ((bytes(offset + 1) & 0xff) << 8)
        offset += 2
        id
      else if lastInsertIdLength == 253 then
        val id = (bytes(offset) & 0xff) |
          ((bytes(offset + 1) & 0xff) << 8) |
          ((bytes(offset + 2) & 0xff) << 16)
        offset += 3
        id
      else
        val id = (bytes(offset) & 0xff) |
          ((bytes(offset + 1) & 0xff) << 8) |
          ((bytes(offset + 2) & 0xff) << 16) |
          ((bytes(offset + 3) & 0xff) << 24)
        offset += 4
        id
      
      val statusFlags: Set[ServerStatusFlags] = if hasClientProtocol41Flag || hasClientTransactionsFlag then
        val int = bytes(offset) & 0xff | ((bytes(offset + 1) & 0xff) << 8)
        offset += 2
        ServerStatusFlags(int.toLong)
      else Set.empty[ServerStatusFlags]

      val warnings = if hasClientProtocol41Flag then
        val int = bytes(offset) & 0xff | ((bytes(offset + 1) & 0xff) << 8)
        offset += 2
        int.some
      else None

      val info = if (hasClientSessionTrackFlag && offset < bytes.length) {
        val size = bytes(offset) & 0xff
        offset += 1
        if (bytes.length >= offset + size) {
          val str = new String(bytes, offset, size, "UTF-8")
          offset += size
          str.some
        } else None
      } else None

      val sessionStateInfo = if statusFlags.contains(ServerStatusFlags.SERVER_SESSION_STATE_CHANGED) && offset < bytes.length then
        val size = bytes(offset) & 0xff
        offset += 1
        if bytes.length >= offset + size then
          val str = new String(bytes, offset, size, "UTF-8")
          offset += size
          str.some
        else None
      else None

      val msg = if !hasClientSessionTrackFlag && offset < bytes.length then
        val size = bytes(offset) & 0xff
        offset += 1
        if bytes.length >= offset + size then
          val str = new String(bytes, offset, size, "UTF-8")
          offset += size
          str.some
        else None
      else None

      Attempt.Successful(DecodeResult(OKPacket(STATUS, affectedRows, lastInsertId, statusFlags, warnings, info, sessionStateInfo, msg), bits))
