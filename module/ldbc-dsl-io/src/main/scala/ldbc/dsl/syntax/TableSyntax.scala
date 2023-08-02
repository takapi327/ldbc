/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.syntax

import scala.deriving.Mirror

import ldbc.core.{ Table, Column }
import ldbc.dsl.statement.Select

trait TableSyntax[F[_]]:

  extension [P <: Product](table: Table[P])(using mirror: Mirror.ProductOf[P])
    def select[T <: Tuple.Union[Tuple.Map[mirror.MirroredElemTypes, Column]] *: NonEmptyTuple](
      columns: T
    ): Select[F, P, T] =
      val statement = s"SELECT ${ columns.toArray.distinct.mkString(", ") } FROM ${ table._name }"
      Select[F, P, T](table, statement, columns, Seq.empty)
