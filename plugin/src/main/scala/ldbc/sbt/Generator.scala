/** This file is part of the Lepus Framework. For the full copyright and license information, please view the LICENSE
  * file that was distributed with this source code.
  */

package ldbc.sbt

import java.nio.file.Files

import scala.language.reflectiveCalls

import sbt._
import sbt.Keys._

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

  private var cacheMap: Map[String, java.nio.file.attribute.FileTime] = Map.empty
  private var generatedCache: Set[File] = Set.empty

  private def changedHits(files: List[File]): List[File] = files.filter(file => {
    val hit = cacheMap.get(file.getName)
    hit match {
      case None =>
        cacheMap = cacheMap.updated(file.getName, Files.getLastModifiedTime(file.toPath))
        false
      case Some(time) =>
        cacheMap = cacheMap.updated(file.getName, Files.getLastModifiedTime(file.toPath))
        time != Files.getLastModifiedTime(file.toPath)
    }
  })

  private def generateCode(
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

    val changed = changedHits(sqlFilePaths.value)

    val executeFiles = (changed.nonEmpty, generatedCache.nonEmpty) match {
      case (true, _) => changed
      case (false, false) => sqlFilePaths.value
      case (false, true) => List.empty
    }

    if (executeFiles.nonEmpty) {
      executeFiles.foreach(file => {
        println(s"[debug] Analyze the ${ file.getName } file.")
      })
    }

    val generated = mainObject.generate(
      executeFiles.toArray,
      classNameFormat.value.toString,
      propertyNameFormat.value.toString,
      sourceManaged.value,
      baseDirectory.value
    )

    if (generatedCache.isEmpty) {
      generatedCache = generated.toSet
      generated
    } else {
      generatedCache = generatedCache ++ generated
      generatedCache.toSeq
    }
  }
}
