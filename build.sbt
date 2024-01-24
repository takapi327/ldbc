/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import ScalaVersions.*
import JavaVersions.*
import BuildSettings.*
import Dependencies.*
import Workflows.*
import ProjectKeys.*
import Implicits.*

ThisBuild / projectName := "ldbc"
ThisBuild / crossScalaVersions := Seq(scala3)
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.corretto(java11), JavaSpec.corretto(java17))
ThisBuild / githubWorkflowBuildPreamble += dockerRun
ThisBuild / githubWorkflowAddedJobs ++= Seq(
  scalaFmt.value, copyrightHeaderCheck.value, sbtScripted.value
)
ThisBuild / githubWorkflowBuildPostamble += dockerStop
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / githubWorkflowPublish := Seq(ciRelease)

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .default("core", "ldbc core project")
  .settings(libraryDependencies ++= Seq(cats, scalaTest) ++ specs2)

lazy val sql = crossProject(JVMPlatform)
  .crossType(CrossType.Full)
  .module("sql", "JDBC API wrapped project with Effect System")
  .dependsOn(core)

lazy val queryBuilder = crossProject(JVMPlatform)
  .crossType(CrossType.Full)
  .module("query-builder", "Project to build type-safe queries")
  .settings(libraryDependencies += scalaTest)
  .dependsOn(sql)

lazy val dsl = crossProject(JVMPlatform)
  .crossType(CrossType.Full)
  .module("dsl", "Projects that provide a way to connect to the database")
  .settings(libraryDependencies ++= Seq(
    catsEffect,
    mockito,
    scalaTest,
    mysql % Test
  ) ++ specs2)
  .dependsOn(queryBuilder)

lazy val schemaSpy = LepusSbtProject("ldbc-schemaSpy", "module/ldbc-schemaspy")
  .settings(description := "Project to generate SchemaSPY documentation")
  .settings(libraryDependencies += schemaspy)
  .dependsOn(core.jvm)

lazy val codegen = crossProject(JVMPlatform)
  .crossType(CrossType.Full)
  .module("codegen", "Project to generate code from Sql")
  .settings(libraryDependencies ++= Seq(parserCombinators, circeYaml, circeGeneric, scalaTest) ++ specs2)
  .dependsOn(core)

lazy val hikari = LepusSbtProject("ldbc-hikari", "module/ldbc-hikari")
  .settings(description := "Project to build HikariCP")
  .settings(libraryDependencies ++= Seq(
    catsEffect,
    typesafeConfig,
    hikariCP
  ) ++ specs2)
  .dependsOn(dsl.jvm)

lazy val plugin = LepusSbtPluginProject("ldbc-plugin", "plugin")
  .settings(description := "Projects that provide sbt plug-ins")
  .settings((Compile / sourceGenerators) += Def.task {
    Generator.version(
      version      = version.value,
      scalaVersion = (core.jvm / scalaVersion).value,
      sbtVersion   = sbtVersion.value,
      dir          = (Compile / sourceManaged).value
    )
  }.taskValue)

lazy val benchmark = (project in file("benchmark"))
  .settings(scalaVersion := (core.jvm / scalaVersion).value)
  .settings(description := "Projects for Benchmark Measurement")
  .settings(scalacOptions ++= scala3Settings)
  .settings(commonSettings)
  .settings(publish / skip := true)
  .settings(libraryDependencies ++= Seq(
    scala3Compiler,
    mysql,
    doobie,
    slick
  ))
  .dependsOn(dsl.jvm)
  .enablePlugins(JmhPlugin, AutomateHeaderPlugin)

lazy val docs = (project in file("docs"))
  .settings(
    scalaVersion := (core.jvm / scalaVersion).value,
    description := "Documentation for ldbc",
    scalacOptions := Nil,
    publish / skip := true,
    mdocIn := baseDirectory.value / "src" / "main" / "mdoc",
    paradoxTheme := Some(builtinParadoxTheme("generic")),
    paradoxProperties ++= Map(
      "org"          -> organization.value,
      "scalaVersion" -> scalaVersion.value,
      "version"      -> version.value.takeWhile(_ != '+'),
      "mysqlVersion" -> mysqlVersion
    ),
    Compile / paradox / sourceDirectory := mdocOut.value,
    Compile / paradoxRoots := List("index.html", "en/index.html", "ja/index.html"),
    makeSite := makeSite.dependsOn(mdoc.toTask("")).value,
    git.remoteRepo := "git@github.com:takapi327/ldbc.git",
    ghpagesNoJekyll := true,
  )
  .settings(commonSettings)
  .dependsOn(
    core.jvm,
    sql.jvm,
    dsl.jvm,
    queryBuilder.jvm,
    schemaSpy,
    codegen.jvm,
    hikari
  )
  .enablePlugins(MdocPlugin, SitePreviewPlugin, ParadoxSitePlugin, GhpagesPlugin)

lazy val jvmProjects: Seq[ProjectReference] = Seq(
  core.jvm,
  sql.jvm,
  queryBuilder.jvm,
  dsl.jvm,
  codegen.jvm,
  plugin,
  docs,
  benchmark,
  schemaSpy,
  hikari
)

lazy val jsProjects: Seq[ProjectReference] = Seq(
  core.js
)

lazy val nativeProjects: Seq[ProjectReference] = Seq(
  core.native
)

lazy val ldbc = project.in(file("."))
  .settings(scalaVersion := (core.jvm / scalaVersion).value)
  .settings(description := "Pure functional JDBC layer with Cats Effect 3 and Scala 3")
  .settings(publish / skip := true)
  .settings(commonSettings)
  .aggregate((jvmProjects ++ jsProjects ++ nativeProjects)*)
