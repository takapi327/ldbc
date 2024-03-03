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
      javas  = List(JavaSpec.temurin(java11), JavaSpec.temurin(java17))
    )
  )

  val installDependencies: WorkflowStep.Run = WorkflowStep.Run(
    commands = List(
      "sudo apt-get update",
      "sudo apt-get install -y cmake libssl-dev libcrypto++-dev"
    ),
    name = Some("Install dependencies"),
    cond = Some("matrix.project == 'ldbcNative'")
  )

  val clones2n: WorkflowStep.Run = WorkflowStep.Run(
    commands = List("git clone https://github.com/aws/s2n-tls.git"),
    name     = Some("Clone s2n repository"),
    cond     = Some("matrix.project == 'ldbcNative'")
  )

  val buildAndInstalls2n: WorkflowStep.Run = WorkflowStep.Run(
    commands = List(
      "cd s2n-tls",
      "cmake . -Bbuild -DCMAKE_INSTALL_PREFIX=/usr/local",
      "cmake --build build",
      "sudo cmake --install build"
    ),
    name = Some("Build and install s2n"),
    cond = Some("matrix.project == 'ldbcNative'")
  )

  val settings2n: List[WorkflowStep.Run] = List(
    installDependencies,
    clones2n,
    buildAndInstalls2n
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
