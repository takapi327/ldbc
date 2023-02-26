/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

import ldbc.core.attribute.{ Attribute, AutoInc, Key }

private[ldbc] trait Alias:

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

  def AUTO_INCREMENT[T <: Byte | Short | Int | Long]: AutoInc[T] = AutoInc[T]()

  def PRIMARY_KEY[T]: Key.Primary[T] = Key.Primary[T]()
  def UNIQUE_KEY[T]:  Key.Unique[T]  = Key.Unique[T]()
