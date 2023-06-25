/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator

import scala.util.parsing.combinator.JavaTokenParsers

import ldbc.generator.model.Comment

trait LdbcParser extends JavaTokenParsers:

  override def stringLiteral: Parser[String] = "'" ~> """[^']*""".r <~ "'"

  private def symbol: Parser[String] = "@" | "=" | "!" | "#" | "$" | "%" | "&" | "(" | ")" | "-" | "+" | ";" | ":" | "," | "." | "?" |
    "<" | ">" | "[" | "]" | "{" | "}" | "|" | "\\" | "^" | "_" | "~" | "'" | "`"

  private def specialChars: Parser[String] =
    ident | wholeNumber | decimalNumber | symbol

  private def normalIdent: Parser[String] = rep1(
    acceptIf(Character.isJavaIdentifierStart)("identifier expected but `" + _ + "' found"),
    elem("identifier part", Character.isJavaIdentifierPart(_: Char))
  ) ^^ (_.mkString)

  protected def sqlIdent: Parser[String] =
    "" ~> // handle whitespace
      opt("`") ~> normalIdent <~ opt("`") ^^ (_.mkString)

  protected def create: Parser[String] = caseSensitivity("create") ^^ (_.toUpperCase)

  protected def ifNotExists: Parser[String] =
    caseSensitivity("if") ~> caseSensitivity("not") ~> caseSensitivity("exists") ^^ (_.toUpperCase)

  protected def character: Parser[String] =
    caseSensitivity("character") ~> caseSensitivity("set") ~> opt("=") ~> sqlIdent
  protected def collate: Parser[String] = caseSensitivity("collate") ~> opt("=") ~> sqlIdent

  /** Rules for allowing upper and lower case letters. */
  protected def caseSensitivity(str: String): Parser[String] =
    s"(?i)$str".r

  protected def comment: Parser[Comment] = ("/*" ~> opt(rep1(specialChars)) <~ "*/" <~ opt(";") | "--+".r ~> opt(rep1(specialChars))) ^^ (
    comment => Comment(comment.getOrElse(List.empty).mkString(" "))
  )

  protected def customError[A](parser: Parser[A], msg: String): Parser[A] = Parser[A] { input =>
    parser(input) match
      case Failure(_, in) => Failure(msg, in)
      case result         => result
  }
