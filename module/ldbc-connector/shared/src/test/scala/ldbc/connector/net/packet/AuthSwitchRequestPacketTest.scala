/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scodec.bits.BitVector
import scodec.Attempt

import ldbc.connector.*
import ldbc.connector.net.packet.response.AuthSwitchRequestPacket

class AuthSwitchRequestPacketTest extends FTestPlatform:

  test("AuthSwitchRequestPacket creation and properties") {
    val pluginData = Array[Byte](1, 2, 3, 4, 5)
    val packet = AuthSwitchRequestPacket(
      status = AuthSwitchRequestPacket.STATUS,
      pluginName = "mysql_native_password",
      pluginProvidedData = pluginData
    )

    assertEquals(packet.status, 254)
    assertEquals(packet.pluginName, "mysql_native_password")
    assertEquals(packet.pluginProvidedData, pluginData)
    assertEquals(packet.toString, "Protocol::AuthSwitchRequest")
  }

  test("AuthSwitchRequestPacket decoder") {
    // Create sample packet data that would be received from server
    val pluginName = "mysql_native_password"
    val pluginData = Array[Byte](1, 2, 3, 4, 5)
    
    // Construct packet bytes: plugin name (null-terminated) + plugin data + ending byte
    val packetBytes = pluginName.getBytes ++ Array[Byte](0) ++ pluginData ++ Array[Byte](0)
    
    val bitVector = BitVector(packetBytes)
    val result = AuthSwitchRequestPacket.decoder.decode(bitVector)

    assert(result.isSuccessful)
    result match {
      case Attempt.Successful(decoded) =>
        val packet = decoded.value
        assertEquals(packet.status, 254)
        assertEquals(packet.pluginName, pluginName)
        assertEquals(packet.pluginProvidedData.toSeq, pluginData.toSeq)
      case _ => fail("Decoding failed")
    }
  }
  
  test("AuthSwitchRequestPacket with empty plugin data") {
    val pluginName = "auth_plugin"
    val emptyPluginData = Array[Byte]()
    
    val packet = AuthSwitchRequestPacket(
      status = AuthSwitchRequestPacket.STATUS,
      pluginName = pluginName,
      pluginProvidedData = emptyPluginData
    )
    
    // Test packet properties
    assertEquals(packet.status, 254)
    assertEquals(packet.pluginName, pluginName)
    assertEquals(packet.pluginProvidedData, emptyPluginData)
    
    // Test decoding with empty plugin data
    val packetBytes = pluginName.getBytes ++ Array[Byte](0) ++ Array[Byte](0)
    val bitVector = BitVector(packetBytes)
    val result = AuthSwitchRequestPacket.decoder.decode(bitVector)
    
    assert(result.isSuccessful)
    result match {
      case Attempt.Successful(decoded) =>
        val decodedPacket = decoded.value
        assertEquals(decodedPacket.status, 254)
        assertEquals(decodedPacket.pluginName, pluginName)
        assertEquals(decodedPacket.pluginProvidedData.length, 0)
      case _ => fail("Decoding failed")
    }
  }