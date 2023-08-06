/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.sql.syntax

import scala.deriving.Mirror

import cats.data.Kleisli

import ldbc.core.Table
import ldbc.core.interpreter.*
import ldbc.sql.{ ResultSet, ResultSetReader }

trait TableSyntax:

  extension [F[_], P <: Product](table: Table[P])
    def applyDynamic[Tag <: Singleton](
      tag: Tag
    )()(using
      mirror: Mirror.ProductOf[P],
      index:  ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]],
      reader: ResultSetReader[
        F,
        Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
      ]
    ): Kleisli[F, ResultSet[F], Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]] =
      Kleisli { resultSet =>
        val column = table.selectDynamic[Tag](tag)
        reader.read(resultSet, column.alias.fold(column.label)(name => s"$name.${ column.label }"))
      }
