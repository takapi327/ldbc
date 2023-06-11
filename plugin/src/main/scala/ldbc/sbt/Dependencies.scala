/** This file is part of the Lepus Framework. For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package ldbc.sbt

import sbt._

object Dependencies {
  private def component(id: String): ModuleID = "com.github.takapi327" %% id % ldbc.build.Version.current

  val ldbcCore = component("ldbc-core")
  val ldbcDslIO = component("ldbc-dsl-io")
  val ldbcGenerator = component("ldbc-generator")
  val ldbcSchemaSPY = component("ldbc-schemaspy")
  val ldbcSql = component("ldbc-sql")
}
