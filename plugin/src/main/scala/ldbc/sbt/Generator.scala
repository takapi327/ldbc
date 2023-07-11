/** This file is part of the Lepus Framework. For the full copyright and license information, please view the LICENSE
  * file that was distributed with this source code.
  */

package ldbc.sbt

import sbt._
import sbt.Keys._

import scala.language.reflectiveCalls

import ldbc.sbt.CustomKeys._
import ldbc.sbt.AutoImport._

object Generator {
  val generate: Def.Initialize[Task[Seq[File]]] =
    generateCode(
      Compile / sqlFiles,
      Compile / classNameFormat,
      Compile / propertyNameFormat,
      Compile / sourceManaged,
      Compile / baseDirectory
    )

  private def convertToUrls(files: Seq[File]): Array[URL] = files.map(_.toURI.toURL).toArray

  def generateCode(
    sqlFilePaths:       SettingKey[List[File]],
    classNameFormat:    SettingKey[Format],
    propertyNameFormat: SettingKey[Format],
    sourceManaged:      SettingKey[File],
    baseDirectory:      SettingKey[File]
  ): Def.Initialize[Task[Seq[File]]] = Def.task {

    type LdbcGenerator = {
      def generate(
        sqlFilePaths:       Array[File],
        classNameFormat:    String,
        propertyNameFormat: String,
        sourceManaged:      File,
        baseDirectory:      File
      ): Array[File]
    }

    val projectClassLoader = new ProjectClassLoader(
      urls   = convertToUrls((Runtime / externalDependencyClasspath).value.files),
      parent = baseClassloader.value
    )

    val mainClass:  Class[_]      = projectClassLoader.loadClass("ldbc.generator.LdbcGenerator$")
    val mainObject: LdbcGenerator = mainClass.getField("MODULE$").get(null).asInstanceOf[LdbcGenerator]

    mainObject.generate(
      sqlFilePaths.value.toArray,
      classNameFormat.value.toString,
      propertyNameFormat.value.toString,
      sourceManaged.value,
      baseDirectory.value
    )
  }
}
