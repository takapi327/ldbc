/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc

import ldbc.core.attribute.Attribute

/** Top-level imports provide aliases for the most commonly used types and modules. A typical starting set of imports
  * might look something like this.
  *
  * example:
  * {{{
  *   import ldbc.core.*
  * }}}
  */
package object core extends DataTypes:

  def column[T](
    label:    String,
    dataType: DataType[T]
  ): Column[T] = Column[T](label, dataType)

  def column[T](
    label:    String,
    dataType: DataType[T],
    comment:  String
  ): Column[T] = Column[T](label, dataType, comment)

  def column[T](
    label:      String,
    dataType:   DataType[T],
    attributes: Attribute[T]*
  ): Column[T] = Column[T](label, dataType, attributes: _*)

  def column[T](
    label:      String,
    dataType:   DataType[T],
    comment:    String,
    attributes: Attribute[T]*
  ): Column[T] = Column[T](label, dataType, comment, attributes: _*)
