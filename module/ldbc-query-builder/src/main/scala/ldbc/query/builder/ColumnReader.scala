/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.query.builder

import cats.data.Kleisli

import ldbc.core.Column
import ldbc.sql.{ ResultSet, ResultSetReader }

case class ColumnReader[F[_], T](column: Column[T], reader: ResultSetReader[F, T]):

  val read: Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
    reader.read(resultSet, column.alias.fold(column.label)(name => s"$name.${ column.label }"))
  }

  override def toString: String = column.toString
