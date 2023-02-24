/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.sql.syntax

import cats.data.Kleisli
import ldbc.core.interpreter.*
import ldbc.sql.{ResultSet, ResultSetReader}

import scala.deriving.Mirror

trait TableSyntax:

  extension[F[_], P <: Product] (table: ldbc.core.Table[P])

    def applyDynamic[Tag <: Singleton](tag: Tag)()(using
      mirror: Mirror.ProductOf[P],
      index: ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]],
      resultSetReader: ResultSetReader[F, Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]
    ): Kleisli[F, ResultSet[F], Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]] =
      Kleisli { resultSet =>
        val column = table.selectDynamic[Tag](tag)
        resultSetReader.read(resultSet, column.label)
      }
