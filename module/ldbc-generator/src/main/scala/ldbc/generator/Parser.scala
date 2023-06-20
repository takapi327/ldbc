/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator

import scala.util.parsing.input.*

import ldbc.generator.model.CreateStatement

object Parser extends DatabaseStatementParser:

  private def end: util.matching.Regex = """\s*""".r

  private def sentence: Parser[Product] = Seq[Parser[Product]](comment, databaseStatement, useDatabase, createStatement).reduceLeft(_ | _)

  private def parser: Parser[Map[String, List[CreateStatement]]] =
    var currentDatabase: String = ""
    phrase(rep(sentence) <~ end) ^^ { statements =>
      statements.foldLeft(Map[String, List[CreateStatement]](currentDatabase -> List.empty)) {
        case (map, statement: CreateStatement) => map.updated(currentDatabase, map.getOrElse(currentDatabase, List.empty) :+ statement)
        case (map, statement: ldbc.generator.model.DatabaseStatement) =>
          currentDatabase = statement.name
          map
        case (map, _) => map
      }
    }

  def parse(sql: String): ParseResult[Map[String, List[CreateStatement]]] =
    parser(new CharSequenceReader(sql))
