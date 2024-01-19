/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
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
