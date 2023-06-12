/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator

import scala.util.parsing.combinator.*
import scala.util.parsing.input.*

import ldbc.generator.model.CreateStatement

object Parser extends RegexParsers, JavaTokenParsers, StatementParser:

  private def end: util.matching.Regex = """\s*""".r

  private def sentence: Parser[Product] = Seq[Parser[Product]](comment, createStatement).reduceLeft(_ | _)

  private def parser: Parser[List[CreateStatement]] =
    phrase(rep(sentence) <~ end) ^^ { statements =>
      statements.foldLeft(List[CreateStatement]()) {
        case (list, statement: CreateStatement) => list :+ statement
        case (list, _)                          => list
      }
    }

  def parse(sql: String): ParseResult[List[CreateStatement]] =
    parser(new CharSequenceReader(sql))
