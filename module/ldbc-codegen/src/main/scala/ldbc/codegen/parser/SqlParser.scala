/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.codegen.parser

import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.input.CharArrayReader.EofCh

import ldbc.codegen.model.*

/** Parser to parse common definitions in MySQL.
  */
trait SqlParser extends JavaTokenParsers:

  def fileName: String

  def failureMessage(format: String, example: String): Input => String =
    input => s"""
      |======================================================
      |There is an error in the format of the $format.
      |Please correct the format according to the following.
      |
      |${ input.pos.longString } ($fileName:${ input.pos.line }:${ input.pos.column })
      |example: $example
      |======================================================
      |""".stripMargin

  protected def customError[A](parser: Parser[A], msg: Input => String): Parser[A] = Parser[A] { input =>
    parser(input) match
      case Failure(_, in) => Failure(msg(in), in)
      case result         => result
  }

  override def stringLiteral: Parser[String] = "'" ~> """[^']*""".r <~ "'"

  private def symbol: Parser[String] =
    "@" | "=" | "!" | "#" | "$" | "%" | "&" | "-" | "+" | ";" | ":" | "," | "." | "?" |
      "<" | ">" | "[" | "]" | "{" | "}" | "(" | ")" | "|" | "\\" | "^" | "_" | "~" | "'" | "`"

  protected def specialChars: Parser[String] =
    ident | wholeNumber | decimalNumber | symbol

  private def normalIdent: Parser[String] = rep1(
    acceptIf(Character.isJavaIdentifierStart)("identifier expected but `" + _ + "' found"),
    elem("identifier part", Character.isJavaIdentifierPart(_: Char))
  ) ^^ (_.mkString)

  protected def sqlIdent: Parser[String] =
    "" ~> // handle whitespace
      opt("`") ~> normalIdent <~ opt("`") ^^ (_.mkString)

  protected def keyValue[T](keyParser: Parser[String], valueParser: Parser[T]): Parser[T] =
    keyParser ~> opt("=") ~> valueParser

  protected def create: Parser[String] = caseSensitivity("create") ^^ (_.toUpperCase)
  protected def drop:   Parser[String] = caseSensitivity("drop") ^^ (_.toUpperCase)

  protected def ifNotExists: Parser[String] =
    customError(
      caseSensitivity("if") ~> opt(caseSensitivity("not")) ~> caseSensitivity("exists") ^^ (_.toUpperCase),
      failureMessage("if not exists", "IF [NOT] EXISTS")
    )

  protected def character: Parser[String] =
    customError(
      ((caseSensitivity("character") ~> caseSensitivity("set")) | caseSensitivity("charset")) ~>
        opt("=") ~> sqlIdent.filter(_ != "="),
      failureMessage("character", "{CHARACTER [SET] | CHARSET}[=]'string'")
    )

  protected def collate: Parser[String] =
    customError(
      caseSensitivity("collate") ~> opt("=") ~> sqlIdent,
      failureMessage("collate", "COLLATE[=]'string'")
    )

  /** Rules for allowing upper and lower case letters. */
  protected def caseSensitivity(str: String): Parser[String] =
    s"(?i)$str".r

  private def chrExcept(cs: Char*) = elem("", ch => !cs.contains(ch))

  private def lineComment: Parser[CommentOut] =
    "--+".r ~> rep(chrExcept(EofCh, '\n')) ^^ { str =>
      CommentOut(str.mkString(" "))
    }

  private def blockComment: Parser[CommentOut] =
    "/*" ~> rep(chrExcept(EofCh, '*')) <~ "*/" <~ opt(";") ^^ { str =>
      CommentOut(str.mkString(" "))
    }

  protected def comment: Parser[CommentOut] = lineComment | blockComment

  protected def commentSet: Parser[CommentSet] =
    customError(
      caseSensitivity("comment") ~> stringLiteral ^^ CommentSet.apply,
      failureMessage("comment", "COMMENT 'string'")
    )

  protected def keyBlockSize: Parser[Key.KeyBlockSize] =
    customError(
      caseSensitivity("key_block_size") ~> opt("=") ~> ("1" | "2" | "4" | "8" | "16") ^^ {
        case "1"  => Key.KeyBlockSize(1)
        case "2"  => Key.KeyBlockSize(2)
        case "4"  => Key.KeyBlockSize(4)
        case "8"  => Key.KeyBlockSize(8)
        case "16" => Key.KeyBlockSize(16)
      },
      failureMessage("key_block_size", "KEY_BLOCK_SIZE[=]{1 | 2 | 4 | 8 | 16}")
    )

  protected def engineAttribute: Parser[Key.EngineAttribute] =
    customError(
      caseSensitivity("engine_attribute") ~> opt("=") ~> ident ^^ Key.EngineAttribute.apply,
      failureMessage("engine_attribute", "ENGINE_ATTRIBUTE[=]'string'")
    )

  protected def secondaryEngineAttribute: Parser[Key.SecondaryEngineAttribute] =
    customError(
      caseSensitivity("secondary_engine_attribute") ~> opt("=") ~> ident ^^ Key.SecondaryEngineAttribute.apply,
      failureMessage("secondary_engine_attribute", "SECONDARY_ENGINE_ATTRIBUTE[=]'string'")
    )
