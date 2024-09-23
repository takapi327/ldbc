/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.util

import java.util.UUID

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

  def getUniqueSavepointId: String =
    val uuid = UUID.randomUUID().toString
    uuid.replaceAll("-", "_")

  /**
   * Surrounds identifier with quoteChar and duplicates these symbols inside the identifier.
   *
   * @param identifier
   *   in pedantic mode (connection property pedantic=true) identifier is treated as unquoted (as it is stored in the database) even if it starts and ends with quoteChar in non-pedantic mode if identifier starts and ends with quoteChar method treats it as already quoted and doesn't modify.
   * @param quoteChar
   *   ` or "
   * @param isPedantic
   *   are we in pedantic mode
   * @return
   *   With quoteChar="`":<br>
   *   <ul>
   *   <li>null {@code ->} null</li>
   *   <li>abc {@code ->} `abc`</li>
   *   <li>ab`c {@code ->} `ab``c`</li>
   *   <li>ab"c {@code ->} `ab"c`</li>
   *   <li>`ab``c` {@code ->} `ab``c` in non-pedantic mode or ```ab````c``` in pedantic mode</li>
   *   </ul>
   *   With quoteChar="\"":<br>
   *   <ul>
   *   <li>null {@code ->} null</li>
   *   <li>abc {@code ->} "abc"</li>
   *   <li>ab`c {@code ->} "ab`c"</li>
   *   <li>ab"c {@code ->} "ab""c"</li>
   *   <li>"ab""c" {@code ->} "ab""c" in non-pedantic mode or """ab""""c""" in pedantic mode</li>
   *   </ul>
   */
  def quoteIdentifier(identifier: String, quoteChar: String, isPedantic: Boolean): String =
    val identifierTrim = identifier.trim
    val quoteCharLength = quoteChar.length

    if quoteCharLength == 0 then identifierTrim
    else
      // Check if the identifier is correctly quoted and if quotes within are correctly escaped. If not, quote and escape it.
      if !isPedantic && identifierTrim.startsWith(quoteChar) && identifierTrim.endsWith(quoteChar) then
        // Trim outermost quotes from the identifier.
        val identifierQuoteTrimmed = identifierTrim.substring(quoteCharLength, identifierTrim.length - quoteCharLength)

        // Check for pairs of quotes.
        var quoteCharPos = identifierQuoteTrimmed.indexOf(quoteChar)

        while quoteCharPos >= 0 do
          val quoteCharNextExpectedPos = quoteCharPos + quoteCharLength
          val quoteCharNextPosition = identifierQuoteTrimmed.indexOf(quoteChar, quoteCharNextExpectedPos)

          if quoteCharNextPosition == quoteCharNextExpectedPos then
            quoteCharPos = identifierQuoteTrimmed.indexOf(quoteChar, quoteCharNextPosition + quoteCharLength)
        end while

        if quoteCharPos < 0 then identifierTrim
        else
          quoteChar + identifierTrim.replaceAll(quoteChar, quoteChar + quoteChar) + quoteChar
      else
        quoteChar + identifierTrim.replaceAll(quoteChar, quoteChar + quoteChar) + quoteChar

  /**
   * Builds and returns a fully qualified name, quoted if necessary, for the given database entity.
   *
   * @param db
   *   database name
   * @param entity
   *   identifier
   * @param quoteId
   *   quote character as defined on server
   * @param isPedantic
   *   are we in pedantic mode
   * @return fully qualified name
   */
  def getFullyQualifiedName(db: Option[String], entity: String, quoteId: String, isPedantic: Boolean): String =
    val fullyQualifiedName = new StringBuilder(quoteIdentifier(db.getOrElse(""), quoteId, isPedantic))
    fullyQualifiedName.append('.')
    fullyQualifiedName.append(quoteIdentifier(entity, quoteId, isPedantic))
    fullyQualifiedName.toString()

  /**
   * Trims the identifier, removes quote chars from first and last positions and replaces double occurrences of quote char from entire identifier, i.e.
   * converts quoted identifier into the form as it is stored in database.
   *
   * @param identifier
   *   identifier
   * @param quoteChar
   *   ` or "
   * @return
   *   <ul>
   *   <li>null {@code ->} null</li>
   *   <li>abc {@code ->} abc</li>
   *   <li>`abc` {@code ->} abc</li>
   *   <li>`ab``c` {@code ->} ab`c</li>
   *   <li>`"ab`c"` {@code ->} "ab`c"</li>
   *   <li>`ab"c` {@code ->} ab"c</li>
   *   <li>"abc" {@code ->} abc</li>
   *   <li>"`ab""c`" {@code ->} `ab"c`</li>
   *   <li>"ab`c" {@code ->} ab`c</li>
   *   </ul>
   */
  def unQuoteIdentifier(identifier: String, quoteChar: String): String =
    val identifierTrim = identifier.trim

    val quoteCharLength = quoteChar.length

    if quoteCharLength == 0 then identifierTrim
    else
      // Check if the identifier is really quoted or if it simply contains quote chars in it (assuming that the value is a valid identifier).
      if identifierTrim.startsWith(quoteChar) && identifierTrim.endsWith(quoteChar) then
        // Trim outermost quotes from the identifier.
        val identifierQuoteTrimmed = identifierTrim.substring(quoteCharLength, identifierTrim.length - quoteCharLength)

        // Check for pairs of quotes.
        var quoteCharPos = identifierQuoteTrimmed.indexOf(quoteChar)
        var result = identifierTrim
        while quoteCharPos >= 0 do
          val quoteCharNextExpectedPos = quoteCharPos + quoteCharLength
          val quoteCharNextPosition = identifierQuoteTrimmed.indexOf(quoteChar, quoteCharNextExpectedPos)

          if quoteCharNextPosition == quoteCharNextExpectedPos then
            quoteCharPos = identifierQuoteTrimmed.indexOf(quoteChar, quoteCharNextPosition + quoteCharLength)
            result = identifierTrim
              .substring(quoteCharLength, identifierTrim.length - quoteCharLength)
              .replaceAll(quoteChar + quoteChar, quoteChar)
        end while

        result

      else
        identifierTrim

  def isCharEqualIgnoreCase(charToCompare: Char, compareTpCHarUC: Char, compareToCharLC: Char): Boolean =
    Character.toLowerCase(charToCompare) == compareToCharLC || Character.toUpperCase(charToCompare) == compareTpCHarUC

  /**
   * Splits stringToSplit into a list, using the given delimiter
   *
   * @param stringToSplit
   *   the string to split
   * @param delimiter
   *   the string to split on
   * @param trim
   *   should the split strings be whitespace trimmed?
   * @return
   *   the list of strings, split by delimiter
   */
  def split(stringToSplit: Option[String], delimiter: String, trim: Boolean): List[String] =
    stringToSplit match
      case None => List.empty
      case Some(string) =>
        val tokens = string.split(delimiter, -1)
        if trim then tokens.map(_.trim).toList
        else tokens.toList

/**
 * Splits stringToSplit into a list, using the given delimiter and skipping all between the given markers.
 *
 * @param stringToSplit
 *   the string to split
 * @param delimiter
 *   the string to split on
 * @param openingMarkers
 *   characters that delimit the beginning of a text block to skip
 * @param closingMarkers
 *   characters that delimit the end of a text block to skip
 * @param trim
 *   should the split strings be whitespace trimmed?
 * @return
 *   the list of strings, split by delimiter
 */
  def split(stringToSplit: Option[String], delimiter: String, openingMarkers: String, closingMarkers: String, trim: Boolean): List[String] =
    split(stringToSplit, delimiter, openingMarkers, closingMarkers, "", trim)

/**
 * Splits stringToSplit into a list, using the given delimiter and skipping all between the given markers.
 *
 * @param stringToSplit
 *   the string to split
 * @param delimiter
 *   the string to split on
 * @param openingMarkers
 *   characters that delimit the beginning of a text block to skip
 * @param closingMarkers
 *   characters that delimit the end of a text block to skip
 * @param trim
 *   should the split strings be whitespace trimmed?
 * @param searchMode
 *   a <code>Set</code>, ideally an <code>EnumSet</code>, containing the flags from the enum <code>StringUtils.SearchMode</code> that determine the
 *   behaviour of the search
 * @return
 *   the list of strings, split by delimiter
 */
  def split(stringToSplit: Option[String], delimiter: String, openingMarkers: String, closingMarkers: String, trim: Boolean, searchMode: Set[SearchMode]): List[String] =
    split(stringToSplit, delimiter, openingMarkers, closingMarkers, "", trim, searchMode)

/**
 * Splits stringToSplit into a list, using the given delimiter and skipping all between the given markers.
 *
 * @param stringToSplit
 *   the string to split
 * @param delimiter
 *   the string to split on
 * @param openingMarkers
 *   characters that delimit the beginning of a text block to skip
 * @param closingMarkers
 *   characters that delimit the end of a text block to skip
 * @param overridingMarkers
 *   the subset of <code>openingMarkers</code> that override the remaining markers, e.g., if <code>openingMarkers = "'("</code> and
 *   <code>overridingMarkers = "'"</code> then the block between the outer parenthesis in <code>"start ('max('); end"</code> is strictly consumed,
 *   otherwise the suffix <code>" end"</code> would end up being consumed too in the process of handling the nested parenthesis.
 * @param trim
 *   should the split strings be whitespace trimmed?
 * @return
 * the list of strings, split by delimiter
 */
  def split(stringToSplit: Option[String], delimiter: String, openingMarkers: String, closingMarkers: String, overridingMarkers: String, trim: Boolean): List[String] =
    split(stringToSplit, delimiter, openingMarkers, closingMarkers, overridingMarkers, trim, Set.empty)

/**
 * Splits stringToSplit into a list, using the given delimiter and skipping all between the given markers.
 *
 * @param stringToSplit
 *   the string to split
 * @param delimiter
 *   the string to split on
 * @param openingMarkers
 *   characters that delimit the beginning of a text block to skip
 * @param closingMarkers
 *   characters that delimit the end of a text block to skip
 * @param overridingMarkers
 *   the subset of <code>openingMarkers</code> that override the remaining markers, e.g., if <code>openingMarkers = "'("</code> and
 *   <code>overridingMarkers = "'"</code> then the block between the outer parenthesis in <code>"start ('max('); end"</code> is strictly consumed,
 *   otherwise the suffix <code>" end"</code> would end up being consumed too in the process of handling the nested parenthesis.
 * @param trim
 *   should the split strings be whitespace trimmed?
 * @param searchMode
 *   a <code>Set</code>, ideally an <code>EnumSet</code>, containing the flags from the enum <code>StringUtils.SearchMode</code> that determine the
 *   behaviour of the search
 * @return
 * the list of strings, split by delimiter
 */
  def split(stringToSplit: Option[String], delimiter: String, openingMarkers: String, closingMarkers: String, overridingMarkers: String, trim: Boolean, searchMode: Set[SearchMode]): List[String] =
    val strInspector = new StringInspector(
      source = stringToSplit.getOrElse(""),
      openingMarkers = openingMarkers,
      closingMarkers = closingMarkers,
      overridingMarkers = overridingMarkers,
      defaultSearchMode = searchMode
    )
    strInspector.split(delimiter, trim)
