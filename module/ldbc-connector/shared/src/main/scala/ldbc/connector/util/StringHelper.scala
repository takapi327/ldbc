/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.util

import cats.syntax.functor.toFunctorOps
import cats.Functor

import cats.effect.std.UUIDGen

object StringHelper:

  /**
   * Determines whether or not the string 'searchIn' contains the string 'searchFor', disregarding case and starting at 'startAt'. Shorthand for a
   * String.regionMatch(...)
   *
   * @param searchIn
   *   the string to search in
   * @param startAt
   *   the position to start at
   * @param searchFor
   *   the string to search for
   * @return whether searchIn starts with searchFor, ignoring case
   */
  def regionMatchesIgnoreCase(searchIn: String, startAt: Int, searchFor: String): Boolean =
    searchIn.regionMatches(true, startAt, searchFor, 0, searchFor.length)

  def isCharAtPosNotEqualIgnoreCase(
    searchIn:               String,
    pos:                    Int,
    firstCharOfSearchForUc: Char,
    firstCharOfSearchForLc: Char
  ): Boolean =
    val charAtPos = searchIn.charAt(pos)
    charAtPos != firstCharOfSearchForUc && charAtPos != firstCharOfSearchForLc

  /**
   * Finds the position of a substring within a string ignoring case.
   *
   * @param startingPosition
   *   the position to start the search from
   * @param searchIn
   *   the string to search in
   * @param searchFor
   *   the array of strings to search for
   * @return the position where <code>searchFor</code> is found within <code>searchIn</code> starting from <code>startingPosition</code>.
   */
  def indexOfIgnoreCase(startingPosition: Int, searchIn: String, searchFor: String): Int =
    val searchInLength  = searchIn.length
    val searchForLength = searchFor.length
    val stopSearchingAt = searchInLength - searchForLength

    if startingPosition > stopSearchingAt || searchForLength == 0 then -1
    else

      // Some locales don't follow upper-case rule, so need to check both
      val firstCharOfSearchForUc = Character.toUpperCase(searchFor.charAt(0))
      val firstCharOfSearchForLc = Character.toLowerCase(searchFor.charAt(0))

      def loop(i: Int): Int =
        if i > stopSearchingAt then -1
        else if isCharAtPosNotEqualIgnoreCase(searchIn, i, firstCharOfSearchForUc, firstCharOfSearchForLc) then
          loop(i + 1)
        else if regionMatchesIgnoreCase(searchIn, i, searchFor) then i
        else loop(i + 1)

      loop(startingPosition)

  end indexOfIgnoreCase

  def getUniqueSavepointId[F[_]: Functor](using F: UUIDGen[F]): F[String] =
    F.randomUUID.map(_.toString().replaceAll("-", "_"))

  /**
   * Does the string contain wildcard symbols ('%' or '_'). Used in DatabaseMetaData.
   *
   * @param src
   *   string
   * @return
   *   true if src contains wildcard symbols
   */
  def hasWildcards(src: String): Boolean =
    indexOfIgnoreCase(0, src, "%") != -1 || indexOfIgnoreCase(0, src, "_") != -1

  /**
   * Maximum length of a MySQL identifier.
   *
   * @see https://dev.mysql.com/doc/refman/en/identifier-length.html
   */
  private val MAX_IDENTIFIER_LENGTH = 64

  /**
   * MySQL reserved words. Reserved words cannot be used as identifiers without quoting.
   *
   * @see https://dev.mysql.com/doc/refman/en/keywords.html
   */
  private val MYSQL_RESERVED_WORDS: Set[String] = Set(
    "ACCESSIBLE",
    "ADD",
    "ALL",
    "ALTER",
    "ANALYZE",
    "AND",
    "AS",
    "ASC",
    "ASENSITIVE",
    "BEFORE",
    "BETWEEN",
    "BIGINT",
    "BINARY",
    "BLOB",
    "BOTH",
    "BY",
    "CALL",
    "CASCADE",
    "CASE",
    "CHANGE",
    "CHAR",
    "CHARACTER",
    "CHECK",
    "COLLATE",
    "COLUMN",
    "CONDITION",
    "CONSTRAINT",
    "CONTINUE",
    "CONVERT",
    "CREATE",
    "CROSS",
    "CUBE",
    "CUME_DIST",
    "CURRENT_DATE",
    "CURRENT_TIME",
    "CURRENT_TIMESTAMP",
    "CURRENT_USER",
    "CURSOR",
    "DATABASE",
    "DATABASES",
    "DAY_HOUR",
    "DAY_MICROSECOND",
    "DAY_MINUTE",
    "DAY_SECOND",
    "DEC",
    "DECIMAL",
    "DECLARE",
    "DEFAULT",
    "DELAYED",
    "DELETE",
    "DENSE_RANK",
    "DESC",
    "DESCRIBE",
    "DETERMINISTIC",
    "DISTINCT",
    "DISTINCTROW",
    "DIV",
    "DOUBLE",
    "DROP",
    "DUAL",
    "EACH",
    "ELSE",
    "ELSEIF",
    "EMPTY",
    "ENCLOSED",
    "ESCAPED",
    "EXCEPT",
    "EXISTS",
    "EXIT",
    "EXPLAIN",
    "FALSE",
    "FETCH",
    "FIRST_VALUE",
    "FLOAT",
    "FLOAT4",
    "FLOAT8",
    "FOR",
    "FORCE",
    "FOREIGN",
    "FROM",
    "FULLTEXT",
    "FUNCTION",
    "GENERATED",
    "GET",
    "GRANT",
    "GROUP",
    "GROUPING",
    "GROUPS",
    "HAVING",
    "HIGH_PRIORITY",
    "HOUR_MICROSECOND",
    "HOUR_MINUTE",
    "HOUR_SECOND",
    "IF",
    "IGNORE",
    "IN",
    "INDEX",
    "INFILE",
    "INNER",
    "INOUT",
    "INSENSITIVE",
    "INSERT",
    "INT",
    "INT1",
    "INT2",
    "INT3",
    "INT4",
    "INT8",
    "INTEGER",
    "INTERSECT",
    "INTERVAL",
    "INTO",
    "IO_AFTER_GTIDS",
    "IO_BEFORE_GTIDS",
    "IS",
    "ITERATE",
    "JOIN",
    "JSON_TABLE",
    "KEY",
    "KEYS",
    "KILL",
    "LAG",
    "LAST_VALUE",
    "LATERAL",
    "LEAD",
    "LEADING",
    "LEAVE",
    "LEFT",
    "LIKE",
    "LIMIT",
    "LINEAR",
    "LINES",
    "LOAD",
    "LOCALTIME",
    "LOCALTIMESTAMP",
    "LOCK",
    "LONG",
    "LONGBLOB",
    "LONGTEXT",
    "LOOP",
    "LOW_PRIORITY",
    "MANUAL",
    "MASTER_BIND",
    "MASTER_SSL_VERIFY_SERVER_CERT",
    "MATCH",
    "MAXVALUE",
    "MEDIUMBLOB",
    "MEDIUMINT",
    "MEDIUMTEXT",
    "MIDDLEINT",
    "MINUTE_MICROSECOND",
    "MINUTE_SECOND",
    "MOD",
    "MODIFIES",
    "NATURAL",
    "NOT",
    "NO_WRITE_TO_BINLOG",
    "NTH_VALUE",
    "NTILE",
    "NULL",
    "NUMERIC",
    "OF",
    "ON",
    "OPTIMIZE",
    "OPTIMIZER_COSTS",
    "OPTION",
    "OPTIONALLY",
    "OR",
    "ORDER",
    "OUT",
    "OUTER",
    "OUTFILE",
    "OVER",
    "PARALLEL",
    "PARTITION",
    "PERCENT_RANK",
    "PRECISION",
    "PRIMARY",
    "PROCEDURE",
    "PURGE",
    "QUALIFY",
    "RANGE",
    "RANK",
    "READ",
    "READS",
    "READ_WRITE",
    "REAL",
    "RECURSIVE",
    "REFERENCES",
    "REGEXP",
    "RELEASE",
    "RENAME",
    "REPEAT",
    "REPLACE",
    "REQUIRE",
    "RESIGNAL",
    "RESTRICT",
    "RETURN",
    "REVOKE",
    "RIGHT",
    "RLIKE",
    "ROW",
    "ROWS",
    "ROW_NUMBER",
    "SCHEMA",
    "SCHEMAS",
    "SECOND_MICROSECOND",
    "SELECT",
    "SENSITIVE",
    "SEPARATOR",
    "SET",
    "SHOW",
    "SIGNAL",
    "SMALLINT",
    "SPATIAL",
    "SPECIFIC",
    "SQL",
    "SQLEXCEPTION",
    "SQLSTATE",
    "SQLWARNING",
    "SQL_BIG_RESULT",
    "SQL_CALC_FOUND_ROWS",
    "SQL_SMALL_RESULT",
    "SSL",
    "STARTING",
    "STORED",
    "STRAIGHT_JOIN",
    "SYSTEM",
    "TABLE",
    "TABLESAMPLE",
    "TERMINATED",
    "THEN",
    "TINYBLOB",
    "TINYINT",
    "TINYTEXT",
    "TO",
    "TRAILING",
    "TRIGGER",
    "TRUE",
    "UNDO",
    "UNION",
    "UNIQUE",
    "UNLOCK",
    "UNSIGNED",
    "UPDATE",
    "USAGE",
    "USE",
    "USING",
    "UTC_DATE",
    "UTC_TIME",
    "UTC_TIMESTAMP",
    "VALUES",
    "VARBINARY",
    "VARCHAR",
    "VARCHARACTER",
    "VARYING",
    "VIRTUAL",
    "WHEN",
    "WHERE",
    "WHILE",
    "WINDOW",
    "WITH",
    "WRITE",
    "XOR",
    "YEAR_MONTH",
    "ZEROFILL"
  )

  /**
   * Tests whether the given character is allowed in an unquoted MySQL identifier.
   *
   * Permitted characters are ASCII letters and digits, '$', '_' and extended
   * characters (U+0080 and above).
   *
   * @see https://dev.mysql.com/doc/refman/en/identifiers.html
   */
  def isValidIdentifierChar(ch: Char): Boolean =
    (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') ||
      ch == '$' || ch == '_' || ch >= 0x0080

  /**
   * Checks whether the supplied string is a "simple" MySQL identifier, meaning it can be
   * used in SQL without quoting: non-empty, at most 64 characters long, consisting only of
   * characters permitted in unquoted identifiers, not consisting solely of digits and not a
   * reserved word.
   */
  def isSimpleIdentifier(identifier: String): Boolean =
    identifier != null && identifier.nonEmpty && identifier.length <= MAX_IDENTIFIER_LENGTH &&
      identifier.forall(isValidIdentifierChar) &&
      !identifier.forall(Character.isDigit) &&
      !MYSQL_RESERVED_WORDS.contains(identifier.toUpperCase(java.util.Locale.ROOT))

  /**
   * Renders the given value as a single-quoted SQL string literal.
   *
   * If the value is already surrounded by valid literal delimiters and its interior quoting
   * is well formed, it is returned unchanged. Otherwise a newly quoted literal is generated,
   * doubling every unescaped single quote and escaping a trailing lone backslash.
   *
   * @param value
   *   the raw value, or an already quoted literal
   * @param ansiQuotes
   *   whether the ANSI_QUOTES SQL mode is enabled; when disabled, a double-quoted value is
   *   also accepted as an already quoted literal
   * @param backslashEscapes
   *   whether backslash works as an escape character (i.e. NO_BACKSLASH_ESCAPES is disabled)
   */
  def enquoteLiteral(value: String, ansiQuotes: Boolean, backslashEscapes: Boolean): String =
    enquote(value, '\'', backslashEscapes, c => c == '\'' || (!ansiQuotes && c == '"'))

  /**
   * Renders the given identifier as a quoted SQL identifier.
   *
   * If the identifier is already surrounded by valid identifier delimiters and its interior
   * quoting is well formed, it is returned unchanged. Otherwise it is quoted with the
   * identifier quote character (backtick, or double quote when ANSI_QUOTES is enabled).
   *
   * Note that backslash never works as an escape character inside quoted identifiers.
   *
   * @param identifier
   *   the raw identifier, or an already quoted identifier
   * @param ansiQuotes
   *   whether the ANSI_QUOTES SQL mode is enabled
   */
  def enquoteIdentifier(identifier: String, ansiQuotes: Boolean): String =
    enquote(identifier, if ansiQuotes then '"' else '`', false, c => c == '`' || (ansiQuotes && c == '"'))

  /**
   * Renders the given value as a national character set literal (N'...').
   *
   * If the value is already in N'...' form with well formed quoting, it is returned
   * unchanged (normalized to an upper case N prefix). Otherwise the whole value is quoted
   * as a single-quoted literal and prefixed with N. Double quotes are never valid
   * delimiters for national character set literals, regardless of the ANSI_QUOTES SQL mode.
   *
   * @param value
   *   the raw value, or an already quoted N'...' literal
   * @param backslashEscapes
   *   whether backslash works as an escape character (i.e. NO_BACKSLASH_ESCAPES is disabled)
   */
  def enquoteNCharLiteral(value: String, backslashEscapes: Boolean): String =
    if value.length > 2 && (value.charAt(0) == 'N' || value.charAt(0) == 'n') && value.charAt(1) == '\'' then
      val literalPart = value.substring(1)
      val fixed       = enquote(literalPart, '\'', backslashEscapes, _ == '\'')
      if fixed == literalPart then "N" + fixed
      else "N" + enquote(value, '\'', backslashEscapes, _ == '\'')
    else "N" + enquote(value, '\'', backslashEscapes, _ == '\'')

  /**
   * Tests whether the value is a well formed quoted string: surrounded by the same accepted
   * delimiter, every interior occurrence of that delimiter doubled or backslash-escaped, and
   * the closing delimiter not escaped.
   */
  private def isWellFormedQuoted(value: String, backslashEscapes: Boolean, isQuoteDelimiter: Char => Boolean): Boolean =
    val end = value.length - 1
    if value.length < 2 || !isQuoteDelimiter(value.charAt(0)) || value.charAt(end) != value.charAt(0) then false
    else
      val delimiter = value.charAt(0)
      var i         = 1
      var ok        = true
      while ok && i < end do
        val c = value.charAt(i)
        if backslashEscapes && c == '\\' then
          // A backslash just before the closing delimiter would escape it.
          if i == end - 1 then ok = false
          else i += 2
        else if c == delimiter then
          if i + 1 < end && value.charAt(i + 1) == delimiter then i += 2
          else ok = false
        else i += 1
      ok

  /**
   * Quotes the value with the given quote character, doubling every unescaped occurrence of
   * the quote character in the value. When the value is detected to be already properly
   * quoted with an accepted delimiter, it is returned unchanged.
   */
  private def enquote(
    value:            String,
    quoteChar:        Char,
    backslashEscapes: Boolean,
    isQuoteDelimiter: Char => Boolean
  ): String =
    if isWellFormedQuoted(value, backslashEscapes, isQuoteDelimiter) then value
    else
      val sb = new StringBuilder(value.length + 2)
      sb.append(quoteChar)
      var i = 0
      while i < value.length do
        val c = value.charAt(i)
        if backslashEscapes && c == '\\' then
          sb.append('\\')
          if i + 1 < value.length then
            sb.append(value.charAt(i + 1))
            i += 2
          else
            // A lone trailing backslash would escape the closing quote.
            sb.append('\\')
            i += 1
        else
          sb.append(c)
          if c == quoteChar then sb.append(c)
          i += 1
      sb.append(quoteChar)
      sb.toString
