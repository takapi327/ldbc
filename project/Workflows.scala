/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import sbt.*

import org.typelevel.sbt.gha.GenerativePlugin.autoImport.*

import JavaVersions.*
import ScalaVersions.*

object Workflows {

  val sbtScripted: Def.Initialize[WorkflowJob] = Def.setting(
    WorkflowJob(
      "sbtScripted",
      "sbt scripted",
      githubWorkflowJobSetup.value.toList ::: List(
        WorkflowStep.Run(
          List("sbt +publishLocal"),
          name = Some("sbt publishLocal")
        ),
        WorkflowStep.Run(
          List("sbt scripted"),
          name = Some("sbt scripted")
        )
      ),
      scalas = List(scala3),
      javas  = List(JavaSpec.temurin(java11), JavaSpec.temurin(java17), JavaSpec.temurin(java21))
    )
  )

  val sbtCoverageReport: Def.Initialize[WorkflowJob] = Def.setting(
    WorkflowJob(
      id     = "coverage",
      name   = "Generate coverage report",
      javas  = List(githubWorkflowJavaVersions.value.last),
      scalas = githubWorkflowScalaVersions.value.toList,
      steps = List(WorkflowStep.Checkout) ++ WorkflowStep.SetupJava(
        List(githubWorkflowJavaVersions.value.last),
      ) ++ githubWorkflowGeneratedCacheSteps.value ++ List(
        WorkflowStep.Sbt(List("coverage", "ldbcJVM/test", "coverageAggregate")),
        WorkflowStep.Use(
          UseRef.Public(
            "codecov",
            "codecov-action",
            "v4",
          ),
          params = Map(
            "flags" -> List("${{matrix.scala}}").mkString(","),
          ),
          env = Map(
            "CODECOV_TOKEN" -> "${{secrets.CODECOV_TOKEN}}",
          ),
        ),
      ),
    )
  )

  val dockerRun: WorkflowStep.Run = WorkflowStep.Run(
    commands = List("docker compose up -d"),
    name     = Some("Start up MySQL on Docker")
  )

  val dockerStop: WorkflowStep.Run = WorkflowStep.Run(
    commands = List("docker compose down"),
    name     = Some("Stop MySQL on Docker")
  )
}
