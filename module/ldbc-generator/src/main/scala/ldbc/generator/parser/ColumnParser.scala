/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.parser

import ldbc.generator.model.*

/** Parser for parsing column definitions.
  *
  * Please refer to the official documentation for MySQL column definitions. SEE:
  * https://dev.mysql.com/doc/refman/8.0/en/create-table.html
  */
trait ColumnParser extends DataTypeParser:

  private def constraint: Parser[String] =
    customError(
      caseSensitivity("not") ~> caseSensitivity("null") ^^ (_ => "NOT NULL") | "NULL",
      failureMessage("Nullable", "[NOT] NULL")
    )

  private def currentTimestamp: Parser[Default.CurrentTimestamp] =
    customError(
      caseSensitivity("default") ~> caseSensitivity("current_timestamp") ~> opt("(" ~> digit <~ ")") ~
        opt(
          caseSensitivity("on") ~> caseSensitivity("update") ~> caseSensitivity("current_timestamp") ~ opt(
            "(" ~> digit <~ ")"
          )
        ) ^^ {
          case _ ~ Some(attribute ~ _) => Default.CurrentTimestamp(true)
          case _ ~ None                => Default.CurrentTimestamp(false)
        },
      failureMessage(
        "default current timestamp",
        "DEFAULT CURRENT_TIMESTAMP[({0 ~ 6})] [ON UPDATE CURRENT_TIMESTAMP[({0 ~ 6})]]"
      )
    )

  private def defaultNull: Parser[Default.Null.type] =
    customError(
      caseSensitivity("default") ~> caseSensitivity("null") ^^ (_ => Default.Null),
      failureMessage("default null", "DEFAULT NULL")
    )

  private def bitValue: Parser[Int] = opt("b") ~> "'" ~> digit <~ "'"

  private def defaultValue: Parser[Default.Value] =
    customError(
      caseSensitivity("default") ~> (bitValue | digit | stringLiteral) ^^ Default.Value.apply,
      failureMessage("default value", "DEFAULT `value`")
    )

  private def default: Parser[Default] = defaultValue | currentTimestamp | defaultNull

  private def visible: Parser[String] =
    caseSensitivity("visible") | caseSensitivity("invisible")

  private def autoInc: Parser[String] =
    caseSensitivity("auto_increment") ^^ (_.toUpperCase)

  protected def primaryKey: Parser[String] =
    customError(
      caseSensitivity("primary") <~ opt(caseSensitivity("key")) ^^ { _ => "PRIMARY_KEY" },
      failureMessage("primary key", "PRIMARY [KEY]")
    )

  protected def uniqueKey: Parser[String] =
    customError(
      caseSensitivity("unique") <~ opt(caseSensitivity("key")) ^^ { _ => "UNIQUE_KEY" },
      failureMessage("unique key", "UNIQUE [KEY]")
    )

  protected def columnComment: Parser[Comment] =
    customError(
      caseSensitivity("comment") ~> stringLiteral ^^ Comment.apply,
      failureMessage("comment", "COMMENT 'string'")
    )

  private def columnFormat: Parser[String] =
    customError(
      caseSensitivity("column_format") ~> (
        caseSensitivity("fixed") | caseSensitivity("dynamic") | caseSensitivity("default")
      ),
      failureMessage("column format", "COLUMN_FORMAT {FIXED | DYNAMIC | DEFAULT}")
    )

  private def storage: Parser[String] =
    customError(
      caseSensitivity("storage") ~> (caseSensitivity("disk") | caseSensitivity("memory")),
      failureMessage("storage", "STORAGE {DISK | MEMORY}")
    )

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
