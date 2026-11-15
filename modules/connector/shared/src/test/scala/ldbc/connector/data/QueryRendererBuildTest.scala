/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import scala.collection.immutable.SortedMap

import ldbc.sql.SQLException

import munit.CatsEffectSuite

/**
 * Unit tests for building a client-side prepared statement.
 *
 * Regression guard for the finding that placeholders were found with `indexOf('?')`, so a `?` inside a string
 * literal or a comment of the statement received a bound value, and the quotes added around that value closed the
 * developer's literal. As in MySQL Connector/J, placeholders must be located by scanning the statement while skipping
 * quoted text and comments, and the parameters must match the placeholders exactly.
 */
class QueryRendererBuildTest extends CatsEffectSuite:

  private def params(values: Parameter*): SortedMap[Int, Parameter] =
    SortedMap(values.zipWithIndex.map { case (value, index) => (index + 1) -> value }*)

  private def build(sql: String, values: Parameter*): String =
    QueryRenderer.build(sql, params(values*), noBackslashEscapes = false)

  test("a question mark inside a string literal is not a placeholder") {
    assertEquals(
      build("SELECT * FROM t WHERE title = 'Why?' AND owner = ?", Parameter.string(" OR 1=1 -- ")),
      "SELECT * FROM t WHERE title = 'Why?' AND owner = ' OR 1=1 -- '"
    )
    assertEquals(build("SELECT \"a?\", ?", Parameter.int(1)), "SELECT \"a?\", 1")
  }

  test("a question mark inside a comment or an identifier is not a placeholder") {
    assertEquals(
      build("SELECT * FROM t WHERE /* why? */ id = ?", Parameter.string("*/ 1=1 -- ")),
      "SELECT * FROM t WHERE /* why? */ id = '*/ 1=1 -- '"
    )
    assertEquals(build("SELECT `a?b` FROM t WHERE id = ? -- why?", Parameter.int(1)), "SELECT `a?b` FROM t WHERE id = 1 -- why?")
    assertEquals(build("SELECT ? # why?\nFROM dual", Parameter.int(1)), "SELECT 1 # why?\nFROM dual")
  }

  test("a question mark inside an executable comment is a placeholder") {
    assertEquals(build("SELECT /*!50000 ? */ FROM dual", Parameter.int(1)), "SELECT /*!50000 1 */ FROM dual")
  }

  test("a question mark inside a bound value does not move the next placeholder") {
    assertEquals(build("SELECT ?, ?", Parameter.string("a?b"), Parameter.int(1)), "SELECT 'a?b', 1")
  }

  test("the quoting rules follow NO_BACKSLASH_ESCAPES") {
    assertEquals(
      QueryRenderer.build("SELECT 'C:\\', ?", params(Parameter.int(1)), noBackslashEscapes = true),
      "SELECT 'C:\\', 1"
    )
    intercept[SQLException] {
      QueryRenderer.build("SELECT 'C:\\', ?", params(Parameter.int(1)), noBackslashEscapes = false)
    }
  }

  test("a placeholder without a parameter is rejected") {
    val exception = intercept[SQLException](build("SELECT ?, ?", Parameter.int(1)))
    assertEquals(exception.getSQLState, "07001")
    assert(exception.getMessage.contains("No value specified for parameter 2"))
  }

  test("a parameter without a placeholder is rejected") {
    val exception = intercept[SQLException](build("SELECT ?", Parameter.int(1), Parameter.int(2)))
    assertEquals(exception.getSQLState, "07001")
    assert(exception.getMessage.contains("Parameter index out of range (2 > number of parameters, which is 1)"))
  }

  test("parameters are bound by index, not by order") {
    val exception = intercept[SQLException](
      QueryRenderer.build("SELECT ?, ?", SortedMap(1 -> Parameter.int(1), 3 -> Parameter.int(3)), noBackslashEscapes = false)
    )
    assert(exception.getMessage.contains("No value specified for parameter 2"))
  }

  test("an unterminated quoted string or comment is rejected") {
    val exception = intercept[SQLException](build("SELECT ? /* why?", Parameter.int(1)))
    assertEquals(exception.getSQLState, "42000")
  }
