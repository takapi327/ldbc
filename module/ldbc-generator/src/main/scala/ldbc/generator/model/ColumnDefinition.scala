/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.model

case class Attributes(
  constraint:               Boolean,
  default:                  Option[Default],
  visible:                  Option[String],
  key:                      Option[List[String]],
  comment:                  Option[Comment],
  collate:                  Option[String],
  columnFormat:             Option[String],
  engineAttribute:          Option[String],
  secondaryEngineAttribute: Option[String],
  storage:                  Option[String]
)

case class ColumnDefinition(
  name:       String,
  dataType:   DataType,
  attributes: Option[Attributes]
):

  val scalaType =
    if attributes.forall(_.constraint) then s"Option[${ dataType.scalaType.code }]"
    else s"${ dataType.scalaType.code }"

  private val default =
    attributes.fold("")(attribute => attribute.default.map(_.toCode(attribute.constraint)).getOrElse(""))

  private val _attributes = attributes.fold("")(attribute =>
    val attributes = Seq(
      attribute.comment.map(comment => s"\"${ comment.message }\""),
      attribute.key.map(keys => if keys.nonEmpty then s"${ keys.mkString(", ") }" else "")
    ).flatten.mkString(", ")
    if attributes.nonEmpty then ", " + attributes else ""
  )

  def toCode: String =
    s"column[$scalaType](\"$name\", ${ dataType.toCode(scalaType) }" + default + _attributes + ")"
