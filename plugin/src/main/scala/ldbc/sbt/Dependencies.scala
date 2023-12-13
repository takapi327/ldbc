/** This file is part of the Lepus Framework. For the full copyright and license information, please view the LICENSE
  * file that was distributed with this source code.
  */

package ldbc.sbt

import sbt._

trait Dependencies {
  private def component(id: String): ModuleID = "io.github.takapi327" %% id % ldbc.build.Version.current

  val ldbcCore         = component("ldbc-core")
  val ldbcDsl          = component("ldbc-dsl")
  val ldbcQueryBuilder = component("ldbc-query-builder")
  val ldbcCodegen      = component("ldbc-codegen")
  val ldbcSchemaSPY    = component("ldbc-schemaspy")
  val ldbcSql          = component("ldbc-sql")
  val ldbcHikari       = component("ldbc-hikari")
}

object Dependencies extends Dependencies
