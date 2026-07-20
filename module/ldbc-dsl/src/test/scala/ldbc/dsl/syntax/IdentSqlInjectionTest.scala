/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.syntax

import munit.CatsEffectSuite

import ldbc.dsl.*

/**
 * Verification test for the security finding: `ident()` is documented as "Safe to use with
 * user input", but it escapes an embedded backtick as `` \` `` (backslash) instead of doubling
 * it (`` `` ``). In MySQL, backslash is NOT an escape character inside a backtick-quoted
 * identifier, so the attacker-supplied backtick still terminates the identifier and the rest of
 * the input is parsed as live SQL — a SQL injection.
 *
 * These tests parse the produced statement exactly the way a MySQL server tokenizes a
 * backtick-quoted identifier and assert that the whole attacker-supplied name stays *inside*
 * the identifier (i.e. nothing leaks out as executable SQL). If the finding is real, they FAIL.
 */
class IdentSqlInjectionTest extends CatsEffectSuite with HelperFunctionsSyntax:

  /**
   * Tokenizes the first backtick-quoted identifier in `sql` starting at `start` (which must point
   * at the opening backtick), applying MySQL's rule: a doubled backtick (`` `` ``) is a literal
   * backtick inside the identifier; a single backtick terminates it.
   *
   * @return (decodedIdentifier, indexOfFirstCharAfterClosingBacktick)
   */
  private def parseBacktickIdentifier(sql: String, start: Int): (String, Int) =
    require(sql.charAt(start) == '`', "expected opening backtick")
    val sb = new StringBuilder
    var i  = start + 1
    var closed = false
    while i < sql.length && !closed do
      sql.charAt(i) match
        case '`' if i + 1 < sql.length && sql.charAt(i + 1) == '`' =>
          sb.append('`'); i += 2 // escaped backtick
        case '`' =>
          closed = true; i += 1 // closing backtick
        case c =>
          sb.append(c); i += 1
    (sb.result(), i)

  test("ident() must fully contain a malicious identifier (no SQL injection)") {
    val malicious = "users` WHERE 1=1 UNION SELECT password FROM secrets -- "

    val query     = sql"SELECT * FROM ${ ident(malicious) }"
    val statement = query.statement

    // Locate the identifier that follows "FROM ".
    val fromIdx = statement.indexOf("FROM `")
    assert(fromIdx >= 0, s"expected a backtick identifier after FROM in: $statement")
    val identStart = fromIdx + "FROM ".length

    val (decoded, afterIdx) = parseBacktickIdentifier(statement, identStart)
    val trailing            = statement.substring(afterIdx)

    // Secure expectation: the MySQL parser must see the ENTIRE malicious string as the identifier,
    // leaving no executable SQL after it.
    assertEquals(
      decoded,
      malicious,
      s"attacker input was not fully contained inside the identifier; statement=[$statement]"
    )
    assertEquals(
      trailing.trim,
      "",
      s"SQL injected outside the identifier: [$trailing] (full statement: [$statement])"
    )
  }

  test("ident() escapes a backtick by doubling it, not with a backslash") {
    // At the Parameter.Static level the escaped form must double the backtick.
    val col = ident("a`b")
    assertEquals(col, Parameter.Static("`a``b`"))
  }
