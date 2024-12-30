/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen

import java.io.File
import java.nio.file.Files

import scala.io.Codec

import ldbc.codegen.formatter.Naming
import ldbc.codegen.model.*
import ldbc.codegen.parser.yml.Parser
import ldbc.codegen.builder.ColumnCodeBuilder

/**
 * An object for generating a model about Table.
 */
private[ldbc] object TableModelGenerator:

  /**
   * Methods for generating models.
   *
   * @param database
   *   Name of the database in which the Table is stored.
   * @param statement
   *   Model generated by parsing a Create table statement.
   * @param classNameFormatter
   *   Value for formatting Class name.
   * @param propertyNameFormatter
   *   Value for formatting Property name.
   * @param sourceManaged
   *   The file to which the model will be generated.
   * @param customParser
   *   Value to customize the code to be generated.
   * @param packageName
   *   A value to specify the package name of the generated file.
   * @return
   *   A file containing the generated table model.
   */
  def generate(
    database:              String,
    statement:             Table.CreateStatement,
    classNameFormatter:    Naming,
    propertyNameFormatter: Naming,
    sourceManaged:         File,
    customParser:          Option[Seq[Parser.Table]],
    packageName:           String
  ): File =

    val custom = customParser.flatMap(_.find(_.name == statement.tableName))

    val className = classNameFormatter.format(statement.tableName)
    val properties = statement.columnDefinitions.map(column =>
      propertyGenerator(
        className,
        column,
        propertyNameFormatter,
        classNameFormatter,
        custom.flatMap(_.findColumn(column.name))
      )
    )

    val objects =
      statement.columnDefinitions.flatMap(column =>
        custom
          .flatMap(_.findColumn(column.name))
          .fold(
            enumGenerator(column, classNameFormatter)
          )(_ => None)
      )

    val directory = sourceManaged.toPath.resolve(database)
    val output = if !directory.toFile.exists() then
      directory.toFile.getParentFile.mkdirs()
      Files.createDirectory(directory)
    else directory

    val outputFile = new File(output.toFile, s"$className.scala")

    if !outputFile.exists() then
      outputFile.getParentFile.mkdirs()
      outputFile.createNewFile()

    val builder = ColumnCodeBuilder(classNameFormatter)

    val columns =
      statement.columnDefinitions.map(column => builder.build(column, custom.flatMap(_.findColumn(column.name))))

    val classExtends = custom.flatMap(_.`class`.map(_.`extends`.mkString(", "))).fold("")(str => s" extends $str")

    val objectExtends = custom.flatMap(_.`object`.map(_.`extends`.mkString(", "))).fold("")(str => s" extends $str")

    val allColumns = statement.columnDefinitions.map(column => Naming.toCamel(column.name))

    val scalaSource =
      s"""
         |package ${ if database.nonEmpty then s"$packageName.$database" else packageName }
         |
         |import ldbc.schema.*
         |
         |case class $className(
         |  ${ properties.mkString(",\n  ") }
         |)$classExtends
         |
         |object $className$objectExtends:
         |
         |  ${ objects.mkString("\n  ") }
         |  val table = TableQuery[${ className }Table]
         |
         |  class ${ className }Table extends Table[$className]("${ statement.tableName }"):
         |    ${ columns.mkString("\n    ") }
         |
         |    override def * : Column[$className] = (${ allColumns.mkString(" *: ") }).to[$className]
         |""".stripMargin

    Files.write(outputFile.toPath, scalaSource.getBytes(summon[Codec].name))
    outputFile

  private def propertyGenerator(
    className:             String,
    column:                ColumnDefinition,
    propertyNameFormatter: Naming,
    classNameFormatter:    Naming,
    custom:                Option[Parser.Column]
  ): String =

    val scalaType = custom.fold(
      column.dataType.scalaType match
        case _: ScalaType.Enum => s"$className.${ classNameFormatter.format(column.name) }"
        case _                 => column.dataType.scalaType.code
    )(_.`type`)

    val `type` = (column.isOptional, column.dataType) match
      case (_, _: DataType.SERIAL) => scalaType
      case (true, _)               => s"Option[$scalaType]"
      case (false, _)              => scalaType

    s"${ propertyNameFormatter.format(column.name) }: ${ `type` }"

  private def enumGenerator(column: ColumnDefinition, formatter: Naming): Option[String] =
    column.dataType.scalaType match
      case ScalaType.Enum(types) =>
        val enumName = formatter.format(column.name)
        Some(s"""enum $enumName extends model.Enum:
           |    case ${ types.mkString(", ") }
           |  object $enumName extends model.EnumDataType[$enumName]:
           |    given ldbc.dsl.codec.Decoder[$enumName] = new ldbc.dsl.codec.Decoder[Int].map($enumName.fromOrdinal)
           |    given ldbc.dsl.codec.Encoder[$enumName] = ldbc.dsl.codec.Encoder[Int].contramap(_.ordinal)
           |""".stripMargin)
      case _ => None
