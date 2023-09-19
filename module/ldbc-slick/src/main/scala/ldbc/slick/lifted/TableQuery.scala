/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.slick.lifted

import scala.deriving.Mirror
import scala.reflect.ClassTag

import slick.ast.Node
import slick.lifted.{ Query, ShapedValue, RepShape, FlatShapeLevel, ToTuple }
import slick.relational.RelationalProfile

import ldbc.core.Table
import ldbc.slick.SlickTable

class TableQuery[P <: Product](cons: Tag => SlickTable[P]) extends Query[SlickTable[P], P, Seq]:

  override lazy val shaped: ShapedValue[SlickTable[P], P] =
    val baseTable = cons(
      new BaseTag:
        base =>
        override def taggedAs(path: Node): SlickTable[?] = cons(
          new RefTag(path):
            override def taggedAs(path: Node) = base.taggedAs(path)
        )
    )
    ShapedValue(baseTable, RepShape[FlatShapeLevel, SlickTable[P], P])

  override lazy val toNode = shaped.toNode

case class TableQueryBuilder(profile: RelationalProfile):
  inline def apply[P <: Product](table: Table[P])(using
    mirror:                             Mirror.ProductOf[P],
    classTag:                           ClassTag[P],
    tt:                                 ToTuple[mirror.MirroredElemTypes, mirror.MirroredElemTypes]
  ): TableQuery[P] =
    new TableQuery[P](tag => SlickTable[P](table, tag, profile))
