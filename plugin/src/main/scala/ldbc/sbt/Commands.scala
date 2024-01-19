/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
  */

package ldbc.sbt

import sbt._
import sbt.Keys._

object Commands {
  val baseClassloaderTask = Def.task {
    val classpath = (Compile / dependencyClasspath).value
    val log       = streams.value.log
    val parent    = ClassLoader.getSystemClassLoader.getParent
    log.debug("Using parent loader for base classloader: " + parent)

    new java.net.URLClassLoader(classpath.map(_.data.toURI.toURL).toArray, parent)
  }

  val generateBySchema = Command.command("generateBySchema") { (state: State) =>
    "generateBySQLSchema" :: state
  }
}
