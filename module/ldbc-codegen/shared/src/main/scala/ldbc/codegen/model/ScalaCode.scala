/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.model

/**
 * Helpers for safely embedding SQL-derived text (comments, DEFAULT values, ENUM members) into the
 * generated Scala source.
 *
 * Values parsed out of a `.sql` file (e.g. a `COMMENT '...'` or `DEFAULT '...'`) can contain any
 * character except a single quote, so splicing them verbatim into generated code allows arbitrary
 * Scala to be injected and executed at the consuming project's compile time. These helpers escape /
 * quote such values so they can only ever be data, never code.
 */
object ScalaCode:

  /**
   * Escapes a string so it can be embedded inside a Scala double-quoted string literal `"..."`.
   * Backslash and double quote are escaped so the value cannot break out of the literal, and the
   * characters that are illegal inside a single-line literal (newline, carriage return, ...) are
   * turned into their escape sequences.
   */
  def escapeString(value: String): String =
    val sb = new StringBuilder(value.length + 2)
    value.foreach {
      case '\\'           => sb.append("\\\\")
      case '"'            => sb.append("\\\"")
      case c if c == 0x08 => sb.append("\\b")
      case c if c == 0x09 => sb.append("\\t")
      case c if c == 0x0a => sb.append("\\n")
      case c if c == 0x0c => sb.append("\\f")
      case c if c == 0x0d => sb.append("\\r")
      case c              => sb.append(c)
    }
    sb.toString

  /**
   * Renders a value as a Scala identifier for use as an `enum` case (and the corresponding
   * `Enum.value` selector). A value that is already a plain identifier is used as-is; anything else
   * is wrapped in backticks (with any backticks removed, since a backtick-quoted identifier cannot
   * contain one) so arbitrary characters cannot break out and inject code.
   */
  def enumMember(value: String): String =
    if value.matches("[A-Za-z_][A-Za-z0-9_]*") then value
    else "`" + value.replace("`", "") + "`"
