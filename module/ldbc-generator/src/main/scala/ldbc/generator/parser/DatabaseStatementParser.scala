/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.parser

import ldbc.generator.model.*

/** Parser for parsing Database definitions.
  */
trait DatabaseStatementParser extends TableParser:

  /** Parser for parsing Database create statement.
    *
    * Please refer to the official documentation for MySQL Database create statement. SEE:
    * https://dev.mysql.com/doc/refman/8.0/en/create-database.html
    */
  private def createStatement: Parser[Database.CreateStatement] =
    opt(comment) ~> create ~> opt(comment) ~> (caseSensitivity("database") | caseSensitivity("schema")) ~>
      opt(comment) ~> opt(ifNotExists) ~> opt(comment) ~> sqlIdent ~ opt(comment) ~ opt(caseSensitivity("default")) ~
      opt(comment) ~ opt(character) ~ opt(collate) ~ opt(comment) ~ opt(encryption) <~
      opt(comment) <~ ";" ^^ {
        case name ~ _ ~ _ ~ _ ~ character ~ collate ~ _ ~ encryption =>
          Database.CreateStatement(name, character, collate, encryption.map(_.value))
      }

  /** Parser for parsing Database drop statement.
    *
    * Please refer to the official documentation for MySQL Database drop statement. SEE:
    * https://dev.mysql.com/doc/refman/8.0/en/drop-database.html
    */
  private[ldbc] def dropStatement: Parser[Database.DropStatement] =
    customError(
      opt(comment) ~> drop ~> opt(comment) ~> (caseSensitivity("database") | caseSensitivity("schema")) ~>
        opt(comment) ~> opt(ifNotExists) ~> opt(comment) ~> sqlIdent <~ ";" ^^ { name =>
          Database.DropStatement(name)
        },
      input => s"""
        |======================================================
        |There is an error in the if drop database statement format.
        |Please correct the format according to the following.
        |
        |${ input.pos.longString } ($fileName:${ input.pos.line }:${ input.pos.column })
        |example: DROP {DATABASE | SCHEMA} [IF EXISTS] `database_name`
        |======================================================
        |""".stripMargin
    )

  /** Parser for parsing Database use statement.
    */
  private def useDatabase: Parser[Database.DropStatement] =
    customError(
      opt(comment) ~> caseSensitivity("use") ~> opt(comment) ~> sqlIdent <~ opt(comment) <~ ";" ^^ { name =>
        Database.DropStatement(name)
      },
      input => s"""
        |======================================================
        |There is an error in the if use database statement format.
        |Please correct the format according to the following.
        |
        |${ input.pos.longString } ($fileName:${ input.pos.line }:${ input.pos.column })
        |example: USE `database_name`
        |======================================================
        |""".stripMargin
    )

  protected def databaseStatements: Parser[Database.CreateStatement | Database.DropStatement | Database.UseStatement] =
    createStatement | dropStatement | useDatabase
