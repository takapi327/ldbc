/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import scodec.*
import scodec.codecs.*
import scodec.interop.cats.*

import cats.syntax.all.*

import ldbc.connector.data.CapabilitiesFlags
import ldbc.connector.data.ColumnDataType.*
import ldbc.connector.data.Formatter.*

case class BinaryProtocolResultSetRowPacket(values: List[Option[String]]) extends ResultSetRowPacket:

  override def toString: String = "Binary Protocol ResultSet Row"

object BinaryProtocolResultSetRowPacket:

  def decodeValue(column: ColumnDefinitionPacket, isColumnNull: Boolean): Decoder[Option[String]] =
    if isColumnNull then provide(None)
    else
      column.columnType match
        case MYSQL_TYPE_STRING | MYSQL_TYPE_VARCHAR | MYSQL_TYPE_VAR_STRING | MYSQL_TYPE_ENUM | MYSQL_TYPE_SET |
          MYSQL_TYPE_LONG_BLOB | MYSQL_TYPE_MEDIUM_BLOB | MYSQL_TYPE_BLOB | MYSQL_TYPE_TINY_BLOB | MYSQL_TYPE_GEOMETRY |
          MYSQL_TYPE_BIT | MYSQL_TYPE_DECIMAL | MYSQL_TYPE_NEWDECIMAL =>
          uint8L.flatMap(bytes(_)).map(_.decodeUtf8Lenient.some)
        case MYSQL_TYPE_LONGLONG                        => int64L.map(_.toString.some)
        case MYSQL_TYPE_LONG | MYSQL_TYPE_INT24         => uint32L.map(_.toString.some)
        case MYSQL_TYPE_SHORT | MYSQL_TYPE_YEAR         => uint16L.map(_.toString.some)
        case MYSQL_TYPE_TINY                            => uint8L.map(_.toString.some)
        case MYSQL_TYPE_DOUBLE                          => doubleL.map(_.toString.some)
        case MYSQL_TYPE_FLOAT                           => floatL.map(_.toString.some)
        case MYSQL_TYPE_DATE                            => timestamp.map(_.map(localDateTime => localDateFormatter.format(localDateTime.toLocalDate)))
        case MYSQL_TYPE_DATETIME | MYSQL_TYPE_TIMESTAMP => timestamp.map(_.map(localDateTimeFormatter(0).format(_)))
        case MYSQL_TYPE_TIME                            => time.map(timeFormatter(0).format(_).some)
        case MYSQL_TYPE_NULL                            => provide(None)
        case MYSQL_TYPE_NEWDATE | MYSQL_TYPE_TIMESTAMP2 | MYSQL_TYPE_DATETIME2 | MYSQL_TYPE_TIME2 | MYSQL_TYPE_JSON =>
          throw new RuntimeException(s"Unsupported column type: ${ column.columnType }")

  def decoder(
    capabilityFlags: Seq[CapabilitiesFlags],
    columns:         Vector[ColumnDefinitionPacket]
  ): Decoder[BinaryProtocolResultSetRowPacket | EOFPacket | ERRPacket] =
    uint8L.flatMap {
      case EOFPacket.STATUS => EOFPacket.decoder(capabilityFlags)
      case ERRPacket.STATUS => ERRPacket.decoder(capabilityFlags)
      case OKPacket.STATUS =>
        for
          nullBitmapBytes <- bytes((columns.length + 7 + 2) / 8)
          values <- columns.zipWithIndex.traverse {
                      case (column, index) =>
                        val isColumnNull = (nullBitmapBytes((index + 2) / 8) & (1 << ((index + 2) % 8))) != 0
                        decodeValue(column, isColumnNull)
                    }
        yield BinaryProtocolResultSetRowPacket(values.toList)
    }
