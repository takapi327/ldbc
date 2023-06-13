/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator

import ldbc.generator.model.Key
import ldbc.generator.model.Key.*

trait KeyParser extends ColumnParser:

  private def indexType: Parser[String] =
    caseSensitivity("using") ~> (caseSensitivity("btree") | caseSensitivity("hash")) ^^ { input =>
      s"Index.Type.${ input.toUpperCase }"
    }

  private def indexOption: Parser[Option[String]] =
    opt(caseSensitivity("key_block_size") ~> "=" ~> digit) ~ opt(indexType) ~
      opt(caseSensitivity("with") ~> caseSensitivity("parser") ~> ident) ~
      opt(columnComment) ~ opt(caseSensitivity("visible") | caseSensitivity("invisible")) ~
      opt(caseSensitivity("engine_attribute") ~> "=" ~> ident) ~
      opt(caseSensitivity("secondary_engine_attribute") ~> "=" ~> ident) ^^ {
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
    (caseSensitivity("index") | caseSensitivity("key")) ~> opt(sqlIdent) ~ opt(indexType) ~ "(" ~
      repsep(sqlIdent, ",") <~ ")" <~ indexOption ^^ {
        case indexName ~ _ ~ _ ~ keyParts => Index(indexName, keyParts)
      }

  private def fulltext: Parser[Index] =
    (caseSensitivity("fulltext") | caseSensitivity("spatial")) ~>
      (caseSensitivity("index") | caseSensitivity("key")) ~>
      opt(sqlIdent) ~ "(" ~ repsep(sqlIdent, ",") <~ ")" <~ indexOption ^^ {
        case indexName ~ _ ~ keyParts => Index(indexName, keyParts)
      }

  private def constraint: Parser[Option[String]] =
    caseSensitivity("constraint") ~> opt(sqlIdent) ^^ { symbol =>
      symbol
    }

  private def referenceOption: Parser[String] =
    (
      caseSensitivity("restrict") | caseSensitivity("cascade") | (caseSensitivity("set") ~
        (caseSensitivity("null") | caseSensitivity("default"))) | (caseSensitivity("no") ~
        caseSensitivity("action"))
    ) ^^ {
      case set ~ option   => s"Reference.ReferenceOption.${ set.toUpperCase }_${ option.toUpperCase }"
      case option: String => s"Reference.ReferenceOption.${ option.toUpperCase }"
    }

  private def referenceDefinition: Parser[Reference] =
    caseSensitivity("references") ~> sqlIdent ~ "(" ~ repsep(sqlIdent, ",") ~ ")" ~
      opt(
        caseSensitivity("match") ~ (caseSensitivity("full") |
          caseSensitivity("partial") | caseSensitivity("simple"))
      ) ~ opt(caseSensitivity("on") ~> caseSensitivity("delete") ~> opt(referenceOption)) ~
      opt(caseSensitivity("on") ~> caseSensitivity("update") ~> opt(referenceOption)) ^^ {
        case tableName ~ _ ~ keyParts ~ _ ~ _ ~ onDelete ~ onUpdate =>
          Reference(tableName, keyParts, onDelete.flatten, onUpdate.flatten)
      }

  private def constraintPrimaryKey: Parser[Primary] =
    opt(constraint) ~ primaryKey ~ opt(indexType) ~ "(" ~ repsep(sqlIdent, ",") ~ ")" ~ indexOption ^^ {
      case constraint ~ _ ~ indexType ~ _ ~ keyParts ~ _ ~ option =>
        Primary(constraint, indexType, keyParts, option)
    }

  private def constraintUniqueKey: Parser[Unique] =
    opt(constraint) ~ caseSensitivity("unique") ~ opt(caseSensitivity("index") | caseSensitivity("key"))
      ~ opt(sqlIdent) ~ opt(indexType) ~ "(" ~ repsep(sqlIdent, ",") ~ ")" ~ indexOption ^^ {
        case constraint ~ _ ~ _ ~ indexName ~ indexType ~ _ ~ keyParts ~ _ ~ option =>
          Unique(constraint, indexName, indexType, keyParts, option)
      }

  private def constraintForeignKey: Parser[Foreign] =
    opt(constraint) ~ caseSensitivity("foreign") ~ caseSensitivity("key") ~ opt(sqlIdent) ~
      "(" ~ repsep(sqlIdent, ",") ~ ")" ~ referenceDefinition ^^ {
        case constraint ~ _ ~ _ ~ indexName ~ _ ~ columnNames ~ _ ~ referenceDefinition =>
          Foreign(constraint, indexName, columnNames, referenceDefinition)
      }

  private def checkConstraintDefinition: Parser[String] =
    opt(constraint) ~ caseSensitivity("check") ~ "(" ~ sqlIdent ~ ")" ~
      opt(caseSensitivity("not")) ~ opt(caseSensitivity("enforced")) ^^ {
        case constraint ~ _ ~ _ ~ expr ~ _ ~ not ~ enforced => s"$constraint $expr $not $enforced"
      }

  protected def keyDefinitions: Parser[Key | String] =
    (indexKey | fulltext | constraintPrimaryKey | constraintUniqueKey | constraintForeignKey | checkConstraintDefinition) ^^ {
      str => str
    }
