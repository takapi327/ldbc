/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scodec.bits.ByteVector
import scodec.bits.BitVector

import ldbc.connector.*
import ldbc.connector.data.*
import ldbc.connector.net.packet.request.ComChangeUserPacket

class ComChangeUserPacketTest extends FTestPlatform:

  test("ComChangeUserPacket creation and properties") {
    val packet = ComChangeUserPacket(
      capabilitiesFlags = Set(CapabilitiesFlags.CLIENT_PROTOCOL_41, CapabilitiesFlags.CLIENT_PLUGIN_AUTH),
      user              = "testuser",
      database          = Some("testdb"),
      characterSet      = 33, // utf8_general_ci
      pluginName        = "mysql_native_password",
      hashedPassword    = ByteVector(Array[Byte](1, 2, 3, 4))
    )

    assertEquals(packet.capabilitiesFlags, Set(CapabilitiesFlags.CLIENT_PROTOCOL_41, CapabilitiesFlags.CLIENT_PLUGIN_AUTH))
    assertEquals(packet.user, "testuser")
    assertEquals(packet.database, Some("testdb"))
    assertEquals(packet.characterSet, 33)
    assertEquals(packet.pluginName, "mysql_native_password")
    assertEquals(packet.hashedPassword, ByteVector(Array[Byte](1, 2, 3, 4)))
    assertEquals(packet.toString, "COM_CHANGE_USER Request")
  }

  test("ComChangeUserPacket encoder with CLIENT_PROTOCOL_41 and CLIENT_PLUGIN_AUTH") {
    val packet = ComChangeUserPacket(
      capabilitiesFlags = Set(CapabilitiesFlags.CLIENT_PROTOCOL_41, CapabilitiesFlags.CLIENT_PLUGIN_AUTH),
      user              = "testuser",
      database          = Some("testdb"),
      characterSet      = 33,
      pluginName        = "mysql_native_password",
      hashedPassword    = ByteVector(Array[Byte](1, 2, 3, 4))
    )

    val encoded = packet.encode
    
    // Verify the command ID
    assertEquals(encoded.take(8).toByte(), CommandId.COM_CHANGE_USER.toByte)
    
    // Convert the encoded BitVector to a ByteVector for easier inspection
    val encodedBytes = encoded.toByteVector
    
    // Check if the username and other fields are correctly encoded
    assert(encodedBytes.containsSlice(ByteVector.encodeAscii("testuser").getOrElse(ByteVector.empty) ++ ByteVector(0)))
    assert(encodedBytes.containsSlice(ByteVector(1, 2, 3, 4)))
    assert(encodedBytes.containsSlice(ByteVector.encodeAscii("testdb").getOrElse(ByteVector.empty) ++ ByteVector(0)))
    assert(encodedBytes.containsSlice(ByteVector(33))) // character set
    assert(encodedBytes.containsSlice(ByteVector.encodeAscii("mysql_native_password").getOrElse(ByteVector.empty) ++ ByteVector(0)))
  }

  test("ComChangeUserPacket encoder with CLIENT_RESERVED2 flag") {
    val packet = ComChangeUserPacket(
      capabilitiesFlags = Set(CapabilitiesFlags.CLIENT_PROTOCOL_41, CapabilitiesFlags.CLIENT_PLUGIN_AUTH, CapabilitiesFlags.CLIENT_RESERVED2),
      user              = "testuser",
      database          = Some("testdb"),
      characterSet      = 33,
      pluginName        = "mysql_native_password",
      hashedPassword    = ByteVector(Array[Byte](1, 2, 3, 4))
    )

    val encoded = packet.encode
    val encodedBytes = encoded.toByteVector
    
    // Check if length-encoded binary format is used for hashed password
    // When CLIENT_RESERVED2 is set, the password is prefixed with its length
    val hashedPwdIndex = encodedBytes.indexOfSlice(ByteVector.encodeAscii("testuser").getOrElse(ByteVector.empty) ++ ByteVector(0)) + 
                         "testuser".length + 1
    
    // Password should be prefixed with length (4)
    assertEquals(encodedBytes.drop(hashedPwdIndex).take(1), ByteVector(4))
  }

  test("ComChangeUserPacket encoder without optional flags") {
    val packet = ComChangeUserPacket(
      capabilitiesFlags = Set(),
      user              = "testuser",
      database          = None,
      characterSet      = 33,
      pluginName        = "mysql_native_password",
      hashedPassword    = ByteVector(Array[Byte](1, 2, 3, 4))
    )

    val encoded = packet.encode
    val encodedBytes = encoded.toByteVector
    
    // Command ID should be COM_CHANGE_USER
    assertEquals(encodedBytes.take(1), ByteVector(CommandId.COM_CHANGE_USER))
    
    // User should be encoded as null-terminated string
    assert(encodedBytes.containsSlice(ByteVector.encodeAscii("testuser").getOrElse(ByteVector.empty) ++ ByteVector(0)))
    
    // Password should be encoded directly without length prefix (no CLIENT_RESERVED2)
    assert(encodedBytes.containsSlice(ByteVector(1, 2, 3, 4)))

    // No character set (no CLIENT_PROTOCOL_41)
    // No plugin name (no CLIENT_PLUGIN_AUTH)
    // No connect attributes (no CLIENT_CONNECT_ATTRS)
    
    // The packet should be shorter compared to one with all flags
    val fullFeaturedPacket = ComChangeUserPacket(
      capabilitiesFlags = Set(
        CapabilitiesFlags.CLIENT_PROTOCOL_41,
        CapabilitiesFlags.CLIENT_PLUGIN_AUTH,
        CapabilitiesFlags.CLIENT_CONNECT_ATTRS
      ),
      user              = "testuser",
      database          = Some("testdb"),
      characterSet      = 33,
      pluginName        = "mysql_native_password",
      hashedPassword    = ByteVector(Array[Byte](1, 2, 3, 4))
    ).encode.toByteVector
    
    assert(encodedBytes.length < fullFeaturedPacket.length)
  }