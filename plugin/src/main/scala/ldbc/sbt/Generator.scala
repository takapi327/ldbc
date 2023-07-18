/** This file is part of the Lepus Framework. For the full copyright and license information, please view the LICENSE
  * file that was distributed with this source code.
  */

package ldbc.sbt

import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.io.FilenameFilter

import scala.language.reflectiveCalls

import sbt._
import sbt.Keys._

import ldbc.sbt.CustomKeys._
import ldbc.sbt.AutoImport._

object Generator {
  val generate: Def.Initialize[Task[Seq[File]]] =
    generateCode(
      Compile / parseFiles,
      Compile / parseDirectories,
      Compile / excludeFiles,
      Compile / classNameFormat,
      Compile / propertyNameFormat,
      Compile / sourceManaged,
      Compile / baseDirectory
    )

  private def convertToUrls(files: Seq[File]): Array[URL] = files.map(_.toURI.toURL).toArray

  private var cacheMap:       Map[String, FileTime] = Map.empty
  private var generatedCache: Set[File]             = Set.empty

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

  private def sqlFileFilter(excludes: List[String]) = new FilenameFilter {
    override def accept(dir: File, name: String): Boolean = {
      if (name.toLowerCase.endsWith(".sql") && !excludes.contains(name)) {
        true
      } else {
        false
      }
    }
  }

  private def generateCode(
    parseFiles:         SettingKey[List[File]],
    parseDirectories:   SettingKey[List[File]],
    excludeFiles:       SettingKey[List[String]],
    classNameFormat:    SettingKey[Format],
    propertyNameFormat: SettingKey[Format],
    sourceManaged:      SettingKey[File],
    baseDirectory:      SettingKey[File]
  ): Def.Initialize[Task[Seq[File]]] = Def.task {

    type LdbcGenerator = {
      def generate(
        parseFiles:         Array[File],
        classNameFormat:    String,
        propertyNameFormat: String,
        sourceManaged:      File,
        baseDirectory:      File
      ): Array[File]
    }

    val sqlFilesInDirectory = parseDirectories.value.flatMap(file => {
      if (file.isDirectory) {
        file.listFiles(sqlFileFilter(excludeFiles.value)).toList
      } else {
        List.empty
      }
    })

    val filtered = parseFiles.value.filter(file => sqlFileFilter(excludeFiles.value).accept(file, file.getName))

    val combinedFiles = (filtered ++ sqlFilesInDirectory).distinct

    val projectClassLoader = new ProjectClassLoader(
      urls   = convertToUrls((Runtime / externalDependencyClasspath).value.files),
      parent = baseClassloader.value
    )

    val mainClass:  Class[_]      = projectClassLoader.loadClass("ldbc.generator.LdbcGenerator$")
    val mainObject: LdbcGenerator = mainClass.getField("MODULE$").get(null).asInstanceOf[LdbcGenerator]

    val changed = changedHits(combinedFiles)

    val executeFiles = (changed.nonEmpty, generatedCache.count(_.exists()) == 0) match {
      case (true, _)      => changed
      case (false, true)  => combinedFiles
      case (false, false) => List.empty
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
