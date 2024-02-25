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
 * The COM_INIT_DB request is used to change the default database for the connection.
 * The database name is specified as an argument to the request.
 * The server changes the default database to the one specified and sends a OK_Packet to the client.
 * If the database does not exist, the server sends an ERR_Packet to the client.
 */
case class ComInitDBPacket() extends RequestPacket:

  override protected def encodeBody: Attempt[BitVector] =
    ComInitDBPacket.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "COM_INIT_DB Request"

object ComInitDBPacket:

  val encoder: Encoder[ComInitDBPacket] = Encoder(_ => Attempt.successful(BitVector(CommandId.COM_INIT_DB)))
