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
import ldbc.connector.net.packet.response.OKPacket

class OKPacketTest extends FTestPlatform:

  test("OKPacket creation and properties") {
    val okPacket = OKPacket(
      status           = OKPacket.STATUS,
      affectedRows     = 5,
      lastInsertId     = 10,
      statusFlags      = Set(ServerStatusFlags.SERVER_STATUS_AUTOCOMMIT),
      warnings         = Some(0),
      info             = Some("OK"),
      sessionStateInfo = None,
      msg              = None
    )

    assertEquals(okPacket.status, 0x00)
    assertEquals(okPacket.affectedRows, 5L)
    assertEquals(okPacket.lastInsertId, 10L)
    assertEquals(okPacket.statusFlags, Set(ServerStatusFlags.SERVER_STATUS_AUTOCOMMIT))
    assertEquals(okPacket.warnings, Some(0))
    assertEquals(okPacket.info, Some("OK"))
    assertEquals(okPacket.sessionStateInfo, None)
    assertEquals(okPacket.msg, None)
    assertEquals(okPacket.toString, "OK_Packet")
  }

  test("OKPacket decoder with CLIENT_PROTOCOL_41") {
    // Create sample packet data that would be received from server
    val packetBytes = Array[Byte](
      0x05,          // affected rows (5)
      0x0a,          // last insert ID (10)
      0x02, 0x00,    // status flags (SERVER_STATUS_AUTOCOMMIT = 2)
      0x00, 0x00,    // warnings (0)
      0x02, 'O', 'K' // info string "OK"
    )

    val bitVector       = BitVector(packetBytes)
    val capabilityFlags = Set(CapabilitiesFlags.CLIENT_PROTOCOL_41)

    val result = OKPacket.decoder(capabilityFlags).decode(bitVector)

    assert(result.isSuccessful)
    result match {
      case Attempt.Successful(decoded) =>
        val okPacket = decoded.value
        assertEquals(okPacket.status, 0x00)
        assertEquals(okPacket.affectedRows, 5L)
        assertEquals(okPacket.lastInsertId, 10L)
        assertEquals(okPacket.statusFlags, Set(ServerStatusFlags.SERVER_STATUS_AUTOCOMMIT))
        assertEquals(okPacket.warnings, Some(0))
        assertEquals(okPacket.info, None) // No session track flag
        assertEquals(okPacket.sessionStateInfo, None)
        assertEquals(okPacket.msg, Some("OK"))
      case _ => fail("Decoding failed")
    }
  }

  test("OKPacket decoder with CLIENT_SESSION_TRACK") {
    // Create sample packet with session state info
    val packetBytes = Array[Byte](
      0x01,               // affected rows (1)
      0x00,               // last insert ID (0)
      0x40, 0x40,         // status flags including SERVER_SESSION_STATE_CHANGED (0x4000)
      0x00, 0x00,         // warnings (0)
      0x02, 'O', 'K',     // info string "OK"
      0x03, 'S', 'S', 'I' // session state info "SSI"
    )

    val bitVector = BitVector(packetBytes)
    val capabilityFlags = Set(
      CapabilitiesFlags.CLIENT_PROTOCOL_41,
      CapabilitiesFlags.CLIENT_SESSION_TRACK
    )

    val result = OKPacket.decoder(capabilityFlags).decode(bitVector)

    assert(result.isSuccessful)
    result match {
      case Attempt.Successful(decoded) =>
        val okPacket = decoded.value
        assertEquals(okPacket.status, 0x00)
        assertEquals(okPacket.affectedRows, 1L)
        assertEquals(okPacket.lastInsertId, 0L)
        assert(ServerStatusFlags.hasBitFlag(okPacket.statusFlags, ServerStatusFlags.SERVER_SESSION_STATE_CHANGED))
        assertEquals(okPacket.warnings, Some(0))
        assertEquals(okPacket.info, Some("OK"))
        assertEquals(okPacket.sessionStateInfo, Some("SSI"))
        assertEquals(okPacket.msg, None) // SESSION_TRACK flag is set
      case _ => fail("Decoding failed")
    }
  }

  test("ServerStatusFlags bit operations") {
    val flags = Set(
      ServerStatusFlags.SERVER_STATUS_AUTOCOMMIT,
      ServerStatusFlags.SERVER_STATUS_IN_TRANS
    )

    // Test hasBitFlag
    assert(ServerStatusFlags.hasBitFlag(flags, ServerStatusFlags.SERVER_STATUS_AUTOCOMMIT))
    assert(ServerStatusFlags.hasBitFlag(flags, ServerStatusFlags.SERVER_STATUS_IN_TRANS))
    assert(!ServerStatusFlags.hasBitFlag(flags, ServerStatusFlags.SERVER_STATUS_CURSOR_EXISTS))

    // Test setBitFlag
    val updatedFlags = ServerStatusFlags.setBitFlag(flags, ServerStatusFlags.SERVER_STATUS_CURSOR_EXISTS)
    assert(ServerStatusFlags.hasBitFlag(updatedFlags, ServerStatusFlags.SERVER_STATUS_CURSOR_EXISTS))

    // Test numeric conversion
    val numericValue = ServerStatusFlags.toBitset(flags)
    assertEquals(numericValue, 3L) // 1 + 2 = 3

    // Convert back to set
    val convertedFlags = ServerStatusFlags.toEnumSet(numericValue)
    assertEquals(convertedFlags, flags)
  }
