/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

import sbt.*

import ScalaVersions.*

object Dependencies {

  val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.4"

  val schemaspy = "org.schemaspy" % "schemaspy" % "6.2.4"

  val parserCombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0"

  val mysqlVersion = "9.1.0"
  val mysql        = "com.mysql" % "mysql-connector-j" % mysqlVersion

  val typesafeConfig = "com.typesafe" % "config" % "1.4.3"

  val hikariCP = "com.zaxxer" % "HikariCP" % "6.0.0"

  val scala3Compiler = "org.scala-lang" %% "scala3-compiler" % scala3

  val doobie = "org.tpolecat" %% "doobie-core" % "1.0.0-RC6"

  val slick = "com.typesafe.slick" %% "slick" % "3.5.2"

  val specs2Version = "5.5.8"
  val specs2: Seq[ModuleID] = Seq(
    "specs2-core",
    "specs2-junit"
  ).map("org.specs2" %% _ % specs2Version % Test)
}
