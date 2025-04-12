/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.core.builder

import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime

import scala.io.Codec

import ldbc.core.syntax.given
import ldbc.core.Database

/**
 * Object to generate sql files that write out Create statements for databases and tables.
 */
object SqlFileBuilder:

  private def build(
    output:   String,
    fileName: Option[String],
    database: Database
  ): Unit =

    val tableQueryBuilder: Seq[TableQueryBuilder] = database.tables.toSeq.sorted.map(TableQueryBuilder(_))

    val characterSet = Seq(
      database.character.map(_.queryString),
      database.collate.map(_.queryString)
    ).flatten

    val queryString: String =
      s"""/** This file is automatically generated by ldbc based on the source code.
         |  * This file cannot be modified manually. Instead, modify the database and table settings in the source code.
         |  *
         |  * Generated at ${ LocalDateTime.now().toString }
         |  */
         |
         |CREATE DATABASE `${ database.name }`${ characterSet.map(str => s" $str").mkString("") };
         |
         |
         |USE `${ database.name }`;
         |
         |${ tableQueryBuilder.map(_.createStatement).mkString("\n") }
         |""".stripMargin

    val outputFile = new File(output, s"${ fileName.getOrElse(database.name) }.sql")

    if !outputFile.exists() then
      outputFile.getParentFile.mkdirs()
      outputFile.createNewFile()

    Files.write(outputFile.toPath, queryString.getBytes(summon[Codec].name))

  def build(output: String, database: Database): Unit = build(output, None, database)
  def build(output: String, fileName: String, database: Database): Unit =
    build(output, Some(fileName), database)
