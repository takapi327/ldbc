/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator

import java.io.File
import java.nio.file.Files
import java.nio.charset.Charset

import scala.io.Codec

import ldbc.generator.formatter.Naming

private[ldbc] object LdbcGenerator:

  def generate(
    sqlFilePaths:  Array[File],
    classNameFormat: String,
    propertyNameFormat: String,
    sourceManaged: File,
    baseDirectory: File
  ): Array[File] =
    sqlFilePaths.flatMap(file =>

      val content = new String(
        Files.readAllBytes(file.toPath),
        Charset.defaultCharset()
      )

      val classNameFormatter = Naming.fromString(classNameFormat)
      val propertyNameFormatter = Naming.fromString(propertyNameFormat)

      Parser.parse(content) match
        case Parser.Success(parsed, _) =>
          parsed.flatMap { (name, statements) =>
            statements.map(statement =>
              val className  = classNameFormatter.format(statement.tableName)
              val properties = statement.columnDefinitions.map(column => s"${ propertyNameFormatter.format(column.name) }: ${ column.scalaType }")

              val outputFile = new File(sourceManaged, s"$className.scala")

              if !outputFile.exists() then
                outputFile.getParentFile.mkdirs()
                outputFile.createNewFile()

              val keyDefinitions = statement.keyDefinitions.map(key =>
                s".keySet(table => ${ key.toCode("table", classNameFormatter, propertyNameFormatter) })"
              )

              val packageName = if name.nonEmpty then s"ldbc.generated.$name" else "ldbc.generated"

              val scalaSource =
                s"""
                 |package $packageName
                 |
                 |import ldbc.core.*
                 |
                 |case class $className(
                 |  ${ properties.mkString(",\n  ") }
                 |)
                 |
                 |object $className:
                 |
                 |  val table: TABLE[$className] = Table[$className]("${ statement.tableName }")(
                 |    ${ statement.columnDefinitions.map(_.toCode).mkString(",\n    ") }
                 |  )
                 |  ${ keyDefinitions.mkString("") }
                 |""".stripMargin

              Files.write(outputFile.toPath, scalaSource.getBytes(summon[Codec].name))
              outputFile
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
