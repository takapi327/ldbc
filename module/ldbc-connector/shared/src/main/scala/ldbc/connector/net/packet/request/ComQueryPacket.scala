/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package request

import java.util.Arrays.copyOf

import scala.collection.immutable.ListMap

import scodec.*
import scodec.bits.*
import scodec.interop.cats.*

import cats.syntax.all.*

import ldbc.connector.data.*

/**
 * The COM_QUERY request is used to send the server a text-based query that is executed immediately.
 * 
 * @see https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_query.html
 * 
 * @param sql
 *   The SQL query to execute
 * @param capabilityFlags
 *   The capabilities of the client
 */
case class ComQueryPacket(sql: String, capabilityFlags: Set[CapabilitiesFlags], params: ListMap[ColumnDataType, Any])
  extends RequestPacket:

  override protected def encodeBody: Attempt[BitVector] = ComQueryPacket.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "COM_QUERY Request"

object ComQueryPacket:

  val encoder: Encoder[ComQueryPacket] = Encoder { comQuery =>
    val sqlBytes = comQuery.sql.getBytes("UTF-8")

    val hasQueryAttributes = comQuery.capabilityFlags.contains(CapabilitiesFlags.CLIENT_QUERY_ATTRIBUTES)

    val parameterCount = comQuery.params.size

    val parameter =
      if hasQueryAttributes then BitVector(parameterCount) |+| BitVector(0x01)
      else BitVector.empty

    val nullBitmaps = if hasQueryAttributes && parameterCount > 0 then
      val names = comQuery.params
        .map { (columnType, param) =>
          val bytes = param.toString.getBytes("UTF-8")
          BitVector(columnType.code) |+| BitVector(0x00) |+| BitVector(copyOf(bytes, bytes.length))
        }
        .toList
        .combineAll

      BitVector(parameterCount) |+|
        nullBitmap(comQuery.params.keys.toList) |+|
        BitVector(0x01) |+|
        names |+|
        BinaryProtocolValuePacket(comQuery.params).encode
    else BitVector.empty

    Attempt.successful(
      BitVector(CommandId.COM_QUERY) |+|
        parameter |+|
        nullBitmaps |+|
        BitVector(copyOf(sqlBytes, sqlBytes.length))
    )
  }
