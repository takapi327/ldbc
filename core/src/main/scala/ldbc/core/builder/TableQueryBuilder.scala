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
    table.*.map {
      case c: Column[?] => c.queryString
      case unknown      => throw new IllegalStateException(s"$unknown is not a Column.")
    }

  private val options: Seq[String] =
    columnDefinitions ++ table.keyDefinitions.map(_.queryString)

  /** Variable that generates the Create statement that creates the Table.
    */
  lazy val createStatement: String =
    s"""
       |CREATE TABLE `${ table.name }` (
       |  ${ options.mkString(",\n  ") }
       |);
       |""".stripMargin

  /** Variable that generates the Drop statement that creates the Table.
    */
  lazy val dropStatement: String =
    s"DROP TABLE `${ table.name }`"

  /** Variable that generates the Truncate statement that creates the Table.
    */
  lazy val truncateStatement: String =
    s"TRUNCATE TABLE `${ table.name }`"

object TableQueryBuilder
