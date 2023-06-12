/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator

import java.io.File
import java.nio.file.Files
import java.nio.charset.Charset

import scala.io.Codec

private[ldbc] object LdbcGenerator:

  def generate(
    sqlFilePaths:  Array[File],
    sourceManaged: File,
    baseDirectory: File
  ): Array[File] =
    sqlFilePaths.flatMap(file =>

      val content = new String(
        Files.readAllBytes(file.toPath),
        Charset.defaultCharset()
      )

      Parser.parse(content) match
        case Parser.Success(statements, _) =>
          statements.map(statement =>
            val className  = s"${ statement.tableName.head.toUpper }${ statement.tableName.tail }"
            val properties = statement.columnDefinitions.map(column => s"${ column.name }: ${ column.scalaType }")
            val hasNullableProperty = statement.columnDefinitions.count(_.attributes.forall(_.constraint))

            val outputFile = new File(sourceManaged, s"$className.scala")

            if !outputFile.exists() then
              outputFile.getParentFile.mkdirs()
              outputFile.createNewFile()

            val scalaSource =
              s"""
                 |import ldbc.core.*
                 |${ if hasNullableProperty > 0 then "import ldbc.core.syntax.given" else "" }
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
                 |""".stripMargin

            Files.write(outputFile.toPath, scalaSource.getBytes(summon[Codec].name))
            outputFile
          )
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
