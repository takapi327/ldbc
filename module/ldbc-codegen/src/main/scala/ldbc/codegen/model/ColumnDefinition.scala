/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.codegen.model

case class ColumnDefinition(
  name:       String,
  dataType:   DataType,
  attributes: Option[List[ColumnDefinition.Attributes]]
):

  val isOptional: Boolean = attributes.fold(true)(_.map {
    case v: ColumnDefinition.Attribute.Condition => v.bool
    case _                                       => true
  }.headOption.getOrElse(true))

  val _attributes = attributes.fold("")(attributes =>
    val result = attributes.flatMap {
      case attribute: CommentSet                              => Some(s"COMMENT(\"${ attribute.message }\")")
      case attribute: ColumnDefinition.Attribute.Key          => Some(s"${ attribute.kind }")
      case attribute: ColumnDefinition.Attribute.Visible      => Some(s"${ attribute.kind }")
      case attribute: ColumnDefinition.Attribute.Collate      => Some(s"Collate.${ attribute.set }")
      case attribute: ColumnDefinition.Attribute.ColumnFormat => Some(s"COLUMN_FORMAT.${ attribute.format }")
      case attribute: ColumnDefinition.Attribute.Storage      => Some(s"STORAGE.${ attribute.kind }")
      case _                                                  => None
    }
    if result.nonEmpty then ", " + result.mkString(", ") else ""
  )

object ColumnDefinition:

  type Attributes = Attribute | CommentSet | Key.EngineAttribute | Key.SecondaryEngineAttribute | CommentOut

  /** Trait representing the attribute to be set on the column. */
  trait Attribute

  object Attribute:

    /** A model for determining if a data type is null tolerant.
      *
      * @param bool
      *   true: NULL, false: NOT NULL
      */
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
      case class Value(value: String | Int | Double | Boolean) extends Default:
        private val str = value match
          case v: String => s"\"$v\""
          case v         => v

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

    /** A model representing the VISIBLE attribute to be set on the column.
      *
      * @param kind
      *   VISIBLE or INVISIBLE
      */
    case class Visible(kind: String) extends Attribute

    /** A model representing the KEY attribute to be set on the column.
      *
      * @param kind
      *   KEY Type
      */
    case class Key(kind: String) extends Attribute

    /** A model representing the Collate attribute to be set on the column.
      *
      * @param set
      *   String to be set for Collate.
      */
    case class Collate(set: String) extends Attribute

    /** A model representing the COLUMN_FORMAT attribute to be set on the column.
      *
      * @param format
      *   FIXED, DYNAMIC or DEFAULT
      */
    case class ColumnFormat(format: String) extends Attribute

    /** A model representing the STORAGE attribute to be set on the column.
      *
      * @param kind
      *   DISK or MEMORY
      */
    case class Storage(kind: String) extends Attribute
