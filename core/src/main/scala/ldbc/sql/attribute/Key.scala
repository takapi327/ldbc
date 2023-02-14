/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.sql.attribute

/**
 * Key to be set for the column
 */
object Key:

  /**
   * Primary Key to be set for the column
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  case class Primary[T]() extends Attribute[T]:

    override def queryString: String = "PRIMARY KEY"

  /**
   * Unique Key to be set for the column
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  case class Unique[T]() extends Attribute[T]:

    override def queryString: String = "UNIQUE KEY"
