/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import munit.CatsEffectSuite

/**
 * Unit tests locking in the mode-aware string escaping used for client-side statement construction.
 *
 * Regression guard for the SQL-injection finding: under `NO_BACKSLASH_ESCAPES` the escaping must
 * switch from backslash escaping (`'` -> `\'`) to quote doubling (`'` -> `''`), otherwise a quote
 * can break out of its string literal.
 */
class ParameterEscapeTest extends CatsEffectSuite:

  private def escape(value: String, noBackslashEscapes: Boolean): String =
    QueryRenderer.render(Parameter.string(value), noBackslashEscapes)

  test("default sql_mode keeps classic backslash escaping") {
    assertEquals(escape("a'b", noBackslashEscapes = false), "'a\\'b'")
    // A backslash is doubled so the server (backslash escapes on) reads a single backslash.
    assertEquals(escape("a\\b", noBackslashEscapes = false), "'a\\\\b'")
  }

  test("NO_BACKSLASH_ESCAPES doubles the quote and leaves backslash literal") {
    // The quote is doubled, never backslash-escaped, so it cannot break out of the literal.
    assertEquals(escape("a'b", noBackslashEscapes = true), "'a''b'")
    // Backslash must stay literal (doubling it would insert two backslashes in this mode).
    assertEquals(escape("a\\b", noBackslashEscapes = true), "'a\\b'")
  }

  test("injection payload cannot escape the literal under NO_BACKSLASH_ESCAPES") {
    val payload  = "zzz' OR 1=1 -- "
    val rendered = escape(payload, noBackslashEscapes = true)
    // Every inner quote is doubled, so there are no standalone quotes -> no breakout.
    assertEquals(rendered, "'zzz'' OR 1=1 -- '")
  }

  test("render dispatches on parameter type") {
    // String parameters are escaped; static parameters render their mode-independent literal.
    assertEquals(QueryRenderer.render(Parameter.string("a'b"), noBackslashEscapes = true), "'a''b'")
    assertEquals(QueryRenderer.render(Parameter.int(42), noBackslashEscapes = true), "42")
    assertEquals(QueryRenderer.render(Parameter.none, noBackslashEscapes = true), "NULL")
  }
