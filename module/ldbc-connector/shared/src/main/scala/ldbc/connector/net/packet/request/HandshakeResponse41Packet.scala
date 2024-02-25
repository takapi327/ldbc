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
 */
case class HandshakeResponse41Packet(
  capabilitiesFlags: Seq[CapabilitiesFlags],
  user:              String,
  hashedPassword:    Array[Byte],
  pluginName:        String,
  characterSet:      Int
) extends HandshakeResponsePacket:

  override protected def encodeBody: Attempt[BitVector] = HandshakeResponse41Packet.encoder.encode(this)

  override def encode: BitVector =
    encodeBody.require

  override def toString: String = "Protocol::HandshakeResponse41"

object HandshakeResponse41Packet:

  val encoder: Encoder[HandshakeResponse41Packet] = Encoder { handshakeResponse =>
    val userBytes = handshakeResponse.user.getBytes("UTF-8")

    val reserved = BitVector.fill(23 * 8)(false) // 23 bytes of zero

    val pluginBytes = handshakeResponse.pluginName.getBytes("UTF-8")

    Attempt.successful(
      handshakeResponse.encodeCapabilitiesFlags() |+|
        handshakeResponse.maxPacketSize |+|
        BitVector(handshakeResponse.characterSet) |+|
        reserved |+|
        BitVector(copyOf(userBytes, userBytes.length + 1)) |+|
        BitVector(copyOf(handshakeResponse.hashedPassword, handshakeResponse.hashedPassword.length)) |+|
        BitVector(copyOf(pluginBytes, pluginBytes.length + 2))
    )
  }
