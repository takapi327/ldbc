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

lazy val LdbcCoreProject = LepusSbtProject("Ldbc-Core", "core")
  .settings(scalaVersion := sys.props.get("scala.version").getOrElse(scala3))
  .settings(libraryDependencies ++= Seq(cats, scalaTest) ++ specs2)

lazy val LdbcSqlProject = LepusSbtProject("Ldbc-Sql", "module/ldbc-sql")
  .settings(scalaVersion := (LdbcCoreProject / scalaVersion).value)
  .dependsOn(LdbcCoreProject)

lazy val LdbcQueryBuilderProject = LepusSbtProject("Ldbc-Query-Builder", "module/ldbc-query-builder")
  .settings(scalaVersion := (LdbcCoreProject / scalaVersion).value)
  .settings(libraryDependencies += scalaTest)
  .dependsOn(LdbcSqlProject)

lazy val LdbcDslProject = LepusSbtProject("Ldbc-Dsl", "module/ldbc-dsl")
  .settings(scalaVersion := (LdbcCoreProject / scalaVersion).value)
  .settings(libraryDependencies ++= Seq(
    catsEffect,
    mockito,
    scalaTest,
    mysql % Test
  ) ++ specs2)
  .dependsOn(LdbcQueryBuilderProject)

lazy val LdbcSchemaSpyProject = LepusSbtProject("Ldbc-SchemaSpy", "module/ldbc-schemaspy")
  .settings(scalaVersion := (LdbcCoreProject / scalaVersion).value)
  .settings(libraryDependencies += schemaspy)
  .dependsOn(LdbcCoreProject)

lazy val LdbcCodegenProject = LepusSbtProject("Ldbc-Codegen", "module/ldbc-codegen")
  .settings(scalaVersion := (LdbcCoreProject / scalaVersion).value)
  .settings(libraryDependencies ++= Seq(parserCombinators, circeYaml, circeGeneric, scalaTest) ++ specs2)
  .dependsOn(LdbcCoreProject)

lazy val LdbcHikari = LepusSbtProject("Ldbc-Hikari", "module/ldbc-hikari")
  .settings(scalaVersion := (LdbcCoreProject / scalaVersion).value)
  .settings(libraryDependencies ++= Seq(
    catsEffect,
    typesafeConfig,
    hikariCP
  ) ++ specs2)
  .dependsOn(LdbcDslProject)

lazy val LdbcPluginProject = LepusSbtPluginProject("Ldbc-Plugin", "plugin")
  .settings((Compile / sourceGenerators) += Def.task {
    Generator.version(
      version      = version.value,
      scalaVersion = (LdbcCoreProject / scalaVersion).value,
      sbtVersion   = sbtVersion.value,
      dir          = (Compile / sourceManaged).value
    )
  }.taskValue)

lazy val coreProjects: Seq[ProjectReference] = Seq(
  LdbcCoreProject
)

lazy val moduleProjects: Seq[ProjectReference] = Seq(
  LdbcSqlProject,
  LdbcDslProject,
  LdbcQueryBuilderProject,
  LdbcSchemaSpyProject,
  LdbcCodegenProject,
  LdbcHikari
)

lazy val pluginProjects: Seq[ProjectReference] = Seq(
  LdbcPluginProject
)

lazy val Ldbc = Project("Ldbc", file("."))
  .settings(scalaVersion := (LdbcCoreProject / scalaVersion).value)
  .settings(publish / skip := true)
  .settings(commonSettings: _*)
  .aggregate((coreProjects ++ moduleProjects ++ pluginProjects): _*)
