/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.syntax

import scala.deriving.Mirror

import ldbc.core.{ Column, Table }
import ldbc.dsl.statement.*

trait TableSyntax[F[_]]:

  extension [P <: Product](table: Table[P])(using mirror: Mirror.ProductOf[P])

    def selectAll: Select[F, P, Tuple.Map[mirror.MirroredElemTypes, Column]] =
      val statement = s"SELECT ${ table.*.mkString(", ") } FROM ${ table._name }"
      Select[F, P, Tuple.Map[mirror.MirroredElemTypes, Column]](
        table,
        statement,
        Tuple.fromArray(table.*.toArray).asInstanceOf[Tuple.Map[mirror.MirroredElemTypes, Column]],
        Seq.empty
      )

    def select[
      T <: Tuple.Union[Tuple.Map[mirror.MirroredElemTypes, Column]] |
        Tuple.Union[Tuple.Map[mirror.MirroredElemTypes, Column]] *: NonEmptyTuple
    ](
      func: Table[P] => T
    ): Select[F, P, T] =
      val columns = func(table)
      val str = columns match
        case v: Tuple => v.toArray.distinct.mkString(", ")
        case v        => v
      val statement = s"SELECT $str FROM ${ table._name }"
      Select[F, P, T](table, statement, columns, Seq.empty)

    def join[O <: Product](other: Table[O]): Join[F, P, O] = Join(table.as("x1"), other.as("x2"))
