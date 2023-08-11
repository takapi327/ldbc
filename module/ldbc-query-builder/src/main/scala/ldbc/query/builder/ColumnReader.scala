/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder

import cats.data.Kleisli

import ldbc.core.*
import ldbc.core.attribute.Attribute
import ldbc.sql.{ ResultSet, ResultSetReader }

private[ldbc] case class ColumnReader[F[_], T](
  label: String,
  dataType: DataType[T],
  attributes: Seq[Attribute[T]],
  _alias: Option[String],
  reader: ResultSetReader[F, T]
) extends Column[T]:

  override private[ldbc] def alias = _alias

  val read: Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
    reader.read(resultSet, alias.fold(label)(name => s"$name.${ label }"))
  }

  override def toString: String = column.toString

object ColumnReader:

  def apply[F[_], T](column: Column[T], reader: ResultSetReader[F, T]): ColumnReader[F, T] =
    ColumnReader(column.label, column.dataType, column.attributes, column.alias, reader)
