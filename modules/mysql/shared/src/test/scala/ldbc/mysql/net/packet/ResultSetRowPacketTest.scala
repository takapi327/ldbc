/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net.packet

import scodec.bits.{ BitVector, ByteVector }
import scodec.Attempt

import ldbc.mysql.*
import ldbc.mysql.data.*
import ldbc.mysql.net.packet.response.*

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
              TextColumnValueDecoder.extractColumn(rowPacket.rawBytes, 0, Vector.empty).map(new String(_, "UTF-8")),
              Some("1")
            )
            assertEquals(
              TextColumnValueDecoder.extractColumn(rowPacket.rawBytes, 1, Vector.empty).map(new String(_, "UTF-8")),
              Some("John")
            )
            assertEquals(
              TextColumnValueDecoder.extractColumn(rowPacket.rawBytes, 2, Vector.empty).map(new String(_, "UTF-8")),
              Some("Doe")
            )
            assertEquals(TextColumnValueDecoder.extractColumn(rowPacket.rawBytes, 3, Vector.empty), None)
            assertEquals(
              TextColumnValueDecoder.extractColumn(rowPacket.rawBytes, 4, Vector.empty).map(new String(_, "UTF-8")),
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
              TextColumnValueDecoder.extractColumn(rowPacket.rawBytes, 0, Vector.empty).map(new String(_, "UTF-8")),
              Some("1")
            )
            assertEquals(
              TextColumnValueDecoder.extractColumn(rowPacket.rawBytes, 1, Vector.empty).map(new String(_, "UTF-8")),
              Some(longString)
            )
            assertEquals(TextColumnValueDecoder.extractColumn(rowPacket.rawBytes, 2, Vector.empty), None)
            assertEquals(
              TextColumnValueDecoder.extractColumn(rowPacket.rawBytes, 3, Vector.empty).map(new String(_, "UTF-8")),
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
              TextColumnValueDecoder.extractColumn(rowPacket.rawBytes, 0, Vector.empty).map(new String(_, "UTF-8")),
              Some("1")
            )
            assertEquals(
              TextColumnValueDecoder.extractColumn(rowPacket.rawBytes, 1, Vector.empty).map(new String(_, "UTF-8")),
              Some(japaneseText)
            )
          case _ => fail("Expected ResultSetRowPacket but got something else")
        }
      case _ => fail("Decoding failed")
    }
  }

  test("a 0xFE payload short enough to be a terminator ends the result set") {
    val eof    = BitVector(Array[Byte](0xfe.toByte, 0x00, 0x00, 0x02, 0x00))
    val result = textResultSetRowDecoder(Set.empty[CapabilitiesFlags]).decode(eof)
    assert(result.isSuccessful, s"a five-byte 0xFE payload is an end-of-file packet, but got $result")
    assert(
      result.require.value.isInstanceOf[EOFPacket],
      s"expected the result set to end, got ${ result.require.value }"
    )
  }

  test("a 0xFE payload the size of a deprecated-EOF OK packet still ends the result set") {
    val ok     = BitVector(Array[Byte](0xfe.toByte, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00))
    val result = textResultSetRowDecoder(Set.empty[CapabilitiesFlags]).decode(ok)
    assert(
      result.isSuccessful && result.require.value.isInstanceOf[EOFPacket],
      s"a terminator longer than five bytes must still end the result set, got $result"
    )
  }

  test("the boundary between a terminator and a row sits at the protocol maximum") {
    val lenenc = Array[Byte](0xfe.toByte) ++ Array.fill(8)(0x00.toByte)
    def payload(size: Long): BitVector =
      (ByteVector(lenenc) ++ ByteVector.viewAt(_ => 0x61.toByte, size - lenenc.length)).toBitVector

    val atBoundary = textResultSetRowDecoder(Set.empty[CapabilitiesFlags]).decode(payload(16777215L))
    assert(
      atBoundary.isSuccessful && atBoundary.require.value.isInstanceOf[ResultSetRowPacket],
      s"a payload of exactly 16,777,215 bytes is a row, got $atBoundary"
    )

    val belowBoundary = textResultSetRowDecoder(Set.empty[CapabilitiesFlags]).decode(payload(16777214L))
    assert(
      belowBoundary.isSuccessful && belowBoundary.require.value.isInstanceOf[EOFPacket],
      s"one byte below the boundary is still a terminator, got $belowBoundary"
    )
  }

  test("a 0xFE payload too large to be a terminator is a row") {
    val lenenc = Array[Byte](0xfe.toByte) ++ Array.fill(8)(0x00.toByte)
    val row    = (ByteVector(lenenc) ++ ByteVector.viewAt(_ => 0x61.toByte, 16777216L)).toBitVector
    val result = textResultSetRowDecoder(Set.empty[CapabilitiesFlags]).decode(row)
    assert(
      result.isSuccessful && result.require.value.isInstanceOf[ResultSetRowPacket],
      s"0xFE also opens an eight-byte length prefix, so a 16MB value must not read as a terminator, got $result"
    )
  }
