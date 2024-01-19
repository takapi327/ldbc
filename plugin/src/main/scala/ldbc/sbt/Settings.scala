/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
  */

package ldbc.sbt

import sbt._
import sbt.Keys._

import CustomKeys._
import AutoImport._

object Settings {

  lazy val projectSettings: Seq[Def.Setting[?]] = Def.settings(
    libraryDependencies += ldbcCodegen,
    baseClassloader    := Commands.baseClassloaderTask.value,
    parseFiles         := List.empty,
    parseDirectories   := List.empty,
    excludeFiles       := List.empty,
    customYamlFiles    := List.empty,
    classNameFormat    := Format.PASCAL,
    propertyNameFormat := Format.CAMEL,
    ldbcPackage        := "ldbc.generated",
    (Compile / sourceGenerators) += Generator.generate.taskValue,
    generateBySQLSchema := {
      Generator.alwaysGenerate.value
    },
    commands += Commands.generateBySchema
  )
}
