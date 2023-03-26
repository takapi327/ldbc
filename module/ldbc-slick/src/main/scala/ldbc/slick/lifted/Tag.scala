/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.slick.lifted

import slick.ast.Node

import ldbc.slick.SlickTable

sealed trait Tag:
  def taggedAs(path: Node): SlickTable[?]

abstract class RefTag(val path: Node) extends Tag

trait BaseTag extends Tag
