/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

import ldbc.core.attribute.Attribute

/** Trait for representing SQL Column
  *
  * @tparam T
  *   Scala types that match SQL DataType
  */
trait Column[T]:

  /** Column Field Name */
  def label: String

  /** Column type */
  def dataType: DataType[T]

  /** Extra attribute of column */
  def attributes: Seq[Attribute[T]]

  /** Define SQL query string for each Column
    *
    * @return
    *   SQL query string
    */
  def queryString: String =
    s"`$label` ${dataType.queryString}" + attributes.map(v => s" ${v.queryString}").mkString("")

  override def toString: String = s"`$label`"

object Column:

  def apply[T](
    _label:    String,
    _dataType: DataType[T]
  ): Column[T] = new Column[T]:

    override def label: String = _label

    override def dataType: DataType[T] = _dataType

    override def attributes: Seq[Attribute[T]] = _dataType match
      case data: DataType.Alias[T] => data.attributes
      case _                       => Seq.empty

  def apply[T](
    _label:      String,
    _dataType:   DataType[T],
    _attributes: Attribute[T]*
  ): Column[T] = new Column[T]:

    override def label: String = _label

    override def dataType: DataType[T] = _dataType

    override def attributes: Seq[Attribute[T]] = _dataType match
      case data: DataType.Alias[T] => data.attributes ++ _attributes
      case _                       => _attributes
