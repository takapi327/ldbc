/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scodec.bits.BitVector
import scodec.Attempt

import ldbc.connector.*
import ldbc.connector.net.packet.response.AuthMoreDataPacket

class AuthMoreDataPacketTest extends FTestPlatform:

  test("AuthMoreDataPacket creation and properties") {
    val authData = Array[Byte](1, 2, 3, 4, 5)
    val packet = AuthMoreDataPacket(
      status                   = AuthMoreDataPacket.STATUS,
      authenticationMethodData = authData
    )

    assertEquals(packet.status, 0x01)
    assertEquals(packet.authenticationMethodData, authData)
    assertEquals(packet.toString, "Protocol::AuthMoreData")
  }

  test("AuthMoreDataPacket decoder") {
    // Create sample packet data that would be received from server
    val authData    = Array[Byte]('t', 'e', 's', 't', '_', 'd', 'a', 't', 'a')
    val packetBytes = authData

    val bitVector = BitVector(packetBytes)
    val result    = AuthMoreDataPacket.decoder.decode(bitVector)

    assert(result.isSuccessful)
    result match {
      case Attempt.Successful(decoded) =>
        val packet = decoded.value
        assertEquals(packet.status, 0x01)
        assertEquals(packet.authenticationMethodData.toSeq, authData.toSeq)
      case _ => fail("Decoding failed")
    }
  }
