/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator

import java.io.File
import java.nio.file.Files
import java.nio.charset.Charset

import ldbc.generator.formatter.Naming
import ldbc.generator.parser.Parser
import ldbc.generator.parser.yml.Parser as YmlParser
import ldbc.generator.model.{ Database, Table }

private[ldbc] object LdbcGenerator:

  def generate(
    parseFiles:         Array[File],
    customYamlFiles:    Array[File],
    classNameFormat:    String,
    propertyNameFormat: String,
    sourceManaged:      File,
    baseDirectory:      File
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
        statements.map {
          case statement: Table.CreateStatement =>
            val customTables = custom.find(_.database.name == name).map(_.database.tables)
            TableModelGenerator.generate(
              name,
              statement,
              classNameFormatter,
              propertyNameFormatter,
              sourceManaged,
              customTables
            )
          case statement: Database.CreateStatement =>
            val tableStatements = statements.flatMap {
              case statement: Table.CreateStatement =>
                Some(s"${ classNameFormatter.format(statement.tableName) }.table")
              case _: Database.CreateStatement => None
            }.toList

            DatabaseModelGenerator.generate(
              statement,
              tableStatements,
              classNameFormatter,
              propertyNameFormatter,
              sourceManaged
            )
        }
      }
      .toArray
