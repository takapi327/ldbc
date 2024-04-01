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
 * The COM_STATISTICS request is used to request statistics from the server.
 * Get a human readable string of some internal status vars.
 *
 * The statistics are refreshed at the time of executing this command.
 * If the returned string is of zero length an error message is returned by mysql_stat to the client application instead of the actual empty statistics string.
 */
case class ComStatisticsPacket() extends RequestPacket:

  override protected def encodeBody: Attempt[BitVector] =
    ComStatisticsPacket.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "COM_STATISTICS Request"

object ComStatisticsPacket:

  val encoder: Encoder[ComStatisticsPacket] = Encoder(_ => Attempt.successful(BitVector(CommandId.COM_STATISTICS)))
