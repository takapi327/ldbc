/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package request

import java.util.Arrays.copyOf

import cats.syntax.all.*

import scodec.*
import scodec.bits.*
import scodec.interop.cats.*

import ldbc.connector.data.CapabilitiesFlags

/**
 * Handshake Response Packet sent by 4.1+ clients supporting CLIENT_PROTOCOL_41 Capabilities Flags flag, 
 * if the server announced it in its Protocol::Handshake. 
 * Otherwise (talking to an old server) the Protocol::HandshakeResponse320 packet must be used.
 * 
 * @param capabilitiesFlags
 *   [[CapabilitiesFlags]], CLIENT_PROTOCOL_41 always set.
 * @param user
 *   The username of the client.
 * @param hashedPassword
 *   The password of the client, hashed with the given method.
 * @param pluginName
 *   The authentication plugin name.
 * @param characterSet
 *   The character set of the client.
 * @param database
 *   Database used for login
 */
case class HandshakeResponse41Packet(
  capabilitiesFlags: List[CapabilitiesFlags],
  user:              String,
  hashedPassword:    Array[Byte],
  pluginName:        String,
  characterSet:      Int,
  database:          Option[String],
) extends HandshakeResponsePacket:

  override protected def encodeBody: Attempt[BitVector] = HandshakeResponse41Packet.encoder.encode(this)

  override def encode: BitVector =
    encodeBody.require

  override def toString: String = "Protocol::HandshakeResponse41"

object HandshakeResponse41Packet:

  val encoder: Encoder[HandshakeResponse41Packet] = Encoder { handshakeResponse =>

    val reserved = BitVector.fill(23 * 8)(false) // 23 bytes of zero

    val authResponse = if handshakeResponse.capabilitiesFlags.contains(CapabilitiesFlags.CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA) then
      BitVector(handshakeResponse.hashedPassword)
    else
      BitVector(copyOf(handshakeResponse.hashedPassword, handshakeResponse.hashedPassword.length))

    val database = (handshakeResponse.capabilitiesFlags.contains(CapabilitiesFlags.CLIENT_CONNECT_WITH_DB), handshakeResponse.database) match
      case (true, Some(db)) => nullTerminatedStringCodec.encode(db).require
      case _                => BitVector.empty

    val pluginName = if handshakeResponse.capabilitiesFlags.contains(CapabilitiesFlags.CLIENT_PLUGIN_AUTH) then
      nullTerminatedStringCodec.encode(handshakeResponse.pluginName).require
    else
      BitVector.empty

    val attrs = if handshakeResponse.capabilitiesFlags.contains(CapabilitiesFlags.CLIENT_CONNECT_ATTRS) then
      BitVector(0x00)
    else
      BitVector.empty

    val zstdCompressionLevel = if handshakeResponse.capabilitiesFlags.contains(CapabilitiesFlags.CLIENT_ZSTD_COMPRESSION_ALGORITHM) then
      BitVector(0x00)
    else
      BitVector.empty

    Attempt.successful(
      handshakeResponse.encodeCapabilitiesFlags() |+|
        handshakeResponse.maxPacketSize |+|
        BitVector(handshakeResponse.characterSet) |+|
        reserved |+|
        nullTerminatedStringCodec.encode(handshakeResponse.user).require |+|
        authResponse |+|
        database |+|
        pluginName |+|
        attrs |+|
        zstdCompressionLevel
    )
  }
