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
    resolvers += "Lepus Maven" at "s3://com.github.takapi327.s3-ap-northeast-1.amazonaws.com/lepus/",
    libraryDependencies += ldbcGenerator,
    baseClassloader    := Commands.baseClassloaderTask.value,
    parseFiles         := List.empty,
    parseDirectories   := List.empty,
    excludeFiles       := List.empty,
    customYamlFiles    := List.empty,
    classNameFormat    := Format.PASCAL,
    propertyNameFormat := Format.CAMEL,
    (Compile / sourceGenerators) += Generator.generate.taskValue
  )
}
