/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.model

case class ColumnDefinition(
  name:       String,
  dataType:   DataType,
  attributes: Option[List[ColumnDefinition.Attributes]]
):

  val isOptional: Boolean = attributes.fold(true)(_.map {
    case v: ColumnDefinition.Attribute.Condition => v.bool
    case _                                       => true
  }.headOption.getOrElse(true))

  private val defaultAttribute: Option[ColumnDefinition.Attribute.Default] = attributes.fold(None)(_.flatMap {
    case v: ColumnDefinition.Attribute.Default => Some(v)
    case _                                     => None
  }.headOption)

  private val scalaType =
    dataType.scalaType match
      case ScalaType.Enum(types) => if isOptional then s"Option[$name]" else name
      case _                     => dataType.propertyType(isOptional)

  private val default =
    attributes.fold("")(attribute =>
      dataType.scalaType match
        case ScalaType.Enum(types) =>
          defaultAttribute
            .map {
              case ColumnDefinition.Attribute.Default.Value(value) =>
                if isOptional then s".DEFAULT(Some($name.$value))"
                else s".DEFAULT($name.$value)"
              case default => default.toCode(isOptional)
            }
            .getOrElse("")
        case _ => defaultAttribute.map(_.toCode(isOptional)).getOrElse("")
    )

  private val _attributes = attributes.fold("")(attributes =>
    /** Only comments need to be brought to the top because the definition method is different. */
    val comment = attributes.flatMap {
      case comment: CommentSet => Some(s"\"${ comment.message }\"")
      case _                   => None
    }
    val result = comment ++ attributes.flatMap {
      case key: ColumnDefinition.Attribute.Key => Some(s"${ key.kind }")
      case _                                   => None
    }
    if result.nonEmpty then ", " + result.mkString(", ") else ""
  )

  def toCode: String =
    dataType.scalaType match
      case ScalaType.Enum(types) =>
        val strings = dataType.toCode(scalaType).split('.')
        s"column(\"$name\", ${ strings.head }(using $name)${ strings.tail.map(str => s".$str").mkString("") }" + default + _attributes + ")"
      case _ =>
        s"column(\"$name\", ${ dataType.toCode(scalaType) }" + default + _attributes + ")"

  def toCode(customType: String): String =
    val `type` =
      if isOptional then s"Option[${ dataType.getTypeMatches(customType) }]" else dataType.getTypeMatches(customType)
    s"column(\"$name\", ${ dataType.toCode(`type`) }" + default + _attributes + ")"

object ColumnDefinition:

  type Attributes = Attribute | CommentSet | Key.EngineAttribute | Key.SecondaryEngineAttribute | CommentOut

  trait Attribute

  object Attribute:

    case class Condition(bool: Boolean) extends Attribute

    /** Trait for setting SQL Default values
      */
    trait Default extends Attribute:

      def toCode(isOptional: Boolean): String

    object Default:

      /** Model to be used when a value matching the DataType type is set.
        *
        * @param value
        *   Value set as the default value for DataType
        */
      case class Value(value: String | Int) extends Default:
        private val str = value match
          case v: String => s"\"$v\""
          case v: Int    => s"$v"

        override def toCode(isOptional: Boolean): String =
          if isOptional then s".DEFAULT(Some($str))"
          else s".DEFAULT($str)"

      /** Object for setting NULL as the Default value when the SQL DataType is NULL-allowed.
        */
      object Null extends Default:
        override def toCode(isOptional: Boolean): String =
          if isOptional then ".DEFAULT(None)"
          else
            throw new IllegalArgumentException("NULL cannot be set as the default value for non-null-allowed columns.")

      /** Model for setting TimeStamp-specific Default values.
        *
        * @param onUpdate
        *   Value to determine whether to set additional information
        */
      case class CurrentTimestamp(onUpdate: Boolean) extends Default:
        override def toCode(isOptional: Boolean): String =
          if onUpdate then ".DEFAULT_CURRENT_TIMESTAMP(true)"
          else ".DEFAULT_CURRENT_TIMESTAMP(false)"

    case class Visible(kind: String) extends Attribute

    case class Key(kind: String) extends Attribute

    case class Collate(set: String) extends Attribute

    case class ColumnFormat(format: String) extends Attribute

    case class Storage(kind: String) extends Attribute
