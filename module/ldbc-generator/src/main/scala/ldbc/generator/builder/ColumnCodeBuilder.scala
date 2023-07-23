/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.generator.builder

import ldbc.generator.model.*
import ldbc.generator.parser.yml.Parser
import ldbc.generator.formatter.Naming

/**
 * Column model for constructing code strings.
 * 
 * @param formatter
 *   A formatter that converts strings to an arbitrary format
 */
case class ColumnCodeBuilder(formatter: Naming):

  def build(column: ColumnDefinition, customColumn: Option[Parser.Column]): String =
    val scalaType = buildScalaType(column, customColumn)

    val builder = DataTypeCodeBuilder(scalaType, formatter)
    val dataType = builder.build(column.dataType)

    s"column(\"${ column.name }\", $dataType" + buildDefault(column).getOrElse("") + column._attributes + ")"

  private def buildScalaType(column: ColumnDefinition, customColumn: Option[Parser.Column]): String =
    val code = customColumn match
      case Some(custom) => column.dataType.getTypeMatches(custom.`type`)
      case None => column.dataType.scalaType match
        case ScalaType.Enum(_) => formatter.format(column.name)
        case _ => column.dataType.scalaType.code

    if column.isOptional then s"Option[$code]" else code

  private def buildDefault(column: ColumnDefinition): Option[String] =
    column.attributes.fold(None)(attribute =>

      val defaultOpt = attribute.flatMap {
        case v: ColumnDefinition.Attribute.Default => Some(v)
        case _ => None
      }.headOption

      defaultOpt.map(default =>
        column.dataType.scalaType match
          case ScalaType.Enum(_) => default match
            case ColumnDefinition.Attribute.Default.Value(value) =>
              val name = formatter.format(column.name)
              if column.isOptional then s".DEFAULT(Some($name.$value))"
              else s".DEFAULT($name.$value)"
            case _ => default.toCode(column.isOptional)
          case _ => default.toCode(column.isOptional)
      )
    )
