/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.parser

import ldbc.generator.model.Comment

import scala.util.parsing.combinator.JavaTokenParsers

trait SqlParser extends JavaTokenParsers:

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

  protected def create: Parser[String] = caseSensitivity("create") ^^ (_.toUpperCase)
  protected def drop:   Parser[String] = caseSensitivity("drop") ^^ (_.toUpperCase)

  protected def ifNotExists: Parser[String] =
    caseSensitivity("if") ~> caseSensitivity("not") ~> caseSensitivity("exists") ^^ (_.toUpperCase)

  protected def character: Parser[String] =
    customError(
      ((caseSensitivity("character") ~> caseSensitivity("set")) | caseSensitivity("charset")) ~>
        opt("=") ~> sqlIdent.filter(_ != "="),
      """
        |======================================================
        |There is an error in the character format.
        |Please correct the format according to the following.
        |
        |Only numbers can be set for size.
        |
        |example: {CHARACTER [SET] | CHARSET} [=] 'string'
        |======================================================
        |""".stripMargin
    )

  protected def collate: Parser[String] =
    customError(
      caseSensitivity("collate") ~> opt("=") ~> sqlIdent,
      """
        |======================================================
        |There is an error in the collate format.
        |Please correct the format according to the following.
        |
        |example: COLLATE [=] 'string'
        |======================================================
        |""".stripMargin
    )

  /** Rules for allowing upper and lower case letters. */
  protected def caseSensitivity(str: String): Parser[String] =
    s"(?i)$str".r

  protected def comment: Parser[Comment] =
    ("/*" ~> opt(rep1(specialChars)) <~ "*/" <~ opt(";") | "--+".r ~> opt(rep1(specialChars))) ^^ (comment =>
      Comment(comment.getOrElse(List.empty).mkString(" "))
    )

  protected def customError[A](parser: Parser[A], msg: String): Parser[A] = Parser[A] { input =>
    parser(input) match
      case Failure(_, in) => Failure(msg, in)
      case result         => result
  }

  protected def keyBlockSize: Parser[Int] =
    customError(
      caseSensitivity("key_block_size") ~> opt("=") ~> ("1" | "2" | "4" | "8" | "16") ^^ (_.toInt),
      """
        |======================================================
        |There is an error in the key_block_size format.
        |Please correct the format according to the following.
        |
        |example: KEY_BLOCK_SIZE[=]{1 | 2 | 4 | 8 | 16}
        |======================================================
        |""".stripMargin
    )

  protected def engineAttribute: Parser[String] =
    customError(
      caseSensitivity("engine_attribute") ~> opt("=") ~> ident,
      """
        |======================================================
        |There is an error in the engine_attribute format.
        |Please correct the format according to the following.
        |
        |example: ENGINE_ATTRIBUTE[=]'string'
        |======================================================
        |""".stripMargin
    )

  protected def secondaryEngineAttribute: Parser[String] =
    customError(
      caseSensitivity("secondary_engine_attribute") ~> opt("=") ~> ident,
      """
        |======================================================
        |There is an error in the secondary_engine_attribute format.
        |Please correct the format according to the following.
        |
        |example: SECONDARY_ENGINE_ATTRIBUTE[=]'string'
        |======================================================
        |""".stripMargin
    )
