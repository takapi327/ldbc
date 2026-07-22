/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

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
   * Builds a batched query fragment. INSERT statements return only the trailing `VALUES (...)` part
   * so callers can join multiple rows; UPDATE/DELETE return the fully bound statement.
   */
  def buildBatch(original: String, params: SortedMap[Int, Parameter], noBackslashEscapes: Boolean): String =
    val placeholderCount = original.split("\\?", -1).length - 1
    require(placeholderCount == params.size, "The number of parameters does not match the number of placeholders")
    original.trim.toLowerCase match
      case q if q.startsWith("insert") =>
        val bindQuery = build(original, params, noBackslashEscapes)
        bindQuery.split("(?i)VALUES").last
      case q if q.startsWith("update") || q.startsWith("delete") => build(original, params, noBackslashEscapes)
      case _ => throw new IllegalArgumentException("The batch query must be an INSERT, UPDATE, or DELETE statement.")
