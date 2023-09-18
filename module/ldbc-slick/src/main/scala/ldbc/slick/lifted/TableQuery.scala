/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.slick.lifted

import scala.deriving.Mirror
import scala.reflect.ClassTag

import slick.lifted.{ Query, ShapedValue, FlatShapeLevel, ToTuple }

import ldbc.slick.SlickTable

class TableQuery[P <: Product](table: SlickTable[P]) extends Query[SlickTable[P], P, Seq]:

  override lazy val shaped: ShapedValue[SlickTable[P], P] =
    ShapedValue(table, slick.lifted.RepShape[FlatShapeLevel, SlickTable[P], P])

  override lazy val toNode = shaped.toNode

case class TableQueryBuilder(profile: slick.relational.RelationalProfile):
  inline def apply[P <: Product](table: ldbc.core.Table[P])(using
                                                            mirror: Mirror.ProductOf[P],
                                                            classTag: ClassTag[P],
                                                            tt: ToTuple[mirror.MirroredElemTypes, mirror.MirroredElemTypes]
  ): TableQuery[P] =
    new TableQuery[P](SlickTable[P](table, profile))
