/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder

import cats.data.Kleisli

import ldbc.core.*
import ldbc.core.attribute.Attribute
import ldbc.sql.{ ResultSet, ResultSetReader }

private[ldbc] trait ColumnReader[F[_], T] extends Column[T]:

  def reader: ResultSetReader[F, T]

  val read: Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
    reader.read(resultSet, alias.fold(label)(name => s"$name.$label"))
  }

  override def toString: String = alias.fold(s"`$label`")(name => s"$name.`$label`")

object ColumnReader:

  def apply[F[_], T](column: Column[T], _reader: ResultSetReader[F, T]): ColumnReader[F, T] =
    new ColumnReader[F, T]:
      override def label: String = column.label
      override def dataType: DataType[T] = column.dataType
      override def attributes: Seq[Attribute[T]] = column.attributes
      override private[ldbc] def alias = column.alias
      override def reader: ResultSetReader[F, T] = _reader
