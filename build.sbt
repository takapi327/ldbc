/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import ScalaVersions._
import JavaVersions._
import BuildSettings._
import Dependencies._
import Workflows._

ThisBuild / crossScalaVersions         := Seq(scala3)
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin(java11))
ThisBuild / githubWorkflowBuildPreamble += dockerRun
ThisBuild / githubWorkflowAddedJobs ++= Seq(
  scalaFmt.value, sbtScripted.value
)
ThisBuild / githubWorkflowBuildPostamble += dockerStop

lazy val core = LepusSbtProject("Ldbc-Core", "core")
  .settings(scalaVersion := sys.props.get("scala.version").getOrElse(scala3))
  .settings(libraryDependencies ++= Seq(cats, scalaTest) ++ specs2)

lazy val sql = LepusSbtProject("Ldbc-Sql", "module/ldbc-sql")
  .settings(scalaVersion := (core / scalaVersion).value)
  .dependsOn(core)

lazy val queryBuilder = LepusSbtProject("Ldbc-Query-Builder", "module/ldbc-query-builder")
  .settings(scalaVersion := (core / scalaVersion).value)
  .settings(libraryDependencies += scalaTest)
  .dependsOn(sql)

lazy val dsl = LepusSbtProject("Ldbc-Dsl", "module/ldbc-dsl")
  .settings(scalaVersion := (core / scalaVersion).value)
  .settings(libraryDependencies ++= Seq(
    catsEffect,
    mockito,
    scalaTest,
    mysql % Test
  ) ++ specs2)
  .dependsOn(queryBuilder)

lazy val schemaSpy = LepusSbtProject("Ldbc-SchemaSpy", "module/ldbc-schemaspy")
  .settings(scalaVersion := (core / scalaVersion).value)
  .settings(libraryDependencies += schemaspy)
  .dependsOn(core)

lazy val codegen = LepusSbtProject("Ldbc-Codegen", "module/ldbc-codegen")
  .settings(scalaVersion := (core / scalaVersion).value)
  .settings(libraryDependencies ++= Seq(parserCombinators, circeYaml, circeGeneric, scalaTest) ++ specs2)
  .dependsOn(core)

lazy val plugin = LepusSbtPluginProject("Ldbc-Plugin", "plugin")
  .settings((Compile / sourceGenerators) += Def.task {
    Generator.version(
      version      = version.value,
      scalaVersion = (core / scalaVersion).value,
      sbtVersion   = sbtVersion.value,
      dir          = (Compile / sourceManaged).value
    )
  }.taskValue)

lazy val docs = (project in file("docs"))
  .settings(
    scalaVersion := (core / scalaVersion).value,
    scalacOptions := Nil,
    publish / skip := true,
    mdocIn := baseDirectory.value / "src" / "main" / "mdoc",
    paradoxTheme := Some(builtinParadoxTheme("generic")),
    paradoxProperties ++= Map(
      "scalaVersion" -> scalaVersion.value,
      "version"      -> version.value.replace("-SNAPSHOT", ""),
    ),
    Compile / paradox / sourceDirectory := mdocOut.value,
    Compile / paradoxRoots := List("index.html", "en/index.html", "ja/index.html"),
    makeSite := makeSite.dependsOn(mdoc.toTask("")).value,
  )
  .settings(commonSettings)
  .dependsOn(
    core,
    sql,
    dsl,
    queryBuilder,
    schemaSpy,
    codegen
  )
  .enablePlugins(MdocPlugin, SitePreviewPlugin, ParadoxSitePlugin)

lazy val projects: Seq[ProjectReference] = Seq(
  core,
  plugin,
  docs
)

lazy val moduleProjects: Seq[ProjectReference] = Seq(
  sql,
  dsl,
  queryBuilder,
  schemaSpy,
  codegen
)

lazy val Ldbc = Project("Ldbc", file("."))
  .settings(scalaVersion := (core / scalaVersion).value)
  .settings(publish / skip := true)
  .settings(commonSettings)
  .aggregate((projects ++ moduleProjects): _*)
