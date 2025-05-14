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
import ldbc.connector.net.packet.response.{ ColumnDefinitionPacket, ColumnDefinition41Packet }

class ColumnDefinitionPacketTest extends FTestPlatform:

  test("ColumnDefinitionPacket creation and properties") {
    val columnPacket = ColumnDefinition41Packet(
      catalog    = "def",
      schema     = "test_db",
      table      = "users",
      orgTable   = "users",
      name       = "id",
      orgName    = "id",
      length    = 12,
      characterSet    = 33,
      columnLength  = 11,
      columnType = ColumnDataType.MYSQL_TYPE_LONG,
      flags      = Seq(ColumnDefinitionFlags.NOT_NULL_FLAG, ColumnDefinitionFlags.PRI_KEY_FLAG),
      decimals   = 0
    )

    assertEquals(columnPacket.catalog, "def")
    assertEquals(columnPacket.schema, "test_db")
    assertEquals(columnPacket.table, "users")
    assertEquals(columnPacket.orgTable, "users")
    assertEquals(columnPacket.name, "id")
    assertEquals(columnPacket.orgName, "id")
    assertEquals(columnPacket.characterSet, 33)
    assertEquals(columnPacket.columnLength, 11L)
    assertEquals(columnPacket.columnType, ColumnDataType.MYSQL_TYPE_LONG)
    assertEquals(columnPacket.flags, Seq(ColumnDefinitionFlags.NOT_NULL_FLAG, ColumnDefinitionFlags.PRI_KEY_FLAG))
    assertEquals(columnPacket.decimals, 0)
    assertEquals(columnPacket.fullName, "users.id")
  }

  test("ColumnDefinitionPacket decoder with CLIENT_PROTOCOL_41") {
    // Create sample packet data for column definition with protocol 41
    val packetBytes = Array[Byte](
      0x03, 'd', 'e', 'f',                    // catalog "def"
      0x07, 't', 'e', 's', 't', '_', 'd', 'b', // schema "test_db"
      0x05, 'u', 's', 'e', 'r', 's',          // table "users"
      0x05, 'u', 's', 'e', 'r', 's',          // org_table "users"
      0x02, 'i', 'd',                         // name "id"
      0x02, 'i', 'd',                         // org_name "id"
      0x0c,                                   // length of fixed-length fields
      0x21, 0x00,                             // character set (33)
      0x0b, 0x00, 0x00, 0x00,                 // column length (11)
      0x03,                                   // type (LONG = 3)
      0x03, 0x00,                             // flags (NOT_NULL_FLAG | PRI_KEY_FLAG)
      0x00,                                   // decimals
      0x00, 0x00                              // filler
    )

    val bitVector       = BitVector(packetBytes)
    val capabilityFlags = Set(CapabilitiesFlags.CLIENT_PROTOCOL_41)

    val result = ColumnDefinitionPacket.decoder(capabilityFlags).decode(bitVector)

    assert(result.isSuccessful)
    result match {
      case Attempt.Successful(decoded) =>
        val columnPacket = decoded.value
        assertEquals(columnPacket.table, "users")
        assertEquals(columnPacket.name, "id")
        assertEquals(columnPacket.columnType, ColumnDataType.MYSQL_TYPE_LONG)
        assert(columnPacket.flags.contains(ColumnDefinitionFlags.NOT_NULL_FLAG))
        assert(columnPacket.flags.contains(ColumnDefinitionFlags.PRI_KEY_FLAG))
        assertEquals(columnPacket.fullName, "users.id")
      case _ => fail("Decoding failed")
    }
  }

  test("ColumnDefinitionFlags operations") {
    // Test flags composition
    val flags = Seq(
      ColumnDefinitionFlags.NOT_NULL_FLAG,
      ColumnDefinitionFlags.PRI_KEY_FLAG,
      ColumnDefinitionFlags.UNSIGNED_FLAG
    )

    // Test contains check
    assert(flags.contains(ColumnDefinitionFlags.NOT_NULL_FLAG))
    assert(flags.contains(ColumnDefinitionFlags.PRI_KEY_FLAG))
    assert(flags.contains(ColumnDefinitionFlags.UNSIGNED_FLAG))
    assert(!flags.contains(ColumnDefinitionFlags.UNIQUE_KEY_FLAG))

    // Test numeric conversion and back (assuming a toBitMask and fromBitMask method exists)
    val flagsValue = flags.foldLeft(0L)((acc, flag) => acc | flag.code)
    assertEquals(flagsValue, ColumnDefinitionFlags.NOT_NULL_FLAG.code |
                           ColumnDefinitionFlags.PRI_KEY_FLAG.code |
                           ColumnDefinitionFlags.UNSIGNED_FLAG.code)

    // From numeric value to flags - use exact matches to avoid false positives
    val reconstructed = ColumnDefinitionFlags.values.filter { flag => 
      (flagsValue & flag.code) != 0 && (flag.code & flagsValue) == flag.code
    }
    
    assert(reconstructed.contains(ColumnDefinitionFlags.NOT_NULL_FLAG))
    assert(reconstructed.contains(ColumnDefinitionFlags.PRI_KEY_FLAG))
    assert(reconstructed.contains(ColumnDefinitionFlags.UNSIGNED_FLAG))
    assertEquals(reconstructed.toSet, flags.toSet)
  }
