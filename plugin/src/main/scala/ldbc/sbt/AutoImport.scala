/** This file is part of the Lepus Framework. For the full copyright and license information, please view the LICENSE
  * file that was distributed with this source code.
  */

package ldbc.sbt

import sbt._

object AutoImport extends Dependencies {

  val sqlFiles = SettingKey[List[File]](
    label       = "sqlFiles",
    description = "List of SQL files to be read"
  )
}
