/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

lazy val root = (project in file("."))
  .settings(name := "ldbc")
  .settings(scalaVersion := "3.2.1")
  .settings(
    organization := "com.github.takapi327",
    startYear    := Some(2023),
    homepage     := Some(url(s"https://github.com/takapi327/ldbc")),
    licenses     := Seq("MIT" -> url("https://img.shields.io/badge/license-MIT-green")),
    developers += Developer("takapi327", "Takahiko Tominaga", "t.takapi0327@gmail.com", url("https://github.com/takapi327"))
  )
