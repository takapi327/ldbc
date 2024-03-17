/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package request

import scodec.codecs.uint32L
import scodec.bits.*

import ldbc.connector.data.CapabilitiesFlags

/**
 * Depending on the servers support for the CLIENT_PROTOCOL_41 capability and the clients understanding of that flag the client has to send either a Protocol::HandshakeResponse320 or Protocol::HandshakeResponse41.
 */
trait HandshakeResponsePacket extends RequestPacket:

  def capabilitiesFlags: Seq[CapabilitiesFlags]
  def user:              String
  def hashedPassword:    Array[Byte]
  def pluginName:        String
  def database:          Option[String]

  val maxPacketSize: BitVector = hex"ffffff00".bits

  def encodeCapabilitiesFlags(): BitVector =
    val bitset = CapabilitiesFlags.toBitset(capabilitiesFlags)
    uint32L.encode(bitset).require

object HandshakeResponsePacket:

  def apply(
    capabilitiesFlags: List[CapabilitiesFlags],
    user:              String,
    hashedPassword:    Array[Byte],
    pluginName:        String,
    characterSet:      Int,
    database:          Option[String]
  ): HandshakeResponsePacket =
    if capabilitiesFlags.contains(CapabilitiesFlags.CLIENT_PROTOCOL_41) then
      HandshakeResponse41Packet(capabilitiesFlags, user, hashedPassword, pluginName, characterSet, database)
    else HandshakeResponse320Packet(capabilitiesFlags, user, hashedPassword, pluginName, database)
