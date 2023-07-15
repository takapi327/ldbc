/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.generator

import java.io.File
import java.nio.file.Files

import scala.io.Codec

import ldbc.generator.formatter.Naming
import ldbc.generator.model.*

private[ldbc] object DatabaseModelGenerator:

  /** Methods for generating models.
   *
   * @param database
   * Name of the database.
   * @param statement
   * Model generated by parsing a Create database statement.
   * @param classNameFormatter
   * Value for formatting Class name.
   * @param propertyNameFormatter
   * Value for formatting Property name.
   * @param sourceManaged
   * The file to which the model will be generated.
   * @return
   * A file containing the generated database model.
   */
  def generate(
    database: String,
    statement: Database.CreateStatement,
    statements: List[String],
    classNameFormatter: Naming,
    propertyNameFormatter: Naming,
    sourceManaged: File
  ): File =
    val className = classNameFormatter.format(statement.name)

    val directory = sourceManaged.toPath.resolve(database)
    val output = if !directory.toFile.exists() then
      directory.toFile.getParentFile.mkdirs()
      Files.createDirectory(directory)
    else directory

    val outputFile = new File(output.toFile, s"$className.scala")

    if !outputFile.exists() then
      outputFile.getParentFile.mkdirs()
      outputFile.createNewFile()

    val packageName = if database.nonEmpty then s"ldbc.generated.$database" else "ldbc.generated"

    val character = statement.charset.fold("None")(str => s"Some(Character(\"$str\"))")
    val collate = statement.collate.fold("None")(str => s"Some(Collate(\"$str\"))")

    val scalaSource =
      s"""
         |package $packageName
         |
         |import ldbc.core.*
         |
         |case class $className(
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
