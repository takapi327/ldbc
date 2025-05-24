/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import ldbc.connector.*

class CapabilitiesFlagsTest extends FTestPlatform:

  test("apply(bitset: Long) should return the correct flags") {
    // Combine the flags using toBitset
    val flags  = Set(CapabilitiesFlags.CLIENT_LONG_PASSWORD, CapabilitiesFlags.CLIENT_FOUND_ROWS)
    val bitset = CapabilitiesFlags.toBitset(flags)

    val result = CapabilitiesFlags(bitset)

    assert(result.contains(CapabilitiesFlags.CLIENT_LONG_PASSWORD), "Should contain CLIENT_LONG_PASSWORD")
    assert(result.contains(CapabilitiesFlags.CLIENT_FOUND_ROWS), "Should contain CLIENT_FOUND_ROWS")
    assert(!result.contains(CapabilitiesFlags.CLIENT_LONG_FLAG), "Should not contain CLIENT_LONG_FLAG")
    assertEquals(result.size, 2)
  }

  test("apply(bitset: Set[Short]) should return the correct flags") {
    val shortBitset = Set[Short](0, 1) // Represents CLIENT_LONG_PASSWORD and CLIENT_FOUND_ROWS

    val result = CapabilitiesFlags(shortBitset)

    assert(result.contains(CapabilitiesFlags.CLIENT_LONG_PASSWORD), "Should contain CLIENT_LONG_PASSWORD")
    assert(result.contains(CapabilitiesFlags.CLIENT_FOUND_ROWS), "Should contain CLIENT_FOUND_ROWS")
    assertEquals(result.size, 2)
  }

  test("toCode should convert bit number to flag code correctly") {
    assertEquals(CapabilitiesFlags.toCode(0), 1L)
    assertEquals(CapabilitiesFlags.toCode(1), 2L)
    assertEquals(CapabilitiesFlags.toCode(5), 32L)
  }

  test("toBitset should combine flags into a single bitset") {
    val flags = Set(
      CapabilitiesFlags.CLIENT_LONG_PASSWORD,
      CapabilitiesFlags.CLIENT_FOUND_ROWS
    )

    val bitset = CapabilitiesFlags.toBitset(flags)

    // CLIENT_LONG_PASSWORD (1) + CLIENT_FOUND_ROWS (2)
    assertEquals(bitset, 3L)
  }

  test("toEnumSet should convert a bitset back to flags") {
    // Combine the flags using toBitset
    val flags  = Set(CapabilitiesFlags.CLIENT_LONG_PASSWORD, CapabilitiesFlags.CLIENT_CONNECT_WITH_DB)
    val bitset = CapabilitiesFlags.toBitset(flags)

    val resultFlags = CapabilitiesFlags.toEnumSet(bitset)

    assertEquals(resultFlags.size, 2)
    assert(resultFlags.contains(CapabilitiesFlags.CLIENT_LONG_PASSWORD), "Should contain CLIENT_LONG_PASSWORD")
    assert(resultFlags.contains(CapabilitiesFlags.CLIENT_CONNECT_WITH_DB), "Should contain CLIENT_CONNECT_WITH_DB")
  }

  test("hasBitFlag(Set[CapabilitiesFlags], flag) should check flag presence") {
    val flags = Set(
      CapabilitiesFlags.CLIENT_LONG_PASSWORD,
      CapabilitiesFlags.CLIENT_FOUND_ROWS
    )

    assert(
      CapabilitiesFlags.hasBitFlag(flags, CapabilitiesFlags.CLIENT_LONG_PASSWORD),
      "Should have CLIENT_LONG_PASSWORD flag"
    )
    assert(
      CapabilitiesFlags.hasBitFlag(flags, CapabilitiesFlags.CLIENT_FOUND_ROWS),
      "Should have CLIENT_FOUND_ROWS flag"
    )
    assert(
      !CapabilitiesFlags.hasBitFlag(flags, CapabilitiesFlags.CLIENT_LONG_FLAG),
      "Should not have CLIENT_LONG_FLAG flag"
    )
  }

  test("hasBitFlag(Long, flag) should check flag presence in bitset") {
    // Combine the flags using toBitset
    val flags  = Set(CapabilitiesFlags.CLIENT_LONG_PASSWORD, CapabilitiesFlags.CLIENT_FOUND_ROWS)
    val bitset = CapabilitiesFlags.toBitset(flags)

    assert(
      CapabilitiesFlags.hasBitFlag(bitset, CapabilitiesFlags.CLIENT_LONG_PASSWORD),
      "Should have CLIENT_LONG_PASSWORD flag"
    )
    assert(
      !CapabilitiesFlags.hasBitFlag(bitset, CapabilitiesFlags.CLIENT_LONG_FLAG),
      "Should not have CLIENT_LONG_FLAG flag"
    )
  }

  test("setBitFlag(Set[CapabilitiesFlags], flag) should add flag to set") {
    val flags = Set(CapabilitiesFlags.CLIENT_LONG_PASSWORD)

    val updatedFlags = CapabilitiesFlags.setBitFlag(flags, CapabilitiesFlags.CLIENT_FOUND_ROWS)

    assert(updatedFlags.contains(CapabilitiesFlags.CLIENT_LONG_PASSWORD), "Should contain CLIENT_LONG_PASSWORD")
    assert(updatedFlags.contains(CapabilitiesFlags.CLIENT_FOUND_ROWS), "Should contain CLIENT_FOUND_ROWS")
    assertEquals(updatedFlags.size, 2)
  }

  test("setBitFlag(Long, flag) should add flag to bitset") {
    // Create a bitset using toBitset
    val flags  = Set(CapabilitiesFlags.CLIENT_LONG_PASSWORD)
    val bitset = CapabilitiesFlags.toBitset(flags)

    val updatedBitset = CapabilitiesFlags.setBitFlag(bitset, CapabilitiesFlags.CLIENT_FOUND_ROWS)

    assert(
      CapabilitiesFlags.hasBitFlag(updatedBitset, CapabilitiesFlags.CLIENT_LONG_PASSWORD),
      "Should have CLIENT_LONG_PASSWORD flag"
    )
    assert(
      CapabilitiesFlags.hasBitFlag(updatedBitset, CapabilitiesFlags.CLIENT_FOUND_ROWS),
      "Should have CLIENT_FOUND_ROWS flag"
    )
  }
