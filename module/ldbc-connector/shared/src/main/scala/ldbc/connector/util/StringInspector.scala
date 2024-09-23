/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.util

import scala.util.boundary, boundary.break

/**
 * Utility class to inspect a MySQL string, typically a query string.
 *
 * Provides string searching and manipulation operations such as finding sub-strings, matching sub-strings or building a comments-free version of a string.
 *
 * This object keeps internal state that affect the operations, e.g., executing an indexOf operation after another causes the second to start the search form
 * where the previous one stopped.
 */
class StringInspector(
  source: String,
  openingMarkers: String,
  closingMarkers: String,
  overridingMarkers: String,
  defaultSearchMode: Set[SearchMode],
  srcLen: Int = 0
):

  // Length of MySQL version reference in comments of type '/*![00000] */'.
  private final val NON_COMMENTS_MYSQL_VERSION_REF_LENGTH = 5

  private var pos: Int = 0
  private var stopAt: Int = 0
  private var escaped: Boolean = false
  private var inMysqlBlock: Boolean = false

  val markedPos = pos
  val markedStopAt = stopAt
  val markedEscaped = escaped
  val markedInMysqlBlock = inMysqlBlock

  /**
   * Resets this object's state to original values. Allows to reuse the object from a fresh start.
   */
  private def restart(): Unit =
    this.pos = 0
    this.stopAt = this.srcLen
    this.escaped = false
    this.inMysqlBlock = false


  /**
   * Returns the position of the next valid character using the given search mode instead of the default one. This method does not increment the current
   * position automatically, i.e., if already positioned in a valid character then repeated calls return always the same index.
   * If the character in the current position matches one of the prefixes that determine a skipping block, then the position marker advances to the first
   * character after the block to skip.
   *
   * @param searchMode
   *   the search mode to use in this operation
   * @return
   *   the position of the next valid character, or the current position if already on a valid character
   */
  private def indexOfNextChar(searchMode: Set[SearchMode]): Int =
    if pos >= stopAt then  -1
    else
      var c0 = Character.MIN_VALUE // Current char.
      var c1 = source.charAt(pos) // Lookahead(1).
      var c2 = if pos + 1 < srcLen then source.charAt(pos + 1) else Character.MIN_VALUE // Lookahead(2).

      while pos < stopAt do
        c0 = c1
        c1 = c2
        c2 = if pos + 2 < srcLen then source.charAt(pos + 2) else Character.MIN_VALUE

        var dashDashCommentImmediateEnd = false
        val checkSkipConditions = !searchMode.contains(SearchMode.ALLOW_BACKSLASH_ESCAPE) || !escaped

        if checkSkipConditions && searchMode.contains(SearchMode.SKIP_BETWEEN_MARKERS) && openingMarkers.indexOf(c0) != -1 then
          // Opening marker found, skip until closing
          indexOfClosingMarker(searchMode)
          if pos >= stopAt then
            pos -= 1 // Reached stop position. Correct position will be set by outer loop
          else
            // Reset lookahead
            c1 = if pos + 1 < srcLen then source.charAt(pos + 1) else Character.MIN_VALUE
            c2 = if pos + 2 < srcLen then source.charAt(pos + 2) else Character.MIN_VALUE
        else if checkSkipConditions && searchMode.contains(SearchMode.SKIP_BLOCK_COMMENTS) && c0 == '/' && c1 == '*' && c2 != '!' && c2 != '+' then
          // Comments block found, skip until end of block ("*/") (backslash escape doesn't work in comments)
          // Does not include hint blocks ("/*!" or "/*+")
          pos += 1 // Move to next char ('*')
          while
            pos += 1
            pos < stopAt && (source.charAt(pos) != '*' || (if pos + 1 < srcLen then source.charAt(pos + 1) else Character.MIN_VALUE) != '/')
          do ()
          if pos >= stopAt then
            pos -= 1 // Reached stop position. Correct position will be set by outer loop
          else
            pos += 1 // Move to next char ('/')

          // Reset lookahead
          c1 = if pos + 1 < srcLen then source.charAt(pos + 1) else Character.MIN_VALUE
          c2 = if pos + 2 < srcLen then source.charAt(pos + 2) else Character.MIN_VALUE

        else if checkSkipConditions && searchMode.contains(SearchMode.SKIP_LINE_COMMENTS) &&
          (c0 == '-' && c1 == '-' && (c2.isWhitespace || {
            dashDashCommentImmediateEnd = c2 == ';'; dashDashCommentImmediateEnd
          } || c2 == Character.MIN_VALUE) || c0 == '#') then
          if dashDashCommentImmediateEnd then
            // Comments line found but closed immediately by query delimiter marker
            pos += 2 // Move to next char (';')
            // Reset lookahead
            c1 = if pos + 1 < srcLen then source.charAt(pos + 1) else Character.MIN_VALUE
            c2 = if pos + 2 < srcLen then source.charAt(pos + 2) else Character.MIN_VALUE
          else
            // Comments line found, skip until EOL (backslash escape doesn't work on comments)
            while
              pos += 1
              pos < stopAt && {
                c0 = source.charAt(pos); c0 != '\n' && c0 != '\r'
              }
            do ()
            if pos >= stopAt then
              pos -= 1 // Reached stop position. Correct position will be set by outer loop
            else
              // Reset lookahead
              c1 = if pos + 1 < srcLen then source.charAt(pos + 1) else Character.MIN_VALUE
              if c0 == '\r' && c1 == '\n' then
                // \r\n sequence found
                pos += 1 // Skip next char ('\n')
                c1 = if pos + 1 < srcLen then source.charAt(pos + 1) else Character.MIN_VALUE
              c2 = if pos + 2 < srcLen then source.charAt(pos + 2) else Character.MIN_VALUE

        else if checkSkipConditions && searchMode.contains(SearchMode.SKIP_HINT_BLOCKS) && c0 == '/' && c1 == '*' && c2 == '+' then
          // Hints block found, skip until end of block ("*/") (backslash escape doesn't work in hints)
          pos += 2 // Move to next char ('+')
          while
            pos += 1
            pos < stopAt && (source.charAt(pos) != '*' || (if pos + 1 < srcLen then source.charAt(pos + 1) else Character.MIN_VALUE) != '/')
          do ()
          if pos >= stopAt then
            pos -= 1 // Reached stop position. Correct position will be set by outer loop
          else
            pos += 1 // Move to next char ('/')

          // Reset lookahead
          c1 = if pos + 1 < srcLen then source.charAt(pos + 1) else Character.MIN_VALUE
          c2 = if pos + 2 < srcLen then source.charAt(pos + 2) else Character.MIN_VALUE

        else if checkSkipConditions && searchMode.contains(SearchMode.SKIP_MYSQL_MARKERS) && c0 == '/' && c1 == '*' && c2 == '!' then
          // MySQL specific block found, move to end of opening marker ("/*![12345]")
          pos += 2 // Move to next char ('!')
          if c2 == '!' then
            // Check if 5 digits MySQL version reference comes next, if so skip them
            val digitsCount = (0 until NON_COMMENTS_MYSQL_VERSION_REF_LENGTH).takeWhile(i =>
              pos + 1 + i < srcLen && source.charAt(pos + 1 + i).isDigit
            ).length
            if digitsCount == NON_COMMENTS_MYSQL_VERSION_REF_LENGTH then
              pos += NON_COMMENTS_MYSQL_VERSION_REF_LENGTH
              if pos >= stopAt then
                pos = stopAt - 1 // Reached stop position. Correct position will be set by outer loop

          // Reset lookahead
          c1 = if pos + 1 < srcLen then source.charAt(pos + 1) else Character.MIN_VALUE
          c2 = if pos + 2 < srcLen then source.charAt(pos + 2) else Character.MIN_VALUE

          inMysqlBlock = true

        else if inMysqlBlock && checkSkipConditions && searchMode.contains(SearchMode.SKIP_MYSQL_MARKERS) && c0 == '*' && c1 == '/' then
          // MySQL block closing marker ("*/") found
          pos += 1 // move to next char ('/')
          // Reset lookahead
          c1 = c2
          c2 = if pos + 2 < srcLen then source.charAt(pos + 2) else Character.MIN_VALUE

          inMysqlBlock = false

        else if !searchMode.contains(SearchMode.SKIP_WHITE_SPACE) || !c0.isWhitespace then
          // Whitespace is not affected by backslash escapes
          return pos

        // Reaching here means that the position has incremented thus an 'escaped' status no longer holds
        escaped = false
        pos += 1

      -1

  /**
   * Returns the position of the next closing marker corresponding to the opening marker in the current position.
   * If the current position is not an opening marker, then -1 is returned instead.
   *
   * @param searchMode
   *   the search mode to use in this operation
   * @return
   *   the position of the next closing marker corresponding to the opening marker in the current position
   */
  private def indexOfClosingMarker(searchMode: Set[SearchMode]): Int =
    if source == null || pos >= stopAt then return -1

    val c0 = source.charAt(pos) // Current char, also the opening marker.
    val markerIndex = openingMarkers.indexOf(c0)
    if markerIndex == -1 then
      // Not at an opening marker.
      return pos

    var nestedMarkersCount = 0
    val openingMarker = c0
    val closingMarker = closingMarkers.charAt(markerIndex)
    val outerIsAnOverridingMarker = overridingMarkers.indexOf(openingMarker) != -1

    while
      pos += 1
      pos < stopAt && {
        val c = source.charAt(pos)
        c != closingMarker || nestedMarkersCount != 0
      }
    do
      val c = source.charAt(pos)
      if !outerIsAnOverridingMarker && overridingMarkers.indexOf(c) != -1 then
        // There is an overriding marker that needs to be consumed before returning to the previous marker.
        val overridingMarkerIndex = openingMarkers.indexOf(c) // OverridingMarkers must be a sub-list of openingMarkers.
        var overridingNestedMarkersCount = 0
        val overridingOpeningMarker = c
        val overridingClosingMarker = closingMarkers.charAt(overridingMarkerIndex)

        while
          pos += 1
          pos < stopAt && {
            val c = source.charAt(pos)
            c != overridingClosingMarker || overridingNestedMarkersCount != 0
          }
        do
          val c = source.charAt(pos)
          if c == overridingOpeningMarker then
            overridingNestedMarkersCount += 1
          else if c == overridingClosingMarker then
            overridingNestedMarkersCount -= 1
          else if searchMode.contains(SearchMode.ALLOW_BACKSLASH_ESCAPE) && c == '\\' then
            pos += 1 // Next char is escaped, skip it.

        if pos >= stopAt then
          pos -= 1 // Reached stop position. Correct position will be set by outer loop.
      else if c == openingMarker then
        nestedMarkersCount += 1
      else if c == closingMarker then
        nestedMarkersCount -= 1
      else if searchMode.contains(SearchMode.ALLOW_BACKSLASH_ESCAPE) && c == '\\' then
        pos += 1 // Next char is escaped, skip it.

    pos

  /**
   * Returns the character in the current position.
   *
   * @return
   *   the character in the current position
   */
  def getChar(): Char =
    if pos >= stopAt then Character.MIN_VALUE
    else source.charAt(pos)

  /**
   * Increments the current position index, by one, taking into consideration the "escaped" status of current character, if the mode
   * [[SearchMode.ALLOW_BACKSLASH_ESCAPE]] is present in the search mode specified.
   *
   * @param searchMode
   *   the search mode to use in this operation
   * @return
   *   the new current position
   */
  def incrementPosition(searchMode: Set[SearchMode]): Int =
    if pos >= stopAt then -1
    else if searchMode.contains(SearchMode.ALLOW_BACKSLASH_ESCAPE) && getChar() == '\\' then
      escaped = !escaped
      pos += 1
      pos
    else if escaped then
      escaped = false
      pos += 1
      pos
    else
      pos += 1
      pos

  /**
   * Increments the current position index, by be given number, taking into consideration the "escaped" status of current character, if the mode
   * [[SearchMode.ALLOW_BACKSLASH_ESCAPE]] is present in the default search mode.
   *
   * @param by
   *   the number of positions to increment
   * @return
   *   the new current position
   */
  def incrementPosition(by: Int): Int = incrementPosition(by, defaultSearchMode)

  /**
   * Increments the current position index, by be given number, taking into consideration the "escaped" status of current character, if the mode
   * [[SearchMode.ALLOW_BACKSLASH_ESCAPE]] is present in the specified search mode.
   *
   * @param by
   *   the number of positions to increment
   * @param searchMode
   *   the search mode to use in this operation
   * @return
   *   the new current position
   */
  def incrementPosition(by: Int, searchMode: Set[SearchMode]): Int =
    boundary:
      for i <- 0 until by do
        if incrementPosition(searchMode) == -1 then break(i)
        -1
    pos

  /**
   * Finds the position of the given string within the source string, ignoring case, with the option to skip text delimited by the specified markers or inside
   * comment blocks.
   *
   * @param searchFor
   *   the sub-string to search for
   * @return
   *   the position where the sub-string is found, starting from the current position, or -1 if not found
   */
  def indexOfIgnoreCase(searchFor: String): Int =
    indexOfIgnoreCase(Some(searchFor), defaultSearchMode)

  /**
   * Finds the position of the given string within the source string, ignoring case, with the option to skip text delimited by the specified markers or inside
   * comment blocks.
   *
   * @param searchFor
   *   the sub-string to search for
   * @param searchMode
   *   the search mode to use in this operation
   * @return
   *   the position where the sub-string is found, starting from the current position, or -1 if not found
   */
  def indexOfIgnoreCase(searchFor: Option[String], searchMode: Set[SearchMode]): Int =
    searchFor match
      case None => -1
      case Some(search) =>

        val searchForLength = search.length
        val tmpLocalStopAt = srcLen - searchForLength + 1

        val localStopAt = if tmpLocalStopAt > stopAt then stopAt else tmpLocalStopAt

        if pos >= localStopAt || searchForLength == 0 then -1
        else
          // Some locales don't follow upper-case rule, so need to check both.
          val firstCharOfSearchForUc = search.toUpperCase.charAt(0)
          val firstCharOfSearchForLc = search.toLowerCase.charAt(0)

          val localSearchMode = if firstCharOfSearchForLc.isWhitespace && defaultSearchMode.contains(SearchMode.SKIP_WHITE_SPACE) then
            // Can't skip white spaces if first searchFor char is one.
            defaultSearchMode.filterNot(_ == SearchMode.SKIP_WHITE_SPACE)
          else defaultSearchMode

          while pos < localStopAt do
            if indexOfNextChar(localSearchMode) == -1 then return -1

            if StringHelper.isCharEqualIgnoreCase(getChar(), firstCharOfSearchForUc, firstCharOfSearchForLc) && StringHelper.regionMatchesIgnoreCase(source, pos, search) then
              return pos

            incrementPosition(localSearchMode)
          end while

          -1

/**
 * Splits the source string by the given delimiter. Consecutive delimiters result in empty string parts.
 *
 * @param delimiter
 *   the characters sequence where to split the source string
 * @param trim
 *   whether each one of the parts should be trimmed or not
 * @return
 *   a {@link List} containing all the string parts
 */
  def split(delimiter: String, trim: Boolean): List[String] =

    restart()

    var startPos = 0
    val splitParts = List.newBuilder[String]
    while indexOfIgnoreCase(delimiter) != -1 do
      val part = if trim then
        source.substring(startPos, pos).trim
      else
        source.substring(startPos, pos)

      splitParts += part
      startPos = incrementPosition(delimiter.length)

    end while

    // Add last part.
    val token = if trim then
      source.substring(startPos).trim
    else
      source.substring(startPos)
    splitParts += token

    splitParts.result()
