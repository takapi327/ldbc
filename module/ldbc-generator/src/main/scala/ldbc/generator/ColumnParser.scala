/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator

import ldbc.generator.model.*

trait ColumnParser extends DataTypeParser:

  private def constraint: Parser[String] =
    "(?i)not".r ~> "(?i)null".r ^^ (_ => "NOT NULL") | "NULL"

  private def default: Parser[String | Int] =
    "(?i)default".r ~> (stringLiteral | digit | "(?i)null".r) ^^ { i => i }

  private def visible: Parser[String] =
    "(?i)visible".r | "(?i)invisible".r ^^ { i => i }

  private def autoInc: Parser[String] =
    "(?i)auto_increment".r <~ opt(comment) ^^ (_.toUpperCase)

  protected def primaryKey: Parser[String] =
    "(?i)primary".r <~ opt("(?i)key".r) <~ opt(comment) ^^ { _ => "PRIMARY_KEY" }

  protected def uniqueKey: Parser[String] =
    "(?i)unique".r <~ opt("(?i)key".r) <~ opt(comment) ^^ { _ => "UNIQUE_KEY" }

  protected def columnComment: Parser[Comment] =
    "(?i)comment".r ~> stringLiteral ^^ Comment.apply

  private def collate: Parser[String] =
    "(?i)collate".r ~> ident ^^ { i => i }

  private def columnFormat: Parser[String] =
    "(?i)column_format".r ~> ("(?i)fixed".r | "(?i)dynamic".r | "(?i)default".r) ^^ { i => i }

  private def engineAttribute: Parser[String] =
    "(?i)engine_attribute".r ~> opt("=") ~> ident ^^ { i => i }

  private def secondaryEngineAttribute: Parser[String] =
    "(?i)secondary_engine_attribute".r ~> opt("=") ~> ident ^^ { i => i }

  private def storage: Parser[String] =
    "(?i)storage".r ~> ("(?i)disk".r | "(?i)memory".r) ^^ { i => i }

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
