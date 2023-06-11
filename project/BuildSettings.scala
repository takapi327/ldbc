/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import sbt._
import sbt.Keys._
import sbt.plugins.SbtPlugin

import sbtrelease.ReleasePlugin.autoImport._
import ReleaseTransformations._

import ScalaVersions._

object BuildSettings {

  val baseScalaSettings: Seq[String] = Seq(
    "-Xfatal-warnings",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-encoding",
    "utf8",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions"
  )

  val scala3Settings: Seq[String] = baseScalaSettings ++ Seq(
    "-Wunused:all",
  )

  /**
   * Set up to publish the project.
   */
  def publishSettings: Seq[Setting[_]] = Seq(
    publishTo := Some("Lepus Maven" at "s3://com.github.takapi327.s3-ap-northeast-1.amazonaws.com/lepus/"),
    (Compile / packageDoc) / publishArtifact := false,
    (Compile / packageSrc) / publishArtifact := false,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )

  /** These settings are used by all projects. */
  def commonSettings: Seq[Setting[_]] = Def.settings(
    organization := "com.github.takapi327",
    startYear    := Some(2023),
    homepage     := Some(url(s"https://github.com/takapi327/ldbc")),
    licenses     := Seq("MIT" -> url("https://img.shields.io/badge/license-MIT-green")),
    Test / fork  := true,
    run / fork   := true,
    developers   += Developer("takapi327", "Takahiko Tominaga", "t.takapi0327@gmail.com", url("https://github.com/takapi327"))
  )

  /** A project that runs in the sbt runtime. */
  object LepusSbtProject {
    def apply(name: String, dir: String): Project =
      Project(name, file(dir))
        .settings(scalacOptions ++= scala3Settings)
        .settings(commonSettings: _*)
        .settings(publishSettings: _*)
  }

  /** A project that is an sbt plugin. */
  object LepusSbtPluginProject {
    def apply(name: String, dir: String): Project =
      Project(name, file(dir))
        .settings(scalaVersion := scala2)
        .settings(scalacOptions ++= baseScalaSettings)
        .settings(commonSettings: _*)
        .settings(publishSettings: _*)
        .enablePlugins(SbtPlugin)
  }
}
