/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.slick

import slick.ast.*

import ldbc.core.{ Column, DataType }
import ldbc.core.attribute.Attribute

trait TypedColumn[T] extends Column[T]:

  def typedType: TypedType[T]

object TypedColumn:

  def apply[T](
    _label:    String,
    _dataType: DataType[T]
  )(using tt:  TypedType[T]): TypedColumn[T] = new TypedColumn[T]:

    override def label: String = _label

    override def dataType: DataType[T] = _dataType

    override def attributes: Seq[Attribute[T]] = Seq.empty

    override def typedType: TypedType[T] = tt

  def apply[T](
    _label:    String,
    _dataType: DataType[T],
    _comment:  String
  )(using tt:  TypedType[T]): TypedColumn[T] = new TypedColumn[T]:

    override def label: String = _label

    override def dataType: DataType[T] = _dataType

    override def attributes: Seq[Attribute[T]] = Seq.empty

    override def typedType: TypedType[T] = tt

  def apply[T](
    _label:      String,
    _dataType:   DataType[T],
    _attributes: Attribute[T]*
  )(using tt:    TypedType[T]): TypedColumn[T] = new TypedColumn[T]:

    override def label: String = _label

    override def dataType: DataType[T] = _dataType

    override def attributes: Seq[Attribute[T]] = _attributes.toSeq

    override def typedType: TypedType[T] = tt

  def apply[T](
    _label:      String,
    _dataType:   DataType[T],
    _comment:    String,
    _attributes: Attribute[T]*
  )(using tt:    TypedType[T]): TypedColumn[T] = new TypedColumn[T]:

    override def label: String = _label

    override def dataType: DataType[T] = _dataType

    override def attributes: Seq[Attribute[T]] = _attributes.toSeq

    override def typedType: TypedType[T] = tt
