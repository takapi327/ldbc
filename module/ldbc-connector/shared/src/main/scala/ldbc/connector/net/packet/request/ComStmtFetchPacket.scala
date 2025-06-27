/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package request

import scodec.*
import scodec.bits.BitVector
import scodec.codecs.uint32L
import scodec.interop.cats.*

import cats.syntax.all.*

import ldbc.connector.data.CommandId

/**
 * COM_STMT_FETCH is used to fetch additional rows from a statement result set
 * after an initial execution that used cursor mode.
 *
 * @param statementId
 *   The ID of the prepared statement from which to fetch rows
 * @param numRows
 *   The maximum number of rows to fetch in this request
 */
case class ComStmtFetchPacket(
  statementId: Long,
  numRows:     Long
) extends RequestPacket:

  override protected def encodeBody: Attempt[BitVector] =
    ComStmtFetchPacket.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "COM_STMT_FETCH Request"

object ComStmtFetchPacket:

  val encoder: Encoder[ComStmtFetchPacket] = Encoder { comStmtFetch =>
    Attempt.Successful(
      BitVector(CommandId.COM_STMT_FETCH) |+|
        uint32L.encode(comStmtFetch.statementId).require |+|
        uint32L.encode(comStmtFetch.numRows).require
    )
  }
