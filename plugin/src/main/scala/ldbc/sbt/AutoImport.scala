/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sbt

import sbt._

object AutoImport extends Dependencies {

  val CAMEL  = Format.CAMEL
  val PASCAL = Format.PASCAL
  val SNAKE  = Format.SNAKE
  val KEBAB  = Format.KEBAB

  val parseFiles = SettingKey[List[File]](
    label       = "parseFiles",
    description = "List of SQL files to be read"
  )

  val parseDirectories = SettingKey[List[File]](
    label       = "parseDirectories",
    description = "Directory to be parsed"
  )

  val excludeFiles = SettingKey[List[String]](
    label       = "excludeFiles",
    description = "List of file names to be excluded from the analysis."
  )

  val customYamlFiles = SettingKey[List[File]](
    label       = "customYamlFiles",
    description = "List of yaml files to customize types."
  )

  val classNameFormat = SettingKey[Format](
    label       = "classNameFormat",
    description = "A value to specify the format of the Class name."
  )

  val propertyNameFormat = SettingKey[Format](
    label       = "propertyNameFormat",
    description = "A value to specify the format of the Property name."
  )

  val ldbcPackage = SettingKey[String](
    label       = "ldbcPackage",
    description = "A value to specify the package name of the generated file."
  )
}
