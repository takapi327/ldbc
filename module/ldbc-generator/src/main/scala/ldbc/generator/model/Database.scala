/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.model

object Database:
  case class CreateStatement(
    name:       String,
    charset:    Option[String],
    collate:    Option[String],
    encryption: Option["Y" | "N"]
  )

  case class DropStatement(name: String)

  case class UseStatement(name: String)
