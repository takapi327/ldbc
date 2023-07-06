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
    caseSensitivity("not") ~> caseSensitivity("null") ^^ (_ => "NOT NULL") | "NULL"

  private def currentTimestamp: Parser[Default.CurrentTimestamp] =
    customError(
      caseSensitivity("default") ~> caseSensitivity("current_timestamp") ~> opt("(" ~> digit <~ ")") ~
        opt(caseSensitivity("on") ~> caseSensitivity("update") ~> caseSensitivity("current_timestamp") ~ opt("(" ~> digit <~ ")")) ^^ {
        case _ ~ Some(attribute ~ _) => Default.CurrentTimestamp(true)
        case _ ~ None => Default.CurrentTimestamp(false)
      },
      """
        |======================================================
        |There is an error in the format of the default current timestamp.
        |Please correct the format according to the following.
        |
        |example: DEFAULT CURRENT_TIMESTAMP[({0 ~ 6})] [ON UPDATE CURRENT_TIMESTAMP[({0 ~ 6})]]
        |======================================================
        |""".stripMargin
    )

  private def defaultNull: Parser[Default.Null.type] =
    customError(
      caseSensitivity("default") ~> caseSensitivity("null") ^^ (_ => Default.Null),
      """
        |======================================================
        |There is an error in the format of the default null.
        |Please correct the format according to the following.
        |
        |example: DEFAULT NULL
        |======================================================
        |""".stripMargin
    )

  private def defaultValue: Parser[Default.Value] =
    customError(
      caseSensitivity("default") ~> (stringLiteral | digit) ^^ Default.Value.apply,
      """
        |======================================================
        |There is an error in the format of the default value.
        |Please correct the format according to the following.
        |
        |example: DEFAULT `value`
        |======================================================
        |""".stripMargin
    )

  private def default: Parser[Default] = defaultValue | defaultNull | currentTimestamp

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
