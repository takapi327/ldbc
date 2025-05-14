/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scodec.bits.{ BitVector, ByteVector }

import ldbc.connector.*
import ldbc.connector.data.*
import ldbc.connector.net.packet.request.HandshakeResponse41Packet

class HandshakeResponse41PacketTest extends FTestPlatform:

  test("HandshakeResponse41Packet creation and properties") {
    val packet = HandshakeResponse41Packet(
      capabilitiesFlags = Set(
        CapabilitiesFlags.CLIENT_PROTOCOL_41, 
        CapabilitiesFlags.CLIENT_PLUGIN_AUTH
      ),
      user              = "testUser",
      hashedPassword    = Array[Byte](1, 2, 3, 4, 5),
      pluginName        = "mysql_native_password",
      characterSet      = 8,
      database          = Some("testdb")
    )

    assertEquals(packet.capabilitiesFlags, Set(CapabilitiesFlags.CLIENT_PROTOCOL_41, CapabilitiesFlags.CLIENT_PLUGIN_AUTH))
    assertEquals(packet.user, "testUser")
    assertEquals(packet.hashedPassword.toSeq, Array[Byte](1, 2, 3, 4, 5).toSeq)
    assertEquals(packet.pluginName, "mysql_native_password")
    assertEquals(packet.characterSet, 8)
    assertEquals(packet.database, Some("testdb"))
    assertEquals(packet.toString, "Protocol::HandshakeResponse41")
  }

  test("HandshakeResponse41Packet encoder basic") {
    val packet = HandshakeResponse41Packet(
      capabilitiesFlags = Set(CapabilitiesFlags.CLIENT_PROTOCOL_41),
      user              = "testUser",
      hashedPassword    = Array[Byte](1, 2, 3, 4, 5),
      pluginName        = "mysql_native_password",
      characterSet      = 8,
      database          = None
    )

    val encoded = packet.encode
    
    // Verify that the encoded packet is not empty
    assert(encoded.nonEmpty)

    // Verify that the encoded result is as expected
    assert(encoded.nonEmpty, "Encoded packet should not be empty")
    
    // Verify character set byte
    val charsetByte = encoded.drop(32 + 32).take(8).toByte()
    assertEquals(charsetByte.toInt, 8)
    
    // Verify the structure within the packet
    val username = "testUser\u0000"  // Null-terminated string
    val encodedString = new String(encoded.toByteArray)
    assert(encodedString.contains(username), "Encoded packet should contain the username")
  }
  
  test("HandshakeResponse41Packet encoder with extended flags") {
    val packet = HandshakeResponse41Packet(
      capabilitiesFlags = Set(
        CapabilitiesFlags.CLIENT_PROTOCOL_41,
        CapabilitiesFlags.CLIENT_PLUGIN_AUTH,
        CapabilitiesFlags.CLIENT_CONNECT_WITH_DB
      ),
      user              = "user",
      hashedPassword    = Array[Byte](10, 20, 30),
      pluginName        = "mysql_native_password",
      characterSet      = 33,
      database          = Some("mydb")
    )

    val encoded = packet.encode
    
    // Verify that the encoded packet contains the database name
    assert(encoded.nonEmpty)
    
    // Extract the part of the BitVector containing the username and check if it ends with a null terminator
    val afterReserved = encoded.drop(32 + 32 + 8 + 23*8)
    val nullTerminatedUsername = ByteVector(afterReserved.bytes.toArray.takeWhile(_ != 0)) ++ ByteVector(0)
    assertEquals(new String(nullTerminatedUsername.toArray.dropRight(1)), "user", "Username should match")
    
    // The database name should appear in the encoded packet after username and auth data
    val encodedString = new String(encoded.toByteArray)
    assert(encodedString.contains("mydb"))
    
    // The plugin name should also appear in the encoded packet
    assert(encodedString.contains("mysql_native_password"))
  }