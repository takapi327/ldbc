/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.model

import ldbc.codegen.formatter.Naming

trait Key:
  def toCode(tableName: String, classNameFormatter: Naming, propertyFormatter: Naming): String

object Key:

  type IndexOptions = KeyBlockSize | IndexType | WithParser | CommentSet | Visible | EngineAttribute |
    SecondaryEngineAttribute

  trait On:
    def option: String

  case class OnDelete(option: String) extends On
  case class OnUpdate(option: String) extends On

  case class Constraint(name: Option[String])

  case class KeyBlockSize(value: 1 | 2 | 4 | 8 | 16)
  case class WithParser(value: String)
  case class Visible(value: "VISIBLE" | "INVISIBLE")
  case class EngineAttribute(value: String)
  case class SecondaryEngineAttribute(value: String)

  case class IndexType(value: "BTREE" | "HASH"):
    def toCode: String = s"Index.Type.$value"

  case class IndexOption(
    size:       Option[KeyBlockSize],
    indexType:  Option[IndexType],
    parserName: Option[WithParser],
    comment:    Option[CommentSet],
    engine:     Option[EngineAttribute],
    secondary:  Option[SecondaryEngineAttribute]
  ):

    def setSize(value: KeyBlockSize): IndexOption = this.copy(size = Some(value))

    def setIndexType(value: IndexType): IndexOption = this.copy(indexType = Some(value))

    def setWithParser(value: WithParser): IndexOption = this.copy(parserName = Some(value))

    def setComment(value: CommentSet): IndexOption = this.copy(comment = Some(value))

    def setEngineAttribute(value: EngineAttribute): IndexOption = this.copy(engine = Some(value))

    def setSecondaryEngineAttribute(value: SecondaryEngineAttribute): IndexOption = this.copy(secondary = Some(value))

    def toCode: String =
      s"Index.IndexOption(${ size.fold("None")(v => s"Some(${ v.value })") }, ${ indexType
          .fold("None")(v => s"Some(${ v.toCode })") }, ${ parserName
          .fold("None")(v => s"Some(${ v.value })") }, ${ comment.fold("None")(v => s"Some($v)") }, ${ engine
          .fold("None")(v => s"Some(${ v.value })") }, ${ secondary.fold("None")(v => s"Some(${ v.value })") })"

  object IndexOption:
    def empty: IndexOption = IndexOption(None, None, None, None, None, None)

  case class Index(
    indexName:   Option[String],
    indexType:   Option[IndexType],
    keyParts:    List[String],
    indexOption: Option[IndexOption]
  ) extends Key:
    def toCode(tableName: String, classNameFormatter: Naming, propertyFormatter: Naming): String =
      val columns = keyParts.map(v => s"${ propertyFormatter.format(v) }")
      s"INDEX_KEY(${ indexName.fold("None")(str => s"Some(\"$str\")") }, ${ indexType
          .fold("None")(v => s"Some(${ v.toCode })") }, ${ indexOption
          .fold("None")(option => s"Some(${ option.toCode })") }, ${ columns.mkString(" *: ") })"

  case class Primary(
    constraint:  Option[Constraint],
    indexType:   Option[IndexType],
    keyParts:    List[String],
    indexOption: Option[IndexOption]
  ) extends Key:
    def toCode(tableName: String, classNameFormatter: Naming, propertyFormatter: Naming): String =
      val columns = keyParts.map(v => s"${ propertyFormatter.format(v) }")
      val key = indexType.fold(s"PRIMARY_KEY(${ columns.mkString(" *: ") })")(v =>
        indexOption match
          case None => s"PRIMARY_KEY(${ v.toCode }, ${ columns.mkString(" *: ") })"
          case Some(o) =>
            s"PRIMARY_KEY(${ v.toCode }, ${ o.toCode }, ${ columns.mkString(" *: ") })"
      )
      constraint.fold(key)(_.name match
        case Some(name) => s"CONSTRAINT(\"$name\", $key)"
        case None       => s"CONSTRAINT($key)"
      )

  case class Unique(
    constraint:  Option[Constraint],
    indexName:   Option[String],
    indexType:   Option[IndexType],
    keyParts:    List[String],
    indexOption: Option[IndexOption]
  ) extends Key:
    def toCode(tableName: String, classNameFormatter: Naming, propertyFormatter: Naming): String =
      val columns = keyParts.map(v => s"${ propertyFormatter.format(v) }")
      val key =
        s"UNIQUE_KEY(${ indexName.fold("None")(v => s"Some(\"$v\")") }, ${ indexType
            .fold("None")(v => s"Some(${ v.toCode })") }, ${ indexOption
            .fold("None")(v => s"Some(${ v.toCode })") }, ${ columns.mkString(" *: ") })"
      constraint.fold(key)(_.name match
        case Some(name) => s"CONSTRAINT(\"$name\", $key)"
        case None       => s"CONSTRAINT($key)"
      )

  case class Foreign(
    constraint: Option[Constraint],
    indexName:  Option[String],
    keyParts:   List[String],
    reference:  Reference
  ) extends Key:
    def toCode(tableName: String, classNameFormatter: Naming, propertyFormatter: Naming): String =
      val columns = keyParts.map(v => s"$tableName.${ propertyFormatter.format(v) }")
      val key =
        s"FOREIGN_KEY(${ indexName.fold("None")(v => s"Some(\"$v\")") }, ${ columns
            .mkString(" *: ") }, ${ reference.toCode(classNameFormatter, propertyFormatter) })"
      constraint.fold(key)(_.name match
        case Some(name) => s"CONSTRAINT(\"$name\", $key)"
        case None       => s"CONSTRAINT($key)"
      )

  case class Reference(tableName: String, keyParts: List[String], on: Option[List[On]]):
    def toCode(classNameFormatter: Naming, propertyFormatter: Naming): String =
      val className = classNameFormatter.format(tableName)
      val columns   = keyParts.map(v => s"$className.table.${ propertyFormatter.format(v) }")
      on match
        case Some(list) =>
          (list.find(_.isInstanceOf[OnDelete]), list.find(_.isInstanceOf[OnUpdate])) match
            case (None, None) => s"REFERENCE($className.table, ${ columns.mkString(" *: ") })"
            case (Some(delete), None) =>
              s"REFERENCE($className.table, ${ columns.mkString(" *: ") }).onDelete(${ delete.option })"
            case (None, Some(update)) =>
              s"REFERENCE($className.table, ${ columns.mkString(" *: ") }).onUpdate(${ update.option })"
            case (Some(delete), Some(update)) =>
              s"REFERENCE($className.table, ${ columns.mkString(" *: ") }).onDelete(${ delete.option }).onUpdate(${ update.option })"
        case None => s"REFERENCE($className.table, ${ columns.mkString(" *: ") })"
