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

lazy val CoreProject = LepusSbtProject("Core", "core")
  .settings(scalaVersion := sys.props.get("scala.version").getOrElse(scala3))
  .settings(libraryDependencies ++= Seq(
    catsEffect
  ))

lazy val coreProjects: Seq[ProjectReference] = Seq(
  CoreProject
)

lazy val ldbc = project.in(file("."))
  .settings(scalaVersion := (CoreProject / scalaVersion).value)
  .settings(publish / skip := true)
  .settings(commonSettings: _*)
  .aggregate(coreProjects: _*)
