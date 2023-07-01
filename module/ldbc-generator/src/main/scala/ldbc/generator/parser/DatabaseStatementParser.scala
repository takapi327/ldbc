/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.parser

import ldbc.generator.model.*

trait DatabaseStatementParser extends TableParser:

  private def createStatement: Parser[DatabaseStatement] =
    opt(comment) ~> create ~> opt(comment) ~> (caseSensitivity("database") | caseSensitivity("schema")) ~>
      opt(comment) ~> opt(ifNotExists) ~> opt(comment) ~> sqlIdent ~ opt(comment) ~ opt(caseSensitivity("default")) ~
      opt(comment) ~ opt(character ~ opt(collate)) ~ opt(comment) ~ opt(encryption) <~
      opt(comment) <~ ";" ^^ {
        case name ~ _ ~ _ ~ _ ~ Some(character ~ collate) ~ _ ~ encryption =>
          DatabaseStatement(name, Some(character), collate, encryption.map(_.value))
        case name ~ _ ~ _ ~ _ ~ None ~ _ ~ encryption =>
          DatabaseStatement(name, None, None, encryption.map(_.value))
      }

  private def useDatabase: Parser[DatabaseStatement] =
    opt(comment) ~> caseSensitivity("use") ~> opt(comment) ~> sqlIdent <~ opt(comment) <~ ";" ^^ { name =>
      DatabaseStatement(name, None, None, None)
    }

  protected def databaseStatement: Parser[DatabaseStatement] =
    createStatement | useDatabase