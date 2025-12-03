/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import com.typesafe.tools.mima.core.*
import BuildSettings.*
import Implicits.*
import JavaVersions.*
import ProjectKeys.*
import ScalaVersions.*
import Workflows.*

ThisBuild / tlBaseVersion              := LdbcVersions.latest
ThisBuild / tlFatalWarnings            := true
ThisBuild / projectName                := "ldbc"
ThisBuild / scalaVersion               := scala3
ThisBuild / crossScalaVersions         := Seq(scala3, scala37)
ThisBuild / githubWorkflowJavaVersions := Seq(
  JavaSpec.corretto(java11),
  JavaSpec.corretto(java17),
  JavaSpec.corretto(java21),
  JavaSpec.corretto(java25)
)
ThisBuild / githubWorkflowBuildPreamble ++= List(dockerRun) ++ nativeBrewInstallWorkflowSteps.value
ThisBuild / nativeBrewInstallCond := Some("matrix.project == 'ldbcNative'")
ThisBuild / githubWorkflowAddedJobs ++= Seq(sbtScripted.value, sbtCoverageReport.value)
ThisBuild / githubWorkflowBuildPostamble += dockerStop
ThisBuild / githubWorkflowTargetBranches        := Seq("**")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / tlSitePublishBranch                 := None

lazy val sql = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .module("sql", "JDBC API wrapped project with Effect System")
  .platformsSettings(JSPlatform, NativePlatform)(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.5.0"
    )
  )

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .module("core", "Core project for ldbc")
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-free"   % "2.10.0",
      "org.typelevel" %%% "cats-effect" % "3.6.2"
    )
  )
  .dependsOn(sql)

lazy val dsl = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .module("dsl", "Projects that provide a way to connect to the database")
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "twiddles-core"     % "0.8.0",
      "co.fs2"        %%% "fs2-core"          % "3.12.0",
      "org.typelevel" %%% "munit-cats-effect" % "2.1.0" % Test
    )
  )
  .dependsOn(core)

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
      "io.circe" %%% "circe-generic" % "0.14.15",
      "io.circe" %%% "circe-yaml"    % "0.16.1"
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
  .dependsOn(core)

lazy val connector = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .module("connector", "MySQL connector written in pure Scala3")
  .settings(
    scalacOptions += "-Ykind-projector:underscores",
    libraryDependencies ++= Seq(
      "co.fs2"        %%% "fs2-core"          % "3.12.2",
      "co.fs2"        %%% "fs2-io"            % "3.12.2",
      "org.scodec"    %%% "scodec-bits"       % "1.1.38",
      "org.scodec"    %%% "scodec-core"       % "2.2.2",
      "org.scodec"    %%% "scodec-cats"       % "1.2.0",
      "org.typelevel" %%% "otel4s-core-trace" % "0.14.0",
      "org.typelevel" %%% "twiddles-core"     % "0.8.0",
      "org.typelevel" %%% "munit-cats-effect" % "2.1.0" % Test
    )
  )
  .jsSettings(
    Test / scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
  )
  .nativeEnablePlugins(ScalaNativeBrewedConfigPlugin)
  .nativeSettings(Test / nativeBrewFormulas += "s2n")
  .dependsOn(core)

lazy val awsAuthenticationPlugin = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .module("aws-authentication-plugin", "")
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-xml"         % "2.2.0",
      "co.fs2"                 %%% "fs2-core"          % "3.12.2",
      "co.fs2"                 %%% "fs2-io"            % "3.12.2",
      "io.circe"      %% "circe-parser"       % "0.14.10",
      "org.typelevel"          %%% "munit-cats-effect" % "2.1.0" % Test
    )
  )
  .jsSettings(
    Test / scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
  )
  .nativeEnablePlugins(ScalaNativeBrewedConfigPlugin)
  .nativeSettings(Test / nativeBrewFormulas += "s2n")

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

lazy val zioInterop = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .module("zio-interop", "Projects that provide a way to connect to the database for ZIO")
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio"              % "2.1.21",
      "dev.zio" %%% "zio-interop-cats" % "23.1.0.5",
      "dev.zio" %%% "zio-test"         % "2.1.21" % Test,
      "dev.zio" %%% "zio-test-sbt"     % "2.1.21" % Test
    )
  )
  .jsSettings(
    Test / scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
  )
  .dependsOn(connector % "test->compile")

lazy val tests = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("tests"))
  .settings(
    crossScalaVersions                      := Seq(scala3, scala37),
    name                                    := "tests",
    description                             := "Projects for testing",
    libraryDependencies += "org.typelevel" %%% "munit-cats-effect" % "2.1.0",
    Test / unmanagedSourceDirectories ++= {
      val sourceDir = (Test / sourceDirectory).value
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, 7)) => Seq(sourceDir / "scala-3.7")
        case _            => Nil
      }
    }
  )
  .defaultSettings
  .jvmSettings(
    Test / fork                       := true,
    libraryDependencies += "com.mysql" % "mysql-connector-j" % "8.4.0" % Test
  )
  .jvmConfigure(_ dependsOn jdbcConnector.jvm)
  .jsSettings(
    Test / scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, 7)) => Seq("-nowarn")
        case _            => Nil
      }
    }
  )
  .nativeEnablePlugins(ScalaNativeBrewedConfigPlugin)
  .nativeSettings(Test / nativeBrewFormulas += "s2n")
  .dependsOn(connector, queryBuilder, schema)
  .enablePlugins(NoPublishPlugin)

lazy val benchmark = (project in file("benchmark"))
  .settings(description := "Projects for Benchmark Measurement")
  .settings(scalacOptions ++= additionalSettings)
  .settings(scalacOptions --= removeSettings)
  .settings(commonSettings)
  .settings(Compile / javacOptions ++= Seq("--release", java21))
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang"     %% "scala3-compiler"   % scala3,
      "com.mysql"           % "mysql-connector-j" % "8.4.0",
      "org.tpolecat"       %% "doobie-core"       % "1.0.0-RC10",
      "com.typesafe.slick" %% "slick"             % "3.6.1",
      "com.zaxxer"          % "HikariCP"          % "7.0.2"
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
      "org.http4s"    %% "http4s-dsl"          % "0.23.33",
      "org.http4s"    %% "http4s-ember-server" % "0.23.33",
      "org.http4s"    %% "http4s-circe"        % "0.23.33",
      "ch.qos.logback" % "logback-classic"     % "1.5.21",
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
      "com.zaxxer" % "HikariCP"          % "7.0.2",
      "com.mysql"  % "mysql-connector-j" % "8.4.0"
    )
  )
  .dependsOn(jdbcConnector, dsl)

lazy val otelExample = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .example("otel", "OpenTelemetry example project")
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel"   %% "otel4s-oteljava"                           % "0.14.0",
      "io.opentelemetry" % "opentelemetry-exporter-otlp"               % "1.56.0" % Runtime,
      "io.opentelemetry" % "opentelemetry-sdk-extension-autoconfigure" % "1.56.0" % Runtime
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

lazy val zioExample = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .example("zio", "ZIO example project")
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-http" % "3.5.1",
      "dev.zio" %% "zio-json" % "0.7.44"
    )
  )
  .dependsOn(connector, dsl, zioInterop)

lazy val docs = (project in file("docs"))
  .settings(
    description              := "Documentation for ldbc",
    mdocIn                   := (Compile / sourceDirectory).value / "mdoc",
    tlSiteIsTypelevelProject := Some(TypelevelProject.Affiliate),
    mdocVariables ++= Map(
      "ORGANIZATION"  -> organization.value,
      "SCALA_VERSION" -> scalaVersion.value,
      "MYSQL_VERSION" -> "8.4.0"
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
    npmPackageName                := "@ldbc/mcp-document-server",
    npmPackageDescription         := (Compile / description).value,
    npmPackageKeywords            := Seq("mcp", "scala", "ldbc"),
    npmPackageAuthor              := "takapi327",
    npmPackageLicense             := Some("MIT"),
    npmPackageBinaryEnable        := true,
    npmPackageVersion             := "0.1.0-alpha6",
    npmPackageREADME              := Some(baseDirectory.value / "README.md"),
    npmPackageAdditionalNpmConfig := Map(
      "homepage"      -> _root_.io.circe.Json.fromString("https://takapi327.github.io/ldbc/"),
      "private"       -> _root_.io.circe.Json.fromBoolean(false),
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
  otelExample,
  zioExample
)

lazy val ldbc = tlCrossRootProject
  .settings(description := "Pure functional JDBC layer with Cats Effect 3 and Scala 3")
  .settings(commonSettings)
  .aggregate(
    sql,
    core,
    jdbcConnector,
    connector,
    dsl,
    statement,
    queryBuilder,
    schema,
    codegen,
    zioInterop,
    awsAuthenticationPlugin,
    plugin,
    tests,
    docs,
    benchmark,
    mcpDocumentServer
  )
  .aggregate(examples *)
  .enablePlugins(NoPublishPlugin)
