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

ThisBuild / tlBaseVersion              := "0.3"
ThisBuild / tlFatalWarnings            := true
ThisBuild / projectName                := "ldbc"
ThisBuild / scalaVersion               := scala3
ThisBuild / crossScalaVersions         := Seq(scala3, scala34)
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.corretto(java11), JavaSpec.corretto(java17))
ThisBuild / githubWorkflowBuildPreamble ++= List(dockerRun) ++ settings2n
ThisBuild / githubWorkflowAddedJobs ++= Seq(sbtScripted.value)
ThisBuild / githubWorkflowBuildPostamble += dockerStop
ThisBuild / githubWorkflowTargetBranches        := Seq("**")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository                 := "https://s01.oss.sonatype.org/service/local"

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .default("core", "ldbc core project")
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core"   % "2.10.0",
      "org.scalatest" %%% "scalatest"   % "3.2.17" % Test,
      "org.specs2"    %%% "specs2-core" % "4.20.5" % Test
    )
  )
  .platformsSettings(JSPlatform, NativePlatform)(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.5.0"
    )
  )

lazy val sql = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .module("sql", "JDBC API wrapped project with Effect System")
  .settings(
    libraryDependencies += "org.typelevel" %%% "cats-core" % "2.10.0"
  )
  .platformsSettings(JSPlatform, NativePlatform)(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.5.0"
    )
  )

lazy val queryBuilder = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .module("query-builder", "Project to build type-safe queries")
  .settings(libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.17" % Test)
  .dependsOn(core, sql)

lazy val dsl = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .module("dsl", "Projects that provide a way to connect to the database")
  .settings(
    libraryDependencies ++= Seq(
      catsEffect,
      mockito,
      scalaTest,
      mysql % Test
    ) ++ specs2
  )
  .dependsOn(queryBuilder)

lazy val schemaSpy = LepusSbtProject("ldbc-schemaSpy", "module/ldbc-schemaspy")
  .settings(description := "Project to generate SchemaSPY documentation")
  .settings(libraryDependencies += schemaspy)
  .dependsOn(core.jvm)

lazy val codegen = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .module("codegen", "Project to generate code from Sql")
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-parser-combinators" % "2.3.0",
      "org.scalatest"          %%% "scalatest"                % "3.2.17" % Test,
      "org.specs2"             %%% "specs2-core"              % "4.20.5" % Test
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-generic" % "0.14.7",
      "io.circe" %%% "circe-yaml"    % "0.15.1"
    )
  )
  .platformsSettings(JSPlatform, NativePlatform)(
    libraryDependencies += "com.armanbilge" %%% "circe-scala-yaml" % "0.0.4"
  )
  .dependsOn(core)

lazy val jdbcConnector = crossProject(JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("module/jdbc-connector"))
  .settings(
    name        := "jdbc-connector",
    description := "JDBC API wrapped project with Effect System."
  )
  .defaultSettings
  .settings(libraryDependencies += catsEffect)
  .dependsOn(sql)

lazy val connector = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .module("connector", "MySQL connector written in pure Scala3")
  .settings(
    scalacOptions += "-Ykind-projector:underscores",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect"       % "3.5.4",
      "co.fs2"        %%% "fs2-core"          % "3.10-365636d",
      "co.fs2"        %%% "fs2-io"            % "3.10-365636d",
      "org.scodec"    %%% "scodec-bits"       % "1.1.38",
      "org.scodec"    %%% "scodec-core"       % "2.2.2",
      "org.scodec"    %%% "scodec-cats"       % "1.2.0",
      "org.typelevel" %%% "otel4s-core-trace" % "0.7.0",
      "org.typelevel" %%% "twiddles-core"     % "0.8.0",
      "org.typelevel" %%% "munit-cats-effect" % "2.0.0-RC1" % Test
    )
  )
  .jsSettings(
    Test / scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
  )
  .dependsOn(sql)

lazy val hikari = LepusSbtProject("ldbc-hikari", "module/ldbc-hikari")
  .settings(description := "Project to build HikariCP")
  .settings(
    libraryDependencies ++= Seq(
      catsEffect,
      typesafeConfig,
      hikariCP
    ) ++ specs2
  )
  .dependsOn(dsl.jvm)

lazy val plugin = LepusSbtPluginProject("ldbc-plugin", "plugin")
  .settings(description := "Projects that provide sbt plug-ins")
  .settings((Compile / sourceGenerators) += Def.task {
    Generator.version(
      version      = version.value,
      scalaVersion = scalaVersion.value,
      sbtVersion   = sbtVersion.value,
      dir          = (Compile / sourceManaged).value
    )
  }.taskValue)

lazy val benchmark = (project in file("benchmark"))
  .settings(description := "Projects for Benchmark Measurement")
  .settings(scalacOptions --= removeSettings)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      scala3Compiler,
      mysql,
      doobie,
      slick
    )
  )
  .dependsOn(jdbcConnector.jvm, dsl.jvm)
  .enablePlugins(JmhPlugin, AutomateHeaderPlugin, NoPublishPlugin)

lazy val docs = (project in file("docs"))
  .settings(
    description   := "Documentation for ldbc",
    scalacOptions := Nil,
    mdocIn        := baseDirectory.value / "src" / "main" / "mdoc",
    paradoxTheme  := Some(builtinParadoxTheme("generic")),
    paradoxProperties ++= Map(
      "org"          -> organization.value,
      "scalaVersion" -> scalaVersion.value,
      "version"      -> version.value.takeWhile(_ != '+'),
      "mysqlVersion" -> mysqlVersion
    ),
    Compile / paradox / sourceDirectory := mdocOut.value,
    Compile / paradoxRoots              := List("index.html", "en/index.html", "ja/index.html"),
    makeSite                            := makeSite.dependsOn(mdoc.toTask("")).value,
    git.remoteRepo                      := "git@github.com:takapi327/ldbc.git",
    ghpagesNoJekyll                     := true
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
  .enablePlugins(MdocPlugin, SitePreviewPlugin, ParadoxSitePlugin, GhpagesPlugin, NoPublishPlugin)

lazy val ldbc = tlCrossRootProject
  .settings(description := "Pure functional JDBC layer with Cats Effect 3 and Scala 3")
  .settings(commonSettings)
  .aggregate(
    core,
    sql,
    queryBuilder,
    dsl,
    codegen,
    jdbcConnector,
    connector,
    plugin,
    docs,
    benchmark,
    schemaSpy,
    hikari
  )
  .enablePlugins(NoPublishPlugin)
