/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package request

import java.util.Arrays.copyOf

import scodec.*
import scodec.bits.*

import cats.syntax.all.*

/**
 * Authentication Method Switch Response Packet which contains response data generated by the authentication method requested in Protocol::OldAuthSwitchRequest: packet.
 *
 * This data is opaque to the protocol.
 * 
 * @param hashedPassword
 *   Type: string<EOF>
 *   Name: data
 *   Description: authentication response data
 */
case class AuthSwitchResponsePacket(
  hashedPassword: Array[Byte]
) extends RequestPacket:

  override protected def encodeBody: Attempt[BitVector] = AuthSwitchResponsePacket.encoder.encode(this)

  override def encode: BitVector =
    encodeBody.require

  override def toString: String = "Protocol::AuthSwitchResponse"

object AuthSwitchResponsePacket:

  val encoder: Encoder[AuthSwitchResponsePacket] = Encoder { authSwitchResponsePacket =>
    Attempt.Successful(
      BitVector(copyOf(authSwitchResponsePacket.hashedPassword, authSwitchResponsePacket.hashedPassword.length))
    )
  }
