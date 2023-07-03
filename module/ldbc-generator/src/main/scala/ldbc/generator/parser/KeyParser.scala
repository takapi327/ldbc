/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.parser

import ldbc.generator.model.{ Comment, Key }
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

  private def withParser: Parser[Key.WithParser] =
    customError(
      caseSensitivity("with") ~> caseSensitivity("parser") ~> ident ^^ Key.WithParser.apply,
      """
        |======================================================
        |There is an error in the format of the with parser type.
        |Please correct the format according to the following.
        |
        |example: WITH PARSER `parser_name`
        |======================================================
        |""".stripMargin
    )

  private def indexType: Parser[Key.IndexType] =
    customError(
      caseSensitivity("using") ~> (caseSensitivity("btree") | caseSensitivity("hash")) ^^ {
        case str if "(?i)btree".r.matches(str) => Key.IndexType("BTREE")
        case str if "(?i)hash".r.matches(str)  => Key.IndexType("HASH")
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

  private def visible: Parser[Key.Visible] =
    customError(
      (caseSensitivity("visible") | caseSensitivity("invisible")) ^^ {
        case str if "(?i)visible".r.matches(str)   => Key.Visible("VISIBLE")
        case str if "(?i)invisible".r.matches(str) => Key.Visible("INVISIBLE")
      },
      """
        |======================================================
        |There is an error in the format of the visible.
        |Please correct the format according to the following.
        |
        |example: {VISIBLE | INVISIBLE}
        |======================================================
        |""".stripMargin
    )

  private def indexOptions: Parser[Key.IndexOptions] =
    keyBlockSize | indexType | withParser | columnComment | visible |
      engineAttribute | secondaryEngineAttribute

  private def indexOption: Parser[IndexOption] =
    rep1(indexOptions) ^^ { indexOptions =>
      indexOptions.foldLeft(IndexOption.empty)((prev, current) =>
        current match
          case value: Key.KeyBlockSize             => prev.setSize(value)
          case value: Key.IndexType                => prev.setIndexType(value)
          case value: Key.WithParser               => prev.setWithParser(value)
          case value: Comment                      => prev.setComment(value)
          case value: Key.EngineAttribute          => prev.setEngineAttribute(value)
          case value: Key.SecondaryEngineAttribute => prev.setSecondaryEngineAttribute(value)
          case value: Key.Visible                  => prev
      )
    }

  private def indexKey: Parser[Index] =
    (caseSensitivity("index") | caseSensitivity("key")) ~> opt(sqlIdent.filter {
      case str if "(?i)using".r.matches(str) => false
      case _                                 => true
    }) ~ opt(indexType) ~ columnsParser ~ opt(indexOption) ^^ {
      case indexName ~ indexType ~ keyParts ~ indexOption =>
        Index(indexName, indexType, keyParts, indexOption)
    }

  private def fulltext: Parser[Index] =
    (caseSensitivity("fulltext") | caseSensitivity("spatial")) ~>
      opt(caseSensitivity("index") | caseSensitivity("key")) ~>
      opt(sqlIdent) ~ columnsParser ~ opt(indexOption) ^^ {
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
    opt(constraint) ~ primaryKey ~ opt(indexType) ~ columnsParser ~ opt(indexOption) ^^ {
      case constraint ~ _ ~ indexType ~ keyParts ~ option =>
        Primary(constraint, indexType, keyParts, option)
    }

  private def constraintUniqueKey: Parser[Unique] =
    opt(constraint) ~ caseSensitivity("unique") ~ opt(caseSensitivity("index") | caseSensitivity("key"))
      ~ opt(sqlIdent) ~ opt(indexType) ~ columnsParser ~ opt(indexOption) ^^ {
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
