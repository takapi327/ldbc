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
  // decodeString
  // =========================================================================

  test("decodeString for MYSQL_TYPE_TINY") {
    assertEquals(BinaryColumnValueDecoder.decodeString(Array(42.toByte), charset, MYSQL_TYPE_TINY), "42")
    assertEquals(BinaryColumnValueDecoder.decodeString(Array(0xff.toByte), charset, MYSQL_TYPE_TINY), "255")
  }

  test("decodeString for MYSQL_TYPE_SHORT") {
    assertEquals(BinaryColumnValueDecoder.decodeString(leShort(1000), charset, MYSQL_TYPE_SHORT), "1000")
  }

  test("decodeString for MYSQL_TYPE_YEAR") {
    assertEquals(BinaryColumnValueDecoder.decodeString(leShort(2026), charset, MYSQL_TYPE_YEAR), "2026")
  }

  test("decodeString for MYSQL_TYPE_LONG") {
    assertEquals(BinaryColumnValueDecoder.decodeString(leInt(123456), charset, MYSQL_TYPE_LONG), "123456")
  }

  test("decodeString for MYSQL_TYPE_INT24") {
    assertEquals(BinaryColumnValueDecoder.decodeString(leInt(8388607), charset, MYSQL_TYPE_INT24), "8388607")
  }

  test("decodeString for MYSQL_TYPE_LONGLONG") {
    val bytes = leLong(1234567890123L)
    assertEquals(BinaryColumnValueDecoder.decodeString(bytes, charset, MYSQL_TYPE_LONGLONG), "1234567890123")
  }

  test("decodeString for MYSQL_TYPE_FLOAT") {
    assertEquals(BinaryColumnValueDecoder.decodeString(leFloat(3.14f), charset, MYSQL_TYPE_FLOAT), "3.14")
  }

  test("decodeString for MYSQL_TYPE_DOUBLE") {
    assertEquals(BinaryColumnValueDecoder.decodeString(leDouble(3.14159), charset, MYSQL_TYPE_DOUBLE), "3.14159")
  }

  test("decodeString for MYSQL_TYPE_BOOL") {
    assertEquals(BinaryColumnValueDecoder.decodeString(Array(1.toByte), charset, MYSQL_TYPE_BOOL), "true")
    assertEquals(BinaryColumnValueDecoder.decodeString(Array(0.toByte), charset, MYSQL_TYPE_BOOL), "false")
  }

  test("decodeString for string-type columns uses charset") {
    val text  = "hello"
    val bytes = text.getBytes(charset)
    assertEquals(BinaryColumnValueDecoder.decodeString(bytes, charset, MYSQL_TYPE_VARCHAR), "hello")
  }

  // =========================================================================
  // decodeBoolean
  // =========================================================================

  test("decodeBoolean for MYSQL_TYPE_BOOL") {
    assert(BinaryColumnValueDecoder.decodeBoolean(Array(1.toByte), charset, MYSQL_TYPE_BOOL))
    assert(!BinaryColumnValueDecoder.decodeBoolean(Array(0.toByte), charset, MYSQL_TYPE_BOOL))
  }

  test("decodeBoolean for MYSQL_TYPE_TINY") {
    assert(BinaryColumnValueDecoder.decodeBoolean(Array(0xff.toByte), charset, MYSQL_TYPE_TINY))
    assert(!BinaryColumnValueDecoder.decodeBoolean(Array(0.toByte), charset, MYSQL_TYPE_TINY))
  }

  test("decodeBoolean for MYSQL_TYPE_SHORT") {
    assert(BinaryColumnValueDecoder.decodeBoolean(leShort(1), charset, MYSQL_TYPE_SHORT))
    assert(!BinaryColumnValueDecoder.decodeBoolean(leShort(0), charset, MYSQL_TYPE_SHORT))
  }

  test("decodeBoolean for MYSQL_TYPE_LONG") {
    assert(BinaryColumnValueDecoder.decodeBoolean(leInt(1), charset, MYSQL_TYPE_LONG))
    assert(!BinaryColumnValueDecoder.decodeBoolean(leInt(0), charset, MYSQL_TYPE_LONG))
  }

  test("decodeBoolean for MYSQL_TYPE_LONGLONG") {
    assert(BinaryColumnValueDecoder.decodeBoolean(leLong(1L), charset, MYSQL_TYPE_LONGLONG))
    assert(!BinaryColumnValueDecoder.decodeBoolean(leLong(0L), charset, MYSQL_TYPE_LONGLONG))
  }

  test("decodeBoolean for MYSQL_TYPE_FLOAT") {
    assert(BinaryColumnValueDecoder.decodeBoolean(leFloat(1.0f), charset, MYSQL_TYPE_FLOAT))
    assert(!BinaryColumnValueDecoder.decodeBoolean(leFloat(0.0f), charset, MYSQL_TYPE_FLOAT))
  }

  test("decodeBoolean for MYSQL_TYPE_DOUBLE") {
    assert(BinaryColumnValueDecoder.decodeBoolean(leDouble(1.0), charset, MYSQL_TYPE_DOUBLE))
    assert(!BinaryColumnValueDecoder.decodeBoolean(leDouble(0.0), charset, MYSQL_TYPE_DOUBLE))
  }

  test("decodeBoolean for string fallback") {
    assert(BinaryColumnValueDecoder.decodeBoolean("true".getBytes(charset), charset, MYSQL_TYPE_VARCHAR))
    assert(BinaryColumnValueDecoder.decodeBoolean("1".getBytes(charset), charset, MYSQL_TYPE_VARCHAR))
    assert(!BinaryColumnValueDecoder.decodeBoolean("false".getBytes(charset), charset, MYSQL_TYPE_VARCHAR))
    assert(!BinaryColumnValueDecoder.decodeBoolean("0".getBytes(charset), charset, MYSQL_TYPE_VARCHAR))
  }

  // =========================================================================
  // decodeByte
  // =========================================================================

  test("decodeByte for MYSQL_TYPE_TINY") {
    assertEquals(BinaryColumnValueDecoder.decodeByte(Array(127.toByte), charset, MYSQL_TYPE_TINY), 127.toByte)
    assertEquals(BinaryColumnValueDecoder.decodeByte(Array(-128.toByte), charset, MYSQL_TYPE_TINY), -128.toByte)
  }

  test("decodeByte for MYSQL_TYPE_BIT") {
    assertEquals(BinaryColumnValueDecoder.decodeByte(Array(0.toByte, 1.toByte), charset, MYSQL_TYPE_BIT), 1.toByte)
  }

  test("decodeByte for string fallback") {
    assertEquals(BinaryColumnValueDecoder.decodeByte("42".getBytes(charset), charset, MYSQL_TYPE_VARCHAR), 42.toByte)
  }

  // =========================================================================
  // decodeShort
  // =========================================================================

  test("decodeShort for MYSQL_TYPE_TINY") {
    assertEquals(BinaryColumnValueDecoder.decodeShort(Array(200.toByte), charset, MYSQL_TYPE_TINY), 200.toShort)
  }

  test("decodeShort for MYSQL_TYPE_SHORT") {
    assertEquals(BinaryColumnValueDecoder.decodeShort(leShort(32000), charset, MYSQL_TYPE_SHORT), 32000.toShort)
    assertEquals(BinaryColumnValueDecoder.decodeShort(leShort(-1), charset, MYSQL_TYPE_SHORT), -1.toShort)
  }

  test("decodeShort for MYSQL_TYPE_YEAR") {
    assertEquals(BinaryColumnValueDecoder.decodeShort(leShort(2026), charset, MYSQL_TYPE_YEAR), 2026.toShort)
  }

  test("decodeShort for string fallback") {
    assertEquals(
      BinaryColumnValueDecoder.decodeShort("100".getBytes(charset), charset, MYSQL_TYPE_VARCHAR),
      100.toShort
    )
  }

  // =========================================================================
  // decodeInt
  // =========================================================================

  test("decodeInt for MYSQL_TYPE_TINY") {
    assertEquals(BinaryColumnValueDecoder.decodeInt(Array(255.toByte), charset, MYSQL_TYPE_TINY), 255)
  }

  test("decodeInt for MYSQL_TYPE_SHORT") {
    assertEquals(BinaryColumnValueDecoder.decodeInt(leShort(30000.toShort), charset, MYSQL_TYPE_SHORT), 30000)
  }

  test("decodeInt for MYSQL_TYPE_LONG") {
    assertEquals(BinaryColumnValueDecoder.decodeInt(leInt(2147483647), charset, MYSQL_TYPE_LONG), 2147483647)
    assertEquals(BinaryColumnValueDecoder.decodeInt(leInt(-1), charset, MYSQL_TYPE_LONG), -1)
  }

  test("decodeInt for MYSQL_TYPE_INT24") {
    assertEquals(BinaryColumnValueDecoder.decodeInt(leInt(8388607), charset, MYSQL_TYPE_INT24), 8388607)
  }

  test("decodeInt for MYSQL_TYPE_LONGLONG") {
    assertEquals(BinaryColumnValueDecoder.decodeInt(leLong(42L), charset, MYSQL_TYPE_LONGLONG), 42)
  }

  test("decodeInt for string fallback") {
    assertEquals(BinaryColumnValueDecoder.decodeInt("99999".getBytes(charset), charset, MYSQL_TYPE_VARCHAR), 99999)
  }

  // =========================================================================
  // decodeLong
  // =========================================================================

  test("decodeLong for MYSQL_TYPE_TINY") {
    assertEquals(BinaryColumnValueDecoder.decodeLong(Array(255.toByte), charset, MYSQL_TYPE_TINY), 255L)
  }

  test("decodeLong for MYSQL_TYPE_SHORT") {
    assertEquals(BinaryColumnValueDecoder.decodeLong(leShort(1000), charset, MYSQL_TYPE_SHORT), 1000L)
  }

  test("decodeLong for MYSQL_TYPE_LONG unsigned") {
    // -1 as signed int = 4294967295 as unsigned
    assertEquals(BinaryColumnValueDecoder.decodeLong(leInt(-1), charset, MYSQL_TYPE_LONG), 4294967295L)
  }

  test("decodeLong for MYSQL_TYPE_LONGLONG") {
    assertEquals(
      BinaryColumnValueDecoder.decodeLong(leLong(9223372036854775807L), charset, MYSQL_TYPE_LONGLONG),
      9223372036854775807L
    )
  }

  test("decodeLong for string fallback") {
    assertEquals(
      BinaryColumnValueDecoder.decodeLong("123456789".getBytes(charset), charset, MYSQL_TYPE_VARCHAR),
      123456789L
    )
  }

  // =========================================================================
  // decodeFloat
  // =========================================================================

  test("decodeFloat for MYSQL_TYPE_FLOAT") {
    val result = BinaryColumnValueDecoder.decodeFloat(leFloat(3.14f), charset, MYSQL_TYPE_FLOAT)
    assertEquals(result, 3.14f)
  }

  test("decodeFloat for string fallback") {
    val result = BinaryColumnValueDecoder.decodeFloat("2.71".getBytes(charset), charset, MYSQL_TYPE_VARCHAR)
    assertEquals(result, 2.71f)
  }

  // =========================================================================
  // decodeDouble
  // =========================================================================

  test("decodeDouble for MYSQL_TYPE_DOUBLE") {
    val result = BinaryColumnValueDecoder.decodeDouble(leDouble(3.141592653589793), charset, MYSQL_TYPE_DOUBLE)
    assertEquals(result, 3.141592653589793)
  }

  test("decodeDouble for MYSQL_TYPE_FLOAT widens to double") {
    val result = BinaryColumnValueDecoder.decodeDouble(leFloat(1.5f), charset, MYSQL_TYPE_FLOAT)
    assertEquals(result, 1.5f.toDouble)
  }

  test("decodeDouble for string fallback") {
    val result = BinaryColumnValueDecoder.decodeDouble("9.99".getBytes(charset), charset, MYSQL_TYPE_VARCHAR)
    assertEquals(result, 9.99)
  }

  // =========================================================================
  // decodeBigDecimal
  // =========================================================================

  test("decodeBigDecimal parses from string") {
    val result =
      BinaryColumnValueDecoder.decodeBigDecimal("12345.6789".getBytes(charset), charset, MYSQL_TYPE_NEWDECIMAL)
    assertEquals(result, BigDecimal("12345.6789"))
  }

  // =========================================================================
  // decodeBytes
  // =========================================================================

  test("decodeBytes returns raw bytes as-is") {
    val bytes = Array[Byte](0x01, 0x02, 0x03)
    assert(BinaryColumnValueDecoder.decodeBytes(bytes, charset, MYSQL_TYPE_VARCHAR).sameElements(bytes))
  }

  // =========================================================================
  // decodeDate
  // =========================================================================

  test("decodeDate returns null for zero-date (0 bytes)") {
    assertEquals(BinaryColumnValueDecoder.decodeDate(Array.empty[Byte], charset, MYSQL_TYPE_DATE), null)
  }

  test("decodeDate decodes 4-byte date") {
    // 2026-03-23: year=2026 (0x07EA LE), month=3, day=23
    val bytes = Array[Byte](0xea.toByte, 0x07, 3, 23)
    assertEquals(BinaryColumnValueDecoder.decodeDate(bytes, charset, MYSQL_TYPE_DATE), LocalDate.of(2026, 3, 23))
  }

  test("decodeDate returns null for unexpected length") {
    assertEquals(BinaryColumnValueDecoder.decodeDate(Array[Byte](1, 2), charset, MYSQL_TYPE_DATE), null)
  }

  // =========================================================================
  // decodeTimestamp
  // =========================================================================

  test("decodeTimestamp returns null for zero-datetime (0 bytes)") {
    assertEquals(BinaryColumnValueDecoder.decodeTimestamp(Array.empty[Byte], charset, MYSQL_TYPE_TIMESTAMP), null)
  }

  test("decodeTimestamp decodes 4-byte timestamp (date only, time defaults to midnight)") {
    val bytes = Array[Byte](0xea.toByte, 0x07, 3, 23)
    assertEquals(
      BinaryColumnValueDecoder.decodeTimestamp(bytes, charset, MYSQL_TYPE_TIMESTAMP),
      LocalDateTime.of(2026, 3, 23, 0, 0, 0, 0)
    )
  }

  test("decodeTimestamp decodes 7-byte timestamp (date + time)") {
    // 2026-03-23 14:30:45
    val bytes = Array[Byte](0xea.toByte, 0x07, 3, 23, 14, 30, 45)
    assertEquals(
      BinaryColumnValueDecoder.decodeTimestamp(bytes, charset, MYSQL_TYPE_TIMESTAMP),
      LocalDateTime.of(2026, 3, 23, 14, 30, 45, 0)
    )
  }

  test("decodeTimestamp decodes 11-byte timestamp (date + time + microseconds)") {
    // 2026-03-23 14:30:45.123456
    // microsecond = 123456 = 0x0001E240 LE => 0x40 0xE2 0x01 0x00
    val bytes = Array[Byte](0xea.toByte, 0x07, 3, 23, 14, 30, 45, 0x40, 0xe2.toByte, 0x01, 0x00)
    assertEquals(
      BinaryColumnValueDecoder.decodeTimestamp(bytes, charset, MYSQL_TYPE_TIMESTAMP),
      LocalDateTime.of(2026, 3, 23, 14, 30, 45, 123456 * 1000)
    )
  }

  test("decodeTimestamp returns null for unexpected length") {
    assertEquals(BinaryColumnValueDecoder.decodeTimestamp(Array[Byte](1, 2, 3), charset, MYSQL_TYPE_TIMESTAMP), null)
  }

  // =========================================================================
  // decodeTime
  // =========================================================================

  test("decodeTime returns null for zero-time (0 bytes)") {
    assertEquals(BinaryColumnValueDecoder.decodeTime(Array.empty[Byte], charset, MYSQL_TYPE_TIME), null)
  }

  test("decodeTime decodes 8-byte time") {
    // isNeg=0, days=0(4LE), hour=14, min=30, sec=45
    val bytes = Array[Byte](0, 0, 0, 0, 0, 14, 30, 45)
    assertEquals(BinaryColumnValueDecoder.decodeTime(bytes, charset, MYSQL_TYPE_TIME), LocalTime.of(14, 30, 45))
  }

  test("decodeTime decodes 12-byte time with microseconds") {
    // isNeg=0, days=0(4LE), hour=14, min=30, sec=45, microsecond=500000 (0x0007A120 LE => 0x20 0xA1 0x07 0x00)
    val bytes = Array[Byte](0, 0, 0, 0, 0, 14, 30, 45, 0x20, 0xa1.toByte, 0x07, 0x00)
    assertEquals(
      BinaryColumnValueDecoder.decodeTime(bytes, charset, MYSQL_TYPE_TIME),
      LocalTime.of(14, 30, 45, 500000 * 1000)
    )
  }

  test("decodeTime returns null for unexpected length") {
    assertEquals(BinaryColumnValueDecoder.decodeTime(Array[Byte](1, 2, 3), charset, MYSQL_TYPE_TIME), null)
  }

  // =========================================================================
  // extractColumn — null bitmap
  // =========================================================================

  test("extractColumn returns None for null column") {
    // 1 column of type TINY. Null bitmap size = (1+7+2)/8 = 1 byte.
    // Column 0 null bit: bit (0+2)%8 = bit 2. So bitmap byte = 0x04.
    val columnTypes = Vector(MYSQL_TYPE_TINY)
    val bytes       = Array[Byte](0x04) // null bitmap only, column 0 is null
    assertEquals(BinaryColumnValueDecoder.extractColumn(bytes, 0, columnTypes), None)
  }

  test("extractColumn returns Some for non-null column") {
    // 1 column of type TINY. Null bitmap = 0x00 (not null). Then 1 byte of data.
    val columnTypes = Vector(MYSQL_TYPE_TINY)
    val bytes       = Array[Byte](0x00, 42)
    val result      = BinaryColumnValueDecoder.extractColumn(bytes, 0, columnTypes)
    assert(result.isDefined)
    assertEquals(result.get(0), 42.toByte)
  }

  test("extractColumn with multiple columns and mixed null/non-null") {
    // 3 columns: TINY, SHORT, TINY
    // Null bitmap size = (3+7+2)/8 = 1 byte (covers bits 2,3,4)
    // Column 0 (bit 2): not null
    // Column 1 (bit 3): null — bitmap = 0x08
    // Column 2 (bit 4): not null
    val columnTypes = Vector(MYSQL_TYPE_TINY, MYSQL_TYPE_SHORT, MYSQL_TYPE_TINY)
    val bytes = Array[Byte](
      0x08,       // null bitmap: column 1 is null (bit 3)
      10,         // column 0 data (TINY = 1 byte)
      99          // column 2 data (TINY = 1 byte); column 1 skipped because null
    )
    val col0 = BinaryColumnValueDecoder.extractColumn(bytes, 0, columnTypes)
    val col1 = BinaryColumnValueDecoder.extractColumn(bytes, 1, columnTypes)
    val col2 = BinaryColumnValueDecoder.extractColumn(bytes, 2, columnTypes)

    assert(col0.isDefined)
    assertEquals(col0.get(0), 10.toByte)
    assertEquals(col1, None)
    assert(col2.isDefined)
    assertEquals(col2.get(0), 99.toByte)
  }

  test("extractColumn with fixed-width types at various positions") {
    // 3 columns: TINY(1), LONG(4), SHORT(2)
    // Null bitmap size = (3+7+2)/8 = 1 byte. All non-null => bitmap = 0x00.
    val columnTypes = Vector(MYSQL_TYPE_TINY, MYSQL_TYPE_LONG, MYSQL_TYPE_SHORT)
    val tinyData    = Array[Byte](0x0a)              // 10
    val longData    = leInt(305419896)                // 0x12345678
    val shortData   = leShort(1000)
    val bytes       = Array.concat(Array[Byte](0x00), tinyData, longData, shortData)

    val col0 = BinaryColumnValueDecoder.extractColumn(bytes, 0, columnTypes)
    val col1 = BinaryColumnValueDecoder.extractColumn(bytes, 1, columnTypes)
    val col2 = BinaryColumnValueDecoder.extractColumn(bytes, 2, columnTypes)

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
    // 2 columns: TINY, VARCHAR (length-encoded)
    // Null bitmap = 0x00 (both non-null).
    val columnTypes = Vector(MYSQL_TYPE_TINY, MYSQL_TYPE_VARCHAR)
    val text        = "hello"
    val textBytes   = text.getBytes(charset)
    // length-encoded: length prefix (1 byte for len<=250) + data
    val bytes = Array.concat(
      Array[Byte](0x00),                        // null bitmap
      Array[Byte](42),                           // column 0: TINY
      Array[Byte](textBytes.length.toByte),      // column 1 length prefix
      textBytes                                  // column 1 data
    )

    val col1 = BinaryColumnValueDecoder.extractColumn(bytes, 1, columnTypes)
    assert(col1.isDefined)
    assertEquals(new String(col1.get, charset), "hello")
  }

  test("extractColumn with LONGLONG and DOUBLE columns") {
    // 2 columns: LONGLONG(8), DOUBLE(8)
    // Null bitmap size = (2+7+2)/8 = 1 byte. All non-null.
    val columnTypes = Vector(MYSQL_TYPE_LONGLONG, MYSQL_TYPE_DOUBLE)
    val longBytes   = leLong(9876543210L)
    val doubleBytes = leDouble(2.71828)
    val bytes       = Array.concat(Array[Byte](0x00), longBytes, doubleBytes)

    val col0 = BinaryColumnValueDecoder.extractColumn(bytes, 0, columnTypes)
    val col1 = BinaryColumnValueDecoder.extractColumn(bytes, 1, columnTypes)

    assert(col0.isDefined)
    assertEquals(ByteBuffer.wrap(col0.get).order(ByteOrder.LITTLE_ENDIAN).getLong, 9876543210L)

    assert(col1.isDefined)
    assertEquals(ByteBuffer.wrap(col1.get).order(ByteOrder.LITTLE_ENDIAN).getDouble, 2.71828)
  }

  test("extractColumn null bitmap spans multiple bytes") {
    // 7 columns (all TINY), null bitmap size = (7+7+2)/8 = 2 bytes
    // Make column 6 (bit 8, i.e. bit 0 of second byte) null
    val columnTypes = Vector.fill(7)(MYSQL_TYPE_TINY)
    // bit positions: col0=bit2, col1=bit3, ..., col5=bit7, col6=bit0 of byte 1
    // Null column 6: second bitmap byte has bit 0 set => 0x01
    val bytes = Array.concat(
      Array[Byte](0x00, 0x01),             // null bitmap: column 6 is null
      Array[Byte](1, 2, 3, 4, 5, 6)       // data for columns 0-5 (column 6 is null, no data)
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
    assertEquals(BinaryColumnValueDecoder.decodeDate(bytes, charset, MYSQL_TYPE_DATE), LocalDate.of(1, 1, 1))
  }

  test("decodeTimestamp for year 9999-12-31 23:59:59") {
    // year=9999 (0x270F LE => 0x0F 0x27)
    val bytes = Array[Byte](0x0f, 0x27, 12, 31, 23, 59, 59)
    assertEquals(
      BinaryColumnValueDecoder.decodeTimestamp(bytes, charset, MYSQL_TYPE_TIMESTAMP),
      LocalDateTime.of(9999, 12, 31, 23, 59, 59, 0)
    )
  }

  test("decodeTime with negative flag is ignored in hour/min/sec extraction") {
    // isNeg=1, days=0(4LE), hour=1, min=2, sec=3
    val bytes = Array[Byte](1, 0, 0, 0, 0, 1, 2, 3)
    assertEquals(BinaryColumnValueDecoder.decodeTime(bytes, charset, MYSQL_TYPE_TIME), LocalTime.of(1, 2, 3))
  }
