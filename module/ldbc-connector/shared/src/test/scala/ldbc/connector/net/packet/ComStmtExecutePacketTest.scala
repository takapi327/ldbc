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

  test("Bug #714: parameter types should be encoded as 2 bytes (uint16L) per MySQL binary protocol spec") {
    // The MySQL binary protocol specifies that each parameter type occupies exactly 2 bytes (uint16L).
    // The buggy implementation uses uint24L (3 bytes per type), which shifts every value field by
    // 1 byte per parameter and causes the server to misparse the packet.
    //
    // We detect the bug by comparing the encoded sizes of a 1-parameter packet vs a 2-parameter packet.
    // Adding one INT (MYSQL_TYPE_LONG) parameter increases the payload by:
    //
    //   - 0 bits  : null bitmap  (still 1 byte for ≤ 8 params)
    //   - 16 bits : type entry   (uint16L = 2 bytes)  ← correct
    //   - 32 bits : INT value    (uint32L = 4 bytes)
    //   ──────────────────────────────────────────────
    //   = 48 bits total
    //
    // With the bug (uint24L) the type entry costs 24 bits → diff would be 56 bits.
    val params1 = SortedMap(1 -> Parameter.int(42))
    val params2 = SortedMap(1 -> Parameter.int(42), 2 -> Parameter.int(99))

    val packet1 = ComStmtExecutePacket(1L, params1, ComStmtExecutePacket.EnumCursorType.CURSOR_TYPE_NO_CURSOR)
    val packet2 = ComStmtExecutePacket(1L, params2, ComStmtExecutePacket.EnumCursorType.CURSOR_TYPE_NO_CURSOR)

    val diff = packet2.encode.size - packet1.encode.size

    assertEquals(
      diff,
      48L,
      s"Adding one INT parameter should increase encoded size by 48 bits " +
        s"(16 for type + 32 for value), but got $diff bits. " +
        s"A diff of 56 bits means the type is encoded as uint24L (3 bytes) instead of uint16L (2 bytes)."
    )
  }

  test("Bug #714: MYSQL_TYPE_LONG type code (0x03) should appear as 2 bytes in the type section") {
    // With uint16L: MYSQL_TYPE_LONG (0x03) → 0x03 0x00  (2 bytes, little-endian)
    // With uint24L: MYSQL_TYPE_LONG (0x03) → 0x03 0x00 0x00  (3 bytes, little-endian)
    //
    // For a 2-parameter INT packet, the type section should be exactly 32 bits (2 × 16 bits).
    // With the bug it would be 48 bits (2 × 24 bits).
    val params = SortedMap(1 -> Parameter.int(1), 2 -> Parameter.int(2))
    val packet = ComStmtExecutePacket(1L, params, ComStmtExecutePacket.EnumCursorType.CURSOR_TYPE_NO_CURSOR)

    // Encode a 0-parameter packet to find the size of the fixed header fields.
    // Note: the null bitmap is 0 bits for 0 params, and 8 bits for 2 params.
    val emptyParams = SortedMap.empty[Int, Parameter]
    val packetEmpty = ComStmtExecutePacket(1L, emptyParams, ComStmtExecutePacket.EnumCursorType.CURSOR_TYPE_NO_CURSOR)

    // Isolate the type section:
    // packet.size = emptyPacket.size
    //             + 8  (null bitmap for 2 params, padded to 1 byte)
    //             + typeSizeBits
    //             + 64 (2 × INT value = 2 × uint32L)
    val nullBitmapBits = 8L
    val valueBits      = 64L
    val typeSizeBits   = packet.encode.size - packetEmpty.encode.size - nullBitmapBits - valueBits

    assertEquals(
      typeSizeBits,
      32L,
      s"Type section for 2 parameters should be 32 bits (2 × uint16L), but got $typeSizeBits bits. " +
        s"48 bits would indicate uint24L (3 bytes per type) — the bug described in #714."
    )
  }
