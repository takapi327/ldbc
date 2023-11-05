/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core.builder

import ldbc.core.*
import ldbc.core.validator.TableValidator

/** Class for generating query strings such as Create statements from Table values.
  *
  * @param table
  *   Trait for generating SQL table information.
  */
private[ldbc] case class TableQueryBuilder(table: Table[?]) extends TableValidator:

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

object TableQueryBuilder
