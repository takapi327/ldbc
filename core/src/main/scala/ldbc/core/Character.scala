/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

/** A model for representing character sets and collations to be set in column definitions for the string data types
  * CHAR, VARCHAR, TEXT, ENUM, SET, and any synonym.
  */
trait Character:

  /** Character name */
  def name: String

  /** Collate information to be set for Character */
  def collate: Option[Character.Collate]

  /** Methods for setting collate to Character */
  def set(collate: Character.Collate): Character

  val queryString: String = collate.fold(s"CHARACTER SET $name")(v => s"CHARACTER SET $name COLLATE ${ v.name }")

object Character:

  trait Collate(val name: String)
