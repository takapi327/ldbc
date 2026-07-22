/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.builder

import munit.CatsEffectSuite

import ldbc.statement.formatter.Naming

import ldbc.codegen.model.*
import ldbc.codegen.model.ColumnDefinition.*

/**
 * Verification test for the security finding: values parsed from a `.sql` file (COMMENT / DEFAULT /
 * ENUM) were spliced verbatim into generated Scala source, allowing arbitrary code to be injected
 * and executed at the consuming project's compile time. The generated fragments must now embed such
 * values only as escaped string literals or quoted identifiers, never as live code.
 */
class ColumnCodeInjectionTest extends CatsEffectSuite:

  private val builder = ColumnCodeBuilder(Naming.PASCAL)

  test("COMMENT must escape the message into a valid Scala string literal") {
    val column = ColumnDefinition("c1", DataType.VARCHAR(255, None, None), Some(List(CommentSet("a\"); evil(); (\""))))
    val code   = builder.build(column, None)
    // The quote must be escaped so the message stays inside the COMMENT("...") string literal.
    assert(code.contains("COMMENT(\"a\\\"); evil(); (\\\"\")"), code)
    assert(!code.contains("COMMENT(\"a\"); evil"), code)
  }

  test("String DEFAULT must escape the value") {
    val column = ColumnDefinition(
      "c2",
      DataType.VARCHAR(255, None, None),
      Some(List(Attribute.Condition(false), Attribute.Default.Value("x\"); evil(); (\"")))
    )
    val code = builder.build(column, None)
    assert(code.contains(".DEFAULT(\"x\\\"); evil(); (\\\"\")"), code)
    assert(!code.contains(".DEFAULT(\"x\"); evil"), code)
  }

  test("ENUM DEFAULT with a non-identifier value must be backtick-quoted, not spliced as code") {
    val column = ColumnDefinition(
      "c3",
      DataType.ENUM(List("active"), None, None),
      Some(List(Attribute.Condition(false), Attribute.Default.Value("active); evil()")))
    )
    val code = builder.build(column, None)
    assert(code.contains(".DEFAULT(C3.`active); evil()`)"), code)
    assert(!code.contains(".DEFAULT(C3.active); evil()"), code)
  }

  test("ENUM DEFAULT with a plain identifier value is left unquoted") {
    val column = ColumnDefinition(
      "c4",
      DataType.ENUM(List("active"), None, None),
      Some(List(Attribute.Condition(false), Attribute.Default.Value("active")))
    )
    assert(builder.build(column, None).contains(".DEFAULT(C4.active)"))
  }

  test("ScalaCode.escapeString / enumMember behave safely") {
    assertEquals(ScalaCode.escapeString("a\"b\\c"), "a\\\"b\\\\c")
    assertEquals(ScalaCode.enumMember("active"), "active")
    assertEquals(ScalaCode.enumMember("a b); x"), "`a b); x`")
    assertEquals(ScalaCode.enumMember("a`b"), "`ab`")
  }
