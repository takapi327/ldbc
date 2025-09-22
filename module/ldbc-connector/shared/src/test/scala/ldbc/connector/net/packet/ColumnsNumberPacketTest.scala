/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scodec.*
import scodec.bits.BitVector

import ldbc.connector.*
import ldbc.connector.net.packet.response.{ ColumnsNumberPacket, ERRPacket, OKPacket }

class ColumnsNumberPacketTest extends FTestPlatform:

  test("ColumnsNumberPacket should be decoded correctly") {
    val size   = 5
    val packet = ColumnsNumberPacket(size)

    // Create a BitVector for testing with sufficient length to avoid ArrayIndexOutOfBoundsException
    val bits = BitVector(size) ++ BitVector.fill(7 * 8)(false)

    // Test the decoder
    val result = ColumnsNumberPacket.decoder(Set.empty).decode(bits)

    assertEquals(result, Attempt.successful(DecodeResult(packet, BitVector.fill(7 * 8)(false))))
  }

  test("ColumnsNumberPacket decoder handles different size values") {
    // Test with additional padding to avoid ArrayIndexOutOfBoundsException
    val padding = BitVector.fill(7 * 8)(false)

    // Test with size 0
    val bits0   = BitVector(0) ++ padding
    val result0 = ColumnsNumberPacket.decoder(Set.empty).decode(bits0)
    val ok      = OKPacket(
      status           = 0,
      affectedRows     = 0,
      lastInsertId     = 0,
      statusFlags      = Set(),
      warnings         = None,
      info             = None,
      sessionStateInfo = None,
      msg              = Some(
        value = ""
      )
    )
    assertEquals(result0, Attempt.successful(DecodeResult(ok, padding)))

    // Test with size 255 (this is ERRPacket.STATUS)
    val bits255   = BitVector(255) ++ padding
    val result255 = ColumnsNumberPacket.decoder(Set.empty).decode(bits255)
    val err       = ERRPacket(
      status         = 255,
      errorCode      = 0,
      sqlStateMarker = 0,
      sqlState       = None,
      errorMessage   = "\u0000\u0000\u0000\u0000\u0000"
    )
    assertEquals(result255, Attempt.successful(DecodeResult(err, BitVector.empty)))

    // Test with size 42 (regular ColumnsNumberPacket)
    val bits42   = BitVector(42) ++ padding
    val result42 = ColumnsNumberPacket.decoder(Set.empty).decode(bits42)
    assertEquals(result42, Attempt.successful(DecodeResult(ColumnsNumberPacket(42), padding)))
  }

  test("toString should return the expected value") {
    val packet = ColumnsNumberPacket(5)
    assertEquals(packet.toString, "ColumnsNumber Packet")
  }
