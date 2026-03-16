/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.util

import munit.FunSuite

class VersionTest extends FunSuite:

  // === compare: basic contract ===

  test("compare should return 0 for equal versions") {
    assertEquals(Version(1, 2, 3).compare(Version(1, 2, 3)), 0)
  }

  test("compare should return 0 for equal versions (0.0.0)") {
    assertEquals(Version(0, 0, 0).compare(Version(0, 0, 0)), 0)
  }

  test("compare should return 1 when major is higher") {
    assertEquals(Version(2, 0, 0).compare(Version(1, 0, 0)), 1)
  }

  test("compare should return -1 when major is lower") {
    assertEquals(Version(1, 0, 0).compare(Version(2, 0, 0)), -1)
  }

  test("compare should return 1 when minor is higher") {
    assertEquals(Version(1, 3, 0).compare(Version(1, 2, 0)), 1)
  }

  test("compare should return -1 when minor is lower") {
    assertEquals(Version(1, 2, 0).compare(Version(1, 3, 0)), -1)
  }

  test("compare should return 1 when patch is higher") {
    assertEquals(Version(1, 2, 4).compare(Version(1, 2, 3)), 1)
  }

  test("compare should return -1 when patch is lower") {
    assertEquals(Version(1, 2, 3).compare(Version(1, 2, 4)), -1)
  }

  // === compare: boundary versions used in codebase ===

  test("compare should return 0 for Version(9, 3, 0) == Version(9, 3, 0)") {
    // Used in DatabaseMetaDataImpl for INFORMATION_SCHEMA migration
    assertEquals(Version(9, 3, 0).compare(Version(9, 3, 0)), 0)
  }

  test("compare should return 0 for Version(5, 6, 4) == Version(5, 6, 4)") {
    // Used in DatabaseMetaDataImpl for fractional seconds support
    assertEquals(Version(5, 6, 4).compare(Version(5, 6, 4)), 0)
  }

  test("compare should return 0 for Version(8, 0, 5) == Version(8, 0, 5)") {
    // Used in CachingSha2PasswordPlugin
    assertEquals(Version(8, 0, 5).compare(Version(8, 0, 5)), 0)
  }

  test("compare should return 0 for Version(8, 4, 0) == Version(8, 4, 0)") {
    // Used in DatabaseMetaDataImpl for legacy version check
    assertEquals(Version(8, 4, 0).compare(Version(8, 4, 0)), 0)
  }

  // === compare: >= 0 pattern used in DatabaseMetaDataImpl ===

  test("compare >= 0 should be true for equal versions") {
    // DatabaseMetaDataImpl uses: version.compare(Version(5, 6, 4)) >= 0
    assert(Version(5, 6, 4).compare(Version(5, 6, 4)) >= 0)
  }

  test("compare >= 0 should be true for higher versions") {
    assert(Version(5, 6, 5).compare(Version(5, 6, 4)) >= 0)
  }

  test("compare >= 0 should be false for lower versions") {
    assert(!(Version(5, 6, 3).compare(Version(5, 6, 4)) >= 0))
  }

  // === compare: < 0 pattern used in DatabaseMetaDataImpl ===

  test("compare < 0 should be false for equal versions") {
    // DatabaseMetaDataImpl uses: version.compare(Version(8, 4, 0)) < 0
    assert(!(Version(8, 4, 0).compare(Version(8, 4, 0)) < 0))
  }

  test("compare < 0 should be true for lower versions") {
    assert(Version(8, 3, 9).compare(Version(8, 4, 0)) < 0)
  }

  // === compare: match case 1 pattern used in DatabaseMetaDataImpl ===

  test("compare match case 1 should not match for equal versions") {
    // DatabaseMetaDataImpl uses:
    // version.compare(Version(9, 3, 0)) match
    //   case 1 => useInformationSchema
    //   case _ => useLegacy
    // Equal versions should fall to case _ (legacy), not case 1
    // But the "case _" behavior for equal versions is a bug in the calling code:
    // Version 9.3.0 should use INFORMATION_SCHEMA, so compare returning 0
    // means the match pattern should be case v if v >= 0 =>
    val result = Version(9, 3, 0).compare(Version(9, 3, 0))
    assertEquals(result, 0)
    // With the fix, case 1 won't match for equal versions (returns 0)
    // The calling code in DatabaseMetaDataImpl may need updating to handle this
  }

  // === compare: symmetry and transitivity ===

  test("compare should be antisymmetric") {
    val a = Version(1, 2, 3)
    val b = Version(1, 3, 0)
    assertEquals(a.compare(b), -b.compare(a))
  }

  test("compare should be transitive") {
    val a = Version(1, 0, 0)
    val b = Version(1, 1, 0)
    val c = Version(1, 2, 0)
    assert(a.compare(b) < 0)
    assert(b.compare(c) < 0)
    assert(a.compare(c) < 0)
  }

  // === isSameSeries ===

  test("isSameSeries should return true for same major and minor") {
    assert(Version(1, 2, 3).isSameSeries(Version(1, 2, 5)))
  }

  test("isSameSeries should return false for different minor") {
    assert(!Version(1, 2, 3).isSameSeries(Version(1, 3, 3)))
  }

  test("isSameSeries should return false for different major") {
    assert(!Version(1, 2, 3).isSameSeries(Version(2, 2, 3)))
  }

  // === toString ===

  test("toString should format as major.minor.patch") {
    assertEquals(Version(1, 2, 3).toString, "1.2.3")
  }

  test("toString should handle zeros") {
    assertEquals(Version(0, 0, 0).toString, "0.0.0")
  }

  // === Version.apply(String) ===

  test("Version.apply(String) should parse valid version string") {
    assertEquals(Version("1.2.3"), Some(Version(1, 2, 3)))
  }

  test("Version.apply(String) should parse version with zeros") {
    assertEquals(Version("0.0.0"), Some(Version(0, 0, 0)))
  }

  test("Version.apply(String) should return None for invalid format") {
    assertEquals(Version("1.2"), None)
  }

  test("Version.apply(String) should return None for non-numeric") {
    assertEquals(Version("a.b.c"), None)
  }

  test("Version.apply(String) should reject leading zeros") {
    assertEquals(Version("01.2.3"), None)
  }
