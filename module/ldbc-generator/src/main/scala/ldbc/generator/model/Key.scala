/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.model

import ldbc.generator.formatter.Naming

trait Key:
  def toCode(tableName: String, classNameFormatter: Naming, propertyFormatter: Naming): String

object Key:

  opaque type OnDelete = String
  opaque type OnUpdate = String

  def onDelete(str: String): OnDelete = str
  def onUpdate(str: String): OnDelete = str

  case class Constraint(name: Option[String])

  case class Index(
    indexName:   Option[String],
    indexType:   Option[String],
    keyParts:    List[String],
    indexOption: Option[String]
  ) extends Key:
    def toCode(tableName: String, classNameFormatter: Naming, propertyFormatter: Naming): String =
      val columns = keyParts.map(v => s"$tableName.${ propertyFormatter.format(v) }")
      s"INDEX_KEY(${ indexName.fold("None")(str => s"Some(\"$str\")") }, ${ indexType.fold("None")(str => s"Some($str)") }, cats.data.NonEmptyList.of(${ columns
          .mkString(",") }), ${ indexOption.fold("None")(str => s"Some($str)") })"

  case class Primary(
    constraint: Option[Constraint],
    indexType:  Option[String],
    keyParts:   List[String],
    option:     Option[String]
  ) extends Key:
    def toCode(tableName: String, classNameFormatter: Naming, propertyFormatter: Naming): String =
      val columns = keyParts.map(v => s"$tableName.${ propertyFormatter.format(v) }")
      val key = indexType.fold(s"PRIMARY_KEY(cats.data.NonEmptyList.of(${ columns.mkString(",") }))")(v =>
        option match
          case None    => s"PRIMARY_KEY($v, cats.data.NonEmptyList.of(${ columns.mkString(",") }))"
          case Some(o) => s"PRIMARY_KEY($v, cats.data.NonEmptyList.of(${ columns.mkString(",") }), $o)"
      )
      constraint.fold(key)(v => s"CONSTRAINT(${ v.name.getOrElse(keyParts.mkString("_")) }, $key)")

  case class Unique(
    constraint: Option[Constraint],
    indexName:  Option[String],
    indexType:  Option[String],
    keyParts:   List[String],
    option:     Option[String]
  ) extends Key:
    def toCode(tableName: String, classNameFormatter: Naming, propertyFormatter: Naming): String =
      val columns = keyParts.map(v => s"$tableName.${ propertyFormatter.format(v) }")
      val key =
        s"UNIQUE_KEY(${ indexName.fold("None")(v => s"Some(\"$v\")") },${ indexType.fold("None")(v => s"Some(\"$v\")") },cats.data.NonEmptyList.of(${ columns
            .mkString(",") }),${ option.fold("None")(v => s"Some(\"$v\")") })"
      constraint.fold(key)(v => s"CONSTRAINT(${ v.name.getOrElse(keyParts.mkString("_")) }, $key)")

  case class Foreign(
    constraint: Option[Constraint],
    indexName:  Option[String],
    keyParts:   List[String],
    reference:  Reference
  ) extends Key:
    def toCode(tableName: String, classNameFormatter: Naming, propertyFormatter: Naming): String =
      val columns = keyParts.map(v => s"$tableName.${ propertyFormatter.format(v) }")
      val key =
        s"""FOREIGN_KEY(
           |  ${ indexName.fold("None")(v => s"Some(\"$v\")") },
           |  cats.data.NonEmptyList.of(${ columns.mkString(",") }),
           |  ${ reference.toCode(classNameFormatter, propertyFormatter) }
           |)
           |""".stripMargin
      constraint.fold(key)(v => s"CONSTRAINT(\"${ v.name.getOrElse(keyParts.mkString("_")) }\", $key)")

  case class Reference(tableName: String, keyParts: List[String], on: Option[List[OnDelete | OnUpdate]]):
    def toCode(classNameFormatter: Naming, propertyFormatter: Naming): String =
      val className = classNameFormatter.format(tableName)
      val columns   = keyParts.map(v => s"$className.table.${ propertyFormatter.format(v) }")
      on match
        case Some(list) =>
          (list.find(_.isInstanceOf[OnDelete]), list.find(_.isInstanceOf[OnUpdate])) match
            case (None, None) => s"REFERENCE($className.table)(${ columns.mkString(",") })"
            case (Some(delete), None) =>
              s"REFERENCE($className.table, cats.data.NonEmptyList.of(${ columns.mkString(",") }), Some($delete), None)"
            case (None, Some(update)) =>
              s"REFERENCE($className.table, cats.data.NonEmptyList.of(${ columns.mkString(",") }), None, Some($update))"
            case (Some(delete), Some(update)) =>
              s"REFERENCE($className.table, cats.data.NonEmptyList.of(${ columns.mkString(",") }), Some($delete), Some($update))"
        case None => s"REFERENCE($className.table)(${ columns.mkString(",") })"
