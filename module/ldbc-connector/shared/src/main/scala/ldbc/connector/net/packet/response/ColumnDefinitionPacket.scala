/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import scodec.*

import ldbc.connector.data.*

/**
 * A column definition packet is sent by the server to the client after a query is executed.
 * It contains information about the columns of the result set.
 */
trait ColumnDefinitionPacket extends ResponsePacket:

  /** Table name */
  def table: String

  /** Column name */
  def name: String

  /** Column Data Type */
  def columnType: ColumnDataType

  /** ColumnDefinitionFlags is a bitset of column definition flags. */
  def flags: Seq[ColumnDefinitionFlags]

object ColumnDefinitionPacket:

  def decoder(capabilitiesFlags: Seq[CapabilitiesFlags]): Decoder[ColumnDefinitionPacket] =
    if capabilitiesFlags.contains(CapabilitiesFlags.CLIENT_PROTOCOL_41) then ColumnDefinition41Packet.decoder
    else ColumnDefinition320Packet.decoder
