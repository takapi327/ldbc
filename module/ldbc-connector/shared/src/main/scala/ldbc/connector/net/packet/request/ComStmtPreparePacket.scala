/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package request

import cats.syntax.all.*

import scodec.*
import scodec.bits.BitVector
import scodec.interop.cats.*

import ldbc.connector.data.CommandId

case class ComStmtPreparePacket(query: String) extends RequestPacket:

  override protected def encodeBody: Attempt[BitVector] =
    ComStmtPreparePacket.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "COM_STMT_PREPARE Request"

object ComStmtPreparePacket:

  val encoder: Encoder[ComStmtPreparePacket] = Encoder { comStmtPrepare =>
    Attempt.successful(
      BitVector(CommandId.COM_STMT_PREPARE) |+|
        nullTerminatedStringCodec.encode(comStmtPrepare.query).require
    )
  }
