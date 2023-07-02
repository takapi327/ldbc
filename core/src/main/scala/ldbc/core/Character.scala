/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

/** A model for representing character sets to be set in column definitions for the string data types
  * CHAR, VARCHAR, TEXT, ENUM, SET, and any synonym.
  *
  * @param name
  *   Character name
  */
case class Character(name: String):

  /** Variable that contains the SQL string of Character
    *
    * @return
    *   SQL query string
    */
  val queryString: String = s"CHARACTER SET $name"

/**
 * A model for representing collations to be set in column definitions for the string data types
 * CHAR, VARCHAR, TEXT, ENUM, SET, and any synonym.
 *
 * @param name
 * Collate name
 */
case class Collate(name: String):

  /** Variable that contains the SQL string of Collate
   *
   * @return
   * SQL query string
   */
  val queryString: String = s"COLLATE $name"
