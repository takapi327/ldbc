/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package request

import cats.syntax.all.*

import scodec.*
import scodec.codecs.uint32L
import scodec.bits.BitVector
import scodec.interop.cats.*

import ldbc.connector.data.CommandId

case class ComStmtClosePacket(statementId: Long) extends RequestPacket:

  override protected def encodeBody: Attempt[BitVector] =
    ComStmtClosePacket.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "COM_STMT_CLOSE Request"
  
object ComStmtClosePacket:

  val encoder: Encoder[ComStmtClosePacket] = Encoder { comStmtClose =>
    Attempt.Successful(
      BitVector(CommandId.COM_STMT_CLOSE) |+|
        uint32L.encode(comStmtClose.statementId).require
    )
  }
