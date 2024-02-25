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
import scodec.codecs.*

import ldbc.connector.data.*
import ldbc.connector.data.ColumnDataType.*

/**
 * value of each parameter: Binary Protocol Value
 *
 * @see https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_binary_resultset.html#sect_protocol_binary_resultset_row_value
 *
 * @param values
 *   a map of column data type and its value
 */
case class BinaryProtocolValuePacket(values: ListMap[ColumnDataType, Any]) extends RequestPacket:

  override protected def encodeBody: Attempt[BitVector] =
    BinaryProtocolValuePacket.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

object BinaryProtocolValuePacket:

  val encoder: Encoder[BinaryProtocolValuePacket] = Encoder { binaryProtocolValue =>
    Attempt.successful(
      binaryProtocolValue.values.foldLeft(BitVector.empty) {
        case (acc, tuple) =>
          tuple match
            case (MYSQL_TYPE_NULL, _) => acc
            case (MYSQL_TYPE_TINY, value: Boolean) => acc ++ uint8L.encode(if value then 1 else 0).require
            case (MYSQL_TYPE_TINY, value: Byte) => acc ++ uint8L.encode(value).require
            case (MYSQL_TYPE_SHORT | MYSQL_TYPE_YEAR, value: Short) => acc ++ uint16L.encode(value).require
            case (MYSQL_TYPE_LONG | MYSQL_TYPE_INT24, value: Int) => acc ++ uint32L.encode(value).require
            case (MYSQL_TYPE_LONGLONG, value: Long) => acc ++ int64L.encode(value).require
            case (MYSQL_TYPE_FLOAT, value: Float) => acc ++ float.encode(value).require
            case (MYSQL_TYPE_DOUBLE, value: Double) => acc ++ double.encode(value).require
            case (
              MYSQL_TYPE_STRING | MYSQL_TYPE_VARCHAR | MYSQL_TYPE_ENUM |
              MYSQL_TYPE_SET | MYSQL_TYPE_LONG_BLOB |
              MYSQL_TYPE_MEDIUM_BLOB | MYSQL_TYPE_BLOB |
              MYSQL_TYPE_TINY_BLOB | MYSQL_TYPE_GEOMETRY |
              MYSQL_TYPE_BIT | MYSQL_TYPE_DECIMAL | MYSQL_TYPE_NEWDECIMAL,
              value: String
            ) =>
              val bytes = value.getBytes("UTF-8")
              acc ++ BitVector(copyOf(bytes, bytes.length))
            case (MYSQL_TYPE_VAR_STRING, value: Array[Byte]) => acc ++ BitVector(copyOf(value, value.length))
            case (MYSQL_TYPE_TIME, value: java.time.LocalTime) => acc ++ time.encode(value).require
            case (MYSQL_TYPE_DATE, value: java.time.LocalDate)          => acc ++ date.encode(value).require
            case (MYSQL_TYPE_DATETIME | MYSQL_TYPE_TIMESTAMP | MYSQL_TYPE_TIMESTAMP2, value: java.time.LocalDateTime) => acc ++ dateTime.encode(value).require
            case (_, unknown) => throw new IllegalArgumentException(s"Unsupported data type: $unknown")
      }
    )
  }
