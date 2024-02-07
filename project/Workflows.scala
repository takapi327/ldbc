/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import sbt.*

import org.typelevel.sbt.gha.GenerativePlugin.autoImport.*

import ScalaVersions.*
import JavaVersions.*

object Workflows {
  val scalaFmt: Def.Initialize[WorkflowJob] = Def.setting(
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
      javas = List(JavaSpec.temurin(java11), JavaSpec.temurin(java17)),
    )
  )

  val copyrightHeaderCheck: Def.Initialize[WorkflowJob] = Def.setting(
    WorkflowJob(
      "headerCheck",
      "HeaderCheck",
      githubWorkflowJobSetup.value.toList ::: List(
        WorkflowStep.Run(
          List("sbt headerCheckAll"),
          name = Some("Copyright Header Check"),
        )
      ),
      scalas = List(scala3),
      javas = List(JavaSpec.temurin(java11), JavaSpec.temurin(java17)),
    )
  )

  val sbtScripted: Def.Initialize[WorkflowJob] = Def.setting(
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
      javas = List(JavaSpec.temurin(java11), JavaSpec.temurin(java17)),
    )
  )

  val dockerRun: WorkflowStep.Run = WorkflowStep.Run(
    commands = List("docker compose up -d"),
    name = Some("Start up MySQL on Docker"),
  )

  val dockerStop: WorkflowStep.Run = WorkflowStep.Run(
    commands = List("docker compose down"),
    name = Some("Stop MySQL on Docker"),
  )

  val ciRelease: WorkflowStep.Sbt = WorkflowStep.Sbt(
    commands = List("ci-release"),
    name = Some("Publish project"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
}
