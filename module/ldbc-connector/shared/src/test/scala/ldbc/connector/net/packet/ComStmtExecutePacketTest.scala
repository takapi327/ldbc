/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scala.collection.immutable.SortedMap

import ldbc.connector.*
import ldbc.connector.data.*
import ldbc.connector.net.packet.request.ComStmtExecutePacket

class ComStmtExecutePacketTest extends FTestPlatform:

  test("ComStmtExecutePacket should start with COM_STMT_EXECUTE command byte (0x17)") {
    val params  = SortedMap(1 -> Parameter.int(1))
    val packet  = ComStmtExecutePacket(1L, params, ComStmtExecutePacket.EnumCursorType.CURSOR_TYPE_NO_CURSOR)
    val encoded = packet.encode
    assertEquals(encoded.take(8).toByte().toInt & 0xff, CommandId.COM_STMT_EXECUTE)
  }

  test("Bug #714: parameter types should be encoded as uint16L (type) + uint8L (unsigned flag) per MySQL binary protocol spec") {
    // The MySQL binary protocol specifies each parameter descriptor as:
    //   - 2 bytes (uint16L): type code
    //   - 1 byte  (uint8L) : unsigned flag (0x80 = unsigned, 0x00 = signed)
    //   = 3 bytes total per parameter
    //
    // The original implementation used a single uint24L (also 3 bytes), which accidentally
    // produced correct bytes for common type codes but was semantically wrong.
    //
    // We verify by comparing encoded sizes of 1-parameter vs 2-parameter packets.
    // Adding one INT (MYSQL_TYPE_LONG) parameter increases the payload by:
    //
    //   - 0 bits  : null bitmap  (still 1 byte for ≤ 8 params)
    //   - 24 bits : type descriptor (uint16L type + uint8L unsigned_flag = 3 bytes)
    //   - 32 bits : INT value    (uint32L = 4 bytes)
    //   ──────────────────────────────────────────────
    //   = 56 bits total
    val params1 = SortedMap(1 -> Parameter.int(42))
    val params2 = SortedMap(1 -> Parameter.int(42), 2 -> Parameter.int(99))

    val packet1 = ComStmtExecutePacket(1L, params1, ComStmtExecutePacket.EnumCursorType.CURSOR_TYPE_NO_CURSOR)
    val packet2 = ComStmtExecutePacket(1L, params2, ComStmtExecutePacket.EnumCursorType.CURSOR_TYPE_NO_CURSOR)

    val diff = packet2.encode.size - packet1.encode.size

    assertEquals(
      diff,
      56L,
      s"Adding one INT parameter should increase encoded size by 56 bits " +
        s"(24 for type descriptor + 32 for value), but got $diff bits."
    )
  }

  test("Bug #714: type descriptor should be uint16L type code followed by uint8L unsigned flag") {
    // Per MySQL binary protocol, each parameter descriptor in the type list is:
    //   bytes 0-1: type code    (uint16L, little-endian)
    //   byte  2  : unsigned flag (0x80 = unsigned, 0x00 = signed)
    //
    // For 2 signed INT parameters the type section should be exactly 48 bits (2 × 24 bits).
    val params = SortedMap(1 -> Parameter.int(1), 2 -> Parameter.int(2))
    val packet = ComStmtExecutePacket(1L, params, ComStmtExecutePacket.EnumCursorType.CURSOR_TYPE_NO_CURSOR)

    val emptyParams = SortedMap.empty[Int, Parameter]
    val packetEmpty = ComStmtExecutePacket(1L, emptyParams, ComStmtExecutePacket.EnumCursorType.CURSOR_TYPE_NO_CURSOR)

    // packet.size = emptyPacket.size
    //             + 8  (null bitmap for 2 params)
    //             + typeSizeBits
    //             + 64 (2 × INT value)
    val nullBitmapBits = 8L
    val valueBits      = 64L
    val typeSizeBits   = packet.encode.size - packetEmpty.encode.size - nullBitmapBits - valueBits

    assertEquals(
      typeSizeBits,
      48L,
      s"Type section for 2 parameters should be 48 bits (2 × 24 bits: uint16L type + uint8L flag), but got $typeSizeBits bits."
    )
  }
