/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net.packet

import scodec.bits.BitVector
import scodec.Attempt

import ldbc.mysql.*
import ldbc.mysql.net.packet.response.AuthMoreDataPacket

class AuthMoreDataPacketTest extends FTestPlatform:

  test("AuthMoreDataPacket creation and properties") {
    val authData = Array[Byte](1, 2, 3, 4, 5)
    val packet   = AuthMoreDataPacket(
      status                   = AuthMoreDataPacket.STATUS,
      authenticationMethodData = authData
    )

    assertEquals(packet.status, 0x01)
    assertEquals(packet.authenticationMethodData, authData)
    assertEquals(packet.toString, "Protocol::AuthMoreData")
  }

  test("AuthMoreDataPacket decoder strips the status tag") {
    val authData  = Array[Byte]('t', 'e', 's', 't', '_', 'd', 'a', 't', 'a')
    val bitVector = BitVector(Array[Byte](0x01) ++ authData)
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

  test("AuthMoreDataPacket decoder reads the status actually sent") {
    val bitVector = BitVector(Array[Byte](0x05, 0x42))
    val result    = AuthMoreDataPacket.decoder.decode(bitVector)

    result match {
      case Attempt.Successful(decoded) =>
        assertEquals(decoded.value.status, 0x05)
        assertEquals(decoded.value.authenticationMethodData.toSeq, Seq(0x42.toByte))
      case _ => fail("Decoding failed")
    }
  }

  test("AuthMoreDataPacket decoder leaves nothing behind") {
    val bitVector = BitVector(Array[Byte](0x01, 0x04))
    val result    = AuthMoreDataPacket.decoder.decode(bitVector)

    result match {
      case Attempt.Successful(decoded) =>
        assertEquals(decoded.remainder, BitVector.empty)
        assertEquals(decoded.value.authenticationMethodData.toSeq, Seq(0x04.toByte))
      case _ => fail("Decoding failed")
    }
  }
