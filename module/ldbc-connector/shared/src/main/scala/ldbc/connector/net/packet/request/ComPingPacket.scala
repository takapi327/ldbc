/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package request

import scodec.*
import scodec.bits.BitVector

import ldbc.connector.data.CommandId

/**
 * A request packet to Check if the server is alive.
 */
case class ComPingPacket() extends RequestPacket:

  override protected def encodeBody: Attempt[BitVector] =
    ComPingPacket.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "COM_PING Request"

object ComPingPacket:

  val encoder: Encoder[ComPingPacket] = Encoder(_ => Attempt.successful(BitVector(CommandId.COM_PING)))
