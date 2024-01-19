/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
  */

package ldbc.core.builder

import ldbc.core.*
import ldbc.core.validator.TableValidator

/** Class for generating query strings such as Create statements from Table values.
  */
trait TableQueryBuilder extends TableValidator:

  private val columnDefinitions: Seq[String] =
    table.all.map(_.queryString)

  private val createDefinitions: Seq[String] =
    columnDefinitions ++ table.keyDefinitions.map(_.queryString)

  private val tableOptions: Seq[String] = table.options.map {
    case character: Character => s"DEFAULT ${ character.queryString }"
    case collate: Collate[?]  => s"DEFAULT ${ collate.queryString }"
    case option: TableOption  => option.queryString
  }

  /** Variable that generates the Create statement that creates the Table.
    */
  lazy val createStatement: String =
    s"""
       |CREATE TABLE `${ table._name }` (
       |  ${ createDefinitions.mkString(",\n  ") }
       |)${ if tableOptions.isEmpty then ";" else s" ${ tableOptions.mkString(" ") };" }
       |""".stripMargin

  /** Variable that generates the Drop statement that creates the Table.
    */
  lazy val dropStatement: String =
    s"DROP TABLE `${ table._name }`"

  /** Variable that generates the Truncate statement that creates the Table.
    */
  lazy val truncateStatement: String =
    s"TRUNCATE TABLE `${ table._name }`"

object TableQueryBuilder:

  /** Factory method for creating a TableQueryBuilder from a Table.
    *
    * @param _table
    *   Trait for generating SQL table information.
    */
  def apply(_table: Table[?]): TableQueryBuilder =
    new TableQueryBuilder:
      override def table: Table[?] = _table
