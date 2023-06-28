/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.parser

import ldbc.generator.model.*

trait ColumnParser extends DataTypeParser:

  private def constraint: Parser[String] =
    caseSensitivity("not") ~> caseSensitivity("null") ^^ (_ => "NOT NULL") | "NULL"

  private def default: Parser[Default] =
    caseSensitivity("default") ~> (stringLiteral | digit | caseSensitivity("null") | caseSensitivity(
      "current_timestamp"
    )) ~
      opt(caseSensitivity("on") ~> caseSensitivity("update") ~> caseSensitivity("current_timestamp")) ^^ {
        case i ~ attribute => Default(i, attribute)
      }

  private def visible: Parser[String] =
    caseSensitivity("visible") | caseSensitivity("invisible") ^^ { i => i }

  private def autoInc: Parser[String] =
    caseSensitivity("auto_increment") <~ opt(comment) ^^ (_.toUpperCase)

  protected def primaryKey: Parser[String] =
    caseSensitivity("primary") <~ opt(caseSensitivity("key")) <~ opt(comment) ^^ { _ => "PRIMARY_KEY" }

  protected def uniqueKey: Parser[String] =
    caseSensitivity("unique") <~ opt(caseSensitivity("key")) <~ opt(comment) ^^ { _ => "UNIQUE_KEY" }

  protected def columnComment: Parser[Comment] =
    caseSensitivity("comment") ~> stringLiteral ^^ Comment.apply

  private def columnFormat: Parser[String] =
    caseSensitivity("column_format") ~> (
      caseSensitivity("fixed") | caseSensitivity("dynamic") | caseSensitivity("default")
    ) ^^ { i => i }

  private def storage: Parser[String] =
    caseSensitivity("storage") ~> (caseSensitivity("disk") | caseSensitivity("memory")) ^^ { i => i }

  private def attributes: Parser[Option[Attributes]] =
    opt(constraint) ~ opt(comment) ~ opt(default) ~ opt(comment) ~ opt(visible) ~
      opt(comment) ~ opt(rep(autoInc | primaryKey | uniqueKey)) ~ opt(comment) ~
      opt(columnComment) ~ opt(comment) ~ opt(collate) ~ opt(comment) ~ opt(columnFormat) ~
      opt(comment) ~ opt(engineAttribute) ~ opt(comment) ~ opt(secondaryEngineAttribute) ~ opt(comment) ~
      opt(storage) ^^ {
        case constraint ~ _ ~ default ~ _ ~ visible ~ _ ~ key ~ _ ~ comment ~ _ ~ collate ~ _ ~ columnFormat ~ _ ~ engineAttribute ~ _ ~ secondaryEngineAttribute ~ _ ~ storage =>
          (
            constraint,
            default,
            visible,
            key,
            comment,
            collate,
            columnFormat,
            engineAttribute,
            secondaryEngineAttribute,
            storage
          ) match
            case (None, None, None, None, None, None, None, None, None, None) => None
            case _ =>
              Some(
                Attributes(
                  constraint.forall(_ == "NULL"),
                  default,
                  visible,
                  key,
                  comment,
                  collate,
                  columnFormat,
                  engineAttribute,
                  secondaryEngineAttribute,
                  storage
                )
              )
      }

  protected def columnDefinition: Parser[ColumnDefinition] =
    opt(comment) ~> sqlIdent ~ opt(comment) ~ dataType ~ opt(comment) ~ attributes <~ opt(comment) ^^ {
      case columnName ~ _ ~ dataType ~ _ ~ attributes => ColumnDefinition(columnName, dataType, attributes)
    }
