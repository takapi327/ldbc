/** This file is part of the Lepus Framework. For the full copyright and license information, please view the LICENSE
  * file that was distributed with this source code.
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
}
