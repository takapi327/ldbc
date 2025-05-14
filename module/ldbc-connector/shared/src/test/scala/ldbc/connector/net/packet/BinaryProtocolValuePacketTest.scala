/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scala.collection.immutable.ListMap

import ldbc.connector.*
import ldbc.connector.data.*
import ldbc.connector.net.packet.request.BinaryProtocolValuePacket

class BinaryProtocolValuePacketTest extends FTestPlatform:

  test("BinaryProtocolValuePacket creation and properties") {
    val values = ListMap(
      ColumnDataType.MYSQL_TYPE_TINY   -> 42.toByte,
      ColumnDataType.MYSQL_TYPE_STRING -> "test"
    )

    val packet = BinaryProtocolValuePacket(values)

    assertEquals(packet.values, values)
    assertEquals(packet.toString.contains("BinaryProtocolValue"), true)
  }

  test("BinaryProtocolValuePacket encoder with primitive types") {
    val values = ListMap(
      ColumnDataType.MYSQL_TYPE_TINY     -> 42.toByte,
      ColumnDataType.MYSQL_TYPE_SHORT    -> 12345.toShort,
      ColumnDataType.MYSQL_TYPE_LONG     -> 1234567,
      ColumnDataType.MYSQL_TYPE_LONGLONG -> 123456789012L
    )

    val packet  = BinaryProtocolValuePacket(values)
    val encoded = packet.encode

    // Check if packet was encoded correctly
    val bytes = encoded.bytes.toArray

    // First byte should be 42 (MYSQL_TYPE_TINY value)
    assertEquals(bytes(0).toInt & 0xff, 42)

    // Next two bytes should be 12345 (MYSQL_TYPE_SHORT value) in little endian
    val shortValue = (bytes(1) & 0xff) | ((bytes(2) & 0xff) << 8)
    assertEquals(shortValue, 12345)

    // Next four bytes should be 1234567 (MYSQL_TYPE_LONG value) in little endian
    val longValue = (bytes(3) & 0xff) |
      ((bytes(4) & 0xff) << 8) |
      ((bytes(5) & 0xff) << 16) |
      ((bytes(6) & 0xff) << 24)
    assertEquals(longValue, 1234567)

    // Next eight bytes should be 123456789012 (MYSQL_TYPE_LONGLONG value) in little endian
    val longlongValue = (bytes(7) & 0xffL) |
      ((bytes(8) & 0xffL) << 8) |
      ((bytes(9) & 0xffL) << 16) |
      ((bytes(10) & 0xffL) << 24) |
      ((bytes(11) & 0xffL) << 32) |
      ((bytes(12) & 0xffL) << 40) |
      ((bytes(13) & 0xffL) << 48) |
      ((bytes(14) & 0xffL) << 56)
    assertEquals(longlongValue, 123456789012L)
  }

  test("BinaryProtocolValuePacket encoder with string types") {
    val testString = "Hello MySQL"
    val values = ListMap(
      ColumnDataType.MYSQL_TYPE_STRING -> testString
    )

    val packet  = BinaryProtocolValuePacket(values)
    val encoded = packet.encode

    // Convert encoded data to bytes
    val bytes = encoded.bytes.toArray

    // Check if string was encoded correctly
    val stringBytes = testString.getBytes("UTF-8")
    assertEquals(bytes.length, stringBytes.length)

    // Compare each byte of encoded string
    for i <- stringBytes.indices do {
      assertEquals(bytes(i), stringBytes(i))
    }
  }

  test("BinaryProtocolValuePacket encoder with boolean") {
    // Test with true
    val trueValues  = ListMap(ColumnDataType.MYSQL_TYPE_TINY -> true)
    val truePacket  = BinaryProtocolValuePacket(trueValues)
    val trueEncoded = truePacket.encode
    assertEquals(trueEncoded.bytes.toArray(0), 1.toByte)

    // Test with false
    val falseValues  = ListMap(ColumnDataType.MYSQL_TYPE_TINY -> false)
    val falsePacket  = BinaryProtocolValuePacket(falseValues)
    val falseEncoded = falsePacket.encode
    assertEquals(falseEncoded.bytes.toArray(0), 0.toByte)
  }

  test("BinaryProtocolValuePacket encoder with date and time") {
    import java.time.{ LocalDate, LocalTime, LocalDateTime }

    val date     = LocalDate.of(2023, 7, 15)
    val time     = LocalTime.of(14, 30, 45)
    val dateTime = LocalDateTime.of(2023, 7, 15, 14, 30, 45)

    val values = ListMap(
      ColumnDataType.MYSQL_TYPE_DATE     -> date,
      ColumnDataType.MYSQL_TYPE_TIME     -> time,
      ColumnDataType.MYSQL_TYPE_DATETIME -> dateTime
    )

    val packet  = BinaryProtocolValuePacket(values)
    val encoded = packet.encode

    // Verify encoding happened without exceptions
    // Actual value verification would require decoding logic
    assert(encoded.nonEmpty)
  }

  test("BinaryProtocolValuePacket encoder with NULL type") {
    val values = ListMap(
      ColumnDataType.MYSQL_TYPE_NULL -> null
    )

    val packet  = BinaryProtocolValuePacket(values)
    val encoded = packet.encode

    // NULL type should not add any bytes
    assertEquals(encoded.isEmpty, true)
  }

  test("BinaryProtocolValuePacket encoder with multiple types") {
    val values = ListMap(
      ColumnDataType.MYSQL_TYPE_NULL   -> null,
      ColumnDataType.MYSQL_TYPE_TINY   -> 42.toByte,
      ColumnDataType.MYSQL_TYPE_STRING -> "test"
    )

    val packet  = BinaryProtocolValuePacket(values)
    val encoded = packet.encode

    // Check if packet was encoded correctly
    val bytes = encoded.bytes.toArray

    // First byte should be 42 (MYSQL_TYPE_TINY value)
    assertEquals(bytes(0).toInt & 0xff, 42)

    // Next four bytes should be "test" (MYSQL_TYPE_STRING value)
    val stringBytes = "test".getBytes("UTF-8")
    for i <- stringBytes.indices do {
      assertEquals(bytes(1 + i), stringBytes(i))
    }
  }
