/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import scala.collection.mutable.ArrayBuffer

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
  def values: List[Option[String]]

  override def toString: String = s"ProtocolText::ResultSetRow"

object ResultSetRowPacket:

  private val NULL = 0xfb
  
  def apply(_values: List[Option[String]]): ResultSetRowPacket =
    new ResultSetRowPacket:
      override val values: List[Option[String]] = _values

  private def decodeValue(length: Int): Decoder[Option[String]] =
    bytes(length).asDecoder
      .map(_.decodeUtf8Lenient.some)

  def decoder(
    capabilityFlags: Seq[CapabilitiesFlags],
    columns:         Vector[ColumnDefinitionPacket]
  ): Decoder[ResultSetRowPacket | EOFPacket | ERRPacket] =
    uint8.flatMap {
      case EOFPacket.STATUS => EOFPacket.decoder(capabilityFlags)
      case ERRPacket.STATUS => ERRPacket.decoder(capabilityFlags)
      case length =>
        columns.zipWithIndex
          .foldLeft(Decoder.pure(ArrayBuffer.empty[Option[String]])) {
            case (acc, (column, index)) =>
              acc.flatMap { buffer =>
                val valueDecoder = length match
                  case NULL            => Decoder.pure(None)
                  case _ if index == 0 => decodeValue(length)
                  case _ =>
                    uint8.flatMap {
                      case NULL  => Decoder.pure(None)
                      case value => decodeValue(value)
                    }

                valueDecoder.map { value =>
                  buffer.append(value)
                  buffer
                }
              }
          }
          .map(array => ResultSetRowPacket(array.toList))
    }
