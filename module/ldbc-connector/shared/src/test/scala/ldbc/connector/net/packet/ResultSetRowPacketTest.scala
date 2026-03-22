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

  test("ResultSetRowPacket rawBytes and toString") {
    val rawBytes  = Array[Byte](0x01, '1'.toByte)
    val rowPacket = ResultSetRowPacket.TextImpl(rawBytes)

    assert(rowPacket.rawBytes.sameElements(rawBytes))
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

    val bitVector       = BitVector(packetBytes)
    val capabilityFlags = Set(CapabilitiesFlags.CLIENT_PROTOCOL_41)

    val result = textResultSetRowDecoder(capabilityFlags).decode(bitVector)

    assert(result.isSuccessful)
    result match {
      case Attempt.Successful(decoded) =>
        decoded.value match {
          case rowPacket: ResultSetRowPacket =>
            assert(rowPacket.rawBytes.sameElements(packetBytes))
            // Verify column extraction
            assertEquals(
              ResultSetRowPacket.extractTextColumn(rowPacket.rawBytes, 0).map(new String(_, "UTF-8")),
              Some("1")
            )
            assertEquals(
              ResultSetRowPacket.extractTextColumn(rowPacket.rawBytes, 1).map(new String(_, "UTF-8")),
              Some("John")
            )
            assertEquals(
              ResultSetRowPacket.extractTextColumn(rowPacket.rawBytes, 2).map(new String(_, "UTF-8")),
              Some("Doe")
            )
            assertEquals(ResultSetRowPacket.extractTextColumn(rowPacket.rawBytes, 3), None)
            assertEquals(
              ResultSetRowPacket.extractTextColumn(rowPacket.rawBytes, 4).map(new String(_, "UTF-8")),
              Some("30")
            )
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

    val bitVector       = BitVector(packetBytes)
    val capabilityFlags = Set(CapabilitiesFlags.CLIENT_PROTOCOL_41)

    val result = textResultSetRowDecoder(capabilityFlags).decode(bitVector)

    assert(result.isSuccessful)
    result match {
      case Attempt.Successful(decoded) =>
        decoded.value match {
          case rowPacket: ResultSetRowPacket =>
            assertEquals(
              ResultSetRowPacket.extractTextColumn(rowPacket.rawBytes, 0).map(new String(_, "UTF-8")),
              Some("1")
            )
            assertEquals(
              ResultSetRowPacket.extractTextColumn(rowPacket.rawBytes, 1).map(new String(_, "UTF-8")),
              Some(longString)
            )
            assertEquals(ResultSetRowPacket.extractTextColumn(rowPacket.rawBytes, 2), None)
            assertEquals(
              ResultSetRowPacket.extractTextColumn(rowPacket.rawBytes, 3).map(new String(_, "UTF-8")),
              Some("100")
            )
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

    val result = textResultSetRowDecoder(capabilityFlags).decode(bitVector)

    assert(result.isSuccessful)
    result match {
      case Attempt.Successful(decoded) =>
        decoded.value match {
          case rowPacket: ResultSetRowPacket =>
            assertEquals(
              ResultSetRowPacket.extractTextColumn(rowPacket.rawBytes, 0).map(new String(_, "UTF-8")),
              Some("1")
            )
            assertEquals(
              ResultSetRowPacket.extractTextColumn(rowPacket.rawBytes, 1).map(new String(_, "UTF-8")),
              Some(japaneseText)
            )
          case _ => fail("Expected ResultSetRowPacket but got something else")
        }
      case _ => fail("Decoding failed")
    }
  }
