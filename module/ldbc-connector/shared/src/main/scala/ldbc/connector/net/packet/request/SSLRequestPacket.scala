/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package request

import cats.syntax.all.*

import scodec.*
import scodec.bits.*
import scodec.codecs.uint32
import scodec.interop.cats.*

import ldbc.connector.data.*

/**
 * SSL Connection Request Packet.
 *
 * It is like Protocol::HandshakeResponse: but is truncated right before username field. 
 * If server supports CLIENT_SSL capability, client can send this packet to request a secure SSL connection. 
 * The CLIENT_SSL capability flag must be set inside the SSL Connection Request Packet.
 * 
 * @param capabilityFlags
 *  The capability flags of the client.
 */
case class SSLRequestPacket(sequenceId: Byte, capabilityFlags: List[CapabilitiesFlags]) extends RequestPacket:

  override protected def encodeBody: Attempt[BitVector] = SSLRequestPacket.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "Protocol::SSLRequest"

object SSLRequestPacket:

  val encoder: Encoder[SSLRequestPacket] = Encoder { (packet: SSLRequestPacket) =>
    val hasClientProtocol41 = packet.capabilityFlags.contains(CapabilitiesFlags.CLIENT_PROTOCOL_41)
    val clientFlag = uint32.encode(CapabilitiesFlags.toBitset(packet.capabilityFlags)).require
    val maxPacketSize =
      if hasClientProtocol41 then BitVector(0xff) |+| BitVector(0xff) |+| BitVector(0xff) |+| BitVector(0x0)
      else BitVector(0xff) |+| BitVector(0xff) |+| BitVector(0xff)
    val payload =
      if hasClientProtocol41 then clientFlag |+| maxPacketSize |+| BitVector(0xff) |+| BitVector(new Array[Byte](23))
      else clientFlag |+| maxPacketSize
    val payloadSize = payload.bytes.size
    val header      = BitVector(payloadSize) |+| BitVector(0) |+| BitVector(0) |+| BitVector(packet.sequenceId)
    Attempt.successful(header |+| payload)
  }
