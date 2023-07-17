/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.parser

import scala.util.parsing.input.*

import ldbc.generator.model.{ Database, Table }

case class Parser(fileName: String) extends DatabaseStatementParser:

  override def failureMessage(format: String, example: String): Input => String =
    input => s"""
       |======================================================
       |There is an error in the format of the $format.
       |Please correct the format according to the following.
       |
       |${ input.pos.longString } ($fileName:${ input.pos.line }:${ input.pos.column })
       |example: $example
       |======================================================
       |""".stripMargin

  private def end: util.matching.Regex = """\s*""".r

  private def sentence: Parser[Product] =
    Seq[Parser[Product]](comment, databaseStatements, tableStatements).reduceLeft(_ | _)

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
