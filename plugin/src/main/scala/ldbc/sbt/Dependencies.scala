/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sbt

import sbt._

trait Dependencies {
  private def component(id: String): ModuleID = "io.github.takapi327" %% id % ldbc.build.Version.current

  val ldbcSql          = component("ldbc-sql")
  val ldbcDsl          = component("ldbc-dsl")
  val ldbcStatement    = component("ldbc-statement")
  val ldbcQueryBuilder = component("ldbc-query-builder")
  val ldbcSchema       = component("ldbc-schema")
  val ldbcCodegen      = component("ldbc-codegen")
  val ldbcHikari       = component("ldbc-hikari")
  val jdbcConnector    = component("jdbc-connector")
  val ldbcConnector    = component("ldbc-connector")
}

object Dependencies extends Dependencies
