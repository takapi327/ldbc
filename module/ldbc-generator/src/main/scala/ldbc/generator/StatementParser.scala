/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator

import scala.util.parsing.combinator.*

import ldbc.generator.model.CreateStatement

trait StatementParser extends ColumnParser:
  self: RegexParsers & JavaTokenParsers =>

  private def create:      Parser[String] = "CREATE" ^^ (_.toUpperCase)
  private def temporary:   Parser[String] = "TEMPORARY" ^^ (_.toUpperCase)
  private def table:       Parser[String] = "TABLE" ^^ (_.toUpperCase)
  private def ifNotExists: Parser[String] = "IF NOT EXISTS" ^^ (_.toUpperCase)

  protected def createStatement: Parser[CreateStatement] =
    create ~> opt(comment) ~> opt(temporary) ~> opt(comment) ~> table ~>
      opt(comment) ~> opt(ifNotExists) ~> opt(comment) ~> ident ~ opt(comment) ~
      "(" ~ repsep(columnDefinition, ",") <~ opt(comment) <~ ")" ~ ";" ^^ {
        case tableName ~ _ ~ _ ~ columnDefs => CreateStatement(tableName, columnDefs)
      }
