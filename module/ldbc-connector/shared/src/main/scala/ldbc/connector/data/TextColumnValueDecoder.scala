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

  override def decodeString(bytes: Array[Byte], charset: String, columnType: ColumnDataType, isUnsigned: Boolean): String =
    asString(bytes, charset)

  override def decodeBoolean(bytes: Array[Byte], charset: String, columnType: ColumnDataType, isUnsigned: Boolean): Boolean =
    asString(bytes, charset) match
      case "true" | "1" => true
      case _            => false

  override def decodeByte(bytes: Array[Byte], charset: String, columnType: ColumnDataType, isUnsigned: Boolean): Byte =
    val str = asString(bytes, charset)
    if str.length == 1 && !str.forall(_.isDigit) then str.getBytes().head
    else str.toByte

  override def decodeShort(bytes: Array[Byte], charset: String, columnType: ColumnDataType, isUnsigned: Boolean): Short =
    asString(bytes, charset).toShort

  override def decodeInt(bytes: Array[Byte], charset: String, columnType: ColumnDataType, isUnsigned: Boolean): Int =
    asString(bytes, charset).toInt

  override def decodeLong(bytes: Array[Byte], charset: String, columnType: ColumnDataType, isUnsigned: Boolean): Long =
    asString(bytes, charset).toLong

  override def decodeFloat(bytes: Array[Byte], charset: String, columnType: ColumnDataType, isUnsigned: Boolean): Float =
    asString(bytes, charset).toFloat

  override def decodeDouble(bytes: Array[Byte], charset: String, columnType: ColumnDataType, isUnsigned: Boolean): Double =
    asString(bytes, charset).toDouble

  override def decodeBigDecimal(bytes: Array[Byte], charset: String, columnType: ColumnDataType, isUnsigned: Boolean): BigDecimal =
    BigDecimal(asString(bytes, charset))

  override def decodeBytes(bytes: Array[Byte], charset: String, columnType: ColumnDataType, isUnsigned: Boolean): Array[Byte] =
    asString(bytes, charset).getBytes(charset)

  override def decodeDate(bytes: Array[Byte], charset: String, columnType: ColumnDataType, isUnsigned: Boolean): LocalDate =
    LocalDate.parse(asString(bytes, charset), localDateFormatter)

  override def decodeTime(bytes: Array[Byte], charset: String, columnType: ColumnDataType, isUnsigned: Boolean): LocalTime =
    LocalTime.parse(asString(bytes, charset), timeFormatter(6))

  override def decodeTimestamp(bytes: Array[Byte], charset: String, columnType: ColumnDataType, isUnsigned: Boolean): LocalDateTime =
    LocalDateTime.parse(asString(bytes, charset), localDateTimeFormatter(6))

  override def extractColumn(bytes: Array[Byte], index: Int, columnTypes: Vector[ColumnDataType]): Option[Array[Byte]] =
    val NULL   = 0xfb
    var offset = 0
    var col    = 0
    while col < index do
      val lenByte = bytes(offset) & 0xff
      if lenByte == NULL then offset += 1
      else
        val (_, totalWidth) = readLengthEncoded(bytes, offset)
        offset += totalWidth
      col += 1

    val lenByte = bytes(offset) & 0xff
    if lenByte == NULL then None
    else
      val (dataLen, _) = readLengthEncoded(bytes, offset)
      val headerSize   = if lenByte <= 250 then 1 else if lenByte == 252 then 3 else if lenByte == 253 then 4 else 9
      Some(bytes.slice(offset + headerSize, offset + headerSize + dataLen))

  private def readLengthEncoded(bytes: Array[Byte], offset: Int): (Int, Int) =
    val lenByte = bytes(offset) & 0xff
    if lenByte <= 250 then (lenByte, 1 + lenByte)
    else if lenByte == 252 then
      val len = (bytes(offset + 1) & 0xff) | ((bytes(offset + 2) & 0xff) << 8)
      (len, 3 + len)
    else if lenByte == 253 then
      val len =
        (bytes(offset + 1) & 0xff) | ((bytes(offset + 2) & 0xff) << 8) | ((bytes(offset + 3) & 0xff) << 16)
      (len, 4 + len)
    else
      val len = (0 until 8).foldLeft(0L)((acc, i) => acc | ((bytes(offset + 1 + i) & 0xffL) << (i * 8))).toInt
      (len, 9 + len)
