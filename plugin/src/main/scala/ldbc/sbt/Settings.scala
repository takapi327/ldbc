/** This file is part of the Lepus Framework. For the full copyright and license information, please view the LICENSE
  * file that was distributed with this source code.
  */

package ldbc.sbt

import sbt._
import sbt.Keys._

import CustomKeys._
import AutoImport._

object Settings {

  lazy val projectSettings = Def.settings(
    libraryDependencies += ldbcCodegen,
    baseClassloader    := Commands.baseClassloaderTask.value,
    parseFiles         := List.empty,
    parseDirectories   := List.empty,
    excludeFiles       := List.empty,
    customYamlFiles    := List.empty,
    classNameFormat    := Format.PASCAL,
    propertyNameFormat := Format.CAMEL,
    ldbcPackage        := "ldbc.generated",
    (Compile / sourceGenerators) += Generator.generate.taskValue
  )
}
