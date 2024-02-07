/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import sbt.*

object Generator {

  def version(
    version:      String,
    scalaVersion: String,
    sbtVersion:   String,
    dir:          File
  ): Seq[File] = {
    val file = dir / "Version.scala"
    val scalaSource =
      s"""|package ldbc.build
          |
          |object Version {
          |  val current      = "$version"
          |  val scalaVersion = "$scalaVersion"
          |  val sbtVersion   = "$sbtVersion"
          |}
          |""".stripMargin

    if !file.exists() || IO.read(file) != scalaSource then {
      IO.write(file, scalaSource)
    }

    Seq(file)
  }
}
