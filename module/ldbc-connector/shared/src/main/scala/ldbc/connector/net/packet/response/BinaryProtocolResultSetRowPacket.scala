/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import scodec.*
import scodec.bits.BitVector
import scodec.codecs.*
import scodec.interop.cats.*

import cats.syntax.all.*

import ldbc.connector.data.CapabilitiesFlags
import ldbc.connector.data.ColumnDataType.*
import ldbc.connector.data.Formatter.*

case class BinaryProtocolResultSetRowPacket(values: Array[Option[BitVector]]) extends ResultSetRowPacket:

  override def toString: String = "Binary Protocol ResultSet Row"

object BinaryProtocolResultSetRowPacket:

  def decodeValue(column: ColumnDefinitionPacket, isColumnNull: Boolean): Decoder[Option[BitVector]] =
    if isColumnNull then provide(None)
    else
      column.columnType match
        case MYSQL_TYPE_STRING | MYSQL_TYPE_VARCHAR | MYSQL_TYPE_VAR_STRING | MYSQL_TYPE_ENUM | MYSQL_TYPE_SET |
          MYSQL_TYPE_LONG_BLOB | MYSQL_TYPE_MEDIUM_BLOB | MYSQL_TYPE_BLOB | MYSQL_TYPE_TINY_BLOB | MYSQL_TYPE_GEOMETRY |
          MYSQL_TYPE_BIT | MYSQL_TYPE_DECIMAL | MYSQL_TYPE_NEWDECIMAL =>
          lengthEncodedIntDecoder.flatMap(length => bytes(length.toInt)).map(_.toBitVector.some)
        case MYSQL_TYPE_LONGLONG                => bytes(8).map(_.toBitVector.some)
        case MYSQL_TYPE_LONG | MYSQL_TYPE_INT24 => uint32L.map(BitVector.fromLong(_).some)
        case MYSQL_TYPE_SHORT | MYSQL_TYPE_YEAR => uint16L.map(BitVector.fromInt(_).some)
        case MYSQL_TYPE_TINY                    => uint8L.map(BitVector.fromInt(_).some)
        case MYSQL_TYPE_DOUBLE                  => doubleL.map(d => BitVector.encodeUtf8(d.toString).toOption)
        case MYSQL_TYPE_FLOAT                   => floatL.map(f => BitVector.encodeUtf8(f.toString).toOption)
        case MYSQL_TYPE_DATE =>
          timestamp.map(_.flatMap(localDateTime => BitVector.encodeUtf8(localDateFormatter.format(localDateTime.toLocalDate)).toOption))
        case MYSQL_TYPE_DATETIME | MYSQL_TYPE_TIMESTAMP => timestamp.map(_.flatMap(localDateTime => BitVector.encodeUtf8(localDateTimeFormatter(0).format(localDateTime)).toOption))
        case MYSQL_TYPE_TIME                            => time.map(_.flatMap(localDateTime => BitVector.encodeUtf8(timeFormatter(0).format(localDateTime)).toOption))
        case MYSQL_TYPE_NULL                            => provide(None)
        case MYSQL_TYPE_NEWDATE | MYSQL_TYPE_TIMESTAMP2 | MYSQL_TYPE_DATETIME2 | MYSQL_TYPE_TIME2 | MYSQL_TYPE_JSON =>
          throw new RuntimeException(s"Unsupported column type: ${ column.columnType }")

  def decoder(
    capabilityFlags: Set[CapabilitiesFlags],
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
        yield BinaryProtocolResultSetRowPacket(values.toArray)
    }
