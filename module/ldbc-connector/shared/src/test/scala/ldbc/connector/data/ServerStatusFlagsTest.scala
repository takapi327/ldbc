/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import ldbc.connector.*

class ServerStatusFlagsTest extends FTestPlatform:

  test("apply(bitset: Long) should return the correct flags") {
    // Combine the flags using toBitset
    val flags  = Set(ServerStatusFlags.SERVER_STATUS_IN_TRANS, ServerStatusFlags.SERVER_STATUS_AUTOCOMMIT)
    val bitset = ServerStatusFlags.toBitset(flags)

    val result = ServerStatusFlags(bitset)

    assert(result.contains(ServerStatusFlags.SERVER_STATUS_IN_TRANS), "Should contain SERVER_STATUS_IN_TRANS")
    assert(result.contains(ServerStatusFlags.SERVER_STATUS_AUTOCOMMIT), "Should contain SERVER_STATUS_AUTOCOMMIT")
    assert(
      !result.contains(ServerStatusFlags.SERVER_MORE_RESULTS_EXISTS),
      "Should not contain SERVER_MORE_RESULTS_EXISTS"
    )
    assertEquals(result.size, 2)
  }

  test("apply(bitset: Set[Short]) should return the correct flags") {
    val shortBitset = Set[Short](0, 1) // Represents SERVER_STATUS_IN_TRANS and SERVER_STATUS_AUTOCOMMIT

    val result = ServerStatusFlags(shortBitset)

    assert(result.contains(ServerStatusFlags.SERVER_STATUS_IN_TRANS), "Should contain SERVER_STATUS_IN_TRANS")
    assert(result.contains(ServerStatusFlags.SERVER_STATUS_AUTOCOMMIT), "Should contain SERVER_STATUS_AUTOCOMMIT")
    assertEquals(result.size, 2)
  }

  test("toCode should convert bit number to flag code correctly") {
    assertEquals(ServerStatusFlags.toCode(0), 1L)
    assertEquals(ServerStatusFlags.toCode(1), 2L)
    assertEquals(ServerStatusFlags.toCode(5), 32L)
  }

  test("toBitset should combine flags into a single bitset") {
    val flags = Set(
      ServerStatusFlags.SERVER_STATUS_IN_TRANS,
      ServerStatusFlags.SERVER_STATUS_AUTOCOMMIT
    )

    val bitset = ServerStatusFlags.toBitset(flags)

    // SERVER_STATUS_IN_TRANS (1) + SERVER_STATUS_AUTOCOMMIT (2)
    assertEquals(bitset, 3L)
  }

  test("toEnumSet should convert a bitset back to flags") {
    // Combine the flags using toBitset
    val flags  = Set(ServerStatusFlags.SERVER_STATUS_IN_TRANS, ServerStatusFlags.SERVER_STATUS_DB_DROPPED)
    val bitset = ServerStatusFlags.toBitset(flags)

    val resultFlags = ServerStatusFlags.toEnumSet(bitset)

    assertEquals(resultFlags.size, 2)
    assert(resultFlags.contains(ServerStatusFlags.SERVER_STATUS_IN_TRANS), "Should contain SERVER_STATUS_IN_TRANS")
    assert(resultFlags.contains(ServerStatusFlags.SERVER_STATUS_DB_DROPPED), "Should contain SERVER_STATUS_DB_DROPPED")
  }

  test("hasBitFlag(Set[ServerStatusFlags], flag) should check flag presence") {
    val flags = Set(
      ServerStatusFlags.SERVER_STATUS_IN_TRANS,
      ServerStatusFlags.SERVER_STATUS_AUTOCOMMIT
    )

    assert(
      ServerStatusFlags.hasBitFlag(flags, ServerStatusFlags.SERVER_STATUS_IN_TRANS),
      "Should have SERVER_STATUS_IN_TRANS flag"
    )
    assert(
      ServerStatusFlags.hasBitFlag(flags, ServerStatusFlags.SERVER_STATUS_AUTOCOMMIT),
      "Should have SERVER_STATUS_AUTOCOMMIT flag"
    )
    assert(
      !ServerStatusFlags.hasBitFlag(flags, ServerStatusFlags.SERVER_STATUS_DB_DROPPED),
      "Should not have SERVER_STATUS_DB_DROPPED flag"
    )
  }

  test("hasBitFlag(Long, flag) should check flag presence in bitset") {
    // Combine the flags using toBitset
    val flags  = Set(ServerStatusFlags.SERVER_STATUS_IN_TRANS, ServerStatusFlags.SERVER_STATUS_AUTOCOMMIT)
    val bitset = ServerStatusFlags.toBitset(flags)

    assert(
      ServerStatusFlags.hasBitFlag(bitset, ServerStatusFlags.SERVER_STATUS_IN_TRANS),
      "Should have SERVER_STATUS_IN_TRANS flag"
    )
    assert(
      !ServerStatusFlags.hasBitFlag(bitset, ServerStatusFlags.SERVER_STATUS_DB_DROPPED),
      "Should not have SERVER_STATUS_DB_DROPPED flag"
    )
  }

  test("setBitFlag(Set[ServerStatusFlags], flag) should add flag to set") {
    val flags = Set(ServerStatusFlags.SERVER_STATUS_IN_TRANS)

    val updatedFlags = ServerStatusFlags.setBitFlag(flags, ServerStatusFlags.SERVER_STATUS_AUTOCOMMIT)

    assert(updatedFlags.contains(ServerStatusFlags.SERVER_STATUS_IN_TRANS), "Should contain SERVER_STATUS_IN_TRANS")
    assert(updatedFlags.contains(ServerStatusFlags.SERVER_STATUS_AUTOCOMMIT), "Should contain SERVER_STATUS_AUTOCOMMIT")
    assertEquals(updatedFlags.size, 2)
  }

  test("setBitFlag(Long, flag) should add flag to bitset") {
    // Create a bitset using toBitset
    val flags  = Set(ServerStatusFlags.SERVER_STATUS_IN_TRANS)
    val bitset = ServerStatusFlags.toBitset(flags)

    val updatedBitset = ServerStatusFlags.setBitFlag(bitset, ServerStatusFlags.SERVER_STATUS_AUTOCOMMIT)

    assert(
      ServerStatusFlags.hasBitFlag(updatedBitset, ServerStatusFlags.SERVER_STATUS_IN_TRANS),
      "Should have SERVER_STATUS_IN_TRANS flag"
    )
    assert(
      ServerStatusFlags.hasBitFlag(updatedBitset, ServerStatusFlags.SERVER_STATUS_AUTOCOMMIT),
      "Should have SERVER_STATUS_AUTOCOMMIT flag"
    )
  }
