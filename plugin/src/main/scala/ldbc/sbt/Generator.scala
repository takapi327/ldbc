/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sbt

import java.io.FilenameFilter
import java.lang.reflect.Method
import java.nio.file.attribute.FileTime
import java.nio.file.Files

import sbt._
import sbt.Keys._

import ldbc.sbt.AutoImport._
import ldbc.sbt.CustomKeys._

import sbtcompat.PluginCompat._
import xsbti.FileConverter

object Generator {

  private val logger = ProcessLogger()

  /**
   * Generate code from SQL schema. Create a cache, and if a cache exists, do not generate it.
   */
  val generate: Def.Initialize[Task[Seq[File]]] =
    generateCode(
      Compile / parseFiles,
      Compile / parseDirectories,
      Compile / excludeFiles,
      Compile / customYamlFiles,
      Compile / classNameFormat,
      Compile / propertyNameFormat,
      Compile / sourceManaged,
      Compile / ldbcPackage
    )

  /**
   * Generate code from SQL schema. Always generate code.
   */
  val alwaysGenerate: Def.Initialize[Task[Seq[File]]] =
    generateCode(
      Compile / parseFiles,
      Compile / parseDirectories,
      Compile / excludeFiles,
      Compile / customYamlFiles,
      Compile / classNameFormat,
      Compile / propertyNameFormat,
      Compile / sourceManaged,
      Compile / ldbcPackage,
      alwaysGenerate = true
    )

  private def convertToUrls(files: Seq[File]): Array[URL] = files.map(_.toURI.toURL).toArray

  /**
   * Resolves `ldbc.codegen.LdbcGenerator.generate` out of the project's own classpath.
   *
   * A structural type would read better, but Scala 3 only permits structural calls on receivers
   * extending `scala.reflect.Selectable`, which has no Scala 2.12 equivalent. Going through
   * `java.lang.reflect` directly keeps one source tree compiling for both sbt 1 and sbt 2.
   */
  private def loadGenerator(classLoader: ClassLoader): (AnyRef, Method) = {
    val moduleClass = classLoader.loadClass("ldbc.codegen.LdbcGenerator$")
    val module      = moduleClass.getField("MODULE$").get(null)
    val method      = moduleClass.getMethod(
      "generate",
      classOf[Array[File]],
      classOf[Array[File]],
      classOf[String],
      classOf[String],
      classOf[File],
      classOf[String]
    )
    (module, method)
  }

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
    packageName:        SettingKey[String],
    alwaysGenerate:     Boolean = false
  ): Def.Initialize[Task[Seq[File]]] = Def.task {

    implicit val conv: FileConverter = fileConverter.value

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
      urls   = convertToUrls(toFiles((Runtime / externalDependencyClasspath).value)),
      parent = baseClassloader.value
    )

    val (generator, generate) = loadGenerator(projectClassLoader)

    val changed = changedHits(combinedFiles)

    val customChanged = changedHits(customYamlFiles.value)

    val executeFiles =
      (alwaysGenerate, changed.nonEmpty, generatedCache.count(_.exists()) == 0, customChanged.nonEmpty) match {
        case (true, _, _, _)          => combinedFiles
        case (false, _, _, true)      => combinedFiles
        case (false, true, _, _)      => changed
        case (false, false, true, _)  => combinedFiles
        case (false, false, false, _) => List.empty
      }

    if (executeFiles.nonEmpty) {
      executeFiles.foreach(file => {
        logger.debug(s"Analyze the ${ file.getName } file.")
      })
    }

    val generated = generate
      .invoke(
        generator,
        executeFiles.toArray,
        customYamlFiles.value.toArray,
        classNameFormat.value.toString,
        propertyNameFormat.value.toString,
        sourceManaged.value,
        packageName.value
      )
      .asInstanceOf[Array[File]]

    if (generated.nonEmpty) {
      logger.debug("Generated files: [" + generated.map(_.getAbsoluteFile.getName).mkString(", ") + "]")
    }

    if (generatedCache.isEmpty) {
      generatedCache = generated.toSet
      generated.toSeq
    } else {
      generatedCache = generatedCache ++ generated
      generatedCache.toSeq
    }
  }
}
