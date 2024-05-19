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
