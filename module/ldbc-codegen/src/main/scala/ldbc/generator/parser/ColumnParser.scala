/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.parser

import ldbc.generator.model.*
import ldbc.generator.model.ColumnDefinition.*

/** Parser for parsing column definitions.
  *
  * Please refer to the official documentation for MySQL column definitions. SEE:
  * https://dev.mysql.com/doc/refman/8.0/en/create-table.html
  */
trait ColumnParser extends DataTypeParser:

  private def condition: Parser[Attribute.Condition] =
    customError(
      opt(caseSensitivity("not")) <~ caseSensitivity("null") ^^ (v => Attribute.Condition(v.isEmpty)),
      failureMessage("Nullable", "[NOT] NULL")
    )

  private def currentTimestamp: Parser[Attribute.Default.CurrentTimestamp] =
    customError(
      caseSensitivity("default") ~> caseSensitivity("current_timestamp") ~> opt("(" ~> digit <~ ")") ~
        opt(
          caseSensitivity("on") ~> caseSensitivity("update") ~> caseSensitivity("current_timestamp") ~ opt(
            "(" ~> digit <~ ")"
          )
        ) ^^ {
          case _ ~ Some(attribute ~ _) => Attribute.Default.CurrentTimestamp(true)
          case _ ~ None                => Attribute.Default.CurrentTimestamp(false)
        },
      failureMessage(
        "default current timestamp",
        "DEFAULT CURRENT_TIMESTAMP[({0 ~ 6})] [ON UPDATE CURRENT_TIMESTAMP[({0 ~ 6})]]"
      )
    )

  private def defaultNull: Parser[Attribute.Default.Null.type] =
    customError(
      caseSensitivity("default") ~> caseSensitivity("null") ^^ (_ => Attribute.Default.Null),
      failureMessage("default null", "DEFAULT NULL")
    )

  private def bitValue: Parser[Int] = opt("b") ~> "'" ~> digit <~ "'"
  private def boolValue: Parser[Boolean] = (caseSensitivity("true") | caseSensitivity("false")) ^^ {
    case str if "(?i)true".r.matches(str)  => true
    case str if "(?i)false".r.matches(str) => false
  }

  private def defaultValue: Parser[Attribute.Default.Value] =
    customError(
      caseSensitivity("default") ~> (bitValue | digit | stringLiteral | boolValue) ^^ Attribute.Default.Value.apply,
      failureMessage("default value", "DEFAULT `value`")
    )

  private def default: Parser[Attribute.Default] = defaultValue | currentTimestamp | defaultNull

  private def visible: Parser[Attribute.Visible] =
    (caseSensitivity("visible") | caseSensitivity("invisible")) ^^ Attribute.Visible.apply

  private def autoInc: Parser[Attribute.Key] =
    caseSensitivity("auto_increment") ^^ (v => Attribute.Key(v.toUpperCase))

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

  private def keys: Parser[Attribute.Key] =
    autoInc | primaryKey ^^ (v => Attribute.Key(v.toUpperCase)) | uniqueKey ^^ (v => Attribute.Key(v.toUpperCase))

  private def columnFormat: Parser[Attribute.ColumnFormat] =
    customError(
      caseSensitivity("column_format") ~> (
        caseSensitivity("fixed") | caseSensitivity("dynamic") | caseSensitivity("default")
      ) ^^ Attribute.ColumnFormat.apply,
      failureMessage("column format", "COLUMN_FORMAT {FIXED | DYNAMIC | DEFAULT}")
    )

  private def storage: Parser[Attribute.Storage] =
    customError(
      caseSensitivity("storage") ~> (caseSensitivity("disk") | caseSensitivity("memory")) ^^ Attribute.Storage.apply,
      failureMessage("storage", "STORAGE {DISK | MEMORY}")
    )

  private def attribute: Parser[ColumnDefinition.Attributes] =
    condition | keys | default | visible | commentSet | collate ^^ Attribute.Collate.apply | columnFormat | engineAttribute | secondaryEngineAttribute | storage | comment

  protected def columnDefinition: Parser[ColumnDefinition] =
    opt(comment) ~> sqlIdent ~ opt(comment) ~ dataType ~ opt(comment) ~ opt(rep1(attribute)) <~ opt(comment) ^^ {
      case columnName ~ _ ~ dataType ~ _ ~ attributes => ColumnDefinition(columnName, dataType, attributes)
    }
