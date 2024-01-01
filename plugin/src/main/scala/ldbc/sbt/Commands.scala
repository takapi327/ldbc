/** This file is part of the Lepus Framework. For the full copyright and license information, please view the LICENSE
  * file that was distributed with this source code.
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
