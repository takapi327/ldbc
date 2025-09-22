/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import ldbc.connector.*

class ColumnDefinitionFlagsTest extends FTestPlatform:

  test("apply(bitset: Long) should return the correct flags") {
    val bitset = ColumnDefinitionFlags.NOT_NULL_FLAG.code | ColumnDefinitionFlags.PRI_KEY_FLAG.code

    val result = ColumnDefinitionFlags(bitset)

    assert(result.contains(ColumnDefinitionFlags.NOT_NULL_FLAG))
    assert(result.contains(ColumnDefinitionFlags.PRI_KEY_FLAG))
    assert(!result.contains(ColumnDefinitionFlags.UNIQUE_KEY_FLAG))
    assertEquals(result.size, 2)
  }

  test("apply(bitset: Seq[Short]) should return the correct flags") {
    val shortBitset = Seq[Short](0, 1) // Represents NOT_NULL_FLAG and PRI_KEY_FLAG

    val result = ColumnDefinitionFlags(shortBitset)

    assert(result.contains(ColumnDefinitionFlags.NOT_NULL_FLAG))
    assert(result.contains(ColumnDefinitionFlags.PRI_KEY_FLAG))
    assertEquals(result.size, 2)
  }

  test("toCode should convert bit number to flag code correctly") {
    assertEquals(ColumnDefinitionFlags.toCode(0), 1L)
    assertEquals(ColumnDefinitionFlags.toCode(1), 2L)
    assertEquals(ColumnDefinitionFlags.toCode(5), 32L)
  }

  test("toBitset should combine flags into a single bitset") {
    val flags = Seq(
      ColumnDefinitionFlags.NOT_NULL_FLAG,
      ColumnDefinitionFlags.AUTO_INCREMENT_FLAG
    )

    val bitset = ColumnDefinitionFlags.toBitset(flags)

    // NOT_NULL_FLAG (1) + AUTO_INCREMENT_FLAG (512)
    assertEquals(bitset, 513L)
  }

  test("toEnumSeq should convert a bitset back to flags") {
    val bitset = ColumnDefinitionFlags.NOT_NULL_FLAG.code | ColumnDefinitionFlags.BLOB_FLAG.code

    val flags = ColumnDefinitionFlags.toEnumSeq(bitset)

    assertEquals(flags.size, 2)
    assert(flags.contains(ColumnDefinitionFlags.NOT_NULL_FLAG))
    assert(flags.contains(ColumnDefinitionFlags.BLOB_FLAG))
  }

  test("hasBitFlag(Seq[ColumnDefinitionFlags], flag) should check flag presence") {
    val flags = Seq(
      ColumnDefinitionFlags.NOT_NULL_FLAG,
      ColumnDefinitionFlags.PRI_KEY_FLAG
    )

    assert(ColumnDefinitionFlags.hasBitFlag(flags, ColumnDefinitionFlags.NOT_NULL_FLAG))
    assert(ColumnDefinitionFlags.hasBitFlag(flags, ColumnDefinitionFlags.PRI_KEY_FLAG))
    assert(!ColumnDefinitionFlags.hasBitFlag(flags, ColumnDefinitionFlags.BLOB_FLAG))
  }

  test("hasBitFlag(Seq[ColumnDefinitionFlags], code) should check flag presence by code") {
    val flags = Seq(
      ColumnDefinitionFlags.NOT_NULL_FLAG,
      ColumnDefinitionFlags.PRI_KEY_FLAG
    )

    assert(ColumnDefinitionFlags.hasBitFlag(flags, 1L))   // NOT_NULL_FLAG
    assert(!ColumnDefinitionFlags.hasBitFlag(flags, 16L)) // UNIQUE_FLAG
  }

  test("hasBitFlag(Long, flag) should check flag presence in bitset") {
    val bitset = ColumnDefinitionFlags.NOT_NULL_FLAG.code | ColumnDefinitionFlags.UNIQUE_KEY_FLAG.code

    assert(ColumnDefinitionFlags.hasBitFlag(bitset, ColumnDefinitionFlags.NOT_NULL_FLAG))
    assert(!ColumnDefinitionFlags.hasBitFlag(bitset, ColumnDefinitionFlags.BLOB_FLAG))
  }

  test("hasBitFlag(Long, code) should check code presence in bitset") {
    val bitset = ColumnDefinitionFlags.NOT_NULL_FLAG.code | ColumnDefinitionFlags.UNIQUE_KEY_FLAG.code

    assert(ColumnDefinitionFlags.hasBitFlag(bitset, 1L))   // NOT_NULL_FLAG
    assert(!ColumnDefinitionFlags.hasBitFlag(bitset, 16L)) // UNIQUE_FLAG (not set)
  }

  test("setBitFlag(Seq[ColumnDefinitionFlags], flag) should add flag to set") {
    val flags = Seq(ColumnDefinitionFlags.NOT_NULL_FLAG)

    val updatedFlags = ColumnDefinitionFlags.setBitFlag(flags, ColumnDefinitionFlags.BLOB_FLAG)

    assert(updatedFlags.contains(ColumnDefinitionFlags.NOT_NULL_FLAG))
    assert(updatedFlags.contains(ColumnDefinitionFlags.BLOB_FLAG))
    assertEquals(updatedFlags.size, 2)
  }

  test("setBitFlag(Seq[ColumnDefinitionFlags], code) should add flag by code") {
    val flags = Seq(ColumnDefinitionFlags.NOT_NULL_FLAG)

    // Fix: We need to use a code that corresponds directly to an enum value
    // 1L << 3 = 8L which is the MULTIPLE_KEY_FLAG code
    val updatedFlags = ColumnDefinitionFlags.setBitFlag(flags, 8L) // MULTIPLE_KEY_FLAG code

    assert(updatedFlags.contains(ColumnDefinitionFlags.NOT_NULL_FLAG))
    assert(updatedFlags.contains(ColumnDefinitionFlags.MULTIPLE_KEY_FLAG))
  }

  test("setBitFlag(Long, flag) should add flag to bitset") {
    val bitset = ColumnDefinitionFlags.NOT_NULL_FLAG.code

    val updatedBitset = ColumnDefinitionFlags.setBitFlag(bitset, ColumnDefinitionFlags.AUTO_INCREMENT_FLAG)

    assert(ColumnDefinitionFlags.hasBitFlag(updatedBitset, ColumnDefinitionFlags.NOT_NULL_FLAG))
    assert(ColumnDefinitionFlags.hasBitFlag(updatedBitset, ColumnDefinitionFlags.AUTO_INCREMENT_FLAG))
  }

  test("setBitFlag(Long, code) should add code to bitset") {
    val bitset = ColumnDefinitionFlags.NOT_NULL_FLAG.code

    // Fix: Use the correct code for MULTIPLE_KEY_FLAG (8L)
    val updatedBitset = ColumnDefinitionFlags.setBitFlag(bitset, 8L) // MULTIPLE_KEY_FLAG code

    assert(ColumnDefinitionFlags.hasBitFlag(updatedBitset, ColumnDefinitionFlags.NOT_NULL_FLAG))
    assert(ColumnDefinitionFlags.hasBitFlag(updatedBitset, ColumnDefinitionFlags.MULTIPLE_KEY_FLAG))
  }
