/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import laika.ast.Path.Root

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
ThisBuild / githubWorkflowBuildPreamble ++= List(dockerRun) ++ nativeBrewInstallWorkflowSteps.value
ThisBuild / nativeBrewInstallCond := Some("matrix.project == 'ldbcNative'")
ThisBuild / githubWorkflowAddedJobs ++= Seq(sbtScripted.value)
ThisBuild / githubWorkflowBuildPostamble += dockerStop
ThisBuild / githubWorkflowTargetBranches        := Seq("**")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / tlSitePublishBranch                 := None

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository                 := "https://s01.oss.sonatype.org/service/local"

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .default("core", "ldbc core project")
  .settings(
    onLoadMessage :=
      s"""
         |${ scala.Console.RED }WARNING: This project is deprecated and will be removed in future versions. Please use ldbc-schema instead.
         |
         |${ scala.Console.RED }${ organization.value } %% ${ name.value } % ${ version.value }
         |
         |         ${ scala.Console.RED }↓↓↓↓↓
         |
         |${ scala.Console.RED }${ organization.value } %% ldbc-schema % ${ version.value }
         |
         |""".stripMargin,
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core"   % "2.10.0",
      "org.scalatest" %%% "scalatest"   % "3.2.18" % Test,
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
  .platformsSettings(JSPlatform, NativePlatform)(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.5.0"
    )
  )

lazy val dsl = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .module("dsl", "Projects that provide a way to connect to the database")
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect"       % "3.5.4",
      "org.typelevel" %%% "munit-cats-effect" % "2.0.0" % Test
    )
  )
  .dependsOn(sql)

lazy val queryBuilder = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .module("query-builder", "Project to build type-safe queries")
  .settings(libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.18" % Test)
  .dependsOn(dsl)

lazy val schema = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .module("schema", "Type safety schema construction project")
  .settings(libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.18" % Test)
  .dependsOn(queryBuilder)

lazy val schemaSpy = LepusSbtProject("ldbc-schemaSpy", "module/ldbc-schemaspy")
  .settings(
    description := "Project to generate SchemaSPY documentation",
    onLoadMessage := s"${ scala.Console.RED }WARNING: This project is deprecated and will be removed in future versions.${ scala.Console.RESET }",
    libraryDependencies += schemaspy
  )
  .dependsOn(core.jvm)

lazy val codegen = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .module("codegen", "Project to generate code from Sql")
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-parser-combinators" % "2.3.0",
      "org.scalatest"          %%% "scalatest"                % "3.2.18" % Test,
      "org.specs2"             %%% "specs2-core"              % "4.20.5" % Test
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-generic" % "0.14.10",
      "io.circe" %%% "circe-yaml"    % "0.16.0"
    )
  )
  .platformsSettings(JSPlatform, NativePlatform)(
    libraryDependencies += "com.armanbilge" %%% "circe-scala-yaml" % "0.0.4"
  )
  .dependsOn(schema)

lazy val jdbcConnector = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
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
      "co.fs2"        %%% "fs2-core"          % "3.11.0",
      "co.fs2"        %%% "fs2-io"            % "3.11.0",
      "org.scodec"    %%% "scodec-bits"       % "1.1.38",
      "org.scodec"    %%% "scodec-core"       % "2.2.2",
      "org.scodec"    %%% "scodec-cats"       % "1.2.0",
      "org.typelevel" %%% "otel4s-core-trace" % "0.9.0",
      "org.typelevel" %%% "twiddles-core"     % "0.8.0",
      "org.typelevel" %%% "munit-cats-effect" % "2.0.0" % Test
    )
  )
  .jsSettings(
    Test / scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
  )
  .nativeEnablePlugins(ScalaNativeBrewedConfigPlugin)
  .nativeSettings(
    libraryDependencies += "com.armanbilge" %%% "epollcat" % "0.1.6",
    Test / nativeBrewFormulas += "s2n"
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

lazy val tests = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .in(file("tests"))
  .settings(
    name        := "tests",
    description := "Projects for testing",
    Test / fork := true
  )
  .defaultSettings
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "munit-cats-effect" % "2.0.0" % Test,
      mysql            % Test
    )
  )
  .dependsOn(jdbcConnector, connector, queryBuilder)
  .enablePlugins(NoPublishPlugin)

lazy val benchmark = (project in file("benchmark"))
  .settings(description := "Projects for Benchmark Measurement")
  .settings(scalacOptions ++= additionalSettings)
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
  .dependsOn(jdbcConnector.jvm, connector.jvm, schema.jvm)
  .enablePlugins(JmhPlugin, AutomateHeaderPlugin, NoPublishPlugin)

lazy val docs = (project in file("docs"))
  .settings(
    description              := "Documentation for ldbc",
    mdocIn                   := (Compile / sourceDirectory).value / "mdoc",
    tlSiteIsTypelevelProject := Some(TypelevelProject.Affiliate),
    mdocVariables ++= Map(
      "ORGANIZATION"  -> organization.value,
      "SCALA_VERSION" -> scalaVersion.value,
      "MYSQL_VERSION" -> mysqlVersion
    ),
    laikaTheme := tlSiteHelium.value.site
      .internalCSS(Root / "css" / "site.css")
      .build
  )
  .settings(commonSettings)
  .dependsOn(
    connector.jvm,
    schema.jvm
  )
  .enablePlugins(AutomateHeaderPlugin, TypelevelSitePlugin, NoPublishPlugin)

lazy val ldbc = tlCrossRootProject
  .settings(description := "Pure functional JDBC layer with Cats Effect 3 and Scala 3")
  .settings(commonSettings)
  .aggregate(
    core,
    sql,
    jdbcConnector,
    connector,
    dsl,
    queryBuilder,
    schema,
    codegen,
    plugin,
    tests,
    docs,
    benchmark,
    schemaSpy,
    hikari
  )
  .enablePlugins(NoPublishPlugin)
