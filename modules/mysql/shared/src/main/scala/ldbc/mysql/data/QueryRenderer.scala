/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.data

import scala.collection.immutable.SortedMap

/**
 * The single place that renders bound [[Parameter]]s into a client-side (text protocol) SQL string.
 *
 * All escaping lives here. Because [[Parameter]] itself exposes no SQL-text representation for
 * strings, the only way to turn a string parameter into a literal is through [[render]], which
 * escapes it according to the server `NO_BACKSLASH_ESCAPES` sql_mode. This makes it impossible to
 * accidentally splice an unescaped or mis-escaped value into an executed query.
 */
private[ldbc] object QueryRenderer:

  /**
   * Renders a single parameter into its executed-SQL literal.
   *
   *   - String parameters are escaped per `NO_BACKSLASH_ESCAPES` via [[escapeStringLiteral]].
   *   - Every other parameter type is `sql_mode`-independent, so its `toString` literal is used
   *     as-is (numbers, dates, NULL, hex bytes, raw fragments).
   */
  def render(param: Parameter, noBackslashEscapes: Boolean): String =
    param match
      case s: Parameter.StringParameter => escapeStringLiteral(s.value, noBackslashEscapes)
      case other                        => other.toString

  /**
   * Escapes a string value into a single-quoted MySQL string literal.
   *
   * With `noBackslashEscapes = false` (default sql_mode) the classic backslash escaping is used.
   * With `noBackslashEscapes = true` (`NO_BACKSLASH_ESCAPES` sql_mode) a backslash is a literal
   * character, so `'` -> `\'` would not neutralize the quote; escaping instead doubles the single
   * quote (`'` -> `''`), the only way to embed a quote such that it can never be consumed by a
   * preceding backslash, so the value cannot break out of the literal.
   */
  private def escapeStringLiteral(value: String, noBackslashEscapes: Boolean): String =
    val sb = new StringBuilder("'")
    if noBackslashEscapes then
      value.foreach {
        case '\'' => sb.append("''")
        case c    => sb.append(c)
      }
    else
      value.foreach {
        case '\''           => sb.append("\\'")
        case '"'            => sb.append("\\\"")
        case '\\'           => sb.append("\\\\")
        case c if c == 0x00 => sb.append("\\0")
        case c if c == 0x08 => sb.append("\\b")
        case c if c == 0x0a => sb.append("\\n")
        case c if c == 0x0d => sb.append("\\r")
        case c if c == 0x1a => sb.append("\\Z")
        case c              => sb.append(c)
      }
    sb.append("'").toString

  /**
   * Substitutes each `?` placeholder in `original` with the rendered literal of the corresponding
   * parameter, honouring the server `NO_BACKSLASH_ESCAPES` sql_mode for string parameters.
   */
  def build(original: String, params: SortedMap[Int, Parameter], noBackslashEscapes: Boolean): String =
    val result    = new StringBuilder(original.length * 2)
    var lastIndex = 0

    params.foreach {
      case (_, param) =>
        val index = original.indexOf('?', lastIndex)
        if index >= 0 then
          result.append(original.substring(lastIndex, index))
          result.append(render(param, noBackslashEscapes))
          lastIndex = index + 1
    }

    if lastIndex < original.length then result.append(original.substring(lastIndex))

    result.toString

  /**
   * Builds one entry of a batch.
   *
   * When the statement is an INSERT that [[multiValuesTemplate]] can rewrite, only the bound
   * `( ... )` rows of its `VALUES` clause are returned, so that the caller can join the rows with `,`
   * between [[MultiValuesTemplate.head]] and [[MultiValuesTemplate.tail]]. Every other INSERT, and
   * every UPDATE/DELETE, is returned as a fully bound statement for the caller to join with `;`.
   *
   * Placeholders are located by scanning the unbound statement while skipping quoted text and
   * comments, so a bound value can never move a placeholder or the boundary of the `VALUES` row.
   */
  def buildBatch(original: String, params: SortedMap[Int, Parameter], noBackslashEscapes: Boolean): String =
    val placeholders = scan(original, noBackslashEscapes)
      .map(_.tokens.collect { case Token.Placeholder(position) => position })
      .getOrElse(
        throw new IllegalArgumentException("The batch query contains an unterminated quoted string or comment.")
      )
    require(placeholders.size == params.size, "The number of parameters does not match the number of placeholders")
    original.trim.toLowerCase match
      case q if q.startsWith("insert") =>
        multiValuesTemplate(original) match
          case Some(template) => renderAt(template.row, template.rowPlaceholders, params, noBackslashEscapes)
          case None           => renderAt(original, placeholders, params, noBackslashEscapes)
      case q if q.startsWith("update") || q.startsWith("delete") =>
        renderAt(original, placeholders, params, noBackslashEscapes)
      case _ => throw new IllegalArgumentException("The batch query must be an INSERT, UPDATE, or DELETE statement.")

  /**
   * An INSERT statement split around the `( ... )` rows of its `VALUES` clause, so that a batch can be
   * sent as `head + row1 + "," + row2 + ... + tail`.
   *
   * This mirrors the multi-values rewrite of MySQL Connector/J (`QueryInfo`): the split points come
   * from the unbound statement only, and bound values are filled into each row afterwards.
   *
   * @param head
   *   the statement up to, but not including, the row, i.e. ending with the `VALUES` keyword
   * @param row
   *   the unbound rows, from the opening parenthesis of the first to the closing one of the last
   * @param rowPlaceholders
   *   the offsets of the placeholders within [[row]]
   * @param tail
   *   the statement after the row, which contains no placeholder
   */
  final case class MultiValuesTemplate(head: String, row: String, rowPlaceholders: Vector[Int], tail: String)

  /**
   * Returns the [[MultiValuesTemplate]] of `original` when its batch can safely be rewritten into a
   * single multi-row INSERT, otherwise `None`.
   *
   * The rewrite is allowed only when all of the following hold, as in MySQL Connector/J:
   *   - the first word of the statement is `INSERT`;
   *   - a `VALUES` (or `VALUE`) keyword outside quoted text and comments is directly followed by one
   *     or more parenthesised rows separated by `,`, and those rows hold every placeholder of the
   *     statement;
   *   - the statement has no `;` and no `/*! ... */` executable comment.
   *
   * The statement is scanned both with and without `NO_BACKSLASH_ESCAPES`, and the template is
   * returned only when both scans agree. The decision therefore does not depend on the sql_mode, and
   * stays the same between `addBatch` and the execution of the batch.
   */
  def multiValuesTemplate(original: String): Option[MultiValuesTemplate] =
    for
      withBackslashEscapes    <- multiValuesTemplate(original, noBackslashEscapes = false)
      withoutBackslashEscapes <- multiValuesTemplate(original, noBackslashEscapes = true)
      if withBackslashEscapes == withoutBackslashEscapes
    yield withBackslashEscapes

  private def multiValuesTemplate(original: String, noBackslashEscapes: Boolean): Option[MultiValuesTemplate] =
    scan(original, noBackslashEscapes).filterNot(_.hasExecutableComment).flatMap { result =>
      val tokens       = result.tokens
      val placeholders = tokens.collect { case Token.Placeholder(position) => position }
      val isInsert     = tokens.headOption.exists {
        case Token.Word(start, end, _) => original.substring(start, end).equalsIgnoreCase("INSERT")
        case _                         => false
      }
      val hasSemicolon = tokens.exists {
        case Token.Punct(_, ';', _) => true
        case _                      => false
      }

      def isValuesKeyword(token: Token): Boolean = token match
        case Token.Word(start, end, 0) =>
          val word = original.substring(start, end)
          word.equalsIgnoreCase("VALUES") || word.equalsIgnoreCase("VALUE")
        case _ => false

      def isPunct(index: Int, char: Char): Boolean = tokens.lift(index).exists {
        case Token.Punct(_, `char`, 0) => true
        case _                         => false
      }

      /**
       * Returns the index of the `)` that closes the last of the rows `(...), (...), ...` starting at
       * `openIndex`, or `-1` when the rows are malformed.
       */
      def lastRowClose(openIndex: Int): Int =
        val closeIndex = matchingClose(tokens, openIndex)
        if closeIndex < 0 then -1
        else if isPunct(closeIndex + 1, ',') then
          if isPunct(closeIndex + 2, '(') then lastRowClose(closeIndex + 2) else -1
        else closeIndex

      def rowAt(keywordIndex: Int): Option[MultiValuesTemplate] =
        if !isPunct(keywordIndex + 1, '(') then None
        else
          val closeIndex = lastRowClose(keywordIndex + 1)
          (tokens(keywordIndex + 1), tokens.lift(closeIndex)) match
            case (Token.Punct(open, _, _), Some(Token.Punct(close, ')', 0)))
              if placeholders.forall(p => p > open && p < close) =>
              Some(
                MultiValuesTemplate(
                  head            = original.substring(0, open),
                  row             = original.substring(open, close + 1),
                  rowPlaceholders = placeholders.map(_ - open),
                  tail            = original.substring(close + 1)
                )
              )
            case _ => None

      if !isInsert || hasSemicolon || placeholders.isEmpty then None
      else tokens.indices.iterator.filter(i => isValuesKeyword(tokens(i))).flatMap(rowAt).nextOption()
    }

  /**
   * Returns the index of the `)` token that closes the `(` token at `openIndex`, or `-1`.
   */
  private def matchingClose(tokens: Vector[Token], openIndex: Int): Int =
    var depth = 0
    var index = openIndex
    var found = -1
    while found < 0 && index < tokens.length do
      tokens(index) match
        case Token.Punct(_, '(', _) => depth += 1
        case Token.Punct(_, ')', _) =>
          depth -= 1
          if depth == 0 then found = index
        case _ => ()
      index += 1
    found

  /**
   * Replaces the placeholders at `positions` in `original` with the rendered `params`, in order.
   */
  private def renderAt(
    original:           String,
    positions:          Vector[Int],
    params:             SortedMap[Int, Parameter],
    noBackslashEscapes: Boolean
  ): String =
    val result    = new StringBuilder(original.length * 2)
    var lastIndex = 0
    positions.zip(params.values).foreach {
      case (position, param) =>
        result.append(original.substring(lastIndex, position))
        result.append(render(param, noBackslashEscapes))
        lastIndex = position + 1
    }
    result.append(original.substring(lastIndex)).toString

  /**
   * A significant piece of a statement found by [[scan]]. Whitespace, comments and the contents of
   * quoted text never produce a placeholder, word or punctuation token.
   */
  private enum Token:

    /** An unquoted `?`. */
    case Placeholder(position: Int)

    /** A run of identifier characters, with the parenthesis depth it appears at. */
    case Word(start: Int, end: Int, depth: Int)

    /** Any other single character, with the parenthesis depth it appears at. */
    case Punct(position: Int, char: Char, depth: Int)

    /** A `'...'` or `"..."` string, or a `` `...` `` identifier. */
    case Quoted(start: Int, end: Int)

  private final case class ScanResult(tokens: Vector[Token], hasExecutableComment: Boolean)

  /**
   * Splits `sql` into [[Token]]s, skipping whitespace, `-- `, `#` and `/* */` comments and the
   * contents of quoted text, following the MySQL lexical rules.
   *
   * The body of a `/*! ... */` executable comment is scanned as SQL, since the server runs it, and is
   * reported through [[ScanResult.hasExecutableComment]]. Returns `None` when a quoted string or a
   * comment is not terminated, or when the parentheses are not balanced.
   */
  private def scan(sql: String, noBackslashEscapes: Boolean): Option[ScanResult] =
    val tokens               = Vector.newBuilder[Token]
    val length               = sql.length
    var index                = 0
    var depth                = 0
    var inExecutableComment  = false
    var hasExecutableComment = false
    var valid                = true

    def isWordChar(c: Char): Boolean = c.isLetterOrDigit || c == '_' || c == '$' || c >= 0x80

    def skipToLineEnd(): Unit =
      while index < length && sql(index) != '\n' do index += 1

    while valid && index < length do
      val c    = sql(index)
      val next = if index + 1 < length then sql(index + 1) else '\u0000'
      if c == '\'' || c == '"' || c == '`' then
        val end = closingQuote(sql, index, c, noBackslashEscapes)
        if end < 0 then valid = false
        else
          tokens += Token.Quoted(index, end)
          index = end
      else if c == '#' then skipToLineEnd()
      else if c == '-' && next == '-' && (index + 2 >= length || sql(index + 2) <= ' ') then skipToLineEnd()
      else if c == '/' && next == '*' then
        if index + 2 < length && sql(index + 2) == '!' then
          hasExecutableComment = true
          inExecutableComment  = true
          index += 3
          while index < length && sql(index).isDigit do index += 1
        else
          val close = sql.indexOf("*/", index + 2)
          if close < 0 then valid = false
          else index              = close + 2
      else if c == '*' && next == '/' && inExecutableComment then
        inExecutableComment = false
        index += 2
      else if c.isWhitespace then index += 1
      else if c == '?' then
        tokens += Token.Placeholder(index)
        index += 1
      else if isWordChar(c) then
        val start = index
        while index < length && isWordChar(sql(index)) do index += 1
        tokens += Token.Word(start, index, depth)
      else
        if c == ')' then depth -= 1
        if depth < 0 then valid = false
        tokens += Token.Punct(index, c, depth)
        if c == '(' then depth += 1
        index += 1

    if valid && depth == 0 && !inExecutableComment then Some(ScanResult(tokens.result(), hasExecutableComment))
    else None

  /**
   * Returns the index just after the quote that closes the quoted text starting at `start`, or `-1`
   * when it is not terminated. A doubled quote character stands for itself, and inside `'` and `"`
   * a backslash escapes the next character unless `NO_BACKSLASH_ESCAPES` is enabled.
   */
  private def closingQuote(sql: String, start: Int, quote: Char, noBackslashEscapes: Boolean): Int =
    var index  = start + 1
    var result = -1
    while result < 0 && index < sql.length do
      val c = sql(index)
      if c == '\\' && quote != '`' && !noBackslashEscapes then index += 2
      else if c == quote then
        if index + 1 < sql.length && sql(index + 1) == quote then index += 2
        else result = index + 1
      else index += 1
    result
