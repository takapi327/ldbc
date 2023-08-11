/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder

import ldbc.core.Table
import ldbc.query.builder.statement.Select
import ldbc.query.builder.interpreter.Tuples

case class TableQuery[F[_], P <: Product](table: Table[P]):

  type ToColumn[T] = T match
    case Tuple => Tuples.MapToColumn[T, F]
    case _     => ColumnReader[F, T]

  def select[T](func: Table[P] => ToColumn[T]): Select[F, P, ToColumn[T]] =
    val columns = func(table)
    val str = columns match
      case v: Tuple => v.toArray.distinct.mkString(", ")
      case v        => v
    val statement = s"SELECT $str FROM ${ table._name }"
    Select[F, P, ToColumn[T]](table, statement, columns, Seq.empty)
