/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.slick.syntax

import scala.deriving.Mirror

import slick.ast.{ TypedType, Select, FieldSymbol }
import slick.lifted.Rep

import ldbc.core.interpreter.*
import ldbc.slick.SlickTable
import ldbc.slick.lifted.RefTag

trait TableSyntax:

  extension[P <: Product] (table: SlickTable[P])
    def applyDynamic[Tag <: Singleton](
      tag: Tag
    )()(using
        mirror: Mirror.ProductOf[P],
        index: ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]],
        tt: TypedType[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]
    ): Rep[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]] =
      val column = table.selectDynamic[Tag](tag)
      new Rep.TypedRep[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]:
        override def toNode =
          Select((table.tag match
            case r: RefTag => r.path
            case _ => table.tableNode),
            FieldSymbol(column.label)(Seq.empty, tt)
          ) :@ tt

        override def toString = (table.tag match
          case r: RefTag => "(" + table.name + " " + r.path + ")"
          case _ => table.name
          ) + "." + column.label
