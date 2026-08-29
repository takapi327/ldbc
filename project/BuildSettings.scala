/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import sbt.*
import sbt.plugins.SbtPlugin
import sbt.Keys.*
import sbt.ScriptedPlugin.autoImport.*

import de.heikoseeberger.sbtheader.{ AutomateHeaderPlugin, CommentBlockCreator, CommentStyle }
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.*
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderPattern.commentBetween

import org.typelevel.sbt.TypelevelGitHubPlugin.autoImport.tlGitHubDev

import ScalaVersions.*
import SbtVersions.*

object BuildSettings {

  val additionalSettings: Seq[String] = Seq(
    "-language:implicitConversions"
  )

  val removeSettings: Seq[String] = Seq(
    "-Ykind-projector:underscores",
    "-Wvalue-discard"
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
    organization     := "io.github.takapi327",
    organizationName := "takapi327",
    startYear        := Some(2023),
    homepage         := Some(url("https://takapi327.github.io/ldbc/")),
    licenses         := Seq("MIT" -> url("https://img.shields.io/badge/license-MIT-green")),
    run / fork       := true,
    developers += tlGitHubDev("takapi327", "Takahiko Tominaga"),
    headerMappings := headerMappings.value + (HeaderFileType.scala -> customCommentStyle),
    headerLicense  := Some(
      HeaderLicense.Custom(
        """|Copyright (c) 2023-2026 by Takahiko Tominaga
         |This software is licensed under the MIT License (MIT).
         |For more information see LICENSE or https://opensource.org/licenses/MIT
         |""".stripMargin
      )
    )
  )

  /**
   * A project that is an sbt plugin.
   *
   * The plugin is cross-built rather than migrated: sbt 1.x loads plugins compiled against Scala
   * 2.12 and sbt 2.x loads plugins compiled against Scala 3, so both artifacts have to be published
   * side by side. `scalaBinaryVersion` selects which sbt the plugin is compiled against.
   *
   * NOTE: sbt-typelevel derives `scalaVersion` from `crossScalaVersions`, so appending with `+=`
   * here produces `Cyclic reference involving crossScalaVersions/scalaVersion` at project load.
   * Both keys must be assigned with `:=`.
   */
  object LepusSbtPluginProject {
    def apply(name: String, dir: String): Project =
      Project(name, file(dir))
        .settings(
          scalaVersion                  := scala2,
          crossScalaVersions            := Seq(scala2, scala38),
          pluginCrossBuild / sbtVersion := {
            scalaBinaryVersion.value match {
              case "2.12" => sbt1
              case _      => sbt2
            }
          }
        )
        .settings(scalacOptions ++= additionalSettings)
        .settings(scalacOptions --= removeSettings)
        .settings(commonSettings)
        .settings(scriptedSettings)
        .enablePlugins(SbtPlugin, AutomateHeaderPlugin)
  }
}
