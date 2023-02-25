/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import ScalaVersions._
import JavaVersions._
import BuildSettings._
import Dependencies._

ThisBuild / crossScalaVersions         := Seq(scala3)
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin(java11))

ThisBuild / githubWorkflowAddedJobs ++= Seq(
  WorkflowJob(
    "scalafmt",
    "Scalafmt",
    githubWorkflowJobSetup.value.toList ::: List(
      WorkflowStep.Run(
        List("sbt scalafmtCheck"),
        name = Some("Scalafmt check"),
      )
    ),
    scalas = List(scala3),
    javas  = List(JavaSpec.temurin(java11)),
  )
)

lazy val LdbcCoreProject = LepusSbtProject("Ldbc-Core", "core")
  .settings(scalaVersion := sys.props.get("scala.version").getOrElse(scala3))
  .settings(libraryDependencies ++= Seq(
    catsEffect,
    scalaTest
  ) ++ specs2)

lazy val LdbcSqlProject = LepusSbtProject("Ldbc-Sql", "module/ldbc-sql")
  .settings(scalaVersion := (LdbcCoreProject / scalaVersion).value)
  .settings(libraryDependencies ++= Seq(
    mockito
  ) ++ specs2)
  .dependsOn(LdbcCoreProject)

lazy val coreProjects: Seq[ProjectReference] = Seq(
  LdbcCoreProject
)

lazy val moduleProjects: Seq[ProjectReference] = Seq(
  LdbcSqlProject
)

lazy val Ldbc = Project("Ldbc", file("."))
  .settings(scalaVersion := (LdbcCoreProject / scalaVersion).value)
  .settings(publish / skip := true)
  .settings(commonSettings: _*)
  .aggregate((coreProjects ++ moduleProjects): _*)
