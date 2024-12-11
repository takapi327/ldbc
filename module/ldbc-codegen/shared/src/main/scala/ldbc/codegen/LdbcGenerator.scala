/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen

import java.io.File
import java.nio.file.Files
import java.nio.charset.Charset

import ldbc.codegen.formatter.Naming
import ldbc.codegen.parser.Parser
import ldbc.codegen.parser.yml.Parser as YmlParser

private[ldbc] object LdbcGenerator:

  def generate(
    parseFiles:         Array[File],
    customYamlFiles:    Array[File],
    classNameFormat:    String,
    propertyNameFormat: String,
    sourceManaged:      File,
    baseDirectory:      File,
    packageName:        String
  ): Array[File] =
    val classNameFormatter    = Naming.fromString(classNameFormat)
    val propertyNameFormatter = Naming.fromString(propertyNameFormat)

    val custom = customYamlFiles.map { file =>
      val content = new String(
        Files.readAllBytes(file.toPath),
        Charset.defaultCharset()
      )

      YmlParser.parse(content)
    }

    val parsed = parseFiles.flatMap { file =>

      val content = new String(
        Files.readAllBytes(file.toPath),
        Charset.defaultCharset()
      )

      val parser = Parser(file.getName)

      parser.parse(content) match
        case parser.Success(parsed, _)         => parsed
        case parser.NoSuccess(errorMessage, _) => throw new IllegalArgumentException(s"Parsed NoSuccess: $errorMessage")
        case parser.Failure(errorMessage, _)   => throw new IllegalArgumentException(s"Parsed Failure: $errorMessage")
        case parser.Error(errorMessage, _)     => throw new IllegalArgumentException(s"Parsed Error: $errorMessage")
    }

    parsed
      .groupBy(_._1)
      .flatMap { (name, list) =>
        val statements = list.flatMap(_._2).toSet
        statements.map { statement =>
          val customTables = custom.find(_.database.name == name).map(_.database.tables)
          TableModelGenerator.generate(
            name,
            statement,
            classNameFormatter,
            propertyNameFormatter,
            sourceManaged,
            customTables,
            packageName
          )
        }
      }
      .toArray
