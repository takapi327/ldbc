/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator

import java.io.File
import java.nio.file.Files
import java.nio.charset.Charset

import ldbc.generator.formatter.Naming
import ldbc.generator.parser.Parser
import ldbc.generator.model.{ Database, Table }

private[ldbc] object LdbcGenerator:

  def generate(
    sqlFilePaths:       Array[File],
    classNameFormat:    String,
    propertyNameFormat: String,
    sourceManaged:      File,
    baseDirectory:      File
  ): Array[File] =
    val classNameFormatter    = Naming.fromString(classNameFormat)
    val propertyNameFormatter = Naming.fromString(propertyNameFormat)

    val parsed = sqlFilePaths.flatMap { file =>

      val content = new String(
        Files.readAllBytes(file.toPath),
        Charset.defaultCharset()
      )

      Parser.parse(content) match
        case Parser.Success(parsed, _)         => parsed
        case Parser.NoSuccess(errorMessage, _) => throw new IllegalArgumentException(s"NoSuccess: $errorMessage")
        case Parser.Failure(errorMessage, _)   => throw new IllegalArgumentException(s"Failure: $errorMessage")
        case Parser.Error(errorMessage, _)     => throw new IllegalArgumentException(s"Error: $errorMessage")
    }

    parsed
      .groupBy(_._1)
      .flatMap { (name, list) =>
        val statements = list.flatMap(_._2).toSet
        statements.map {
          case statement: Table.CreateStatement =>
            TableModelGenerator.generate(name, statement, classNameFormatter, propertyNameFormatter, sourceManaged)
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
