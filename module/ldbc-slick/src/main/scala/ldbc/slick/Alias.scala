/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.slick

import slick.ast.TypedType

import ldbc.core.{ DataType, Alias as CoreAilias }
import ldbc.core.attribute.Attribute

private[ldbc] trait Alias extends CoreAilias:

  def column[T](
    label:    String,
    dataType: DataType[T]
  )(using tt: TypedType[T]): TypedColumn[T] = TypedColumn[T](label, dataType)

  def column[T](
    label:    String,
    dataType: DataType[T],
    comment:  String
  )(using tt: TypedType[T]): TypedColumn[T] = TypedColumn[T](label, dataType, comment)

  def column[T](
    label:      String,
    dataType:   DataType[T],
    attributes: Attribute[T]*
  )(using tt:   TypedType[T]): TypedColumn[T] = TypedColumn[T](label, dataType, attributes: _*)

  def column[T](
    label:      String,
    dataType:   DataType[T],
    comment:    String,
    attributes: Attribute[T]*
  )(using tt:   TypedType[T]): TypedColumn[T] = TypedColumn[T](label, dataType, comment, attributes: _*)
