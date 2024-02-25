/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import scodec.*

import ldbc.connector.data.CapabilitiesFlags

trait ColumnDefinitionPacket extends ResponsePacket

object ColumnDefinitionPacket:
  
  def decoder(capabilitiesFlags: Seq[CapabilitiesFlags]): Decoder[ColumnDefinitionPacket] =
    if capabilitiesFlags.contains(CapabilitiesFlags.CLIENT_PROTOCOL_41) then ColumnDefinition41Packet.decoder
    else ColumnDefinition320Packet.decoder
