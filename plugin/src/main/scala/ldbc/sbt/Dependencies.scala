/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
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
