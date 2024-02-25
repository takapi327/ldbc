/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import cats.syntax.all.*

import scodec.*
import scodec.codecs.*
import scodec.interop.cats.*

import ldbc.connector.data.CapabilitiesFlags

/**
 * Represents a row in a result set.
 *
 * @see https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_query_response_text_resultset_row.html
 *
 * A row with data for each column.
 *   - NULL is sent as 0xFB
 *   - everything else is converted to a string and is sent as string<lenenc>
 *
 * @param values
 *   The values of the row.
 */
case class ResultSetRowPacket(values: List[Option[String]]) extends ResponsePacket:

  override def toString: String = s"ProtocolText::ResultSetRow"

object ResultSetRowPacket:

  private def decodeValue(length: Int): Decoder[Option[String]] =
    bytes(length).asDecoder
      .map(_.decodeUtf8Lenient)
      .map(value => if value.toUpperCase == "NULL" then None else value.some)

  def decoder(capabilityFlags: Seq[CapabilitiesFlags], columns: Seq[ColumnDefinitionPacket]): Decoder[ResultSetRowPacket | EOFPacket | ERRPacket] =
    uint8.flatMap {
      case EOFPacket.STATUS => EOFPacket.decoder(capabilityFlags)
      case ERRPacket.STATUS => ERRPacket.decoder(capabilityFlags)
      case length =>
        columns.zipWithIndex.toList
          .traverse((_, index) =>
            if index == 0 then decodeValue(length)
            else uint8.flatMap(length => decodeValue(length))
          )
          .map(ResultSetRowPacket(_))
    }
