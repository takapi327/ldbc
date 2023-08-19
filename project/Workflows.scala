/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import sbt._

import sbtghactions.GenerativePlugin.autoImport._

import ScalaVersions._
import JavaVersions._

object Workflows {
  val scalaFmt = Def.setting(
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
      javas = List(JavaSpec.temurin(java11)),
    )
  )

  val sbtScripted = Def.setting(
    WorkflowJob(
      "sbtScripted",
      "sbt scripted",
      githubWorkflowJobSetup.value.toList ::: List(
        WorkflowStep.Run(
          List("sbt +publishLocal"),
          name = Some("sbt publishLocal"),
        ),
        WorkflowStep.Run(
          List("sbt scripted"),
          name = Some("sbt scripted"),
        )
      ),
      scalas = List(scala3),
      javas = List(JavaSpec.temurin(java11)),
    )
  )

  val dockerRun = WorkflowStep.Run(
    commands = List("docker compose up -d"),
    name = Some("Start up MySQL on Docker"),
  )

  val dockerStop = WorkflowStep.Run(
    commands = List("docker compose down"),
    name = Some("Stop MySQL on Docker"),
  )
}
