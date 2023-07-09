/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.model

import ldbc.generator.formatter.Naming

case class Attributes(
  constraint:               Boolean,
  default:                  Option[Default],
  visible:                  Option[String],
  key:                      Option[List[String]],
  comment:                  Option[Comment],
  collate:                  Option[String],
  columnFormat:             Option[String],
  engineAttribute:          Option[Key.EngineAttribute],
  secondaryEngineAttribute: Option[Key.SecondaryEngineAttribute],
  storage:                  Option[String]
)

case class ColumnDefinition(
  name:       String,
  dataType:   DataType,
  attributes: Option[Attributes]
):

  val scalaType =
    val `type` = dataType.scalaType match
      case ScalaType.Enum(types) => name
      case _                     => dataType.scalaType.code
    if attributes.forall(_.constraint) then s"Option[${ `type` }]"
    else s"${ `type` }"

  private def default(formatter: Naming) =
    attributes.fold("")(attribute =>
      dataType.scalaType match
        case ScalaType.Enum(types) =>
          attribute.default
            .map {
              case Default.Value(value) =>
                val `type` = formatter.format(name)
                if attribute.constraint then s".DEFAULT(Some(${ `type` }.$value))"
                else s".DEFAULT(${ `type` }.$value)"
              case default => default.toCode(attribute.constraint)
            }
            .getOrElse("")
        case _ => attribute.default.map(_.toCode(attribute.constraint)).getOrElse("")
    )

  private val _attributes = attributes.fold("")(attribute =>
    val attributes = Seq(
      attribute.comment.map(comment => s"\"${ comment.message }\""),
      attribute.key.flatMap(keys => if keys.nonEmpty then Some(s"${ keys.mkString(", ") }") else None)
    ).flatten
    if attributes.nonEmpty then ", " + attributes.mkString(", ") else ""
  )

  def toCode(formatter: Naming): String =
    val `type` = dataType.scalaType match
      case ScalaType.Enum(types) => formatter.format(scalaType)
      case _                     => scalaType
    s"column[${ `type` }](\"$name\", ${ dataType.toCode(`type`) }" + default(formatter) + _attributes + ")"
