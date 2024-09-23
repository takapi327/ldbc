/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.util

import java.util.UUID

import scala.annotation.tailrec

object StringHelper:

  private val WILD_COMPARE_MATCH = 0
  private val WILD_COMPARE_CONTINUE_WITH_WILD = 1
  private val WILD_COMPARE_NO_MATCH = -1

  private val WILDCARD_MANY = '%'
  private val WILDCARD_ONE = '_'
  private val WILDCARD_ESCAPE = '\\'

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
    val trimmedIdentifier = identifier.trim

    val quoteCharLength = quoteChar.length

    if quoteCharLength == 0 then trimmedIdentifier
    else if trimmedIdentifier.startsWith(quoteChar) && trimmedIdentifier.endsWith(quoteChar) then
      val identifierQuoteTrimmed = trimmedIdentifier.substring(quoteCharLength, trimmedIdentifier.length - quoteCharLength)

      def hasMatchingQuotes(str: String): Boolean =

        @tailrec
        def loop(pos: Int): Boolean =
          val quoteCharPos = str.indexOf(quoteChar, pos)
          if quoteCharPos < 0 then
            true
          else
            val quoteCharNextExpectedPos = quoteCharPos + quoteCharLength
            val quoteCharNextPosition = str.indexOf(quoteChar, quoteCharNextExpectedPos)
            if quoteCharNextPosition == quoteCharNextExpectedPos then
              loop(quoteCharNextPosition + quoteCharLength)
            else
              false

        loop(0)

      if hasMatchingQuotes(identifierQuoteTrimmed) then
        identifierQuoteTrimmed.replace(quoteChar + quoteChar, quoteChar)
      else
        trimmedIdentifier
    else
      trimmedIdentifier

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

  /**
   * Compares searchIn against searchForWildcard with wildcards, in a case insensitive manner.
   *
   * @param searchIn
   *   the string to search in
   * @param searchFor
   *   the string to search for, using the 'standard' SQL wildcard chars of '%' and '_'
   * @return
   *   true if matches
   */
  def wildCompareIgnoreCase(searchIn: String, searchFor: String): Boolean =
    wildCompareInternal(searchIn.toLowerCase, searchFor.toLowerCase) == WILD_COMPARE_MATCH

  private def wildCompareInternal(searchIn: String, searchFor: String): Int =
    if searchFor.equals("%") then WILD_COMPARE_MATCH
    else
      var searchForPos = 0
      val searchForEnd = searchFor.length()

      var searchInPos = 0
      val searchInEnd = searchIn.length()

      var result = WILD_COMPARE_NO_MATCH /* Not found, using wildcards */

      while searchForPos != searchForEnd do
        while searchFor.charAt(searchForPos) != WILDCARD_MANY && searchFor.charAt(searchForPos) != WILDCARD_ONE do
          if searchFor.charAt(searchForPos) == WILDCARD_ESCAPE && searchForPos + 1 != searchForEnd then
            searchForPos += 1
          end if

          if searchInPos == searchInEnd || Character.toUpperCase(searchFor.charAt({
            searchForPos += 1
            searchForPos
          })) != Character.toUpperCase(searchIn.charAt({searchInPos += 1; searchInPos})) then
            return WILD_COMPARE_NO_MATCH /* No match */
          end if

          if searchForPos == searchForEnd then
            return if searchInPos != searchInEnd then WILD_COMPARE_CONTINUE_WITH_WILD else WILD_COMPARE_MATCH /* Match if both are at end */
          end if

          result = WILD_COMPARE_CONTINUE_WITH_WILD

        end while

        if searchFor.charAt(searchForPos) == WILDCARD_ONE then
          while
            if searchInPos == searchInEnd then // Skip one char if possible
              return result
            searchInPos += 1
            {
              searchForPos += 1; searchForPos < searchForEnd && searchFor.charAt(searchForPos) == WILDCARD_ONE
            }
          do ()

        /* Found w_many */
        if searchFor.charAt(searchForPos) == WILDCARD_MANY then
          searchForPos += 1

          /* Remove any '%' and '_' from the wild search string */

          if searchForPos == searchForEnd then return WILD_COMPARE_MATCH /* Ok if w_many is last */
          if searchInPos == searchInEnd then return WILD_COMPARE_NO_MATCH

          var cmp: Char = searchFor.charAt(searchForPos)
          if cmp == WILDCARD_ESCAPE && searchForPos + 1 != searchForEnd then
            cmp = searchFor.charAt(searchForPos)
          end if

          searchForPos += 1

          @tailrec
          def loop(): Int =
            while searchInPos != searchInEnd && Character.toUpperCase(searchIn.charAt(searchInPos)) != Character.toUpperCase(cmp) do
              searchInPos += 1

            if searchInPos == searchInEnd then
              WILD_COMPARE_NO_MATCH
            else
              searchInPos += 1
              val tmp = wildCompareInternal(searchIn.substring(searchInPos), searchFor.substring(searchForPos))
              if tmp <= 0 then tmp
              else if searchInPos != searchInEnd then loop()
              else WILD_COMPARE_NO_MATCH

          loop()

          return WILD_COMPARE_NO_MATCH
        end if

      end while

      if searchInPos != searchInEnd then WILD_COMPARE_CONTINUE_WITH_WILD else WILD_COMPARE_MATCH
