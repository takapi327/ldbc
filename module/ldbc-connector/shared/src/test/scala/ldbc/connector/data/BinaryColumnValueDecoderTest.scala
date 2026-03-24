/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import java.nio.{ ByteBuffer, ByteOrder }
import java.time.*

import ldbc.connector.*
import ldbc.connector.data.ColumnDataType.*

class BinaryColumnValueDecoderTest extends FTestPlatform:

  private val charset = "UTF-8"

  // ---- helper to build little-endian byte arrays ----

  private def leShort(v: Short): Array[Byte] =
    val buf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
    buf.putShort(v)
    buf.array()

  private def leInt(v: Int): Array[Byte] =
    val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
    buf.putInt(v)
    buf.array()

  private def leLong(v: Long): Array[Byte] =
    val buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
    buf.putLong(v)
    buf.array()

  private def leFloat(v: Float): Array[Byte] =
    val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
    buf.putFloat(v)
    buf.array()

  private def leDouble(v: Double): Array[Byte] =
    val buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
    buf.putDouble(v)
    buf.array()

  // =========================================================================
  // decodeString (SIGNED — isUnsigned = false)
  // =========================================================================

  test("decodeString for SIGNED MYSQL_TYPE_TINY") {
    assertEquals(BinaryColumnValueDecoder.decodeString(Array(42.toByte), charset, MYSQL_TYPE_TINY, false), "42")
    assertEquals(BinaryColumnValueDecoder.decodeString(Array(0xff.toByte), charset, MYSQL_TYPE_TINY, false), "-1")
  }

  test("decodeString for SIGNED MYSQL_TYPE_SHORT") {
    assertEquals(BinaryColumnValueDecoder.decodeString(leShort(1000), charset, MYSQL_TYPE_SHORT, false), "1000")
    assertEquals(BinaryColumnValueDecoder.decodeString(leShort(-1), charset, MYSQL_TYPE_SHORT, false), "-1")
  }

  test("decodeString for MYSQL_TYPE_YEAR") {
    assertEquals(BinaryColumnValueDecoder.decodeString(leShort(2026), charset, MYSQL_TYPE_YEAR, false), "2026")
  }

  test("decodeString for SIGNED MYSQL_TYPE_LONG") {
    assertEquals(BinaryColumnValueDecoder.decodeString(leInt(123456), charset, MYSQL_TYPE_LONG, false), "123456")
    assertEquals(BinaryColumnValueDecoder.decodeString(leInt(-1), charset, MYSQL_TYPE_LONG, false), "-1")
  }

  test("decodeString for SIGNED MYSQL_TYPE_INT24") {
    assertEquals(BinaryColumnValueDecoder.decodeString(leInt(8388607), charset, MYSQL_TYPE_INT24, false), "8388607")
  }

  test("decodeString for SIGNED MYSQL_TYPE_LONGLONG") {
    val bytes = leLong(1234567890123L)
    assertEquals(BinaryColumnValueDecoder.decodeString(bytes, charset, MYSQL_TYPE_LONGLONG, false), "1234567890123")
    assertEquals(BinaryColumnValueDecoder.decodeString(leLong(-1L), charset, MYSQL_TYPE_LONGLONG, false), "-1")
  }

  test("decodeString for MYSQL_TYPE_FLOAT") {
    assertEquals(BinaryColumnValueDecoder.decodeString(leFloat(3.14f), charset, MYSQL_TYPE_FLOAT, false), "3.14")
  }

  test("decodeString for MYSQL_TYPE_DOUBLE") {
    assertEquals(BinaryColumnValueDecoder.decodeString(leDouble(3.14159), charset, MYSQL_TYPE_DOUBLE, false), "3.14159")
  }

  test("decodeString for MYSQL_TYPE_BOOL") {
    assertEquals(BinaryColumnValueDecoder.decodeString(Array(1.toByte), charset, MYSQL_TYPE_BOOL, false), "true")
    assertEquals(BinaryColumnValueDecoder.decodeString(Array(0.toByte), charset, MYSQL_TYPE_BOOL, false), "false")
  }

  test("decodeString for string-type columns uses charset") {
    val text  = "hello"
    val bytes = text.getBytes(charset)
    assertEquals(BinaryColumnValueDecoder.decodeString(bytes, charset, MYSQL_TYPE_VARCHAR, false), "hello")
  }

  // =========================================================================
  // decodeString (UNSIGNED — isUnsigned = true)
  // =========================================================================

  test("decodeString for UNSIGNED MYSQL_TYPE_TINY 0xFF") {
    assertEquals(BinaryColumnValueDecoder.decodeString(Array(0xff.toByte), charset, MYSQL_TYPE_TINY, true), "255")
  }

  test("decodeString for UNSIGNED MYSQL_TYPE_SHORT 0xFFFF") {
    assertEquals(BinaryColumnValueDecoder.decodeString(leShort(-1), charset, MYSQL_TYPE_SHORT, true), "65535")
  }

  test("decodeString for UNSIGNED MYSQL_TYPE_LONG 0xFFFFFFFF") {
    assertEquals(BinaryColumnValueDecoder.decodeString(leInt(-1), charset, MYSQL_TYPE_LONG, true), "4294967295")
  }

  test("decodeString for UNSIGNED MYSQL_TYPE_LONGLONG 0xFFFFFFFFFFFFFFFF") {
    assertEquals(
      BinaryColumnValueDecoder.decodeString(leLong(-1L), charset, MYSQL_TYPE_LONGLONG, true),
      "18446744073709551615"
    )
  }

  test("decodeString SIGNED vs UNSIGNED consistency for MYSQL_TYPE_LONG") {
    val bytes    = leInt(-1)
    val signed   = BinaryColumnValueDecoder.decodeString(bytes, charset, MYSQL_TYPE_LONG, false)
    val unsigned = BinaryColumnValueDecoder.decodeString(bytes, charset, MYSQL_TYPE_LONG, true)
    assertEquals(signed, "-1")
    assertEquals(unsigned, "4294967295")
  }

  // =========================================================================
  // decodeBoolean
  // =========================================================================

  test("decodeBoolean for MYSQL_TYPE_BOOL") {
    assert(BinaryColumnValueDecoder.decodeBoolean(Array(1.toByte), charset, MYSQL_TYPE_BOOL, false))
    assert(!BinaryColumnValueDecoder.decodeBoolean(Array(0.toByte), charset, MYSQL_TYPE_BOOL, false))
  }

  test("decodeBoolean for MYSQL_TYPE_TINY") {
    assert(BinaryColumnValueDecoder.decodeBoolean(Array(0xff.toByte), charset, MYSQL_TYPE_TINY, false))
    assert(!BinaryColumnValueDecoder.decodeBoolean(Array(0.toByte), charset, MYSQL_TYPE_TINY, false))
  }

  test("decodeBoolean for MYSQL_TYPE_SHORT") {
    assert(BinaryColumnValueDecoder.decodeBoolean(leShort(1), charset, MYSQL_TYPE_SHORT, false))
    assert(!BinaryColumnValueDecoder.decodeBoolean(leShort(0), charset, MYSQL_TYPE_SHORT, false))
  }

  test("decodeBoolean for MYSQL_TYPE_LONG") {
    assert(BinaryColumnValueDecoder.decodeBoolean(leInt(1), charset, MYSQL_TYPE_LONG, false))
    assert(!BinaryColumnValueDecoder.decodeBoolean(leInt(0), charset, MYSQL_TYPE_LONG, false))
  }

  test("decodeBoolean for MYSQL_TYPE_LONGLONG") {
    assert(BinaryColumnValueDecoder.decodeBoolean(leLong(1L), charset, MYSQL_TYPE_LONGLONG, false))
    assert(!BinaryColumnValueDecoder.decodeBoolean(leLong(0L), charset, MYSQL_TYPE_LONGLONG, false))
  }

  test("decodeBoolean for MYSQL_TYPE_FLOAT") {
    assert(BinaryColumnValueDecoder.decodeBoolean(leFloat(1.0f), charset, MYSQL_TYPE_FLOAT, false))
    assert(!BinaryColumnValueDecoder.decodeBoolean(leFloat(0.0f), charset, MYSQL_TYPE_FLOAT, false))
  }

  test("decodeBoolean for MYSQL_TYPE_DOUBLE") {
    assert(BinaryColumnValueDecoder.decodeBoolean(leDouble(1.0), charset, MYSQL_TYPE_DOUBLE, false))
    assert(!BinaryColumnValueDecoder.decodeBoolean(leDouble(0.0), charset, MYSQL_TYPE_DOUBLE, false))
  }

  test("decodeBoolean for string fallback") {
    assert(BinaryColumnValueDecoder.decodeBoolean("true".getBytes(charset), charset, MYSQL_TYPE_VARCHAR, false))
    assert(BinaryColumnValueDecoder.decodeBoolean("1".getBytes(charset), charset, MYSQL_TYPE_VARCHAR, false))
    assert(!BinaryColumnValueDecoder.decodeBoolean("false".getBytes(charset), charset, MYSQL_TYPE_VARCHAR, false))
    assert(!BinaryColumnValueDecoder.decodeBoolean("0".getBytes(charset), charset, MYSQL_TYPE_VARCHAR, false))
  }

  // =========================================================================
  // decodeByte
  // =========================================================================

  test("decodeByte for MYSQL_TYPE_TINY") {
    assertEquals(BinaryColumnValueDecoder.decodeByte(Array(127.toByte), charset, MYSQL_TYPE_TINY, false), 127.toByte)
    assertEquals(BinaryColumnValueDecoder.decodeByte(Array(-128.toByte), charset, MYSQL_TYPE_TINY, false), -128.toByte)
  }

  test("decodeByte for MYSQL_TYPE_BIT") {
    assertEquals(
      BinaryColumnValueDecoder.decodeByte(Array(0.toByte, 1.toByte), charset, MYSQL_TYPE_BIT, false),
      1.toByte
    )
  }

  test("decodeByte for string fallback") {
    assertEquals(
      BinaryColumnValueDecoder.decodeByte("42".getBytes(charset), charset, MYSQL_TYPE_VARCHAR, false),
      42.toByte
    )
  }

  // =========================================================================
  // decodeShort
  // =========================================================================

  test("decodeShort for UNSIGNED MYSQL_TYPE_TINY") {
    assertEquals(BinaryColumnValueDecoder.decodeShort(Array(200.toByte), charset, MYSQL_TYPE_TINY, true), 200.toShort)
  }

  test("decodeShort for SIGNED MYSQL_TYPE_TINY") {
    // 0xFF as signed byte = -1
    assertEquals(BinaryColumnValueDecoder.decodeShort(Array(0xff.toByte), charset, MYSQL_TYPE_TINY, false), -1.toShort)
  }

  test("decodeShort for MYSQL_TYPE_SHORT") {
    assertEquals(BinaryColumnValueDecoder.decodeShort(leShort(32000), charset, MYSQL_TYPE_SHORT, false), 32000.toShort)
    assertEquals(BinaryColumnValueDecoder.decodeShort(leShort(-1), charset, MYSQL_TYPE_SHORT, false), -1.toShort)
  }

  test("decodeShort for MYSQL_TYPE_YEAR") {
    assertEquals(BinaryColumnValueDecoder.decodeShort(leShort(2026), charset, MYSQL_TYPE_YEAR, false), 2026.toShort)
  }

  test("decodeShort for string fallback") {
    assertEquals(
      BinaryColumnValueDecoder.decodeShort("100".getBytes(charset), charset, MYSQL_TYPE_VARCHAR, false),
      100.toShort
    )
  }

  // =========================================================================
  // decodeInt
  // =========================================================================

  test("decodeInt for SIGNED MYSQL_TYPE_TINY") {
    // isUnsigned=false: signed byte -1 → int -1
    assertEquals(BinaryColumnValueDecoder.decodeInt(Array(0xff.toByte), charset, MYSQL_TYPE_TINY, false), -1)
  }

  test("decodeInt for UNSIGNED MYSQL_TYPE_TINY") {
    assertEquals(BinaryColumnValueDecoder.decodeInt(Array(255.toByte), charset, MYSQL_TYPE_TINY, true), 255)
  }

  test("decodeInt for SIGNED MYSQL_TYPE_SHORT") {
    assertEquals(BinaryColumnValueDecoder.decodeInt(leShort(30000.toShort), charset, MYSQL_TYPE_SHORT, false), 30000)
    assertEquals(BinaryColumnValueDecoder.decodeInt(leShort(-1), charset, MYSQL_TYPE_SHORT, false), -1)
  }

  test("decodeInt for UNSIGNED MYSQL_TYPE_SHORT") {
    assertEquals(BinaryColumnValueDecoder.decodeInt(leShort(-1), charset, MYSQL_TYPE_SHORT, true), 65535)
  }

  test("decodeInt for SIGNED MYSQL_TYPE_LONG") {
    assertEquals(BinaryColumnValueDecoder.decodeInt(leInt(2147483647), charset, MYSQL_TYPE_LONG, false), 2147483647)
    assertEquals(BinaryColumnValueDecoder.decodeInt(leInt(-1), charset, MYSQL_TYPE_LONG, false), -1)
  }

  test("decodeInt for UNSIGNED MYSQL_TYPE_LONG within range") {
    assertEquals(BinaryColumnValueDecoder.decodeInt(leInt(100), charset, MYSQL_TYPE_LONG, true), 100)
  }

  test("decodeInt for UNSIGNED MYSQL_TYPE_LONG overflow throws") {
    // 0xFFFFFFFF = 4294967295 > Int.MaxValue
    intercept[NumberFormatException] {
      BinaryColumnValueDecoder.decodeInt(leInt(-1), charset, MYSQL_TYPE_LONG, true)
    }
  }

  test("decodeInt for MYSQL_TYPE_INT24") {
    assertEquals(BinaryColumnValueDecoder.decodeInt(leInt(8388607), charset, MYSQL_TYPE_INT24, false), 8388607)
  }

  test("decodeInt for MYSQL_TYPE_LONGLONG within range") {
    assertEquals(BinaryColumnValueDecoder.decodeInt(leLong(42L), charset, MYSQL_TYPE_LONGLONG, false), 42)
  }

  test("decodeInt for MYSQL_TYPE_LONGLONG overflow throws") {
    intercept[NumberFormatException] {
      BinaryColumnValueDecoder.decodeInt(leLong(5000000000L), charset, MYSQL_TYPE_LONGLONG, false)
    }
  }

  test("decodeInt for string fallback") {
    assertEquals(
      BinaryColumnValueDecoder.decodeInt("99999".getBytes(charset), charset, MYSQL_TYPE_VARCHAR, false),
      99999
    )
  }

  // =========================================================================
  // decodeLong
  // =========================================================================

  test("decodeLong for UNSIGNED MYSQL_TYPE_TINY") {
    assertEquals(BinaryColumnValueDecoder.decodeLong(Array(255.toByte), charset, MYSQL_TYPE_TINY, true), 255L)
  }

  test("decodeLong for SIGNED MYSQL_TYPE_TINY") {
    assertEquals(BinaryColumnValueDecoder.decodeLong(Array(0xff.toByte), charset, MYSQL_TYPE_TINY, false), -1L)
  }

  test("decodeLong for UNSIGNED MYSQL_TYPE_SHORT") {
    assertEquals(BinaryColumnValueDecoder.decodeLong(leShort(1000), charset, MYSQL_TYPE_SHORT, true), 1000L)
  }

  test("decodeLong for SIGNED MYSQL_TYPE_SHORT") {
    assertEquals(BinaryColumnValueDecoder.decodeLong(leShort(-1), charset, MYSQL_TYPE_SHORT, false), -1L)
  }

  test("decodeLong for UNSIGNED MYSQL_TYPE_LONG") {
    // -1 as signed int = 4294967295 as unsigned
    assertEquals(BinaryColumnValueDecoder.decodeLong(leInt(-1), charset, MYSQL_TYPE_LONG, true), 4294967295L)
  }

  test("decodeLong for SIGNED MYSQL_TYPE_LONG") {
    assertEquals(BinaryColumnValueDecoder.decodeLong(leInt(-1), charset, MYSQL_TYPE_LONG, false), -1L)
  }

  test("decodeLong for SIGNED MYSQL_TYPE_LONGLONG") {
    assertEquals(
      BinaryColumnValueDecoder.decodeLong(leLong(9223372036854775807L), charset, MYSQL_TYPE_LONGLONG, false),
      9223372036854775807L
    )
    assertEquals(BinaryColumnValueDecoder.decodeLong(leLong(-1L), charset, MYSQL_TYPE_LONGLONG, false), -1L)
  }

  test("decodeLong for UNSIGNED MYSQL_TYPE_LONGLONG within range") {
    assertEquals(
      BinaryColumnValueDecoder.decodeLong(leLong(9223372036854775807L), charset, MYSQL_TYPE_LONGLONG, true),
      9223372036854775807L
    )
  }

  test("decodeLong for UNSIGNED MYSQL_TYPE_LONGLONG overflow throws") {
    // -1L as unsigned = 18446744073709551615 > Long.MaxValue
    intercept[NumberFormatException] {
      BinaryColumnValueDecoder.decodeLong(leLong(-1L), charset, MYSQL_TYPE_LONGLONG, true)
    }
  }

  test("decodeLong for string fallback") {
    assertEquals(
      BinaryColumnValueDecoder.decodeLong("123456789".getBytes(charset), charset, MYSQL_TYPE_VARCHAR, false),
      123456789L
    )
  }

  // =========================================================================
  // decodeFloat
  // =========================================================================

  test("decodeFloat for MYSQL_TYPE_FLOAT") {
    val result = BinaryColumnValueDecoder.decodeFloat(leFloat(3.14f), charset, MYSQL_TYPE_FLOAT, false)
    assertEquals(result, 3.14f)
  }

  test("decodeFloat for string fallback") {
    val result = BinaryColumnValueDecoder.decodeFloat("2.71".getBytes(charset), charset, MYSQL_TYPE_VARCHAR, false)
    assertEquals(result, 2.71f)
  }

  // =========================================================================
  // decodeDouble
  // =========================================================================

  test("decodeDouble for MYSQL_TYPE_DOUBLE") {
    val result = BinaryColumnValueDecoder.decodeDouble(leDouble(3.141592653589793), charset, MYSQL_TYPE_DOUBLE, false)
    assertEquals(result, 3.141592653589793)
  }

  test("decodeDouble for MYSQL_TYPE_FLOAT widens to double") {
    val result = BinaryColumnValueDecoder.decodeDouble(leFloat(1.5f), charset, MYSQL_TYPE_FLOAT, false)
    assertEquals(result, 1.5f.toDouble)
  }

  test("decodeDouble for string fallback") {
    val result = BinaryColumnValueDecoder.decodeDouble("9.99".getBytes(charset), charset, MYSQL_TYPE_VARCHAR, false)
    assertEquals(result, 9.99)
  }

  // =========================================================================
  // decodeBigDecimal
  // =========================================================================

  test("decodeBigDecimal parses from string") {
    val result =
      BinaryColumnValueDecoder.decodeBigDecimal("12345.6789".getBytes(charset), charset, MYSQL_TYPE_NEWDECIMAL, false)
    assertEquals(result, BigDecimal("12345.6789"))
  }

  // =========================================================================
  // decodeBytes
  // =========================================================================

  test("decodeBytes returns raw bytes as-is") {
    val bytes = Array[Byte](0x01, 0x02, 0x03)
    assert(BinaryColumnValueDecoder.decodeBytes(bytes, charset, MYSQL_TYPE_VARCHAR, false).sameElements(bytes))
  }

  // =========================================================================
  // decodeDate
  // =========================================================================

  test("decodeDate returns null for zero-date (0 bytes)") {
    assertEquals(BinaryColumnValueDecoder.decodeDate(Array.empty[Byte], charset, MYSQL_TYPE_DATE, false), null)
  }

  test("decodeDate decodes 4-byte date") {
    // 2026-03-23: year=2026 (0x07EA LE), month=3, day=23
    val bytes = Array[Byte](0xea.toByte, 0x07, 3, 23)
    assertEquals(BinaryColumnValueDecoder.decodeDate(bytes, charset, MYSQL_TYPE_DATE, false), LocalDate.of(2026, 3, 23))
  }

  test("decodeDate returns null for unexpected length") {
    assertEquals(BinaryColumnValueDecoder.decodeDate(Array[Byte](1, 2), charset, MYSQL_TYPE_DATE, false), null)
  }

  // =========================================================================
  // decodeTimestamp
  // =========================================================================

  test("decodeTimestamp returns null for zero-datetime (0 bytes)") {
    assertEquals(
      BinaryColumnValueDecoder.decodeTimestamp(Array.empty[Byte], charset, MYSQL_TYPE_TIMESTAMP, false),
      null
    )
  }

  test("decodeTimestamp decodes 4-byte timestamp (date only, time defaults to midnight)") {
    val bytes = Array[Byte](0xea.toByte, 0x07, 3, 23)
    assertEquals(
      BinaryColumnValueDecoder.decodeTimestamp(bytes, charset, MYSQL_TYPE_TIMESTAMP, false),
      LocalDateTime.of(2026, 3, 23, 0, 0, 0, 0)
    )
  }

  test("decodeTimestamp decodes 7-byte timestamp (date + time)") {
    val bytes = Array[Byte](0xea.toByte, 0x07, 3, 23, 14, 30, 45)
    assertEquals(
      BinaryColumnValueDecoder.decodeTimestamp(bytes, charset, MYSQL_TYPE_TIMESTAMP, false),
      LocalDateTime.of(2026, 3, 23, 14, 30, 45, 0)
    )
  }

  test("decodeTimestamp decodes 11-byte timestamp (date + time + microseconds)") {
    val bytes = Array[Byte](0xea.toByte, 0x07, 3, 23, 14, 30, 45, 0x40, 0xe2.toByte, 0x01, 0x00)
    assertEquals(
      BinaryColumnValueDecoder.decodeTimestamp(bytes, charset, MYSQL_TYPE_TIMESTAMP, false),
      LocalDateTime.of(2026, 3, 23, 14, 30, 45, 123456 * 1000)
    )
  }

  test("decodeTimestamp returns null for unexpected length") {
    assertEquals(
      BinaryColumnValueDecoder.decodeTimestamp(Array[Byte](1, 2, 3), charset, MYSQL_TYPE_TIMESTAMP, false),
      null
    )
  }

  // =========================================================================
  // decodeTime
  // =========================================================================

  test("decodeTime returns null for zero-time (0 bytes)") {
    assertEquals(BinaryColumnValueDecoder.decodeTime(Array.empty[Byte], charset, MYSQL_TYPE_TIME, false), null)
  }

  test("decodeTime decodes 8-byte time") {
    val bytes = Array[Byte](0, 0, 0, 0, 0, 14, 30, 45)
    assertEquals(BinaryColumnValueDecoder.decodeTime(bytes, charset, MYSQL_TYPE_TIME, false), LocalTime.of(14, 30, 45))
  }

  test("decodeTime decodes 12-byte time with microseconds") {
    val bytes = Array[Byte](0, 0, 0, 0, 0, 14, 30, 45, 0x20, 0xa1.toByte, 0x07, 0x00)
    assertEquals(
      BinaryColumnValueDecoder.decodeTime(bytes, charset, MYSQL_TYPE_TIME, false),
      LocalTime.of(14, 30, 45, 500000 * 1000)
    )
  }

  test("decodeTime returns null for unexpected length") {
    assertEquals(BinaryColumnValueDecoder.decodeTime(Array[Byte](1, 2, 3), charset, MYSQL_TYPE_TIME, false), null)
  }

  // =========================================================================
  // extractColumn — null bitmap (unchanged, no isUnsigned param)
  // =========================================================================

  test("extractColumn returns None for null column") {
    val columnTypes = Vector(MYSQL_TYPE_TINY)
    val bytes       = Array[Byte](0x04)
    assertEquals(BinaryColumnValueDecoder.extractColumn(bytes, 0, columnTypes), None)
  }

  test("extractColumn returns Some for non-null column") {
    val columnTypes = Vector(MYSQL_TYPE_TINY)
    val bytes       = Array[Byte](0x00, 42)
    val result      = BinaryColumnValueDecoder.extractColumn(bytes, 0, columnTypes)
    assert(result.isDefined)
    assertEquals(result.get(0), 42.toByte)
  }

  test("extractColumn with multiple columns and mixed null/non-null") {
    val columnTypes = Vector(MYSQL_TYPE_TINY, MYSQL_TYPE_SHORT, MYSQL_TYPE_TINY)
    val bytes       = Array[Byte](0x08, 10, 99)
    val col0        = BinaryColumnValueDecoder.extractColumn(bytes, 0, columnTypes)
    val col1        = BinaryColumnValueDecoder.extractColumn(bytes, 1, columnTypes)
    val col2        = BinaryColumnValueDecoder.extractColumn(bytes, 2, columnTypes)
    assert(col0.isDefined)
    assertEquals(col0.get(0), 10.toByte)
    assertEquals(col1, None)
    assert(col2.isDefined)
    assertEquals(col2.get(0), 99.toByte)
  }

  test("extractColumn with fixed-width types at various positions") {
    val columnTypes = Vector(MYSQL_TYPE_TINY, MYSQL_TYPE_LONG, MYSQL_TYPE_SHORT)
    val tinyData    = Array[Byte](0x0a)
    val longData    = leInt(305419896)
    val shortData   = leShort(1000)
    val bytes       = Array.concat(Array[Byte](0x00), tinyData, longData, shortData)
    val col0        = BinaryColumnValueDecoder.extractColumn(bytes, 0, columnTypes)
    val col1        = BinaryColumnValueDecoder.extractColumn(bytes, 1, columnTypes)
    val col2        = BinaryColumnValueDecoder.extractColumn(bytes, 2, columnTypes)
    assert(col0.isDefined)
    assertEquals(col0.get(0), 0x0a.toByte)
    assert(col1.isDefined)
    assertEquals(col1.get.length, 4)
    assertEquals(ByteBuffer.wrap(col1.get).order(ByteOrder.LITTLE_ENDIAN).getInt, 305419896)
    assert(col2.isDefined)
    assertEquals(col2.get.length, 2)
    assertEquals(ByteBuffer.wrap(col2.get).order(ByteOrder.LITTLE_ENDIAN).getShort, 1000.toShort)
  }

  test("extractColumn with length-encoded string column") {
    val columnTypes = Vector(MYSQL_TYPE_TINY, MYSQL_TYPE_VARCHAR)
    val text        = "hello"
    val textBytes   = text.getBytes(charset)
    val bytes       = Array.concat(
      Array[Byte](0x00),
      Array[Byte](42),
      Array[Byte](textBytes.length.toByte),
      textBytes
    )
    val col1 = BinaryColumnValueDecoder.extractColumn(bytes, 1, columnTypes)
    assert(col1.isDefined)
    assertEquals(new String(col1.get, charset), "hello")
  }

  test("extractColumn with LONGLONG and DOUBLE columns") {
    val columnTypes = Vector(MYSQL_TYPE_LONGLONG, MYSQL_TYPE_DOUBLE)
    val longBytes   = leLong(9876543210L)
    val doubleBytes = leDouble(2.71828)
    val bytes       = Array.concat(Array[Byte](0x00), longBytes, doubleBytes)
    val col0        = BinaryColumnValueDecoder.extractColumn(bytes, 0, columnTypes)
    val col1        = BinaryColumnValueDecoder.extractColumn(bytes, 1, columnTypes)
    assert(col0.isDefined)
    assertEquals(ByteBuffer.wrap(col0.get).order(ByteOrder.LITTLE_ENDIAN).getLong, 9876543210L)
    assert(col1.isDefined)
    assertEquals(ByteBuffer.wrap(col1.get).order(ByteOrder.LITTLE_ENDIAN).getDouble, 2.71828)
  }

  test("extractColumn null bitmap spans multiple bytes") {
    val columnTypes = Vector.fill(7)(MYSQL_TYPE_TINY)
    val bytes       = Array.concat(
      Array[Byte](0x00, 0x01),
      Array[Byte](1, 2, 3, 4, 5, 6)
    )
    for i <- 0 until 6 do
      val col = BinaryColumnValueDecoder.extractColumn(bytes, i, columnTypes)
      assert(col.isDefined, s"column $i should not be null")
      assertEquals(col.get(0), (i + 1).toByte)
    assertEquals(BinaryColumnValueDecoder.extractColumn(bytes, 6, columnTypes), None)
  }

  // =========================================================================
  // Edge cases for date/time boundary values
  // =========================================================================

  test("decodeDate for year 0001-01-01") {
    val bytes = Array[Byte](0x01, 0x00, 1, 1)
    assertEquals(BinaryColumnValueDecoder.decodeDate(bytes, charset, MYSQL_TYPE_DATE, false), LocalDate.of(1, 1, 1))
  }

  test("decodeTimestamp for year 9999-12-31 23:59:59") {
    val bytes = Array[Byte](0x0f, 0x27, 12, 31, 23, 59, 59)
    assertEquals(
      BinaryColumnValueDecoder.decodeTimestamp(bytes, charset, MYSQL_TYPE_TIMESTAMP, false),
      LocalDateTime.of(9999, 12, 31, 23, 59, 59, 0)
    )
  }

  test("decodeTime with negative flag is ignored in hour/min/sec extraction") {
    val bytes = Array[Byte](1, 0, 0, 0, 0, 1, 2, 3)
    assertEquals(BinaryColumnValueDecoder.decodeTime(bytes, charset, MYSQL_TYPE_TIME, false), LocalTime.of(1, 2, 3))
  }
