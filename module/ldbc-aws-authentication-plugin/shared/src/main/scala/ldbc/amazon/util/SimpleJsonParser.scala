/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.util

/**
 * Simple JSON parser for AWS credential responses.
 * 
 * This parser is designed specifically for parsing AWS service responses that contain
 * credentials and other simple data structures. It avoids external dependencies to
 * maintain compatibility with Scala.js and Scala Native environments.
 * 
 * The parser handles flat JSON objects with string, number, boolean, and null values
 * but does not support nested objects or arrays.
 */
object SimpleJsonParser:

  /**
   * Represents a parsed JSON object with string-valued fields.
   * 
   * This class provides convenient methods for accessing JSON fields with
   * proper null handling and error reporting for missing required fields.
   * 
   * @param fields Map of field names to optional string values (None represents JSON null)
   */
  case class JsonObject(fields: Map[String, Option[String]]):

    /**
     * Gets an optional field value.
     * 
     * @param key The field name to retrieve
     * @return Some(value) if the field exists and is not null, None otherwise
     */
    def get(key: String): Option[String] = fields.get(key).flatten

    /**
     * Gets a field value with empty string as fallback.
     * 
     * @param key The field name to retrieve
     * @return The field value or empty string if the field is missing or null
     */
    def getOrEmpty(key: String): String = fields.get(key).flatten.getOrElse("")

    /**
     * Requires a field to exist with a non-null value.
     * 
     * This method is useful for extracting required fields from JSON responses
     * with proper error messaging.
     * 
     * @param key The field name to retrieve
     * @return Right(value) if the field exists and is not null, Left(error message) otherwise
     */
    def require(key: String): Either[String, String] =
      fields.get(key) match
        case None              => Left(s"Required field '$key' not found")
        case Some(None)        => Left(s"Field '$key' is null")
        case Some(Some(value)) => Right(value)

  /**
   * Parses a flat JSON object with string values.
   *
   * Supports:
   * - String values
   * - Number values (returned as string)
   * - Boolean values (returned as "true"/"false")
   * - Null values (returned as empty string)
   *
   * Does NOT support:
   * - Nested objects
   * - Arrays
   */
  def parse(json: String): Either[String, JsonObject] =
    try
      val trimmed = json.trim
      if !trimmed.startsWith("{") || !trimmed.endsWith("}") then Left("Invalid JSON: must be an object")
      else
        val content = trimmed.substring(1, trimmed.length - 1).trim
        if content.isEmpty then Right(JsonObject(Map.empty))
        else
          val fields = parseFields(content)
          Right(JsonObject(fields))
    catch case ex: Exception => Left(s"JSON parse error: ${ ex.getMessage }")

  private def parseFields(content: String): Map[String, Option[String]] =
    val result = scala.collection.mutable.Map[String, Option[String]]()
    var idx    = 0

    while idx < content.length do
      // Skip whitespace
      idx = skipWhitespace(content, idx)
      if idx < content.length && content.charAt(idx) != '}' then
        // Parse key
        val (key, nextIdx1) = parseString(content, idx)
        idx = skipWhitespace(content, nextIdx1)

        // Expect colon
        if idx >= content.length || content.charAt(idx) != ':' then
          throw new IllegalArgumentException(s"Expected ':' after key '$key' at position $idx")

        idx = skipWhitespace(content, idx + 1)

        // Parse value
        val (value, nextIdx2) = parseValue(content, idx)
        result(key) = Option(value)
        idx         = skipWhitespace(content, nextIdx2)

        // Skip comma if present
        if idx < content.length && content.charAt(idx) == ',' then idx += 1

    result.toMap

  private def skipWhitespace(s: String, from: Int): Int =
    var idx = from
    while idx < s.length && s.charAt(idx).isWhitespace do idx += 1
    idx

  private def parseString(s: String, from: Int): (String, Int) =
    if from >= s.length || s.charAt(from) != '"' then
      throw new IllegalArgumentException(s"Expected '\"' at position $from")

    val sb      = new StringBuilder
    var idx     = from + 1
    var escaped = false

    while idx < s.length do
      val ch = s.charAt(idx)
      if escaped then
        ch match
          case '"'  => sb.append('"')
          case '\\' => sb.append('\\')
          case '/'  => sb.append('/')
          case 'b'  => sb.append('\b')
          case 'f'  => sb.append('\f')
          case 'n'  => sb.append('\n')
          case 'r'  => sb.append('\r')
          case 't'  => sb.append('\t')
          case 'u'  =>
            if idx + 4 >= s.length then throw new IllegalArgumentException("Invalid unicode escape sequence")
            val hex = s.substring(idx + 1, idx + 5)
            sb.append(Integer.parseInt(hex, 16).toChar)
            idx += 4
          case _ => throw new IllegalArgumentException(s"Invalid escape sequence: \\$ch")
        escaped = false
      else if ch == '\\' then escaped = true
      else if ch == '"' then return (sb.toString, idx + 1)
      else sb.append(ch)
      idx += 1

    throw new IllegalArgumentException("Unterminated string")

  private def parseValue(s: String, from: Int): (String, Int) =
    if from >= s.length then throw new IllegalArgumentException(s"Unexpected end of input at position $from")

    val ch = s.charAt(from)

    if ch == '"' then
      // String value
      parseString(s, from)
    else if ch == 'n' && s.length >= from + 4 && s.substring(from, from + 4) == "null" then
      // null value
      (null, from + 4)
    else if ch == 't' && s.length >= from + 4 && s.substring(from, from + 4) == "true" then ("true", from + 4)
    else if ch == 'f' && s.length >= from + 5 && s.substring(from, from + 5) == "false" then ("false", from + 5)
    else if ch == '-' || ch.isDigit then
      // Number value
      var idx = from
      while idx < s.length && isNumberChar(s.charAt(idx)) do idx += 1
      (s.substring(from, idx), idx)
    else if ch == '{' then
      // Nested object - skip it entirely
      val endIdx = findMatchingBrace(s, from)
      ("{...}", endIdx + 1)
    else if ch == '[' then
      // Array - skip it entirely
      val endIdx = findMatchingBracket(s, from)
      ("[...]", endIdx + 1)
    else throw new IllegalArgumentException(s"Unexpected character '$ch' at position $from")

  private def isNumberChar(ch: Char): Boolean =
    ch.isDigit || ch == '.' || ch == '-' || ch == '+' || ch == 'e' || ch == 'E'

  private def findMatchingBrace(s: String, from: Int): Int =
    var depth    = 0
    var idx      = from
    var inString = false
    var escaped  = false

    while idx < s.length do
      val ch = s.charAt(idx)
      if escaped then escaped = false
      else if ch == '\\' && inString then escaped = true
      else if ch == '"' then inString = !inString
      else if !inString then
        if ch == '{' then depth += 1
        else if ch == '}' then
          depth -= 1
          if depth == 0 then return idx
      idx += 1

    throw new IllegalArgumentException("Unmatched brace")

  private def findMatchingBracket(s: String, from: Int): Int =
    var depth    = 0
    var idx      = from
    var inString = false
    var escaped  = false

    while idx < s.length do
      val ch = s.charAt(idx)
      if escaped then escaped = false
      else if ch == '\\' && inString then escaped = true
      else if ch == '"' then inString = !inString
      else if !inString then
        if ch == '[' then depth += 1
        else if ch == ']' then
          depth -= 1
          if depth == 0 then return idx
      idx += 1

    throw new IllegalArgumentException("Unmatched bracket")
