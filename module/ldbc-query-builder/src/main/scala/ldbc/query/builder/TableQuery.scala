/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder

import ldbc.core.Table
import ldbc.query.builder.statement.{ Select, Join }
import ldbc.query.builder.interpreter.Tuples

case class TableQuery[F[_], P <: Product](table: Table[P]):

  def select[T](func: Table[P] => Tuples.ToColumn[F, T]): Select[F, P, Tuples.ToColumn[F, T]] =
    val columns = func(table)
    val str = columns match
      case v: Tuple => v.toArray.distinct.mkString(", ")
      case v        => v
    val statement = s"SELECT $str FROM ${ table._name }"
    Select[F, P, Tuples.ToColumn[F, T]](table, statement, columns, Seq.empty)

  def join[O <: Product](other: Table[O]):         Join[F, P, O] = Join(table.as("x1"), other.as("x2"))
  def join[O <: Product](other: TableQuery[F, O]): Join[F, P, O] = Join(table.as("x1"), other.table.as("x2"))
