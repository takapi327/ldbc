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
 * Old Handshake Response Packet used by old clients or if the server doesn't support CLIENT_PROTOCOL_41 Capabilities Flags flag.
 *
 * @param capabilitiesFlags
 *   [[CapabilitiesFlags]], only the lower 16 bits. CLIENT_PROTOCOL_41 should never be set.
 * @param user
 *   The username of the client.
 * @param hashedPassword
 *   The password of the client, hashed with the given method.
 * @param pluginName
 *   The authentication plugin name.
 */
case class HandshakeResponse320Packet(
  capabilitiesFlags: Seq[CapabilitiesFlags],
  user:              String,
  hashedPassword:    Array[Byte],
  pluginName:        String
) extends HandshakeResponsePacket:

  override protected def encodeBody: Attempt[BitVector] = HandshakeResponse320Packet.encoder.encode(this)

  override def encode: BitVector =
    encodeBody.require

  override def toString: String = "Protocol::HandshakeResponse320"

object HandshakeResponse320Packet:

  val encoder: Encoder[HandshakeResponse320Packet] = Encoder { handshakeResponse =>
    val maxPacketSize = hex"ffffff00".bits
    val userBytes     = handshakeResponse.user.getBytes("UTF-8")

    Attempt.successful(
      handshakeResponse.encodeCapabilitiesFlags() |+|
        maxPacketSize |+|
        BitVector(copyOf(userBytes, userBytes.length + 1)) |+|
        BitVector(copyOf(handshakeResponse.hashedPassword, handshakeResponse.hashedPassword.length))
    )
  }
