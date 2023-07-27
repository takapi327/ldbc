/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.codegen

import java.io.File
import java.nio.file.Files

import scala.io.Codec

import ldbc.codegen.formatter.Naming
import ldbc.codegen.model.*

private[ldbc] object DatabaseModelGenerator:

  /** Methods for generating models.
    *
    * @param statement
    *   Model generated by parsing a Create database statement.
    * @param classNameFormatter
    *   Value for formatting Class name.
    * @param propertyNameFormatter
    *   Value for formatting Property name.
    * @param sourceManaged
    *   The file to which the model will be generated.
    * @param packageName
    *   A value to specify the package name of the generated file.
    * @return
    *   A file containing the generated database model.
    */
  def generate(
    statement:             Database.CreateStatement,
    statements:            List[String],
    classNameFormatter:    Naming,
    propertyNameFormatter: Naming,
    sourceManaged:         File,
    packageName:           String
  ): File =
    val className = classNameFormatter.format(statement.name)

    val directory = sourceManaged.toPath.resolve(statement.name)
    val output = if !directory.toFile.exists() then
      directory.toFile.getParentFile.mkdirs()
      Files.createDirectory(directory)
    else directory

    val outputFile = new File(output.toFile, s"${ className }Database.scala")

    if !outputFile.exists() then
      outputFile.getParentFile.mkdirs()
      outputFile.createNewFile()

    val character = statement.charset.fold("None")(str => s"Some(Character(\"$str\"))")
    val collate   = statement.collate.fold("None")(str => s"Some(Collate(\"$str\"))")

    val scalaSource =
      s"""
         |package $packageName.${ statement.name }
         |
         |import ldbc.core.*
         |
         |case class ${ className }Database(
         |  schemaMeta: Option[String] = None,
         |  catalog: Option[String] = Some("def"),
         |  host: String = "127.0.0.1",
         |  port: Int = 3306
         |) extends Database:
         |
         |  override val databaseType: Database.Type = Database.Type.MySQL
         |
         |  override val name: String = \"${ statement.name }\"
         |
         |  override val schema: String = \"${ statement.name }\"
         |
         |  override val character: Option[Character] = $character
         |
         |  override val collate: Option[Collate] = $collate
         |
         |  override val tables = Set(
         |    ${ statements.mkString(",\n    ") }
         |  )
         |""".stripMargin

    Files.write(outputFile.toPath, scalaSource.getBytes(summon[Codec].name))
    outputFile
