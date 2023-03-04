/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder

import ldbc.core.Character

/** A model representing SQL database information.
  *
  * @param label
  *   Database Name
  * @param characterSet
  *   A value to represent the character set and collation.
  */
case class Database(label: String, characterSet: Option[Character]):

  def queryString: String =
    s"CREATE DATABASE `$label`" + characterSet.map(v => s" ${ v.queryString }").getOrElse("")

object Database:

  def apply(label: String): Database = Database(label, None)
