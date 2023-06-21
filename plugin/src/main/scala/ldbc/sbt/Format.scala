/** This file is part of the Lepus Framework. For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package ldbc.sbt

trait Format

object Format {
  case object CAMEL extends Format
  case object PASCAL extends Format
  case object SNAKE extends Format
  case object KEBAB extends Format
}
