/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import java.nio.{ ByteBuffer, ByteOrder }
import java.time.*

import ldbc.connector.data.ColumnDataType.*

/**
 * Binary protocol implementation of ColumnValueDecoder.
 * Interprets byte arrays according to the MySQL binary protocol wire format.
 *
 * Byte layouts follow the scodec decoders defined in package.scala:
 *   - timestamp4:  year(2LE) + month(1) + day(1)
 *   - timestamp7:  same + hour(1) + min(1) + sec(1)
 *   - timestamp11: same + microsecond(4LE)
 *   - time8:       isNeg(1) + days(4LE) + hour(1) + min(1) + sec(1)
 *   - time12:      same + microsecond(4LE)
 */
private[ldbc] object BinaryColumnValueDecoder extends ColumnValueDecoder:

  private def le(bytes: Array[Byte]): ByteBuffer =
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

  override def decodeString(bytes: Array[Byte], charset: String, columnType: ColumnDataType): String =
    columnType match
      case MYSQL_TYPE_TINY                    => (bytes(0) & 0xff).toString
      case MYSQL_TYPE_SHORT | MYSQL_TYPE_YEAR => le(bytes).getShort.toString
      case MYSQL_TYPE_LONG | MYSQL_TYPE_INT24 => le(bytes).getInt.toString
      case MYSQL_TYPE_LONGLONG                => le(bytes).getLong.toString
      case MYSQL_TYPE_FLOAT                   => le(bytes).getFloat.toString
      case MYSQL_TYPE_DOUBLE                  => le(bytes).getDouble.toString
      case MYSQL_TYPE_BOOL                    => (bytes(0) != 0).toString
      case _                                  => new String(bytes, charset)

  override def decodeBoolean(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Boolean =
    columnType match
      case MYSQL_TYPE_BOOL => bytes(0) != 0
      case _ =>
        new String(bytes, charset) match
          case "true" | "1" => true
          case _            => false

  override def decodeByte(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Byte =
    columnType match
      case MYSQL_TYPE_TINY => bytes(0)
      case _               => new String(bytes, charset).toByte

  override def decodeShort(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Short =
    columnType match
      case MYSQL_TYPE_TINY                    => (bytes(0) & 0xff).toShort
      case MYSQL_TYPE_SHORT | MYSQL_TYPE_YEAR => le(bytes).getShort
      case _                                  => new String(bytes, charset).toShort

  override def decodeInt(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Int =
    columnType match
      case MYSQL_TYPE_TINY                    => bytes(0) & 0xff
      case MYSQL_TYPE_SHORT | MYSQL_TYPE_YEAR => le(bytes).getShort.toInt
      case MYSQL_TYPE_LONG | MYSQL_TYPE_INT24 => le(bytes).getInt
      case MYSQL_TYPE_LONGLONG                => le(bytes).getLong.toInt
      case _                                  => new String(bytes, charset).toInt

  override def decodeLong(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Long =
    columnType match
      case MYSQL_TYPE_TINY                    => (bytes(0) & 0xff).toLong
      case MYSQL_TYPE_SHORT | MYSQL_TYPE_YEAR => le(bytes).getShort.toLong
      case MYSQL_TYPE_LONG | MYSQL_TYPE_INT24 => le(bytes).getInt.toLong
      case MYSQL_TYPE_LONGLONG                => le(bytes).getLong
      case _                                  => new String(bytes, charset).toLong

  override def decodeFloat(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Float =
    columnType match
      case MYSQL_TYPE_FLOAT => le(bytes).getFloat
      case _                => new String(bytes, charset).toFloat

  override def decodeDouble(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Double =
    columnType match
      case MYSQL_TYPE_DOUBLE => le(bytes).getDouble
      case MYSQL_TYPE_FLOAT  => le(bytes).getFloat.toDouble
      case _                 => new String(bytes, charset).toDouble

  override def decodeBigDecimal(bytes: Array[Byte], charset: String, columnType: ColumnDataType): BigDecimal =
    BigDecimal(new String(bytes, charset))

  override def decodeBytes(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Array[Byte] =
    bytes

  override def decodeDate(bytes: Array[Byte], charset: String, columnType: ColumnDataType): LocalDate =
    // Follows package.scala timestamp4 layout: year(2LE) + month(1) + day(1)
    // bytes.length == 0 means MySQL sent "0000-00-00" (zero date)
    bytes.length match
      case 0 => null
      case 4 =>
        val year  = (bytes(0) & 0xff) | ((bytes(1) & 0xff) << 8)
        val month = bytes(2) & 0xff
        val day   = bytes(3) & 0xff
        LocalDate.of(year, month, day)
      case _ => null

  override def decodeTimestamp(bytes: Array[Byte], charset: String, columnType: ColumnDataType): LocalDateTime =
    // Follows package.scala timestamp4/7/11 layouts
    bytes.length match
      case 0 => null
      case 4 =>
        val year  = (bytes(0) & 0xff) | ((bytes(1) & 0xff) << 8)
        val month = bytes(2) & 0xff
        val day   = bytes(3) & 0xff
        LocalDateTime.of(year, month, day, 0, 0, 0, 0)
      case 7 =>
        val year   = (bytes(0) & 0xff) | ((bytes(1) & 0xff) << 8)
        val month  = bytes(2) & 0xff
        val day    = bytes(3) & 0xff
        val hour   = bytes(4) & 0xff
        val minute = bytes(5) & 0xff
        val second = bytes(6) & 0xff
        LocalDateTime.of(year, month, day, hour, minute, second, 0)
      case 11 =>
        val year        = (bytes(0) & 0xff) | ((bytes(1) & 0xff) << 8)
        val month       = bytes(2) & 0xff
        val day         = bytes(3) & 0xff
        val hour        = bytes(4) & 0xff
        val minute      = bytes(5) & 0xff
        val second      = bytes(6) & 0xff
        val microsecond = (bytes(7) & 0xff) | ((bytes(8) & 0xff) << 8) |
          ((bytes(9) & 0xff) << 16) | ((bytes(10) & 0xff) << 24)
        LocalDateTime.of(year, month, day, hour, minute, second, microsecond * 1000)
      case _ => null

  override def decodeTime(bytes: Array[Byte], charset: String, columnType: ColumnDataType): LocalTime =
    // Follows package.scala time8/12 layouts: isNeg(1) + days(4LE) + hour(1) + min(1) + sec(1)
    // Note: days field is ignored (same as existing package.scala behavior)
    bytes.length match
      case 0 => null
      case 8 =>
        val hour   = bytes(5) & 0xff
        val minute = bytes(6) & 0xff
        val second = bytes(7) & 0xff
        LocalTime.of(hour, minute, second)
      case 12 =>
        val hour        = bytes(5) & 0xff
        val minute      = bytes(6) & 0xff
        val second      = bytes(7) & 0xff
        val microsecond = (bytes(8) & 0xff) | ((bytes(9) & 0xff) << 8) |
          ((bytes(10) & 0xff) << 16) | ((bytes(11) & 0xff) << 24)
        LocalTime.of(hour, minute, second, microsecond * 1000)
      case _ => null

  override def extractColumn(bytes: Array[Byte], index: Int, columnTypes: Vector[ColumnDataType]): Option[Array[Byte]] =
    val nullBitmapSize = (columnTypes.length + 7 + 2) / 8

    val isNull = (bytes((index + 2) / 8) & (1 << ((index + 2) % 8))) != 0

    if isNull then None
    else
      var offset = nullBitmapSize
      var col    = 0
      while col < index do
        val nullBit = (bytes((col + 2) / 8) & (1 << ((col + 2) % 8))) != 0
        if !nullBit then
          offset += binaryFieldTotalWidth(bytes, offset, columnTypes(col))
        col += 1

      Some(extractBinaryFieldData(bytes, offset, columnTypes(index)))

  private def binaryFieldTotalWidth(bytes: Array[Byte], offset: Int, columnType: ColumnDataType): Int =
    columnType match
      case MYSQL_TYPE_TINY                                       => 1
      case MYSQL_TYPE_SHORT | MYSQL_TYPE_YEAR                    => 2
      case MYSQL_TYPE_LONG | MYSQL_TYPE_INT24 | MYSQL_TYPE_FLOAT => 4
      case MYSQL_TYPE_LONGLONG | MYSQL_TYPE_DOUBLE               => 8
      case _ =>
        val lenByte = bytes(offset) & 0xff
        if lenByte <= 250 then 1 + lenByte
        else if lenByte == 252 then
          3 + ((bytes(offset + 1) & 0xff) | ((bytes(offset + 2) & 0xff) << 8))
        else if lenByte == 253 then
          4 + ((bytes(offset + 1) & 0xff) | ((bytes(offset + 2) & 0xff) << 8) |
            ((bytes(offset + 3) & 0xff) << 16))
        else
          9 + (0 until 8).foldLeft(0L)((acc, i) => acc | ((bytes(offset + 1 + i) & 0xffL) << (i * 8))).toInt

  private def extractBinaryFieldData(bytes: Array[Byte], offset: Int, columnType: ColumnDataType): Array[Byte] =
    columnType match
      case MYSQL_TYPE_TINY                                       => bytes.slice(offset, offset + 1)
      case MYSQL_TYPE_SHORT | MYSQL_TYPE_YEAR                    => bytes.slice(offset, offset + 2)
      case MYSQL_TYPE_LONG | MYSQL_TYPE_INT24 | MYSQL_TYPE_FLOAT => bytes.slice(offset, offset + 4)
      case MYSQL_TYPE_LONGLONG | MYSQL_TYPE_DOUBLE               => bytes.slice(offset, offset + 8)
      case _ =>
        val lenByte = bytes(offset) & 0xff
        if lenByte <= 250 then
          bytes.slice(offset + 1, offset + 1 + lenByte)
        else if lenByte == 252 then
          val len = (bytes(offset + 1) & 0xff) | ((bytes(offset + 2) & 0xff) << 8)
          bytes.slice(offset + 3, offset + 3 + len)
        else if lenByte == 253 then
          val len = (bytes(offset + 1) & 0xff) | ((bytes(offset + 2) & 0xff) << 8) |
            ((bytes(offset + 3) & 0xff) << 16)
          bytes.slice(offset + 4, offset + 4 + len)
        else
          val len =
            (0 until 8).foldLeft(0L)((acc, i) => acc | ((bytes(offset + 1 + i) & 0xffL) << (i * 8))).toInt
          bytes.slice(offset + 9, offset + 9 + len)
