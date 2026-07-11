/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.util

import munit.FunSuite

class StringHelperTest extends FunSuite:

  private val bools = List(false, true)

  test("isSimpleIdentifier should accept valid simple identifiers") {
    assert(StringHelper.isSimpleIdentifier("a"))
    assert(StringHelper.isSimpleIdentifier("abc"))
    assert(StringHelper.isSimpleIdentifier("ab_cd"))
    assert(StringHelper.isSimpleIdentifier("ab$cd"))
    assert(StringHelper.isSimpleIdentifier("123abc"))
    assert(StringHelper.isSimpleIdentifier("_123"))
    assert(StringHelper.isSimpleIdentifier("$123"))
    assert(StringHelper.isSimpleIdentifier("X" * 64))
  }

  test("isSimpleIdentifier should reject invalid identifiers") {
    assert(!StringHelper.isSimpleIdentifier(null))
    assert(!StringHelper.isSimpleIdentifier(""))
    assert(!StringHelper.isSimpleIdentifier(" "))
    assert(!StringHelper.isSimpleIdentifier("."))
    assert(!StringHelper.isSimpleIdentifier("ab cd"))
    assert(!StringHelper.isSimpleIdentifier("ab.cd"))
    assert(!StringHelper.isSimpleIdentifier("ab-cd"))
    assert(!StringHelper.isSimpleIdentifier("ab`cd"))
    assert(!StringHelper.isSimpleIdentifier("ab\"cd"))
    assert(!StringHelper.isSimpleIdentifier("`abc`"))
    assert(!StringHelper.isSimpleIdentifier("123456"))
    assert(!StringHelper.isSimpleIdentifier("\"abc\""))
    assert(!StringHelper.isSimpleIdentifier("X" * 65))
  }

  test("isSimpleIdentifier should reject reserved words") {
    assert(!StringHelper.isSimpleIdentifier("SELECT"))
    assert(!StringHelper.isSimpleIdentifier("select"))
    assert(!StringHelper.isSimpleIdentifier("update"))
    assert(!StringHelper.isSimpleIdentifier("Table"))
  }

  test("enquoteLiteral: values without quotes") {
    for
      asq <- bools
      bse <- bools
    do
      val ctx            = s"ansiQuotes=$asq, backslashEscapes=$bse"
      def enq(v: String) = StringHelper.enquoteLiteral(v, asq, bse)
      assertEquals(enq(""), "''", ctx)
      assertEquals(enq(" "), "' '", ctx)
      assertEquals(enq("abc"), "'abc'", ctx)
      assertEquals(enq("abc\ndef"), "'abc\ndef'", ctx)
      assertEquals(enq("abc\\"), if bse then "'abc\\\\'" else "'abc\\'", ctx)
      assertEquals(enq("abc\\\\"), "'abc\\\\'", ctx)
      assertEquals(enq("abc\\\\\\"), if bse then "'abc\\\\\\\\'" else "'abc\\\\\\'", ctx)
  }

  test("enquoteLiteral: values containing single quotes") {
    for
      asq <- bools
      bse <- bools
    do
      val ctx            = s"ansiQuotes=$asq, backslashEscapes=$bse"
      def enq(v: String) = StringHelper.enquoteLiteral(v, asq, bse)
      assertEquals(enq("'"), "''''", ctx)
      assertEquals(enq("\\'"), if bse then "'\\''" else "'\\'''", ctx)
      assertEquals(enq("\\\\'"), "'\\\\'''", ctx)
      assertEquals(enq("''"), "''", ctx)
      assertEquals(enq("\\'abc'"), if bse then "'\\'abc'''" else "'\\''abc'''", ctx)
      assertEquals(enq("\\\\'abc'"), "'\\\\''abc'''", ctx)
      assertEquals(enq("\\\\\\'abc'"), if bse then "'\\\\\\'abc'''" else "'\\\\\\''abc'''", ctx)
      assertEquals(enq("'abc\\'"), if bse then "'''abc\\''" else "'abc\\'", ctx)
      assertEquals(enq("'abc\\\\'"), "'abc\\\\'", ctx)
      assertEquals(enq("'abc\\\\\\'"), if bse then "'''abc\\\\\\''" else "'abc\\\\\\'", ctx)
      assertEquals(enq("abc'def"), "'abc''def'", ctx)
      assertEquals(enq("abc\\'def"), if bse then "'abc\\'def'" else "'abc\\''def'", ctx)
      assertEquals(enq("abc\\\\'def"), "'abc\\\\''def'", ctx)
      assertEquals(enq("abc''def"), "'abc''''def'", ctx)
      assertEquals(enq("'abc'"), "'abc'", ctx)
      assertEquals(enq("'abc\ndef'"), "'abc\ndef'", ctx)
      assertEquals(enq("'abc'def'"), "'''abc''def'''", ctx)
      assertEquals(enq("'abc\\'def'"), if bse then "'abc\\'def'" else "'''abc\\''def'''", ctx)
      assertEquals(enq("'abc\\\\'def'"), "'''abc\\\\''def'''", ctx)
      assertEquals(enq("'abc''def'"), "'abc''def'", ctx)
      assertEquals(enq("abc'def\\'ghi"), if bse then "'abc''def\\'ghi'" else "'abc''def\\''ghi'", ctx)
      assertEquals(enq("abc'def\\\\'ghi"), "'abc''def\\\\''ghi'", ctx)
      assertEquals(enq("abc''def\\'ghi"), if bse then "'abc''''def\\'ghi'" else "'abc''''def\\''ghi'", ctx)
      assertEquals(enq("abc''def\\\\'ghi"), "'abc''''def\\\\''ghi'", ctx)
      assertEquals(enq("'abc\""), "'''abc\"'", ctx)
      assertEquals(enq("'abc\"def'"), "'abc\"def'", ctx)
      assertEquals(enq("'abc\"def''ghi\""), "'''abc\"def''''ghi\"'", ctx)
      assertEquals(enq("'abc\"def'ghi'"), "'''abc\"def''ghi'''", ctx)
      assertEquals(enq("'abc\"def''ghi'"), "'abc\"def''ghi'", ctx)
  }

  test("enquoteLiteral: values containing double quotes") {
    for
      asq <- bools
      bse <- bools
    do
      val ctx            = s"ansiQuotes=$asq, backslashEscapes=$bse"
      def enq(v: String) = StringHelper.enquoteLiteral(v, asq, bse)
      assertEquals(enq("\""), "'\"'", ctx)
      assertEquals(enq("\\\""), "'\\\"'", ctx)
      assertEquals(enq("\\\\\""), "'\\\\\"'", ctx)
      assertEquals(enq("\"\""), if asq then "'\"\"'" else "\"\"", ctx)
      assertEquals(enq("\\\"abc\""), "'\\\"abc\"'", ctx)
      assertEquals(enq("\\\\\"abc\""), "'\\\\\"abc\"'", ctx)
      assertEquals(enq("\\\\\\\"abc\""), "'\\\\\\\"abc\"'", ctx)
      assertEquals(enq("\"abc\\\""), if asq || bse then "'\"abc\\\"'" else "\"abc\\\"", ctx)
      assertEquals(enq("\"abc\\\\\""), if asq then "'\"abc\\\\\"'" else "\"abc\\\\\"", ctx)
      assertEquals(enq("\"abc\\\\\\\""), if asq || bse then "'\"abc\\\\\\\"'" else "\"abc\\\\\\\"", ctx)
      assertEquals(enq("abc\"def"), "'abc\"def'", ctx)
      assertEquals(enq("abc\\\"def"), "'abc\\\"def'", ctx)
      assertEquals(enq("abc\\\\\"def"), "'abc\\\\\"def'", ctx)
      assertEquals(enq("abc\"\"def"), "'abc\"\"def'", ctx)
      assertEquals(enq("\"abc\""), if asq then "'\"abc\"'" else "\"abc\"", ctx)
      assertEquals(enq("\"abc\ndef\""), if asq then "'\"abc\ndef\"'" else "\"abc\ndef\"", ctx)
      assertEquals(enq("\"abc\"def\""), "'\"abc\"def\"'", ctx)
      assertEquals(enq("\"abc\\\"def\""), if asq || !bse then "'\"abc\\\"def\"'" else "\"abc\\\"def\"", ctx)
      assertEquals(enq("\"abc\\\\\"def\""), "'\"abc\\\\\"def\"'", ctx)
      assertEquals(enq("\"abc\"\"def\""), if asq then "'\"abc\"\"def\"'" else "\"abc\"\"def\"", ctx)
      assertEquals(enq("abc\"def\\\"ghi"), "'abc\"def\\\"ghi'", ctx)
      assertEquals(enq("abc\"def\\\\\"ghi"), "'abc\"def\\\\\"ghi'", ctx)
      assertEquals(enq("abc\"\"def\\\"ghi"), "'abc\"\"def\\\"ghi'", ctx)
      assertEquals(enq("abc\"\"def\\\\\"ghi"), "'abc\"\"def\\\\\"ghi'", ctx)
      assertEquals(enq("\"abc'"), "'\"abc'''", ctx)
      assertEquals(enq("\"abc'def\""), if asq then "'\"abc''def\"'" else "\"abc'def\"", ctx)
      assertEquals(enq("\"abc'def\"\"ghi'"), "'\"abc''def\"\"ghi'''", ctx)
      assertEquals(enq("\"abc'def\"ghi\""), "'\"abc''def\"ghi\"'", ctx)
      assertEquals(enq("\"abc'def\"\"ghi\""), if asq then "'\"abc''def\"\"ghi\"'" else "\"abc'def\"\"ghi\"", ctx)
  }

  test("enquoteIdentifier: backtick quoting (ANSI_QUOTES disabled)") {
    def enq(v: String) = StringHelper.enquoteIdentifier(v, false)
    assertEquals(enq(""), "``")
    assertEquals(enq(" "), "` `")
    assertEquals(enq("abc"), "`abc`")
    assertEquals(enq("abc\ndef"), "`abc\ndef`")
    assertEquals(enq("abc\\"), "`abc\\`")
    assertEquals(enq("abc\\\\"), "`abc\\\\`")
    assertEquals(enq("SELECT"), "`SELECT`")
    assertEquals(enq("`"), "````")
    assertEquals(enq("\\`"), "`\\```")
    assertEquals(enq("``"), "``")
    assertEquals(enq("\\`abc`"), "`\\``abc```")
    assertEquals(enq("`abc\\`"), "`abc\\`")
    assertEquals(enq("abc`def"), "`abc``def`")
    assertEquals(enq("abc``def"), "`abc````def`")
    assertEquals(enq("`abc`"), "`abc`")
    assertEquals(enq("`abc`def`"), "```abc``def```")
    assertEquals(enq("`abc``def`"), "`abc``def`")
    assertEquals(enq("\""), "`\"`")
    assertEquals(enq("\"\""), "`\"\"`")
    assertEquals(enq("\"abc\""), "`\"abc\"`")
    assertEquals(enq("abc\"def"), "`abc\"def`")
  }

  test("enquoteIdentifier: double quoting (ANSI_QUOTES enabled)") {
    def enq(v: String) = StringHelper.enquoteIdentifier(v, true)
    assertEquals(enq(""), "\"\"")
    assertEquals(enq(" "), "\" \"")
    assertEquals(enq("abc"), "\"abc\"")
    assertEquals(enq("abc\\"), "\"abc\\\"")
    assertEquals(enq("`"), "\"`\"")
    assertEquals(enq("``"), "``")
    assertEquals(enq("`abc`"), "`abc`")
    assertEquals(enq("\""), "\"\"\"\"")
    assertEquals(enq("\"\""), "\"\"")
    assertEquals(enq("\"abc\""), "\"abc\"")
    assertEquals(enq("abc\"def"), "\"abc\"\"def\"")
    assertEquals(enq("abc\"\"def"), "\"abc\"\"\"\"def\"")
    assertEquals(enq("\"abc\"def\""), "\"\"\"abc\"\"def\"\"\"")
  }

  test("enquoteNCharLiteral: raw values") {
    for bse <- bools do
      val ctx            = s"backslashEscapes=$bse"
      def enq(v: String) = StringHelper.enquoteNCharLiteral(v, bse)
      assertEquals(enq(""), "N''", ctx)
      assertEquals(enq(" "), "N' '", ctx)
      assertEquals(enq("abc"), "N'abc'", ctx)
      assertEquals(enq("abc\ndef"), "N'abc\ndef'", ctx)
      assertEquals(enq("abc\\"), if bse then "N'abc\\\\'" else "N'abc\\'", ctx)
      assertEquals(enq("abc\\\\"), "N'abc\\\\'", ctx)
      assertEquals(enq("abc\\\\\\"), if bse then "N'abc\\\\\\\\'" else "N'abc\\\\\\'", ctx)
      assertEquals(enq("'"), "N''''", ctx)
      assertEquals(enq("\\'"), if bse then "N'\\''" else "N'\\'''", ctx)
      assertEquals(enq("\\\\'"), "N'\\\\'''", ctx)
      assertEquals(enq("''"), "N''", ctx)
      assertEquals(enq("'abc'"), "N'abc'", ctx)
  }

  test("enquoteNCharLiteral: values already in N'...' form") {
    for bse <- bools do
      val ctx            = s"backslashEscapes=$bse"
      def enq(v: String) = StringHelper.enquoteNCharLiteral(v, bse)
      assertEquals(enq("N'abc'"), "N'abc'", ctx)
      assertEquals(enq("n'abc'"), "N'abc'", ctx)
      assertEquals(enq("N'abc'def'"), "N'N''abc''def'''", ctx)
      assertEquals(enq("N'abc''def'"), "N'abc''def'", ctx)
      assertEquals(enq("N'abc\\'def'"), if bse then "N'abc\\'def'" else "N'N''abc\\''def'''", ctx)
      assertEquals(enq("N'abc\"def'"), "N'abc\"def'", ctx)
      assertEquals(enq("N'abc\"\"def'"), "N'abc\"\"def'", ctx)
      assertEquals(enq("N'abc\\\"def'"), "N'abc\\\"def'", ctx)
  }

  test("enquoteNCharLiteral: double quotes are never valid NCHAR literal delimiters") {
    for bse <- bools do
      val ctx            = s"backslashEscapes=$bse"
      def enq(v: String) = StringHelper.enquoteNCharLiteral(v, bse)
      assertEquals(enq("\""), "N'\"'", ctx)
      assertEquals(enq("\\\""), "N'\\\"'", ctx)
      assertEquals(enq("\\\\\""), "N'\\\\\"'", ctx)
      assertEquals(enq("\"\""), "N'\"\"'", ctx)
      assertEquals(enq("\"abc\""), "N'\"abc\"'", ctx)
      assertEquals(enq("N\"abc\""), "N'N\"abc\"'", ctx)
      assertEquals(enq("n\"abc\""), "N'n\"abc\"'", ctx)
      assertEquals(enq("N\"abc\"def\""), "N'N\"abc\"def\"'", ctx)
      assertEquals(enq("N\"abc\"\"def\""), "N'N\"abc\"\"def\"'", ctx)
      assertEquals(enq("N\"abc\\\"def\""), "N'N\"abc\\\"def\"'", ctx)
      assertEquals(enq("N\"abc'def\""), "N'N\"abc''def\"'", ctx)
      assertEquals(enq("N\"abc''def\""), "N'N\"abc''''def\"'", ctx)
      assertEquals(enq("N\"abc\\'def\""), if bse then "N'N\"abc\\'def\"'" else "N'N\"abc\\''def\"'", ctx)
  }
