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
 * A request packet to reset the connection.
 */
case class ComResetConnectionPacket() extends RequestPacket:

  override protected def encodeBody: Attempt[BitVector] =
    ComResetConnectionPacket.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "COM_RESET_CONNECTION Request"

object ComResetConnectionPacket:

  val encoder: Encoder[ComResetConnectionPacket] =
    Encoder(_ => Attempt.successful(BitVector(CommandId.COM_RESET_CONNECTION)))
