/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.sql

/** A model for representing character sets and collations to be set in column definitions for the string data types
  * CHAR, VARCHAR, TEXT, ENUM, SET, and any synonym.
  */
trait Character:

  def queryString: String

object Character:

  case class Big5(collate: Option[Big5.Collate]) extends Character:

    override def queryString: String = collate.fold("CHARACTER SET Big5")(v => s"CHARACTER SET Big5 COLLATE $v")

  object Big5:

    enum Collate:
      case big5_chinese_ci
      case big5_bin

  case class Dec8(collate: Option[Dec8.Collate]) extends Character:

    override def queryString: String = collate.fold("CHARACTER SET dec8")(v => s"CHARACTER SET dec8 COLLATE $v")

  object Dec8:

    enum Collate:
      case dec8_swedish_ci
      case dec8_bin
