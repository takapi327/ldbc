/** This file is part of the Lepus Framework. For the full copyright and license information, please view the LICENSE
  * file that was distributed with this source code.
  */

package ldbc.sbt

import sbt._

object CustomKeys {
  val baseClassloader = TaskKey[ClassLoader](
    "baseClassloader",
    "The base classloader"
  )

  val generateBySQLSchema = TaskKey[Seq[File]]("generateBySQLSchema", "Generate models from SQL schema")
}
