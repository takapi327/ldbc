/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.codegen.parser

import scala.util.parsing.input.*

import ldbc.codegen.model.{ Database, Table }

case class Parser(fileName: String) extends DatabaseStatementParser, SetParser:

  private def end: util.matching.Regex = """\s*""".r

  private def sentence: Parser[Product | List[Product]] =
    Seq[Parser[Product | List[Product]]](comment, databaseStatements, tableStatements, setStatements).reduceLeft(_ | _)

  private type Statements = Table.CreateStatement | Database.CreateStatement

  private def parser: Parser[List[(String, List[Statements])]] =
    var currentDatabase: String = ""
    phrase(rep(sentence) <~ end) ^^ { statements =>
      statements.flatMap {
        case statement: Table.CreateStatement => Some(currentDatabase -> List(statement))
        case statement: Database.CreateStatement =>
          currentDatabase = statement.name
          Some(currentDatabase -> List(statement))
        case _ => None
      }
    }

  def parse(sql: String): ParseResult[List[(String, List[Statements])]] =
    parser(new CharSequenceReader(sql))
