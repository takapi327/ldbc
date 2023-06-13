/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator

import ldbc.generator.model.Key
import ldbc.generator.model.Key.*

trait KeyParser extends ColumnParser:

  private def indexType: Parser[String] =
    "(?i)using".r ~> ("(?i)btree".r | "(?i)hash".r) ^^ { input =>
      s"Index.Type.${ input.toUpperCase }"
    }

  private def indexOption: Parser[Option[String]] =
    opt("(?i)key_block_size".r ~> "=" ~> digit) ~ opt(indexType) ~ opt("(?i)with".r ~> "(?i)parser".r ~> ident) ~
      opt(columnComment) ~ opt("(?i)visible".r | "(?i)invisible".r) ~
      opt("(?i)engine_attribute".r ~> "=" ~> ident) ~ opt("(?i)secondary_engine_attribute".r ~> "=" ~> ident) ^^ {
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
    ("(?i)index".r | "(?i)key".r) ~> opt(sqlIdent) ~ opt(indexType) ~ "(" ~
      repsep(sqlIdent, ",") <~ ")" <~ indexOption ^^ {
        case indexName ~ _ ~ _ ~ keyParts => Index(indexName, keyParts)
      }

  private def fulltext: Parser[Index] =
    ("(?i)fulltext".r | "(?i)spatial".r) ~> ("(?i)index".r | "(?i)key".r) ~>
      opt(sqlIdent) ~ "(" ~ repsep(sqlIdent, ",") <~ ")" <~ indexOption ^^ {
        case indexName ~ _ ~ keyParts => Index(indexName, keyParts)
      }

  private def constraint: Parser[Option[String]] =
    "(?i)constraint".r ~> opt(sqlIdent) ^^ { symbol =>
      symbol
    }

  private def referenceOption: Parser[String] =
    ("(?i)restrict".r | "(?i)cascade".r | ("(?i)set".r ~ ("(?i)null".r | "(?i)default".r)) | ("(?i)no".r ~ "(?i)action".r)) ^^ {
      case set ~ option   => s"Reference.ReferenceOption.${ set.toUpperCase }_${ option.toUpperCase }"
      case option: String => s"Reference.ReferenceOption.${ option.toUpperCase }"
    }

  private def referenceDefinition: Parser[Reference] =
    "(?i)references".r ~> sqlIdent ~ "(" ~ repsep(sqlIdent, ",") ~ ")" ~
      opt("(?i)match".r ~ ("(?i)full".r | "(?i)partial".r | "(?i)simple".r)) ~
      opt("(?i)on".r ~> "(?i)delete".r ~> opt(referenceOption)) ~
      opt("(?i)on".r ~> "(?i)update".r ~> opt(referenceOption)) ^^ {
        case tableName ~ _ ~ keyParts ~ _ ~ _ ~ onDelete ~ onUpdate =>
          Reference(tableName, keyParts, onDelete.flatten, onUpdate.flatten)
      }

  private def constraintPrimaryKey: Parser[Primary] =
    opt(constraint) ~ primaryKey ~ opt(indexType) ~ "(" ~ repsep(sqlIdent, ",") ~ ")" ~ indexOption ^^ {
      case constraint ~ _ ~ indexType ~ _ ~ keyParts ~ _ ~ option =>
        Primary(constraint, indexType, keyParts, option)
    }

  private def constraintUniqueKey: Parser[Unique] =
    opt(constraint) ~ "(?i)unique".r ~ opt("(?i)index".r | "(?i)key".r) ~ opt(sqlIdent) ~
      opt(indexType) ~ "(" ~ repsep(sqlIdent, ",") ~ ")" ~ indexOption ^^ {
        case constraint ~ _ ~ _ ~ indexName ~ indexType ~ _ ~ keyParts ~ _ ~ option =>
          Unique(constraint, indexName, indexType, keyParts, option)
      }

  private def constraintForeignKey: Parser[Foreign] =
    opt(constraint) ~ "(?i)foreign".r ~ "(?i)key".r ~ opt(sqlIdent) ~
      "(" ~ repsep(sqlIdent, ",") ~ ")" ~ referenceDefinition ^^ {
        case constraint ~ _ ~ _ ~ indexName ~ _ ~ columnNames ~ _ ~ referenceDefinition =>
          Foreign(constraint, indexName, columnNames, referenceDefinition)
      }

  private def checkConstraintDefinition: Parser[String] =
    opt(constraint) ~ "(?i)check".r ~ "(" ~ sqlIdent ~ ")" ~ opt("(?i)not".r) ~ opt("(?i)enforced".r) ^^ {
      case constraint ~ _ ~ _ ~ expr ~ _ ~ not ~ enforced => s"$constraint $expr $not $enforced"
    }

  protected def keyDefinitions: Parser[Key | String] =
    (indexKey | fulltext | constraintPrimaryKey | constraintUniqueKey | constraintForeignKey | checkConstraintDefinition) ^^ {
      str => str
    }
