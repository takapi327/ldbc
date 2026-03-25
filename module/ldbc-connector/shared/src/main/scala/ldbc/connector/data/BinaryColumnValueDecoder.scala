/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import java.math.MathContext
import java.nio.{ ByteBuffer, ByteOrder }
import java.time.*

import ldbc.connector.data.ColumnDataType.*
import ldbc.connector.exception.SQLDataException

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

  /**
   * Portable Float-to-String conversion that produces consistent output across JVM and Scala.js.
   *
   * On Scala.js, `Float.toString` delegates to JavaScript's string coercion which prints floats
   * as if they were doubles, exposing the precision loss (e.g., `3.14f.toString` → `"3.140000104904175"`).
   * This is documented as "as-designed" in Scala.js (issue #106) and the official docs recommend
   * using `String.format()` for portable float formatting.
   *
   * Uses `BigDecimal` with `MathContext.DECIMAL32` (7 significant digits) to match IEEE 754
   * single-precision float's decimal precision, then strips trailing zeros.
   */
  private def floatToString(f: Float): String =
    if f.isNaN || f.isInfinite then f.toString
    else BigDecimal(f.toDouble, MathContext.DECIMAL32).bigDecimal.stripTrailingZeros().toPlainString

  override def decodeString(
    bytes:      Array[Byte],
    charset:    String,
    columnType: ColumnDataType,
    isUnsigned: Boolean
  ): String =
    columnType match
      case MYSQL_TYPE_TINY =>
        if isUnsigned then (bytes(0) & 0xff).toString
        else bytes(0).toString
      case MYSQL_TYPE_SHORT | MYSQL_TYPE_YEAR =>
        if isUnsigned then (le(bytes).getShort & 0xffff).toString
        else le(bytes).getShort.toString
      case MYSQL_TYPE_LONG | MYSQL_TYPE_INT24 =>
        if isUnsigned then (le(bytes).getInt & 0xffffffffL).toString
        else le(bytes).getInt.toString
      case MYSQL_TYPE_LONGLONG =>
        if isUnsigned then BigInt(1, bytes.reverse).toString
        else le(bytes).getLong.toString
      case MYSQL_TYPE_FLOAT  => floatToString(le(bytes).getFloat)
      case MYSQL_TYPE_DOUBLE => le(bytes).getDouble.toString
      case MYSQL_TYPE_BOOL   => (bytes(0) != 0).toString
      case _                 => new String(bytes, charset)

  override def decodeBoolean(
    bytes:      Array[Byte],
    charset:    String,
    columnType: ColumnDataType,
    isUnsigned: Boolean
  ): Boolean =
    columnType match
      case MYSQL_TYPE_BOOL | MYSQL_TYPE_TINY  => bytes(0) != 0
      case MYSQL_TYPE_SHORT | MYSQL_TYPE_YEAR => le(bytes).getShort != 0
      case MYSQL_TYPE_LONG | MYSQL_TYPE_INT24 => le(bytes).getInt != 0
      case MYSQL_TYPE_LONGLONG                => le(bytes).getLong != 0L
      case MYSQL_TYPE_FLOAT                   => le(bytes).getFloat != 0f
      case MYSQL_TYPE_DOUBLE                  => le(bytes).getDouble != 0.0
      case _                                  =>
        new String(bytes, charset) match
          case "true" | "1" => true
          case _            => false

  override def decodeByte(bytes: Array[Byte], charset: String, columnType: ColumnDataType, isUnsigned: Boolean): Byte =
    columnType match
      case MYSQL_TYPE_TINY => bytes(0)
      case MYSQL_TYPE_BIT  => bytes.last
      case _               => new String(bytes, charset).toByte

  override def decodeShort(
    bytes:      Array[Byte],
    charset:    String,
    columnType: ColumnDataType,
    isUnsigned: Boolean
  ): Short =
    columnType match
      case MYSQL_TYPE_TINY =>
        if isUnsigned then (bytes(0) & 0xff).toShort
        else bytes(0).toShort
      case MYSQL_TYPE_SHORT | MYSQL_TYPE_YEAR => le(bytes).getShort
      case _                                  => new String(bytes, charset).toShort

  override def decodeInt(bytes: Array[Byte], charset: String, columnType: ColumnDataType, isUnsigned: Boolean): Int =
    columnType match
      case MYSQL_TYPE_TINY =>
        if isUnsigned then bytes(0) & 0xff
        else bytes(0).toInt
      case MYSQL_TYPE_SHORT | MYSQL_TYPE_YEAR =>
        if isUnsigned then le(bytes).getShort & 0xffff
        else le(bytes).getShort.toInt
      case MYSQL_TYPE_LONG | MYSQL_TYPE_INT24 =>
        if isUnsigned then
          val v = le(bytes).getInt & 0xffffffffL
          if v > Int.MaxValue then throw new NumberFormatException(s"UNSIGNED INT value $v out of Int range")
          v.toInt
        else le(bytes).getInt
      case MYSQL_TYPE_LONGLONG =>
        val v = le(bytes).getLong
        if v < Int.MinValue || v > Int.MaxValue then
          throw new NumberFormatException(s"BIGINT value $v out of Int range")
        v.toInt
      case _ => new String(bytes, charset).toInt

  override def decodeLong(bytes: Array[Byte], charset: String, columnType: ColumnDataType, isUnsigned: Boolean): Long =
    columnType match
      case MYSQL_TYPE_TINY =>
        if isUnsigned then (bytes(0) & 0xff).toLong
        else bytes(0).toLong
      case MYSQL_TYPE_SHORT | MYSQL_TYPE_YEAR =>
        if isUnsigned then le(bytes).getShort & 0xffffL
        else le(bytes).getShort.toLong
      case MYSQL_TYPE_LONG | MYSQL_TYPE_INT24 =>
        if isUnsigned then le(bytes).getInt & 0xffffffffL
        else le(bytes).getInt.toLong
      case MYSQL_TYPE_LONGLONG =>
        if isUnsigned then
          val v = le(bytes).getLong
          if v < 0 then throw new NumberFormatException(s"UNSIGNED BIGINT value out of Long range")
          v
        else le(bytes).getLong
      case _ => new String(bytes, charset).toLong

  override def decodeFloat(
    bytes:      Array[Byte],
    charset:    String,
    columnType: ColumnDataType,
    isUnsigned: Boolean
  ): Float =
    columnType match
      case MYSQL_TYPE_FLOAT => le(bytes).getFloat
      case _                => new String(bytes, charset).toFloat

  override def decodeDouble(
    bytes:      Array[Byte],
    charset:    String,
    columnType: ColumnDataType,
    isUnsigned: Boolean
  ): Double =
    columnType match
      case MYSQL_TYPE_DOUBLE => le(bytes).getDouble
      case MYSQL_TYPE_FLOAT  => le(bytes).getFloat.toDouble
      case _                 => new String(bytes, charset).toDouble

  override def decodeBigDecimal(
    bytes:      Array[Byte],
    charset:    String,
    columnType: ColumnDataType,
    isUnsigned: Boolean
  ): BigDecimal =
    BigDecimal(new String(bytes, charset))

  override def decodeBytes(
    bytes:      Array[Byte],
    charset:    String,
    columnType: ColumnDataType,
    isUnsigned: Boolean
  ): Array[Byte] =
    bytes

  override def decodeDate(
    bytes:      Array[Byte],
    charset:    String,
    columnType: ColumnDataType,
    isUnsigned: Boolean
  ): LocalDate =
    // Follows MySQL binary protocol DATE layout: year(2LE) + month(1) + day(1)
    // Accepted lengths: 0 (zero-date "0000-00-00") or 4.
    bytes.length match
      case 0 => null
      case 4 =>
        val year  = (bytes(0) & 0xff) | ((bytes(1) & 0xff) << 8)
        val month = bytes(2) & 0xff
        val day   = bytes(3) & 0xff
        LocalDate.of(year, month, day)
      case len =>
        throw new SQLDataException(s"Invalid length $len for DATE field. Expected 0 or 4.", sqlState = Some("S1009"))

  override def decodeTimestamp(
    bytes:      Array[Byte],
    charset:    String,
    columnType: ColumnDataType,
    isUnsigned: Boolean
  ): LocalDateTime =
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
      case len =>
        throw new SQLDataException(s"Invalid length $len for TIMESTAMP field. Expected 0, 4, 7, or 11.", sqlState = Some("S1009"))

  override def decodeTime(
    bytes:      Array[Byte],
    charset:    String,
    columnType: ColumnDataType,
    isUnsigned: Boolean
  ): LocalTime =
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
      case len =>
        throw new SQLDataException(s"Invalid length $len for TIME field. Expected 0, 8, or 12.", sqlState = Some("S1009"))

  override def extractColumn(bytes: Array[Byte], index: Int, columnTypes: Vector[ColumnDataType]): Option[Array[Byte]] =
    val nullBitmapSize = (columnTypes.length + 7 + 2) / 8

    val isNull = (bytes((index + 2) / 8) & (1 << ((index + 2) % 8))) != 0

    if isNull then None
    else
      var offset = nullBitmapSize
      var col    = 0
      while col < index do
        val nullBit = (bytes((col + 2) / 8) & (1 << ((col + 2) % 8))) != 0
        if !nullBit then offset += binaryFieldTotalWidth(bytes, offset, columnTypes(col))
        col += 1

      Some(extractBinaryFieldData(bytes, offset, columnTypes(index)))

  private def binaryFieldTotalWidth(bytes: Array[Byte], offset: Int, columnType: ColumnDataType): Int =
    columnType match
      case MYSQL_TYPE_TINY                                       => 1
      case MYSQL_TYPE_SHORT | MYSQL_TYPE_YEAR                    => 2
      case MYSQL_TYPE_LONG | MYSQL_TYPE_INT24 | MYSQL_TYPE_FLOAT => 4
      case MYSQL_TYPE_LONGLONG | MYSQL_TYPE_DOUBLE               => 8
      case _                                                     =>
        val lenByte = bytes(offset) & 0xff
        if lenByte <= 250 then 1 + lenByte
        else if lenByte == 252 then 3 + ((bytes(offset + 1) & 0xff) | ((bytes(offset + 2) & 0xff) << 8))
        else if lenByte == 253 then
          4 + ((bytes(offset + 1) & 0xff) | ((bytes(offset + 2) & 0xff) << 8) |
            ((bytes(offset + 3) & 0xff) << 16))
        else 9 + (0 until 8).foldLeft(0L)((acc, i) => acc | ((bytes(offset + 1 + i) & 0xffL) << (i * 8))).toInt

  private def extractBinaryFieldData(bytes: Array[Byte], offset: Int, columnType: ColumnDataType): Array[Byte] =
    columnType match
      case MYSQL_TYPE_TINY                                       => bytes.slice(offset, offset + 1)
      case MYSQL_TYPE_SHORT | MYSQL_TYPE_YEAR                    => bytes.slice(offset, offset + 2)
      case MYSQL_TYPE_LONG | MYSQL_TYPE_INT24 | MYSQL_TYPE_FLOAT => bytes.slice(offset, offset + 4)
      case MYSQL_TYPE_LONGLONG | MYSQL_TYPE_DOUBLE               => bytes.slice(offset, offset + 8)
      case _                                                     =>
        val lenByte = bytes(offset) & 0xff
        if lenByte <= 250 then bytes.slice(offset + 1, offset + 1 + lenByte)
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
