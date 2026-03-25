/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import java.time.*

import ldbc.connector.*
import ldbc.connector.data.ColumnDataType.*

class TextColumnValueDecoderTest extends FTestPlatform:

  private val charset = "UTF-8"

  /** Helper: encode a string as UTF-8 bytes (simulating text protocol wire data). */
  private def bytes(s: String): Array[Byte] = s.getBytes(charset)

  // =========================================================================
  // decodeString
  // =========================================================================

  test("decodeString returns the string as-is") {
    assertEquals(TextColumnValueDecoder.decodeString(bytes("hello"), charset, MYSQL_TYPE_VARCHAR, false), "hello")
  }

  test("decodeString with numeric value") {
    assertEquals(TextColumnValueDecoder.decodeString(bytes("12345"), charset, MYSQL_TYPE_LONG, false), "12345")
  }

  test("decodeString with empty string") {
    assertEquals(TextColumnValueDecoder.decodeString(bytes(""), charset, MYSQL_TYPE_VARCHAR, false), "")
  }

  test("decodeString with non-ASCII characters") {
    assertEquals(TextColumnValueDecoder.decodeString(bytes("日本語"), charset, MYSQL_TYPE_VARCHAR, false), "日本語")
  }

  // =========================================================================
  // decodeBoolean
  // =========================================================================

  test("decodeBoolean returns true for \"true\"") {
    assert(TextColumnValueDecoder.decodeBoolean(bytes("true"), charset, MYSQL_TYPE_VARCHAR, false))
  }

  test("decodeBoolean returns true for \"1\"") {
    assert(TextColumnValueDecoder.decodeBoolean(bytes("1"), charset, MYSQL_TYPE_VARCHAR, false))
  }

  test("decodeBoolean returns false for \"false\"") {
    assert(!TextColumnValueDecoder.decodeBoolean(bytes("false"), charset, MYSQL_TYPE_VARCHAR, false))
  }

  test("decodeBoolean returns false for \"0\"") {
    assert(!TextColumnValueDecoder.decodeBoolean(bytes("0"), charset, MYSQL_TYPE_VARCHAR, false))
  }

  test("decodeBoolean returns false for arbitrary string") {
    assert(!TextColumnValueDecoder.decodeBoolean(bytes("yes"), charset, MYSQL_TYPE_VARCHAR, false))
  }

  // =========================================================================
  // decodeByte
  // =========================================================================

  test("decodeByte parses numeric string") {
    assertEquals(TextColumnValueDecoder.decodeByte(bytes("42"), charset, MYSQL_TYPE_TINY, false), 42.toByte)
  }

  test("decodeByte parses negative numeric string") {
    assertEquals(TextColumnValueDecoder.decodeByte(bytes("-1"), charset, MYSQL_TYPE_TINY, false), -1.toByte)
  }

  test("decodeByte parses max byte value") {
    assertEquals(TextColumnValueDecoder.decodeByte(bytes("127"), charset, MYSQL_TYPE_TINY, false), 127.toByte)
  }

  test("decodeByte parses min byte value") {
    assertEquals(TextColumnValueDecoder.decodeByte(bytes("-128"), charset, MYSQL_TYPE_TINY, false), -128.toByte)
  }

  test("decodeByte returns byte value of single non-digit character") {
    // Single non-digit character → getBytes().head
    assertEquals(TextColumnValueDecoder.decodeByte(bytes("A"), charset, MYSQL_TYPE_TINY, false), 65.toByte)
  }

  test("decodeByte parses single digit character as number") {
    assertEquals(TextColumnValueDecoder.decodeByte(bytes("5"), charset, MYSQL_TYPE_TINY, false), 5.toByte)
  }

  test("decodeByte throws for overflow value") {
    intercept[NumberFormatException] {
      TextColumnValueDecoder.decodeByte(bytes("128"), charset, MYSQL_TYPE_TINY, false)
    }
  }

  // =========================================================================
  // decodeShort
  // =========================================================================

  test("decodeShort parses positive value") {
    assertEquals(TextColumnValueDecoder.decodeShort(bytes("32000"), charset, MYSQL_TYPE_SHORT, false), 32000.toShort)
  }

  test("decodeShort parses negative value") {
    assertEquals(TextColumnValueDecoder.decodeShort(bytes("-1"), charset, MYSQL_TYPE_SHORT, false), -1.toShort)
  }

  test("decodeShort parses max short value") {
    assertEquals(TextColumnValueDecoder.decodeShort(bytes("32767"), charset, MYSQL_TYPE_SHORT, false), Short.MaxValue)
  }

  test("decodeShort throws for overflow value") {
    intercept[NumberFormatException] {
      TextColumnValueDecoder.decodeShort(bytes("32768"), charset, MYSQL_TYPE_SHORT, false)
    }
  }

  // =========================================================================
  // decodeInt
  // =========================================================================

  test("decodeInt parses positive value") {
    assertEquals(TextColumnValueDecoder.decodeInt(bytes("123456"), charset, MYSQL_TYPE_LONG, false), 123456)
  }

  test("decodeInt parses negative value") {
    assertEquals(TextColumnValueDecoder.decodeInt(bytes("-1"), charset, MYSQL_TYPE_LONG, false), -1)
  }

  test("decodeInt parses max int value") {
    assertEquals(TextColumnValueDecoder.decodeInt(bytes("2147483647"), charset, MYSQL_TYPE_LONG, false), Int.MaxValue)
  }

  test("decodeInt throws for UNSIGNED INT max (overflow)") {
    intercept[NumberFormatException] {
      TextColumnValueDecoder.decodeInt(bytes("4294967295"), charset, MYSQL_TYPE_LONG, true)
    }
  }

  // =========================================================================
  // decodeLong
  // =========================================================================

  test("decodeLong parses positive value") {
    assertEquals(TextColumnValueDecoder.decodeLong(bytes("1234567890123"), charset, MYSQL_TYPE_LONGLONG, false), 1234567890123L)
  }

  test("decodeLong parses negative value") {
    assertEquals(TextColumnValueDecoder.decodeLong(bytes("-1"), charset, MYSQL_TYPE_LONGLONG, false), -1L)
  }

  test("decodeLong parses max long value") {
    assertEquals(
      TextColumnValueDecoder.decodeLong(bytes("9223372036854775807"), charset, MYSQL_TYPE_LONGLONG, false),
      Long.MaxValue
    )
  }

  test("decodeLong throws for UNSIGNED BIGINT max (overflow)") {
    intercept[NumberFormatException] {
      TextColumnValueDecoder.decodeLong(bytes("18446744073709551615"), charset, MYSQL_TYPE_LONGLONG, true)
    }
  }

  // =========================================================================
  // decodeFloat
  // =========================================================================

  test("decodeFloat parses value") {
    assertEquals(TextColumnValueDecoder.decodeFloat(bytes("3.14"), charset, MYSQL_TYPE_FLOAT, false), 3.14f)
  }

  test("decodeFloat parses integer string") {
    assertEquals(TextColumnValueDecoder.decodeFloat(bytes("100"), charset, MYSQL_TYPE_FLOAT, false), 100.0f)
  }

  test("decodeFloat parses negative value") {
    assertEquals(TextColumnValueDecoder.decodeFloat(bytes("-2.5"), charset, MYSQL_TYPE_FLOAT, false), -2.5f)
  }

  // =========================================================================
  // decodeDouble
  // =========================================================================

  test("decodeDouble parses value") {
    assertEquals(TextColumnValueDecoder.decodeDouble(bytes("3.141592653589793"), charset, MYSQL_TYPE_DOUBLE, false), 3.141592653589793)
  }

  test("decodeDouble parses integer string") {
    assertEquals(TextColumnValueDecoder.decodeDouble(bytes("100"), charset, MYSQL_TYPE_DOUBLE, false), 100.0)
  }

  test("decodeDouble parses negative value") {
    assertEquals(TextColumnValueDecoder.decodeDouble(bytes("-9.99"), charset, MYSQL_TYPE_DOUBLE, false), -9.99)
  }

  // =========================================================================
  // decodeBigDecimal
  // =========================================================================

  test("decodeBigDecimal parses decimal string") {
    assertEquals(
      TextColumnValueDecoder.decodeBigDecimal(bytes("12345.6789"), charset, MYSQL_TYPE_NEWDECIMAL, false),
      BigDecimal("12345.6789")
    )
  }

  test("decodeBigDecimal parses large value") {
    assertEquals(
      TextColumnValueDecoder.decodeBigDecimal(bytes("99999999999999999999.99"), charset, MYSQL_TYPE_NEWDECIMAL, false),
      BigDecimal("99999999999999999999.99")
    )
  }

  test("decodeBigDecimal parses negative value") {
    assertEquals(
      TextColumnValueDecoder.decodeBigDecimal(bytes("-0.001"), charset, MYSQL_TYPE_NEWDECIMAL, false),
      BigDecimal("-0.001")
    )
  }

  // =========================================================================
  // decodeBytes
  // =========================================================================

  test("decodeBytes returns raw bytes as-is") {
    val raw = Array[Byte](0x00, 0x01, 0xff.toByte, 0x80.toByte)
    assert(TextColumnValueDecoder.decodeBytes(raw, charset, MYSQL_TYPE_BLOB, false).sameElements(raw))
  }

  test("decodeBytes preserves bytes that are not valid UTF-8") {
    val invalidUtf8 = Array[Byte](0xc0.toByte, 0xaf.toByte)
    val result      = TextColumnValueDecoder.decodeBytes(invalidUtf8, charset, MYSQL_TYPE_BLOB, false)
    assert(result.sameElements(invalidUtf8))
  }

  // =========================================================================
  // decodeDate
  // =========================================================================

  test("decodeDate parses yyyy-MM-dd format") {
    assertEquals(
      TextColumnValueDecoder.decodeDate(bytes("2026-03-25"), charset, MYSQL_TYPE_DATE, false),
      LocalDate.of(2026, 3, 25)
    )
  }

  test("decodeDate parses min date") {
    assertEquals(
      TextColumnValueDecoder.decodeDate(bytes("0001-01-01"), charset, MYSQL_TYPE_DATE, false),
      LocalDate.of(1, 1, 1)
    )
  }

  test("decodeDate parses max date") {
    assertEquals(
      TextColumnValueDecoder.decodeDate(bytes("9999-12-31"), charset, MYSQL_TYPE_DATE, false),
      LocalDate.of(9999, 12, 31)
    )
  }

  test("decodeDate throws for invalid format") {
    intercept[java.time.format.DateTimeParseException] {
      TextColumnValueDecoder.decodeDate(bytes("not-a-date"), charset, MYSQL_TYPE_DATE, false)
    }
  }

  // =========================================================================
  // decodeTime
  // =========================================================================

  test("decodeTime parses HH:mm:ss format") {
    assertEquals(
      TextColumnValueDecoder.decodeTime(bytes("14:30:45"), charset, MYSQL_TYPE_TIME, false),
      LocalTime.of(14, 30, 45)
    )
  }

  test("decodeTime parses time with microseconds") {
    assertEquals(
      TextColumnValueDecoder.decodeTime(bytes("14:30:45.123456"), charset, MYSQL_TYPE_TIME, false),
      LocalTime.of(14, 30, 45, 123456000)
    )
  }

  test("decodeTime parses midnight") {
    assertEquals(
      TextColumnValueDecoder.decodeTime(bytes("00:00:00"), charset, MYSQL_TYPE_TIME, false),
      LocalTime.of(0, 0, 0)
    )
  }

  test("decodeTime throws for invalid format") {
    intercept[java.time.format.DateTimeParseException] {
      TextColumnValueDecoder.decodeTime(bytes("not-a-time"), charset, MYSQL_TYPE_TIME, false)
    }
  }

  // =========================================================================
  // decodeTimestamp
  // =========================================================================

  test("decodeTimestamp parses yyyy-MM-dd HH:mm:ss format") {
    assertEquals(
      TextColumnValueDecoder.decodeTimestamp(bytes("2026-03-25 14:30:45"), charset, MYSQL_TYPE_TIMESTAMP, false),
      LocalDateTime.of(2026, 3, 25, 14, 30, 45)
    )
  }

  test("decodeTimestamp parses datetime with microseconds") {
    assertEquals(
      TextColumnValueDecoder.decodeTimestamp(bytes("2026-03-25 14:30:45.123456"), charset, MYSQL_TYPE_TIMESTAMP, false),
      LocalDateTime.of(2026, 3, 25, 14, 30, 45, 123456000)
    )
  }

  test("decodeTimestamp parses midnight datetime") {
    assertEquals(
      TextColumnValueDecoder.decodeTimestamp(bytes("2026-03-25 00:00:00"), charset, MYSQL_TYPE_TIMESTAMP, false),
      LocalDateTime.of(2026, 3, 25, 0, 0, 0)
    )
  }

  test("decodeTimestamp throws for invalid format") {
    intercept[java.time.format.DateTimeParseException] {
      TextColumnValueDecoder.decodeTimestamp(bytes("not-a-timestamp"), charset, MYSQL_TYPE_TIMESTAMP, false)
    }
  }

  // =========================================================================
  // extractColumn — text protocol length-encoded strings
  // =========================================================================

  test("extractColumn returns None for NULL (0xFB)") {
    val row = Array[Byte](0xfb.toByte)
    assertEquals(TextColumnValueDecoder.extractColumn(row, 0, Vector.empty), None)
  }

  test("extractColumn extracts first column") {
    val text     = "hello"
    val textData = text.getBytes(charset)
    val row      = Array[Byte](textData.length.toByte) ++ textData
    val result   = TextColumnValueDecoder.extractColumn(row, 0, Vector.empty)
    assert(result.isDefined)
    assertEquals(new String(result.get, charset), "hello")
  }

  test("extractColumn extracts second column skipping first") {
    val col0Data = "abc".getBytes(charset)
    val col1Data = "xyz".getBytes(charset)
    val row      = Array[Byte](col0Data.length.toByte) ++ col0Data ++ Array[Byte](col1Data.length.toByte) ++ col1Data
    val result   = TextColumnValueDecoder.extractColumn(row, 1, Vector.empty)
    assert(result.isDefined)
    assertEquals(new String(result.get, charset), "xyz")
  }

  test("extractColumn skips NULL column correctly") {
    val col1Data = "data".getBytes(charset)
    // column 0 = NULL (0xFB), column 1 = "data"
    val row    = Array[Byte](0xfb.toByte, col1Data.length.toByte) ++ col1Data
    val result = TextColumnValueDecoder.extractColumn(row, 1, Vector.empty)
    assert(result.isDefined)
    assertEquals(new String(result.get, charset), "data")
  }

  test("extractColumn handles empty string (length = 0)") {
    val row    = Array[Byte](0x00)
    val result = TextColumnValueDecoder.extractColumn(row, 0, Vector.empty)
    assert(result.isDefined)
    assertEquals(result.get.length, 0)
  }

  test("extractColumn handles 2-byte length prefix (0xFC)") {
    // Create a string of 256 bytes (exceeds single-byte length prefix max of 250)
    val data = Array.fill[Byte](256)(0x41) // 256 'A's
    // 0xFC prefix + 2-byte LE length (256 = 0x00 0x01)
    val row    = Array[Byte](0xfc.toByte, 0x00, 0x01) ++ data
    val result = TextColumnValueDecoder.extractColumn(row, 0, Vector.empty)
    assert(result.isDefined)
    assertEquals(result.get.length, 256)
  }
