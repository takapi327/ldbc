/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
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

  /** Column alias name */
  def alias: Option[String] = None

  /** Define SQL query string for each Column
    *
    * @return
    *   SQL query string
    */
  def queryString: String =
    val str = s"`$label` ${ dataType.queryString }" + attributes.map(v => s" ${ v.queryString }").mkString("")
    alias.fold(str)(name => s"$name.$str")

  def as(name: String): Column[T] = Column[T](label, dataType, attributes, Some(name))

  override def toString: String = alias.fold(s"`$label`")(name => s"$name.`$label`")

object Column:

  type Extract[T] <: Tuple = T match
    case Column[t]               => t *: EmptyTuple
    case Column[t] *: EmptyTuple => t *: EmptyTuple
    case Column[t] *: ts         => t *: Extract[ts]

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

  private[ldbc] def apply[T](
    _label:      String,
    _dataType:   DataType[T],
    _attributes: Seq[Attribute[T]],
    _alias:      Option[String]
  ): Column[T] = new Column[T]:

    override def label: String = _label

    override def dataType: DataType[T] = _dataType

    override def attributes: Seq[Attribute[T]] = _attributes

    override def alias: Option[String] = _alias
