/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.generator

import scala.util.parsing.combinator.*

import ldbc.generator.model.*

trait ColumnParser extends DataTypeParser:
  self: RegexParsers & JavaTokenParsers =>

  private def stringLiteral: Parser[String] = "'" ~> """[^']*""".r <~ "'"

  private def constraint: Parser[String] =
    "(?i)not".r ~> "(?i)null".r ^^ (_ => "NOT NULL") | "NULL"

  private def default: Parser[String | Int] =
    "(?i)default".r ~> (stringLiteral | digit | "(?i)null".r) ^^ { i => i }

  private def visible: Parser[String] =
    "(?i)visible".r | "(?i)invisible".r ^^ { i => i }

  private def autoInc: Parser[String] =
    "(?i)auto_increment".r ^^ (_.toUpperCase)

  private def primaryKey: Parser[String] =
    "(?i)primary".r <~ opt("(?i)key".r) ^^ { _ => "PRIMARY_KEY" }

  private def uniqueKey: Parser[String] =
    "(?i)unique".r <~ opt("(?i)key".r) ^^ { _ => "UNIQUE_KEY" }

  private def columnComment: Parser[Comment] =
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
    opt(constraint) ~ opt(default) ~ opt(visible) ~ opt(rep(autoInc | primaryKey | uniqueKey)) ~ opt(columnComment) ~ opt(collate) ~ opt(columnFormat) ~ opt(engineAttribute) ~ opt(secondaryEngineAttribute) ~ opt(storage) ^^ {
      case constraint ~ default ~ visible ~ key ~ comment ~ collate ~ columnFormat ~ engineAttribute ~ secondaryEngineAttribute ~ storage =>
        (constraint, default, visible, key, comment, collate, columnFormat, engineAttribute, secondaryEngineAttribute, storage) match
          case (None, None, None, None, None, None, None, None, None, None) => None
          case _ => Some(Attributes(constraint, default, visible, key, comment, collate, columnFormat, engineAttribute, secondaryEngineAttribute, storage))
    }

  protected def columnDefinition: Parser[ColumnDefinition] =
    ident ~ dataType ~ attributes <~ opt(comment) ^^ {
      case columnName ~ dataType ~ attributes => ColumnDefinition(columnName, dataType, attributes)
    }
