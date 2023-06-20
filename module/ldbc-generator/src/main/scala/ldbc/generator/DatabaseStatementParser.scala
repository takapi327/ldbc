/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator

import ldbc.generator.model.*

trait DatabaseStatementParser extends StatementParser:

  protected def databaseStatement: Parser[DatabaseStatement] =
    opt(comment) ~> create ~> opt(comment) ~> (caseSensitivity("database") | caseSensitivity("schema")) ~>
      opt(comment) ~> opt(ifNotExists) ~> opt(comment) ~> sqlIdent ~ opt(comment) ~ opt(caseSensitivity("default")) ~
      opt(comment) ~ opt(character) ~ opt(comment) ~ opt(collate) ~ opt(comment) ~
      opt(caseSensitivity("encryption") ~> opt(comment) ~> opt("=") ~> opt(comment) ~> ("Y" | "N") <~ opt(comment)) <~
      opt(comment) <~ ";" ^^ {
        case name ~ _ ~ _ ~ _ ~ character ~ _ ~ collate ~ _ ~ encryption =>
          DatabaseStatement(name, character, collate, encryption)
      }

  protected def useDatabase: Parser[DatabaseStatement] =
    opt(comment) ~> caseSensitivity("use") ~> opt(comment) ~> sqlIdent <~ opt(comment) <~ ";" ^^ { name =>
      DatabaseStatement(name, None, None, None)
    }
