/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import ldbc.connector.*
import ldbc.connector.data.*
import ldbc.connector.net.packet.request.SSLRequestPacket

class SSLRequestPacketTest extends FTestPlatform:

  test("SSLRequestPacket creation and properties") {
    val sequenceId      = 1.toByte
    val capabilityFlags = Set(
      CapabilitiesFlags.CLIENT_SSL,
      CapabilitiesFlags.CLIENT_PROTOCOL_41
    )

    val sslRequestPacket = SSLRequestPacket(
      sequenceId      = sequenceId,
      capabilityFlags = capabilityFlags
    )

    assertEquals(sslRequestPacket.sequenceId, sequenceId)
    assertEquals(sslRequestPacket.capabilityFlags, capabilityFlags)
    assertEquals(sslRequestPacket.toString, "Protocol::SSLRequest")
  }

  test("SSLRequestPacket encoder with CLIENT_PROTOCOL_41") {
    val sequenceId      = 1.toByte
    val capabilityFlags = Set(
      CapabilitiesFlags.CLIENT_SSL,
      CapabilitiesFlags.CLIENT_PROTOCOL_41
    )

    val sslRequestPacket = SSLRequestPacket(
      sequenceId      = sequenceId,
      capabilityFlags = capabilityFlags
    )

    val encoded = sslRequestPacket.encode

    // Expected size: 4 bytes header + 32 bytes payload
    assertEquals(encoded.size.toInt, (4 + 32) * 8)

    // Check header bytes (payload size = 32, sequence = 1)
    val headerBytes = encoded.take(32).bytes.toArray
    assertEquals(headerBytes(0).toInt, 32) // payload size
    assertEquals(headerBytes(1).toInt, 0)  // zero
    assertEquals(headerBytes(2).toInt, 0)  // zero
    assertEquals(headerBytes(3).toInt, 1)  // sequence id

    // Extract capability flags (first 4 bytes of payload)
    val capabilityBits = encoded.drop(32).take(32).bytes.toArray
    // Convert bytes to integer (little endian)
    val capabilityValue =
      (capabilityBits(0) & 0xff) |
        ((capabilityBits(1) & 0xff) << 8) |
        ((capabilityBits(2) & 0xff) << 16) |
        ((capabilityBits(3) & 0xff) << 24)

    // Check if CLIENT_SSL (0x00000800) and CLIENT_PROTOCOL_41 (0x00000200) bits are set
    assert((capabilityValue & 0x00000800) != 0) // CLIENT_SSL
    assert((capabilityValue & 0x00000200) != 0) // CLIENT_PROTOCOL_41
  }

  test("SSLRequestPacket encoder without CLIENT_PROTOCOL_41") {
    val sequenceId      = 2.toByte
    val capabilityFlags = Set(
      CapabilitiesFlags.CLIENT_SSL
        // No CLIENT_PROTOCOL_41
    )

    val sslRequestPacket = SSLRequestPacket(
      sequenceId      = sequenceId,
      capabilityFlags = capabilityFlags
    )

    val encoded = sslRequestPacket.encode

    // Expected size: 4 bytes header + 7 bytes payload (no padding needed)
    assertEquals(encoded.size.toInt, (4 + 7) * 8)

    // Check header bytes (payload size = 7, sequence = 2)
    val headerBytes = encoded.take(32).bytes.toArray
    assertEquals(headerBytes(0).toInt, 7) // payload size
    assertEquals(headerBytes(1).toInt, 0) // zero
    assertEquals(headerBytes(2).toInt, 0) // zero
    assertEquals(headerBytes(3).toInt, 2) // sequence id

    // Extract capability flags (first 4 bytes of payload)
    val capabilityBits = encoded.drop(32).take(32).bytes.toArray
    // Convert bytes to integer (little endian)
    val capabilityValue =
      (capabilityBits(0) & 0xff) |
        ((capabilityBits(1) & 0xff) << 8) |
        ((capabilityBits(2) & 0xff) << 16) |
        ((capabilityBits(3) & 0xff) << 24)

    // Check if CLIENT_SSL (0x00000800) bit is set and CLIENT_PROTOCOL_41 (0x00000200) is not set
    assert((capabilityValue & 0x00000800) != 0) // CLIENT_SSL
    assert((capabilityValue & 0x00000200) == 0) // CLIENT_PROTOCOL_41 should not be set
  }
