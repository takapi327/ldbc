/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.parser

import ldbc.generator.model.Key
import ldbc.generator.model.Key.*

/** Parser for parsing create table key definitions.
  */
trait KeyParser extends ColumnParser:

  private def columnsParser: Parser[List[String]] =
    customError(
      "(" ~> repsep(sqlIdent, ",") <~ ")",
      """
        |======================================================
        |There is an error in the column list format.
        |Please correct the format according to the following.
        |
        |example: (`column_name`) or (`column_name`, `column_name`, ...)
        |======================================================
        |""".stripMargin
    )

  private def withParser: Parser[String] =
    customError(
      caseSensitivity("with") ~> caseSensitivity("parser") ~> ident,
      """
        |======================================================
        |There is an error in the format of the with parser type.
        |Please correct the format according to the following.
        |
        |example: WITH PARSER `parser_name`
        |======================================================
        |""".stripMargin
    )

  private def indexType: Parser[String] =
    customError(
      caseSensitivity("using") ~> (caseSensitivity("btree") | caseSensitivity("hash")) ^^ { input =>
        s"Index.Type.${ input.toUpperCase }"
      },
      """
        |======================================================
        |There is an error in the format of the Index type.
        |Please correct the format according to the following.
        |
        |example: USING {BTREE | HASH}
        |======================================================
        |""".stripMargin
    )

  private def indexOption: Parser[Option[String]] =
    opt(keyBlockSize) ~ opt(indexType) ~ opt(withParser) ~
      opt(columnComment) ~ opt(caseSensitivity("visible") | caseSensitivity("invisible")) ~
      opt(engineAttribute) ~ opt(secondaryEngineAttribute) ^^ {
        case size ~ indexType ~ parserName ~ comment ~ _ ~ engine ~ secondary =>
          (size, indexType, parserName, comment, engine, secondary) match
            case (None, None, None, None, None, None) => None
            case _ =>
              Some(s"""
                 |Index.IndexOption(
                 |  ${ size.fold("None")(v => s"Some($v)") },
                 |  ${ indexType.fold("None")(v => s"Some($v)") },
                 |  ${ parserName.fold("None")(v => s"Some($v)") },
                 |  ${ comment.fold("None")(v => s"Some($v)") },
                 |  ${ engine.fold("None")(v => s"Some($v)") },
                 |  ${ secondary.fold("None")(v => s"Some($v)") }
                 |)
                 |""".stripMargin)
      }

  private def indexKey: Parser[Index] =
    (caseSensitivity("index") | caseSensitivity("key")) ~> opt(sqlIdent.filter {
      case str if "(?i)using".r.matches(str) => false
      case _                                 => true
    }) ~ opt(indexType) ~ columnsParser ~ indexOption ^^ {
      case indexName ~ indexType ~ keyParts ~ indexOption => Index(indexName, indexType, keyParts, indexOption)
    }

  private def fulltext: Parser[Index] =
    (caseSensitivity("fulltext") | caseSensitivity("spatial")) ~>
      opt(caseSensitivity("index") | caseSensitivity("key")) ~>
      opt(sqlIdent) ~ columnsParser ~ indexOption ^^ {
        case indexName ~ keyParts ~ indexOption => Index(indexName, None, keyParts, indexOption)
      }

  private def constraint: Parser[Constraint] =
    caseSensitivity("constraint") ~> opt(sqlIdent.filter {
      case str if "(?i)primary".r.matches(str) => false
      case str if "(?i)unique".r.matches(str)  => false
      case str if "(?i)foreign".r.matches(str) => false
      case str if "(?i)check".r.matches(str)   => false
      case _                                   => true
    }) ^^ Constraint.apply

  private def referenceOption: Parser[String] =
    customError(
      (
        caseSensitivity("restrict") | caseSensitivity("cascade") | (caseSensitivity("set") ~
          (caseSensitivity("null") | caseSensitivity("default"))) | (caseSensitivity("no") ~
          caseSensitivity("action"))
      ) ^^ {
        case set ~ option   => s"Reference.ReferenceOption.${ set.toUpperCase }_${ option.toUpperCase }"
        case option: String => s"Reference.ReferenceOption.${ option.toUpperCase }"
      },
      """
        |======================================================
        |There is an error in the format of the referenceOption type.
        |Please correct the format according to the following.
        |
        |example: RESTRICT | CASCADE | SET NULL | NO ACTION | SET DEFAULT
        |======================================================
        |""".stripMargin
    )

  private def matchParser: Parser[String ~ String] =
    customError(
      caseSensitivity("match") ~ (caseSensitivity("full") | caseSensitivity("partial") | caseSensitivity("simple")),
      """
        |======================================================
        |There is an error in the format of the match type.
        |Please correct the format according to the following.
        |
        |example: MATCH {FULL | PARTIAL | SIMPLE}
        |======================================================
        |""".stripMargin
    )

  private def onDeleteUpdate: Parser[Key.OnDelete | Key.OnUpdate] =
    customError(
      caseSensitivity("on") ~> (caseSensitivity("delete") | caseSensitivity("update")) ~ referenceOption ^^ {
        case str ~ option =>
          str match
            case str if "(?i)delete".r.matches(str) => Key.onDelete(option)
            case str if "(?i)update".r.matches(str) => Key.onUpdate(option)
      },
      """
        |======================================================
        |There is an error in the format of the on delete/update type.
        |Please correct the format according to the following.
        |
        |example: ON {DELETE | UPDATE} [RESTRICT | CASCADE | SET NULL | NO ACTION | SET DEFAULT]
        |======================================================
        |""".stripMargin
    )

  private def referenceDefinition: Parser[Reference] =
    caseSensitivity("references") ~> sqlIdent ~ columnsParser ~
      opt(matchParser) ~ opt(rep1(onDeleteUpdate)) ^^ {
        case tableName ~ keyParts ~ _ ~ on =>
          Reference(tableName, keyParts, on)
      }

  private def constraintPrimaryKey: Parser[Primary] =
    opt(constraint) ~ primaryKey ~ opt(indexType) ~ columnsParser ~ indexOption ^^ {
      case constraint ~ _ ~ indexType ~ keyParts ~ option =>
        Primary(constraint, indexType, keyParts, option)
    }

  private def constraintUniqueKey: Parser[Unique] =
    opt(constraint) ~ caseSensitivity("unique") ~ opt(caseSensitivity("index") | caseSensitivity("key"))
      ~ opt(sqlIdent) ~ opt(indexType) ~ columnsParser ~ indexOption ^^ {
        case constraint ~ _ ~ _ ~ indexName ~ indexType ~ keyParts ~ option =>
          Unique(constraint, indexName, indexType, keyParts, option)
      }

  private def constraintForeignKey: Parser[Foreign] =
    opt(constraint) ~ caseSensitivity("foreign") ~ caseSensitivity("key") ~ opt(sqlIdent) ~
      columnsParser ~ referenceDefinition ^^ {
        case constraint ~ _ ~ _ ~ indexName ~ columnNames ~ referenceDefinition =>
          Foreign(constraint, indexName, columnNames, referenceDefinition)
      }

  private def checkConstraintDefinition: Parser[String] =
    customError(
      opt(constraint) ~ caseSensitivity("check") ~ "(" ~ rep1(specialChars.filter(_ != ")")) ~ ")" ~
        opt(caseSensitivity("not")) ~ opt(caseSensitivity("enforced")) ^^ {
          case constraint ~ _ ~ _ ~ expr ~ _ ~ not ~ enforced =>
            s"$constraint ${ expr.mkString(" ") } $not $enforced"
        },
      """
        |======================================================
        |There is an error in the format of the check type.
        |Please correct the format according to the following.
        |
        |example: [CONSTRAINT [symbol]] CHECK (expr) [[NOT] ENFORCED]
        |======================================================
        |""".stripMargin
    )

  protected def keyDefinitions: Parser[Key | String] =
    (indexKey | fulltext | constraintPrimaryKey | constraintUniqueKey | constraintForeignKey | checkConstraintDefinition) ^^ {
      str => str
    }
