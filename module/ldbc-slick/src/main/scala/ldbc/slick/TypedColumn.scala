/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.slick

import slick.ast.*
import slick.lifted.{ Rep, RefTag }

import ldbc.core.{ Column, DataType }
import ldbc.core.attribute.Attribute

import ldbc.slick.lifted.Tag

trait TypedColumn[T] extends Column[T]:

  def typedType: TypedType[T]

  def toRep(
             column: TypedColumn[T],
             name: String,
             tag: Tag,
             tableNode: TableNode
           ): TypedColumn[T] with Rep[T] = new TypedColumn[T] with Rep[T]:
    override def label: String = column.label

    override def dataType: DataType[T] = column.dataType

    override def attributes: Seq[Attribute[T]] = column.attributes

    override def comment: Option[String] = column.comment

    override def typedType = column.typedType

    override def encodeRef(path: Node): Rep[T] =
      Rep.forNode(path)(using typedType)

    override def toNode =
      Select(
        (tag match
          case r: RefTag => r.path
          case _ => tableNode
          ),
        FieldSymbol(label)(Seq.empty, typedType)
      ) :@ typedType

    override def toString = (tag match
      case r: RefTag => "(" + name + " " + r.path + ")"
      case _ => name
      ) + "." + label

object TypedColumn:

  def apply[T](
    _label:    String,
    _dataType: DataType[T]
  )(using tt:  TypedType[T]): TypedColumn[T] = new TypedColumn[T]:

    override def label: String = _label

    override def dataType: DataType[T] = _dataType

    override def comment: Option[String] = None

    override def attributes: Seq[Attribute[T]] = Seq.empty

    override def typedType: TypedType[T] = tt

  def apply[T](
    _label:    String,
    _dataType: DataType[T],
    _comment:  String
  )(using tt:  TypedType[T]): TypedColumn[T] = new TypedColumn[T]:

    override def label: String = _label

    override def dataType: DataType[T] = _dataType

    override def comment: Option[String] = Some(_comment)

    override def attributes: Seq[Attribute[T]] = Seq.empty

    override def typedType: TypedType[T] = tt

  def apply[T](
    _label:      String,
    _dataType:   DataType[T],
    _attributes: Attribute[T]*
  )(using tt:    TypedType[T]): TypedColumn[T] = new TypedColumn[T]:

    override def label: String = _label

    override def dataType: DataType[T] = _dataType

    override def comment: Option[String] = None

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

    override def comment: Option[String] = Some(_comment)

    override def attributes: Seq[Attribute[T]] = _attributes.toSeq

    override def typedType: TypedType[T] = tt
