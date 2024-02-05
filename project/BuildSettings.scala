/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import sbt.*
import sbt.Keys.*
import sbt.plugins.SbtPlugin
import sbt.ScriptedPlugin.autoImport.*

import de.heikoseeberger.sbtheader.{ CommentStyle, CommentBlockCreator, AutomateHeaderPlugin }
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderPattern.commentBetween
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.*

import ScalaVersions.*

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

  val scala2Settings: Seq[String] = baseScalaSettings ++ Seq(
    "-Xsource:3",
  )

  val scala3Settings: Seq[String] = baseScalaSettings ++ Seq(
    "-Wunused:all",
  )

  /**
   * Set up a scripted framework to test the plugin.
   */
  def scriptedSettings: Seq[Setting[?]] = Seq(
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )

  val customCommentStyle: CommentStyle =
    CommentStyle(
      new CommentBlockCreator("/**", " *", " */"),
      commentBetween("""/\**+""", "*", """\*/""")
    )

  /** These settings are used by all projects. */
  def commonSettings: Seq[Setting[?]] = Def.settings(
    organization := "io.github.takapi327",
    organizationName := "takapi327",
    startYear := Some(2023),
    homepage := Some(url("https://takapi327.github.io/ldbc/")),
    licenses := Seq("MIT" -> url("https://img.shields.io/badge/license-MIT-green")),
    run / fork := true,
    developers += Developer("takapi327", "Takahiko Tominaga", "t.takapi0327@gmail.com", url("https://github.com/takapi327")),
    headerMappings := headerMappings.value + (HeaderFileType.scala -> customCommentStyle),
    headerLicense  := Some(HeaderLicense.Custom(
      """|Copyright (c) 2023-2024 by Takahiko Tominaga
         |This software is licensed under the MIT License (MIT).
         |For more information see LICENSE or https://opensource.org/licenses/MIT
         |""".stripMargin
    )),
  )

  /** A project that runs in the sbt runtime. */
  object LepusSbtProject {
    def apply(name: String, dir: String): Project =
      Project(name, file(dir))
        .settings(scalaVersion := scala3)
        .settings(scalacOptions ++= scala3Settings)
        .settings(commonSettings)
        .enablePlugins(AutomateHeaderPlugin)
  }

  /** A project that is an sbt plugin. */
  object LepusSbtPluginProject {
    def apply(name: String, dir: String): Project =
      Project(name, file(dir))
        .settings(scalaVersion := scala2)
        .settings(scalacOptions ++= scala2Settings)
        .settings(commonSettings)
        .settings(scriptedSettings)
        .enablePlugins(SbtPlugin, AutomateHeaderPlugin)
  }
}
