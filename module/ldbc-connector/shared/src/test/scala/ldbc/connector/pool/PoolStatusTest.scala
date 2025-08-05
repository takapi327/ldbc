/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import ldbc.connector.*

class PoolStatusTest extends FTestPlatform:

  test("PoolStatus should be created with correct values") {
    val status = PoolStatus(
      total   = 10,
      active  = 3,
      idle    = 7,
      waiting = 2
    )

    assertEquals(status.total, 10)
    assertEquals(status.active, 3)
    assertEquals(status.idle, 7)
    assertEquals(status.waiting, 2)
  }

  test("PoolStatus should support copy with updated values") {
    val original = PoolStatus(
      total   = 10,
      active  = 3,
      idle    = 7,
      waiting = 2
    )

    val updated = original.copy(active = 5, idle = 5)

    assertEquals(updated.total, 10)
    assertEquals(updated.active, 5)
    assertEquals(updated.idle, 5)
    assertEquals(updated.waiting, 2)

    // Original should remain unchanged
    assertEquals(original.active, 3)
    assertEquals(original.idle, 7)
  }

  test("PoolStatus should have correct equality behavior") {
    val status1 = PoolStatus(
      total   = 10,
      active  = 3,
      idle    = 7,
      waiting = 2
    )

    val status2 = PoolStatus(
      total   = 10,
      active  = 3,
      idle    = 7,
      waiting = 2
    )

    val status3 = PoolStatus(
      total   = 10,
      active  = 4,
      idle    = 6,
      waiting = 2
    )

    assertEquals(status1, status2)
    assertNotEquals(status1, status3)
  }

  test("PoolStatus should have a meaningful toString representation") {
    val status = PoolStatus(
      total   = 10,
      active  = 3,
      idle    = 7,
      waiting = 2
    )

    val str = status.toString
    assert(str.contains("PoolStatus"), "toString should contain class name")
    assert(str.contains("10"), "toString should contain total value")
    assert(str.contains("3"), "toString should contain active value")
    assert(str.contains("7"), "toString should contain idle value")
    assert(str.contains("2"), "toString should contain waiting value")
  }

  test("PoolStatus should handle edge cases with zero values") {
    val emptyPool = PoolStatus(
      total   = 0,
      active  = 0,
      idle    = 0,
      waiting = 0
    )

    assertEquals(emptyPool.total, 0)
    assertEquals(emptyPool.active, 0)
    assertEquals(emptyPool.idle, 0)
    assertEquals(emptyPool.waiting, 0)
  }

  test("PoolStatus should maintain consistency between total, active, and idle") {
    val status = PoolStatus(
      total   = 10,
      active  = 3,
      idle    = 7,
      waiting = 5
    )

    // total should equal active + idle
    assertEquals(status.total, status.active + status.idle)
  }
