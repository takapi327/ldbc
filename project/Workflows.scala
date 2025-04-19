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

  val installSpecificOpenSSLVersion: WorkflowStep.Run = WorkflowStep.Run(
    commands = List(
      "sudo apt-get update",
      "sudo apt-get install -y build-essential checkinstall zlib1g-dev",
      "wget https://www.openssl.org/source/openssl-3.4.0.tar.gz",
      "tar -xf openssl-3.4.0.tar.gz",
      "cd openssl-3.4.0",
      "./config --prefix=/usr/local/ssl --openssldir=/usr/local/ssl shared zlib",
      "make -j$(nproc)",
      "sudo make install",
      "sudo ln -sf /usr/local/ssl/bin/openssl /usr/bin/openssl",
      "echo 'export PATH=/usr/local/ssl/bin:$PATH' >> $GITHUB_ENV",
      "echo 'export LD_LIBRARY_PATH=/usr/local/ssl/lib:$LD_LIBRARY_PATH' >> $GITHUB_ENV",
      "echo \"/usr/local/ssl/lib\" | sudo tee /etc/ld.so.conf.d/openssl.conf",
      "sudo ldconfig",
      "openssl version"
    ),
    name = Some("Install specific OpenSSL version")
  )

  val generateSSLCerts: WorkflowStep.Run = WorkflowStep.Run(
    commands = List("./script/generate-ssl-certs.sh"),
    name     = Some("Generate SSL certificates")
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
