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

import ldbc.connector.data.CapabilitiesFlags

/**
 * Represents a row in a result set.
 *
 * @see https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_query_response_text_resultset_row.html
 *
 * A row with data for each column.
 *   - NULL is sent as 0xFB
 *   - everything else is converted to a string and is sent as string<lenenc>
 */
trait ResultSetRowPacket extends ResponsePacket:

  /**
   * The values of the row.
   */
  def values: Array[Option[String]]

  override def toString: String = "ProtocolText::ResultSetRow"

object ResultSetRowPacket:

  private val NULL = 0xfb

  def apply(_values: Array[Option[String]]): ResultSetRowPacket =
    new ResultSetRowPacket:
      override val values: Array[Option[String]] = _values

  private def decodeValue(length: Int): Decoder[Option[String]] =
    if length == NULL then Decoder.pure(None)
    else bytes(length).asDecoder.map(_.decodeUtf8Lenient.some)

  def decoder(
    capabilityFlags: Set[CapabilitiesFlags],
    columns:         Vector[ColumnDefinitionPacket]
  ): Decoder[ResultSetRowPacket | EOFPacket | ERRPacket] =
    uint8.flatMap {
      case EOFPacket.STATUS => EOFPacket.decoder(capabilityFlags)
      case ERRPacket.STATUS => ERRPacket.decoder(capabilityFlags)
      case length =>
        val buffer = new Array[Option[String]](columns.length)

        def decodeRow(index: Int, remainingLength: Option[Int]): Decoder[ResultSetRowPacket] =
          if index >= columns.length then Decoder.pure(ResultSetRowPacket(buffer))
          else
            val valueDecoder =
              if index == 0 then decodeValue(length)
              else
                remainingLength match
                  case Some(length) => decodeValue(length)
                  case None         => lengthEncodedIntDecoder.flatMap(length => decodeValue(length.toInt))

            valueDecoder.flatMap { value =>
              buffer(index) = value
              if index == 0 then decodeRow(index + 1, None)
              else
                lengthEncodedIntDecoder.flatMap { nextLength =>
                  decodeRow(index + 1, Some(nextLength.toInt))
                }
            }

        decodeRow(0, Some(length))
    }
