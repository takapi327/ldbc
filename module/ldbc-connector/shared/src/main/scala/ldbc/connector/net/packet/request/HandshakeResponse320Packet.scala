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
 * @param database
 *   Database used for login
 */
case class HandshakeResponse320Packet(
  capabilitiesFlags: List[CapabilitiesFlags],
  user:              String,
  hashedPassword:    Array[Byte],
  pluginName:        String,
  database:          Option[String]
) extends HandshakeResponsePacket:

  override protected def encodeBody: Attempt[BitVector] = HandshakeResponse320Packet.encoder.encode(this)

  override def encode: BitVector =
    encodeBody.require

  override def toString: String = "Protocol::HandshakeResponse320"

object HandshakeResponse320Packet:

  val encoder: Encoder[HandshakeResponse320Packet] = Encoder { handshakeResponse =>
    val userBytes = handshakeResponse.user.getBytes("UTF-8")

    val authResponse = (
      handshakeResponse.capabilitiesFlags.contains(CapabilitiesFlags.CLIENT_CONNECT_WITH_DB),
      handshakeResponse.database
    ) match
      case (true, Some(database)) =>
        BitVector(copyOf(handshakeResponse.hashedPassword, handshakeResponse.hashedPassword.length)) |+| BitVector(
          database.getBytes("UTF-8")
        )
      case _ => BitVector(copyOf(handshakeResponse.hashedPassword, handshakeResponse.hashedPassword.length))

    Attempt.successful(
      handshakeResponse.encodeCapabilitiesFlags() |+|
        handshakeResponse.maxPacketSize |+|
        BitVector(copyOf(userBytes, userBytes.length + 1)) |+|
        authResponse
    )
  }
