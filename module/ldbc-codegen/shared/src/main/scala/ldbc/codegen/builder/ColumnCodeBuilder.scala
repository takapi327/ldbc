/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.builder

import ldbc.codegen.model.*
import ldbc.codegen.parser.yml.Parser
import ldbc.query.builder.formatter.Naming

/**
 * Column model for constructing code strings.
 *
 * @param formatter
 *   A formatter that converts strings to an arbitrary format
 */
case class ColumnCodeBuilder(formatter: Naming):

  def build(column: ColumnDefinition, customColumn: Option[Parser.Column]): String =
    val scalaType = buildScalaType(column, customColumn)

    val builder  = DataTypeCodeBuilder(scalaType, formatter)
    val dataType = builder.build(column.dataType)

    s"def ${Naming.toCamel(column.name)}: Column[$scalaType] = column[$scalaType](\"${column.name}\", $dataType" + buildDefault(column).getOrElse("") + column._attributes + ")"

  private def buildScalaType(column: ColumnDefinition, customColumn: Option[Parser.Column]): String =
    val code = customColumn match
      case Some(custom) => column.dataType.getTypeMatches(custom.`type`)
      case None =>
        column.dataType.scalaType match
          case ScalaType.Enum(_) => formatter.format(column.name)
          case _                 => column.dataType.scalaType.code

    if column.isOptional then s"Option[$code]" else code

  private def buildDefault(column: ColumnDefinition): Option[String] =
    column.attributes.fold(None)(attribute =>

      val defaultOpt = attribute.flatMap {
        case v: ColumnDefinition.Attribute.Default => Some(v)
        case _                                     => None
      }.headOption

      defaultOpt.map(default =>
        column.dataType.scalaType match
          case ScalaType.Enum(_) =>
            default match
              case ColumnDefinition.Attribute.Default.Value(value) =>
                val name = formatter.format(column.name)
                if column.isOptional then s".DEFAULT(Some($name.$value))"
                else s".DEFAULT($name.$value)"
              case _ => default.toCode(column.isOptional)
          case _ => default.toCode(column.isOptional)
      )
    )
