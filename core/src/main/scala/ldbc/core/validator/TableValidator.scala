/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.core.validator

import ldbc.core.*
import ldbc.core.attribute.*

/**
 * Trait for validation of table definitions.
 */
private[ldbc] trait TableValidator:

  /** Trait for generating SQL table information. */
  def table: Table[?]

  protected val autoInc = table.all.filter(_.attributes.contains(AutoInc()))

  protected val primaryKey = table.all.filter(_.attributes.exists {
    case _: PrimaryKey => true
    case _             => false
  })

  protected val keyPart = table.keyDefinitions.flatMap {
    case key: PrimaryKey with Index => key.keyPart
    case key: UniqueKey with Index  => key.keyPart
    case _                          => List.empty
  }

  protected val constraints = table.keyDefinitions.flatMap {
    case key: Constraint => Some(key)
    case _               => None
  }

  require(
    table.all.distinctBy(_.label).length == table.all.length,
    "Columns with the same name cannot be defined in a single table."
  )

  require(
    autoInc.length <= 1,
    "AUTO_INCREMENT can only be set on one of the table columns."
  )

  require(
    primaryKey.length <= 1
      && table.keyDefinitions.count(_.label == "PRIMARY KEY") <= 1
      && primaryKey.length + table.keyDefinitions.count(_.label == "PRIMARY KEY") <= 1,
    "PRIMARY KEY can only be set on one of the table columns."
  )

  require(
    !(autoInc.nonEmpty &&
      autoInc.count(column =>
        column.attributes.exists {
          case _: PrimaryKey => true
          case _: UniqueKey  => true
          case _             => false
        }
      ) == 0 && keyPart.count(key =>
        autoInc
          .map(_.label)
          .contains(key.label)
      ) == 0),
    "The columns with AUTO_INCREMENT must have a Primary Key or Unique Key."
  )

  if constraints.nonEmpty then
    require(
      constraints.exists(_.key match
        case key: ForeignKey[?] =>
          key.columns.toList.map(_.asInstanceOf[Column[?]].dataType) == key.reference.keyPart.toList
            .map(_.asInstanceOf[Column[?]].dataType)
        case _ => false
      ),
      s"""
         |The type of the column set in FOREIGN KEY does not match.
         |
         |`${ table._name }` Table
         |
         |============================================================
         |${ initForeignKeyErrorMsg(constraints) }
         |============================================================
         |""".stripMargin
    )

    require(
      constraints.exists(_.key match
        case key: ForeignKey[?] =>
          key.reference.keyPart.toList
            .flatMap(_.asInstanceOf[Column[?]].attributes)
            .exists(_.isInstanceOf[PrimaryKey]) ||
          key.reference.table.keyDefinitions.exists {
            case v: PrimaryKey with Index => v.keyPart.exists(c => key.reference.keyPart.toList.exists(_ == c))
            case _                        => false
          }
        case _ => false
      ),
      "The column referenced by FOREIGN KEY must be a PRIMARY KEY."
    )

  private def initForeignKeyErrorMsg(constraints: Seq[Constraint]): String =
    constraints
      .flatMap(_.key match
        case key: ForeignKey[?] =>
          for
            (column, index)             <- key.columns.toList.asInstanceOf[List[Column[?]]].zipWithIndex
            (refColumn, refColumnIndex) <- key.reference.keyPart.toList.asInstanceOf[List[Column[?]]].zipWithIndex
          yield
            if index == refColumnIndex then s"""
             |(${ column.dataType == refColumn.dataType }) `${ column.label }` ${ column.dataType.typeName } =:= `${ refColumn.label }` ${ refColumn.dataType.typeName }
             |""".stripMargin
            else ""
        case _ => ""
      )
      .mkString("\n")
