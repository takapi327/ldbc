/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder

import ldbc.core.*
import ldbc.core.attribute.*

private[ldbc] case class TableQueryBuilder(table: Table[?]):

  private val autoInc = table.*.filter {
    case c: Column[?] => c.attributes.contains(AutoInc())
    case unknown      => throw new IllegalStateException(s"$unknown is not a Column.")
  }
  private val primaryKey = table.*.filter {
    case c: Column[?] => c.attributes.exists(_.isInstanceOf[PrimaryKey])
    case unknown      => throw new IllegalStateException(s"$unknown is not a Column.")
  }
  private val keyPart = table.keyDefinitions.flatMap {
    case key: PrimaryKey with Index => key.keyPart.toList
    case key: UniqueKey with Index  => key.keyPart.toList
    case _                          => List.empty
  }

  private val constraints = table.keyDefinitions.flatMap {
    case key: Constraint => Some(key)
    case _               => None
  }

  require(
    table.*.distinctBy {
      case c: Column[?] => c.label
      case unknown      => throw new IllegalStateException(s"$unknown is not a Column.")
    }.length == table.*.length,
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
    autoInc.count {
      case column: Column[?] =>
        column.attributes.exists(_.isInstanceOf[PrimaryKey]) || column.attributes.exists(_.isInstanceOf[UniqueKey])
      case unknown => throw new IllegalStateException(s"$unknown is not a Column.")
    } == 1 || keyPart.count(key =>
      autoInc
        .map {
          case c: Column[?] => c.label
          case unknown      => throw new IllegalStateException(s"$unknown is not a Column.")
        }
        .contains(key.label)
    ) == 1,
    "The columns with AUTO_INCREMENT must have a Primary Key or Unique Key."
  )

  if constraints.nonEmpty then
    require(
      constraints.exists(_.key match
        case key: ForeignKey => key.colName.map(_.dataType) == key.reference.keyPart.map(_.dataType)
        case _ => false
      ),
      "The type of the column set in FOREIGN KEY does not match."
    )

    require(
       constraints.exists(_.key match
        case key: ForeignKey =>
          key.reference.keyPart.toList.flatMap(_.attributes).exists(_.isInstanceOf[PrimaryKey]) ||
            key.reference.table.keyDefinitions.exists(_ match
              case v: PrimaryKey with Index => v.keyPart.exists(c => key.reference.keyPart.exists(_ == c))
              case _ => false
            )
        case _ => false
      ),
      "The column referenced by FOREIGN KEY must be a PRIMARY KEY."
    )

  private val columnDefinitions: Seq[String] =
    table.*.map {
      case c: Column[?] => c.queryString
      case unknown      => throw new IllegalStateException(s"$unknown is not a Column.")
    }

  private val options: Seq[String] =
    columnDefinitions ++ table.keyDefinitions.map(_.queryString)

  def querySting: String =
    s"""
       |CREATE TABLE `${ table.name }` (
       |  ${ options.mkString(",\n  ") }
       |);
       |""".stripMargin

object TableQueryBuilder
