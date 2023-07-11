/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.slick

import scala.language.dynamics

import slick.ast.TableNode
import slick.lifted.{ Rep, ProvenShape }

import ldbc.core.Table
import ldbc.slick.lifted.Tag

trait SlickTable[P <: Product] extends Table[P], Rep[P]:

  def tag: Tag

  def tableNode: TableNode

  def allColumnShape: ProvenShape[?]
