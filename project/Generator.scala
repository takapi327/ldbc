/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import sbt._

object Generator {

  def lepusVersion(
    version:      String,
    scalaVersion: String,
    sbtVersion:   String,
    dir:          File
  ): Seq[File] = {
    val file = dir / "LepusVersion.scala"
    val scalaSource =
      s"""|package lepus.core
          |
          |object LepusVersion {
          |  val current      = "$version"
          |  val scalaVersion = "$scalaVersion"
          |  val sbtVersion   = "$sbtVersion"
          |}
          |""".stripMargin

    if (!file.exists() || IO.read(file) != scalaSource) {
      IO.write(file, scalaSource)
    }

    Seq(file)
  }
}
