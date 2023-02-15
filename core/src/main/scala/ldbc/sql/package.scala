/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc

import ldbc.sql.attribute.Attribute

/** Top-level imports provide aliases for the most commonly used types and modules. A typical starting set of imports
  * might look something like this.
  *
  * example:
  * {{{
  *   import ldbc.sql.*
  * }}}
  */
package object sql extends DataTypes:

  def column[F[_], T](
    label:    String,
    dataType: DataType[T]
  ): Column[F, T] = Column[F, T](label, dataType)

  def column[F[_], T](
    label: String,
    dataType: DataType[T],
    comment: String
  ): Column[F, T] = Column[F, T](label, dataType, comment)

  def column[F[_], T](
    label: String,
    dataType: DataType[T],
    attributes: Attribute[T]*
  ): Column[F, T] = Column[F, T](label, dataType, attributes: _*)

  def column[F[_], T](
    label: String,
    dataType: DataType[T],
    comment: String,
    attributes: Attribute[T]*
  ): Column[F, T] = Column[F, T](label, dataType, comment, attributes: _*)
