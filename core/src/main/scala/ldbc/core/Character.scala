/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

/** A model for representing character sets and collations to be set in column definitions for the string data types
  * CHAR, VARCHAR, TEXT, ENUM, SET, and any synonym.
  *
  * @param name
  *   Character name
  *
  * @param collate
  *   Collate information to be set for Character
  */
case class Character(name: String, collate: Option[String]):

  /** Methods for setting collate to Character */
  def set(collate: String): Character = this.copy(collate = Some(collate))

  /** Variable that contains the SQL string of Character
    *
    * @return
    *   SQL query string
    */
  val queryString: String = collate.fold(s"CHARACTER SET $name")(v => s"CHARACTER SET $name COLLATE $v")

object Character:

  def apply(name: String): Character = Character(name, None)

  def apply(name: String, collate: String): Character = Character(name, Some(collate))
