/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scodec.bits.BitVector
import scodec.Attempt

import ldbc.connector.*
import ldbc.connector.data.*
import ldbc.connector.net.packet.response.*

class ResultSetRowPacketTest extends FTestPlatform:

  private def buildTestColumnDefinition(characterSet: Int, columnType: ColumnDataType): ColumnDefinition41Packet =
    ColumnDefinition41Packet("", "", "", "", "", "", 0, characterSet, 0L, columnType, Seq.empty, 0)

  test("ResultSetRowPacket creation and properties") {
    val values    = Array[Option[String]](Some("1"), Some("John"), Some("Doe"), None, Some("30"))
    val rowPacket = ResultSetRowPacket(values)

    assertEquals(rowPacket.values, values)
    assertEquals(rowPacket.values.length, 5)
    assertEquals(rowPacket.values(0), Some("1"))
    assertEquals(rowPacket.values(1), Some("John"))
    assertEquals(rowPacket.values(2), Some("Doe"))
    assertEquals(rowPacket.values(3), None)
    assertEquals(rowPacket.values(4), Some("30"))
    assertEquals(rowPacket.toString, "ProtocolText::ResultSetRow")
  }

  test("ResultSetRowPacket decoder with simple values") {
    // Create sample packet data that would be received from server
    val packetBytes = Array[Byte](
      0x01,
      '1', // first column: value "1"
      0x04,
      'J',
      'o',
      'h',
      'n', // second column: value "John"
      0x03,
      'D',
      'o',
      'e',         // third column: value "Doe"
      0xfb.toByte, // fourth column: NULL value
      0x02,
      '3',
      '0' // fifth column: value "30"
    )

    val bitVector         = BitVector(packetBytes)
    val capabilityFlags   = Set(CapabilitiesFlags.CLIENT_PROTOCOL_41)
    val columnDefinitions = Vector(
      buildTestColumnDefinition(11, ColumnDataType.MYSQL_TYPE_LONG),
      buildTestColumnDefinition(45, ColumnDataType.MYSQL_TYPE_VARCHAR),
      buildTestColumnDefinition(45, ColumnDataType.MYSQL_TYPE_VARCHAR),
      buildTestColumnDefinition(45, ColumnDataType.MYSQL_TYPE_VARCHAR),
      buildTestColumnDefinition(11, ColumnDataType.MYSQL_TYPE_LONG)
    )

    val result = ResultSetRowPacket.decoder(capabilityFlags, columnDefinitions).decode(bitVector)

    assert(result.isSuccessful)
    result match {
      case Attempt.Successful(decoded) =>
        decoded.value match {
          case rowPacket: ResultSetRowPacket =>
            assertEquals(rowPacket.values.length, 5)
            assertEquals(rowPacket.values(0), Some("1"))
            assertEquals(rowPacket.values(1), Some("John"))
            assertEquals(rowPacket.values(2), Some("Doe"))
            assertEquals(rowPacket.values(3), None)
            assertEquals(rowPacket.values(4), Some("30"))
          case _ => fail("Expected ResultSetRowPacket but got something else")
        }
      case _ => fail("Decoding failed")
    }
  }

  test("ResultSetRowPacket decoder with length-encoded strings") {
    // Create sample packet data with length-encoded strings
    val longString  = "a" * 300 // String that exceeds 251 chars
    val packetBytes = Array.concat(
      Array[Byte](0x01, '1'), // first column: value "1"
      Array[Byte](0xfc.toByte) ++ Array[Byte](0x2c, 0x01), // length indicator for 300 bytes (0x012c)
      longString.getBytes(), // actual 300 bytes of data
      Array[Byte](0xfb.toByte), // NULL value
      Array[Byte](0x03, '1', '0', '0') // value "100"
    )

    val bitVector         = BitVector(packetBytes)
    val capabilityFlags   = Set(CapabilitiesFlags.CLIENT_PROTOCOL_41)
    val columnDefinitions = Vector(
      buildTestColumnDefinition(11, ColumnDataType.MYSQL_TYPE_LONG),
      buildTestColumnDefinition(11, ColumnDataType.MYSQL_TYPE_BIT),
      buildTestColumnDefinition(11, ColumnDataType.MYSQL_TYPE_BIT),
      buildTestColumnDefinition(11, ColumnDataType.MYSQL_TYPE_LONG)
    )

    val result = ResultSetRowPacket.decoder(capabilityFlags, columnDefinitions).decode(bitVector)

    assert(result.isSuccessful)
    result match {
      case Attempt.Successful(decoded) =>
        decoded.value match {
          case rowPacket: ResultSetRowPacket =>
            assertEquals(rowPacket.values.length, 4)
            assertEquals(rowPacket.values(0), Some("1"))
            assertEquals(rowPacket.values(1), Some(longString))
            assertEquals(rowPacket.values(2), None)
            assertEquals(rowPacket.values(3), Some("100"))
          case _ => fail("Expected ResultSetRowPacket but got something else")
        }
      case _ => fail("Decoding failed")
    }
  }

  test("ResultSetRowPacket decoder with non-ASCII characters") {
    // Test with Japanese characters
    val japaneseText = "こんにちは世界"
    val bytesLength  = japaneseText.getBytes("UTF-8").length.toByte
    val packetBytes  = Array.concat(
      Array[Byte](0x01, '1'),                                    // first column: value "1"
      Array[Byte](bytesLength) ++ japaneseText.getBytes("UTF-8") // Japanese text
    )

    val bitVector       = BitVector(packetBytes)
    val capabilityFlags = Set(CapabilitiesFlags.CLIENT_PROTOCOL_41)

    val columnDefinitions = Vector(
      buildTestColumnDefinition(11, ColumnDataType.MYSQL_TYPE_LONG),
      buildTestColumnDefinition(45, ColumnDataType.MYSQL_TYPE_VARCHAR)
    )

    val result = ResultSetRowPacket.decoder(capabilityFlags, columnDefinitions).decode(bitVector)

    assert(result.isSuccessful)
    result match {
      case Attempt.Successful(decoded) =>
        decoded.value match {
          case rowPacket: ResultSetRowPacket =>
            assertEquals(rowPacket.values.length, 2)
            assertEquals(rowPacket.values(0), Some("1"))
            assertEquals(rowPacket.values(1), Some(japaneseText))
          case _ => fail("Expected ResultSetRowPacket but got something else")
        }
      case _ => fail("Decoding failed")
    }
  }
