/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.slick.lifted

import slick.lifted.{ Query, ShapedValue, FlatShapeLevel }

import ldbc.slick.SlickTable

class TableQuery[T <: SlickTable[?]](table: T) extends Query[T, TableQuery.Extract[T], Seq]:

  override lazy val shaped: ShapedValue[T, TableQuery.Extract[T]] =
    ShapedValue(table, slick.lifted.RepShape[FlatShapeLevel, T, TableQuery.Extract[T]])

  override lazy val toNode = shaped.toNode

object TableQuery:

  type Extract[T] = T match
    case SlickTable[t] => t
  
  def apply[T <: SlickTable[?]](table: T): TableQuery[T] =
    new TableQuery[T](table)
