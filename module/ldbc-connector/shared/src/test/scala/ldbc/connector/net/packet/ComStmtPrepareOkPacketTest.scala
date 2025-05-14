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
import ldbc.connector.net.packet.response.ComStmtPrepareOkPacket

class ComStmtPrepareOkPacketTest extends FTestPlatform:

  test("ComStmtPrepareOkPacket creation and properties") {
    val packet = ComStmtPrepareOkPacket(
      status          = 0x00,
      statementId     = 123456,
      numColumns      = 3,
      numParams       = 2,
      reserved1       = 0,
      warningCount    = 0,
      metadataFollows = Some(0)
    )

    assertEquals(packet.status, 0x00)
    assertEquals(packet.statementId, 123456L)
    assertEquals(packet.numColumns, 3)
    assertEquals(packet.numParams, 2)
    assertEquals(packet.reserved1, 0)
    assertEquals(packet.warningCount, 0)
    assertEquals(packet.metadataFollows, Some(0))
    assertEquals(packet.toString, "COM_STMT_PREPARE_OK Packet")
  }

  test("ComStmtPrepareOkPacket decoder without CLIENT_OPTIONAL_RESULTSET_METADATA") {
    // Create sample packet data that would be received from server
    val packetBytes = Array[Byte](
      0x00, // status (OK)
      0x40,
      0xe2.toByte,
      0x01,
      0x00, // statementId (123456 in little-endian)
      0x03,
      0x00, // numColumns (3)
      0x02,
      0x00, // numParams (2)
      0x00, // reserved1
      0x00,
      0x00, // warningCount (0)
      0x00  // metadataFollows (0)
    )

    val bitVector       = BitVector(packetBytes)
    val capabilityFlags = Set[CapabilitiesFlags]() // No CLIENT_OPTIONAL_RESULTSET_METADATA

    val result = ComStmtPrepareOkPacket.decoder(capabilityFlags).decode(bitVector)

    assert(result.isSuccessful)
    result match {
      case Attempt.Successful(decoded) =>
        val packet = decoded.value
        packet match {
          case prepareOkPacket: ComStmtPrepareOkPacket =>
            assertEquals(prepareOkPacket.status, 0x00)
            assertEquals(prepareOkPacket.statementId, 123456L)
            assertEquals(prepareOkPacket.numColumns, 3)
            assertEquals(prepareOkPacket.numParams, 2)
            assertEquals(prepareOkPacket.reserved1, 0)
            assertEquals(prepareOkPacket.warningCount, 0)
            assertEquals(prepareOkPacket.metadataFollows, Some(0))
          case _ => fail("Decoded to incorrect packet type")
        }
      case _ => fail("Decoding failed")
    }
  }

  test("ComStmtPrepareOkPacket decoder with CLIENT_OPTIONAL_RESULTSET_METADATA") {
    // Create sample packet data without metadataFollows field
    val packetBytes = Array[Byte](
      0x00, // status (OK)
      0x40,
      0xe2.toByte,
      0x01,
      0x00, // statementId (123456 in little-endian)
      0x03,
      0x00, // numColumns (3)
      0x02,
      0x00, // numParams (2)
      0x00, // reserved1
      0x00,
      0x00 // warningCount (0)
      // No metadataFollows field when CLIENT_OPTIONAL_RESULTSET_METADATA is set
    )

    val bitVector       = BitVector(packetBytes)
    val capabilityFlags = Set(CapabilitiesFlags.CLIENT_OPTIONAL_RESULTSET_METADATA)

    val result = ComStmtPrepareOkPacket.decoder(capabilityFlags).decode(bitVector)

    assert(result.isSuccessful)
    result match {
      case Attempt.Successful(decoded) =>
        val packet = decoded.value
        packet match {
          case prepareOkPacket: ComStmtPrepareOkPacket =>
            assertEquals(prepareOkPacket.status, 0x00)
            assertEquals(prepareOkPacket.statementId, 123456L)
            assertEquals(prepareOkPacket.numColumns, 3)
            assertEquals(prepareOkPacket.numParams, 2)
            assertEquals(prepareOkPacket.reserved1, 0)
            assertEquals(prepareOkPacket.warningCount, 0)
            assertEquals(prepareOkPacket.metadataFollows, None)
          case _ => fail("Decoded to incorrect packet type")
        }
      case _ => fail("Decoding failed")
    }
  }
