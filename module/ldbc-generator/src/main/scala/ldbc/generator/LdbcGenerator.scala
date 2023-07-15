/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator

import java.io.File
import java.nio.file.Files
import java.nio.charset.Charset

import ldbc.generator.formatter.Naming
import ldbc.generator.parser.Parser

private[ldbc] object LdbcGenerator:

  def generate(
    sqlFilePaths:       Array[File],
    classNameFormat:    String,
    propertyNameFormat: String,
    sourceManaged:      File,
    baseDirectory:      File
  ): Array[File] =
    sqlFilePaths.flatMap(file =>

      val content = new String(
        Files.readAllBytes(file.toPath),
        Charset.defaultCharset()
      )

      val classNameFormatter    = Naming.fromString(classNameFormat)
      val propertyNameFormatter = Naming.fromString(propertyNameFormat)

      Parser.parse(content) match
        case Parser.Success(parsed, _) =>
          parsed.flatMap { (name, statements) =>
            statements.map(statement =>
              TableModelGenerator.generate(name, statement, classNameFormatter, propertyNameFormatter, sourceManaged)
            )
          }
        case Parser.NoSuccess(errorMessage, _) =>
          println(s"NoSuccess: $errorMessage")
          List.empty
        case Parser.Failure(errorMessage, _) =>
          println(s"Failure: $errorMessage")
          List.empty
        case Parser.Error(errorMessage, _) =>
          println(s"Error: $errorMessage")
          List.empty
    )
