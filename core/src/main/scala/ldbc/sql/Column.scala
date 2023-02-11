/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.sql

import ldbc.sql.attribute.Attribute

/** Trait for representing SQL Column
  *
  * @tparam F
  *   The effect type
  * @tparam T
  *   Scala types that match SQL DataType
  */
trait Column[F[_], T]:

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
  def queryString: String =
    s"`$label` ${ dataType.queryString }" + attributes.map(v => s" ${ v.queryString }").mkString("") + comment.fold("")(
      str => s" COMMENT '$str'"
    )

  override def toString: String = s"`$label`"

object Column:

  def apply[F[_], T](
    _label:    String,
    _dataType: DataType[T]
  ): Column[F, T] = new Column[F, T]:

    override def label: String = _label

    override def dataType: DataType[T] = _dataType

    override def comment: Option[String] = None

    override def attributes: Seq[Attribute[T]] = Seq.empty

  def apply[F[_], T](
    _label:    String,
    _dataType: DataType[T],
    _comment:  String
  ): Column[F, T] = new Column[F, T]:

    override def label: String = _label

    override def dataType: DataType[T] = _dataType

    override def comment: Option[String] = Some(_comment)

    override def attributes: Seq[Attribute[T]] = Seq.empty

  def apply[F[_], T](
    _label:      String,
    _dataType:   DataType[T],
    _attributes: Attribute[T]*
  ): Column[F, T] = new Column[F, T]:

    override def label: String = _label

    override def dataType: DataType[T] = _dataType

    override def comment: Option[String] = None

    override def attributes: Seq[Attribute[T]] = _attributes.toSeq

  def apply[F[_], T](
    _label:      String,
    _dataType:   DataType[T],
    _comment:    String,
    _attributes: Attribute[T]*
  ): Column[F, T] = new Column[F, T]:

    override def label: String = _label

    override def dataType: DataType[T] = _dataType

    override def comment: Option[String] = Some(_comment)

    override def attributes: Seq[Attribute[T]] = _attributes.toSeq
