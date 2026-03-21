/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import java.time.*

import ldbc.connector.data.Formatter.*

/**
 * Text protocol implementation of ColumnValueDecoder.
 * Converts byte arrays to strings using charset, then parses to the target type.
 */
private[ldbc] object TextColumnValueDecoder extends ColumnValueDecoder:

  private def asString(bytes: Array[Byte], charset: String): String =
    new String(bytes, charset)

  override def decodeString(bytes: Array[Byte], charset: String, columnType: ColumnDataType): String =
    asString(bytes, charset)

  override def decodeBoolean(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Boolean =
    asString(bytes, charset) match
      case "true" | "1" => true
      case _            => false

  override def decodeByte(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Byte =
    val str = asString(bytes, charset)
    if str.length == 1 && !str.forall(_.isDigit) then str.getBytes().head
    else str.toByte

  override def decodeShort(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Short =
    asString(bytes, charset).toShort

  override def decodeInt(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Int =
    asString(bytes, charset).toInt

  override def decodeLong(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Long =
    asString(bytes, charset).toLong

  override def decodeFloat(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Float =
    asString(bytes, charset).toFloat

  override def decodeDouble(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Double =
    asString(bytes, charset).toDouble

  override def decodeBigDecimal(bytes: Array[Byte], charset: String, columnType: ColumnDataType): BigDecimal =
    BigDecimal(asString(bytes, charset))

  override def decodeBytes(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Array[Byte] =
    asString(bytes, charset).getBytes(charset)

  override def decodeDate(bytes: Array[Byte], charset: String, columnType: ColumnDataType): LocalDate =
    LocalDate.parse(asString(bytes, charset), localDateFormatter)

  override def decodeTime(bytes: Array[Byte], charset: String, columnType: ColumnDataType): LocalTime =
    LocalTime.parse(asString(bytes, charset), timeFormatter(6))

  override def decodeTimestamp(bytes: Array[Byte], charset: String, columnType: ColumnDataType): LocalDateTime =
    LocalDateTime.parse(asString(bytes, charset), localDateTimeFormatter(6))
