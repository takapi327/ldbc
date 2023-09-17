/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import sbt._

object Dependencies {

  val cats = "org.typelevel" %% "cats-core" % "2.10.0"
  val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.1"

  val schemaspy = "org.schemaspy" % "schemaspy" % "6.2.3"

  val parserCombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0"

  val circeYaml = "io.circe" %% "circe-yaml" % "0.14.2"
  val circeGeneric = "io.circe" %% "circe-generic" % "0.14.6"

  val mysql = "mysql" % "mysql-connector-java" % "8.0.33"

  val specs2Version = "5.3.2"
  val specs2: Seq[ModuleID] = Seq(
    "specs2-core",
    "specs2-junit",
  ).map("org.specs2" %% _ % specs2Version % Test)

  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.17" % Test

  val mockito = "org.mockito" % "mockito-inline" % "5.2.0" % Test
}
