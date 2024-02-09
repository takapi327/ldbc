/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package request

import ldbc.connector.data.CapabilitiesFlags

trait HandshakeResponsePacket extends RequestPacket:

  def capabilitiesFlags: Seq[CapabilitiesFlags]
  def user:              String
  def hashedPassword:    Array[Byte]
  def pluginName:        String

object HandshakeResponsePacket:

  def apply(
    capabilitiesFlags: Seq[CapabilitiesFlags],
    user:              String,
    hashedPassword:    Array[Byte],
    pluginName:        String
  ): HandshakeResponsePacket =
    if capabilitiesFlags.contains(CapabilitiesFlags.CLIENT_PROTOCOL_41) then
      HandshakeResponse41Packet(capabilitiesFlags, user, hashedPassword, pluginName)
    else HandshakeResponse320Packet(capabilitiesFlags, user, hashedPassword, pluginName)
