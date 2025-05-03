/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import BuildSettings.*
import Dependencies.*
import Implicits.*
import JavaVersions.*
import ProjectKeys.*
import ScalaVersions.*
import Workflows.*

ThisBuild / tlBaseVersion      := LdbcVersions.latest
ThisBuild / tlFatalWarnings    := true
ThisBuild / projectName        := "ldbc"
ThisBuild / scalaVersion       := scala3
ThisBuild / crossScalaVersions := Seq(scala3, scala36)
ThisBuild / githubWorkflowJavaVersions := Seq(
  JavaSpec.corretto(java11),
  JavaSpec.corretto(java17),
  JavaSpec.corretto(java21)
)
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
    ),
    Test / scalacOptions -= "-Werror"
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
      "org.typelevel" %%% "twiddles-core"     % "0.8.0",
      "org.typelevel" %%% "cats-free"         % "2.10.0",
      "org.typelevel" %%% "cats-effect"       % "3.6.1",
      "org.typelevel" %%% "munit-cats-effect" % "2.1.0" % Test
    )
  )
  .dependsOn(sql)

lazy val statement = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .module("statement", "Project for building type-safe statements")
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % "3.2.18" % Test
    )
  )
  .dependsOn(dsl)
  .platformsEnablePlugins(JVMPlatform, JSPlatform, NativePlatform)(
    spray.boilerplate.BoilerplatePlugin
  )

lazy val queryBuilder = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .module("query-builder", "Project to build type-safe queries")
  .settings(libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.18" % Test)
  .dependsOn(statement)

lazy val schema = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .module("schema", "Type safety schema construction project")
  .settings(libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.18" % Test)
  .settings(Test / scalacOptions -= "-Werror")
  .dependsOn(statement)

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
      "org.typelevel"          %%% "munit-cats-effect"        % "2.1.0" % Test
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-generic" % "0.14.13",
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
      "org.typelevel" %%% "cats-effect"       % "3.6.1",
      "co.fs2"        %%% "fs2-core"          % "3.12.0",
      "co.fs2"        %%% "fs2-io"            % "3.12.0",
      "org.scodec"    %%% "scodec-bits"       % "1.1.38",
      "org.scodec"    %%% "scodec-core"       % "2.2.2",
      "org.scodec"    %%% "scodec-cats"       % "1.2.0",
      "org.typelevel" %%% "otel4s-core-trace" % "0.12.0",
      "org.typelevel" %%% "twiddles-core"     % "0.8.0",
      "org.typelevel" %%% "munit-cats-effect" % "2.1.0" % Test
    )
  )
  .jsSettings(
    Test / scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
  )
  .nativeEnablePlugins(ScalaNativeBrewedConfigPlugin)
  .nativeSettings(Test / nativeBrewFormulas += "s2n")
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
    Test / fork := true,
    scalacOptions += "-Ximplicit-search-limit:100000"
  )
  .defaultSettings
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "munit-cats-effect" % "2.1.0" % Test,
      mysql            % Test
    )
  )
  .dependsOn(jdbcConnector, connector, queryBuilder, schema)
  .enablePlugins(NoPublishPlugin)

lazy val benchmark = (project in file("benchmark"))
  .settings(description := "Projects for Benchmark Measurement")
  .settings(scalacOptions ++= additionalSettings)
  .settings(scalacOptions --= removeSettings)
  .settings(commonSettings)
  .settings(Compile / javacOptions ++= Seq("--release", java21))
  .settings(
    libraryDependencies ++= Seq(
      scala3Compiler,
      mysql,
      doobie,
      slick
    )
  )
  .dependsOn(jdbcConnector.jvm, connector.jvm, queryBuilder.jvm)
  .enablePlugins(JmhPlugin, AutomateHeaderPlugin, NoPublishPlugin)

lazy val http4sExample = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .example("http4s", "Http4s example project")
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s"    %% "http4s-dsl"          % "0.23.30",
      "org.http4s"    %% "http4s-ember-server" % "0.23.30",
      "org.http4s"    %% "http4s-circe"        % "0.23.30",
      "ch.qos.logback" % "logback-classic"     % "1.5.18",
      "io.circe"      %% "circe-generic"       % "0.14.10"
    )
  )
  .dependsOn(connector, schema)

lazy val hikariCPExample = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .example("hikariCP", "HikariCP example project")
  .settings(
    libraryDependencies ++= Seq(
      hikariCP,
      mysql
    )
  )
  .dependsOn(jdbcConnector, dsl)

lazy val otelExample = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .example("otel", "OpenTelemetry example project")
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel"   %% "otel4s-oteljava"                           % "0.12.0",
      "io.opentelemetry" % "opentelemetry-exporter-otlp"               % "1.49.0" % Runtime,
      "io.opentelemetry" % "opentelemetry-sdk-extension-autoconfigure" % "1.49.0" % Runtime
    )
  )
  .settings(
    javaOptions ++= Seq(
      "-Dotel.java.global-autoconfigure.enabled=true",
      "-Dotel.service.name=ldbc-otel-example",
      "-Dotel.metrics.exporter=none"
    )
  )
  .dependsOn(connector, dsl)

lazy val docs = (project in file("docs"))
  .settings(
    description              := "Documentation for ldbc",
    mdocIn                   := (Compile / sourceDirectory).value / "mdoc",
    tlSiteIsTypelevelProject := Some(TypelevelProject.Affiliate),
    mdocVariables ++= Map(
      "ORGANIZATION"  -> organization.value,
      "SCALA_VERSION" -> scalaVersion.value,
      "MYSQL_VERSION" -> mysqlVersion,
      "VERSION" -> "0.3.0-RC2" // TODO: Manually set sbt typelevel as RC is not allowed as a VERSION in sbt typelevel setting
    ),
    laikaTheme := LaikaSettings.helium.value,
    // Modify tlSite task to run the LLM docs script after the site is generated
    tlSite := {
      tlSite.value
      val log        = streams.value.log
      val scriptPath = baseDirectory.value.getParentFile / "script" / "build-llm-docs.sh"

      if (!scriptPath.exists) {
        log.warn(s"LLM docs script not found at: $scriptPath")
      } else {
        log.info(s"Running LLM docs script: $scriptPath")
        val exitCode = scala.sys.process.Process(scriptPath.toString).!
        if (exitCode != 0) {
          log.warn(s"LLM docs script exited with code: $exitCode")
        } else {
          log.success("LLM docs successfully generated")
        }
      }
    }
  )
  .settings(commonSettings)
  .dependsOn(
    connector.jvm,
    schema.jvm
  )
  .enablePlugins(AutomateHeaderPlugin, TypelevelSitePlugin, NoPublishPlugin)

lazy val mcpDocumentServer = crossProject(JSPlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JSPlatform)
  .in(file("mcp/document-server"))
  .settings(
    name        := "mcp-ldbc-document-server",
    description := "Project for MCP document server for ldbc",
    run / fork  := false
  )
  .settings((Compile / sourceGenerators) += Def.task {
    Generator.version(
      version      = version.value,
      scalaVersion = scalaVersion.value,
      sbtVersion   = sbtVersion.value,
      dir          = (Compile / sourceManaged).value
    )
  }.taskValue)
  .settings(
    libraryDependencies += "io.github.takapi327" %%% "mcp-scala-server" % "0.1.0-alpha2"
  )
  .jsSettings(
    npmPackageName         := "@ldbc/mcp-document-server",
    npmPackageDescription  := (Compile / description).value,
    npmPackageKeywords     := Seq("mcp", "scala", "ldbc"),
    npmPackageAuthor       := "takapi327",
    npmPackageLicense      := Some("MIT"),
    npmPackageBinaryEnable := true,
    npmPackageVersion      := "0.1.0-alpha1",
    npmPackageREADME := Some(baseDirectory.value / "README.md"),
    npmPackageAdditionalNpmConfig := Map(
      "homepage" -> _root_.io.circe.Json.fromString("https://takapi327.github.io/ldbc/"),
      "private"  -> _root_.io.circe.Json.fromBoolean(false),
      "publishConfig" -> _root_.io.circe.Json.obj(
        "access" -> _root_.io.circe.Json.fromString("public")
      )
    ),
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
    Compile / mainClass := Some("ldbc.mcp.StdioServer"),

    // Custom task to copy mdoc docs to npm package directory
    npmPackage := {
      (Compile / npmPackage).value
      val log = streams.value.log

      val docsSourceDir = file("docs/target/mdoc")
      val docsTargetDir = (Compile / npmPackageOutputDirectory).value / "docs"

      if (docsSourceDir.exists()) {
        log.info(s"Copying mdoc documentation from ${ docsSourceDir } to ${ docsTargetDir }")
        IO.copyDirectory(docsSourceDir, docsTargetDir)
        log.success("Documentation copied successfully")
      } else {
        log.warn(s"Source docs directory not found: ${ docsSourceDir }")
      }
    }
  )
  .defaultSettings
  .enablePlugins(NpmPackagePlugin, NoPublishPlugin)

lazy val examples = Seq(
  http4sExample,
  hikariCPExample,
  otelExample
)

lazy val ldbc = tlCrossRootProject
  .settings(description := "Pure functional JDBC layer with Cats Effect 3 and Scala 3")
  .settings(commonSettings)
  .aggregate(
    core,
    sql,
    jdbcConnector,
    connector,
    dsl,
    statement,
    queryBuilder,
    schema,
    codegen,
    plugin,
    tests,
    docs,
    benchmark,
    schemaSpy,
    hikari,
    mcpDocumentServer
  )
  .aggregate(examples *)
  .enablePlugins(NoPublishPlugin)
