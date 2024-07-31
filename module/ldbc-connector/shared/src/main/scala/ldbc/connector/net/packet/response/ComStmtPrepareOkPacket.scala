/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import scala.annotation.switch

import scodec.*
import scodec.codecs.*

import ldbc.connector.data.CapabilitiesFlags

/**
 * Packet indicating success when [[request.ComStmtPreparePacket]] packet is sent.
 * 
 * @param status
 *   0x00: No error
 * @param statementId
 *   The server assigns a statement ID to the prepared statement. This value is used for subsequent operations using the statement.
 * @param numColumns
 *   The number of columns in the result set.
 * @param numParams
 *   The number of parameters in the prepared statement.
 * @param reserved1
 *   Always 0x00
 * @param warningCount
 *   The number of warnings.
 * @param metadataFollows
 *   Flag specifying if metadata are skipped or not.
 */
case class ComStmtPrepareOkPacket(
  status:          Int,
  statementId:     Long,
  numColumns:      Int,
  numParams:       Int,
  reserved1:       Int,
  warningCount:    Int,
  metadataFollows: Option[Int]
) extends ResponsePacket:

  override def toString: String = "COM_STMT_PREPARE_OK Packet"

object ComStmtPrepareOkPacket:

  def decoder(capabilityFlags: Set[CapabilitiesFlags]): Decoder[ComStmtPrepareOkPacket | ERRPacket] =
    uint8L.flatMap { status =>
      (status: @switch) match
        case ERRPacket.STATUS => ERRPacket.decoder(capabilityFlags)
        case OKPacket.STATUS =>
          for
            statementId  <- int32L
            numColumns   <- int16L
            numParams    <- int16L
            reserved1    <- int8L
            warningCount <- int16L
            metadataFollows <-
              (if capabilityFlags.contains(CapabilitiesFlags.CLIENT_OPTIONAL_RESULTSET_METADATA) then provide(None)
               else int8L.map(Some(_)))
          yield ComStmtPrepareOkPacket(
            status,
            statementId,
            numColumns,
            numParams,
            reserved1,
            warningCount,
            metadataFollows
          )
    }
