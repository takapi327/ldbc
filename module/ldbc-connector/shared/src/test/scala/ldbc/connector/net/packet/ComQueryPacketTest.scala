/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scala.collection.immutable.ListMap

import ldbc.connector.*
import ldbc.connector.data.*
import ldbc.connector.net.packet.request.ComQueryPacket

class ComQueryPacketTest extends FTestPlatform:

  test("ComQueryPacket creation and properties") {
    val sql = "SELECT * FROM users"
    val capabilityFlags = Set(
      CapabilitiesFlags.CLIENT_PROTOCOL_41
    )
    val params = ListMap.empty[ColumnDataType, Any]

    val comQueryPacket = ComQueryPacket(
      sql             = sql,
      capabilityFlags = capabilityFlags,
      params          = params
    )

    assertEquals(comQueryPacket.sql, sql)
    assertEquals(comQueryPacket.capabilityFlags, capabilityFlags)
    assertEquals(comQueryPacket.params, params)
    assertEquals(comQueryPacket.toString, "COM_QUERY Request")
  }

  test("ComQueryPacket encoder without query attributes") {
    val sql = "SELECT * FROM users WHERE id = 1"
    val capabilityFlags = Set(
      CapabilitiesFlags.CLIENT_PROTOCOL_41
    )
    val params = ListMap.empty[ColumnDataType, Any]

    val comQueryPacket = ComQueryPacket(
      sql             = sql,
      capabilityFlags = capabilityFlags,
      params          = params
    )

    val encoded = comQueryPacket.encode

    // Check if the first byte is COM_QUERY command code (0x03)
    val commandByte = encoded.take(8).toByte()
    assertEquals(commandByte.toInt, CommandId.COM_QUERY)

    // Extract SQL part starting from command byte
    val sqlBytes   = encoded.drop(8).toByteArray
    val decodedSql = new String(sqlBytes, "UTF-8")
    assertEquals(decodedSql, sql)
  }

  test("ComQueryPacket encoder with query attributes") {
    val sql = "SELECT * FROM users WHERE name = ?"
    val capabilityFlags = Set(
      CapabilitiesFlags.CLIENT_PROTOCOL_41,
      CapabilitiesFlags.CLIENT_QUERY_ATTRIBUTES
    )
    val params = ListMap(ColumnDataType.MYSQL_TYPE_STRING -> "Alice")

    val comQueryPacket = ComQueryPacket(
      sql             = sql,
      capabilityFlags = capabilityFlags,
      params          = params
    )

    val encoded = comQueryPacket.encode

    // Check if the first byte is COM_QUERY command code (0x03)
    val commandByte = encoded.take(8).toByte()
    assertEquals(commandByte.toInt, CommandId.COM_QUERY)

    // Parameter count should be 1
    val paramCountByte = encoded.drop(8).take(8).toByte()
    assertEquals(paramCountByte.toInt, 1)

    // Next byte should be 0x01 for parameters
    val paramFlagByte = encoded.drop(16).take(8).toByte()
    assertEquals(paramFlagByte.toInt, 0x01)

    // Verify total length is reasonable (checking exact bytes would be complex due to encoding)
    assert(
      encoded.size > (sql.length * 8 + 24),
      "Encoded data should be longer than just command + param count + flag + sql"
    )
  }
