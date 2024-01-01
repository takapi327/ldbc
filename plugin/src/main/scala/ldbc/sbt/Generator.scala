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

  private val logger = ProcessLogger()

  val generate: Def.Initialize[Task[Seq[File]]] =
    generateCode(
      Compile / parseFiles,
      Compile / parseDirectories,
      Compile / excludeFiles,
      Compile / customYamlFiles,
      Compile / classNameFormat,
      Compile / propertyNameFormat,
      Compile / sourceManaged,
      Compile / baseDirectory,
      Compile / ldbcPackage
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
    override def accept(dir: File, name: String): Boolean =
      name.toLowerCase.endsWith(".sql") && !excludes.contains(name)
  }

  private def generateCode(
    parseFiles:         SettingKey[List[File]],
    parseDirectories:   SettingKey[List[File]],
    excludeFiles:       SettingKey[List[String]],
    customYamlFiles:    SettingKey[List[File]],
    classNameFormat:    SettingKey[Format],
    propertyNameFormat: SettingKey[Format],
    sourceManaged:      SettingKey[File],
    baseDirectory:      SettingKey[File],
    packageName:        SettingKey[String]
  ): Def.Initialize[Task[Seq[File]]] = Def.task {

    type LdbcGenerator = {
      def generate(
        parseFiles:         Array[File],
        customYamlFiles:    Array[File],
        classNameFormat:    String,
        propertyNameFormat: String,
        sourceManaged:      File,
        baseDirectory:      File,
        packageName:        String
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

    val mainClass:  Class[_]      = projectClassLoader.loadClass("ldbc.codegen.LdbcGenerator$")
    val mainObject: LdbcGenerator = mainClass.getField("MODULE$").get(null).asInstanceOf[LdbcGenerator]

    val changed = changedHits(combinedFiles)

    val customChanged = changedHits(customYamlFiles.value)

    val executeFiles = (changed.nonEmpty, generatedCache.count(_.exists()) == 0, customChanged.nonEmpty) match {
      case (_, _, true)      => combinedFiles
      case (true, _, _)      => changed
      case (false, true, _)  => combinedFiles
      case (false, false, _) => List.empty
    }

    if (executeFiles.nonEmpty) {
      executeFiles.foreach(file => {
        logger.debug(s"Analyze the ${ file.getName } file.")
      })
    }

    val generated = mainObject.generate(
      executeFiles.toArray,
      customYamlFiles.value.toArray,
      classNameFormat.value.toString,
      propertyNameFormat.value.toString,
      sourceManaged.value,
      baseDirectory.value,
      packageName.value
    )

    logger.debug("Generated files: [" + generated.map(_.getAbsoluteFile.getName).mkString(", ") + "]")

    if (generatedCache.isEmpty) {
      generatedCache = generated.toSet
      generated
    } else {
      generatedCache = generatedCache ++ generated
      generatedCache.toSeq
    }
  }
}
