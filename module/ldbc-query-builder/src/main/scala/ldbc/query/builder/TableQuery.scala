/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder

import scala.deriving.Mirror
import scala.compiletime.*

import ldbc.core.*
import ldbc.sql.ResultSetReader
import ldbc.query.builder.statement.{ Select, Join }
import ldbc.query.builder.interpreter.Tuples

case class TableQuery[F[_], P <: Product](table: Table[P]):

  private inline def inferResultSetReader[T]: ResultSetReader[F, T] =
    summonFrom[ResultSetReader[F, T]] {
      case reader: ResultSetReader[F, T] => reader
      case _                             => error("ResultSetReader cannot be inferred")
    }

  private inline def foldResultSetReader[T <: Tuple]: Tuples.MapToResultSetReader[F, T] =
    inline erasedValue[T] match
      case _: EmptyTuple => EmptyTuple
      case _: (h *: t)   => inferResultSetReader[h] *: foldResultSetReader[t]

  inline def selectAll(using mirror: Mirror.ProductOf[P]): Select[F, P, Tuples.ToColumn[F, mirror.MirroredElemTypes]] =
    val columns = table.*.zip(foldResultSetReader[mirror.MirroredElemTypes])
      .map(
        [t] =>
          (x: t) =>
            val (column, reader) = x.asInstanceOf[(Column[t], ResultSetReader[F, t])]
            ColumnReader(column, reader)
      )
      .asInstanceOf[Tuples.ToColumn[F, mirror.MirroredElemTypes]]
    val statement = s"SELECT ${ table.*.toList.mkString(", ") } FROM ${ table._name }"
    new Select[F, P, Tuples.ToColumn[F, mirror.MirroredElemTypes]](table, statement, columns, Seq.empty)

  def select[T](func: Table[P] => Tuples.ToColumn[F, T]): Select[F, P, Tuples.ToColumn[F, T]] =
    val columns = func(table)
    val str = columns match
      case v: Tuple => v.toArray.distinct.mkString(", ")
      case v        => v
    val statement = s"SELECT $str FROM ${ table._name }"
    Select[F, P, Tuples.ToColumn[F, T]](table, statement, columns, Seq.empty)

  def join[O <: Product](other: Table[O]):         Join[F, P, O] = Join(table.as("x1"), other.as("x2"))
  def join[O <: Product](other: TableQuery[F, O]): Join[F, P, O] = Join(table.as("x1"), other.table.as("x2"))
