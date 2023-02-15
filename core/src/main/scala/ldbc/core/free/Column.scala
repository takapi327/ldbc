/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core.free

import ldbc.core.DataType
import ldbc.core.attribute.Attribute

/** Trait for representing SQL Column
  *
  * @tparam T
  *   Scala types that match SQL DataType
  */
private[ldbc] trait Column[T]:

  /** Column Field Name */
  def label: String

  /** Column type */
  def dataType: DataType[T]

  /** Extra attribute of column */
  def attributes: Seq[Attribute[T]]

  /** Column comment */
  def comment: Option[String]

  /** Define SQL query string for each Column
    *
    * @return
    *   SQL query string
    */
  def queryString: String
