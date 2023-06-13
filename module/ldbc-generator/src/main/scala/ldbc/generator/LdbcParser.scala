/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator

import scala.util.parsing.combinator.JavaTokenParsers

import ldbc.generator.model.Comment

trait LdbcParser extends JavaTokenParsers:

  override def stringLiteral: Parser[String] = "'" ~> """[^']*""".r <~ "'"

  private def normalIdent: Parser[String] = rep1(acceptIf(Character.isJavaIdentifierStart)("identifier expected but `" + _ + "' found"),
    elem("identifier part", Character.isJavaIdentifierPart(_: Char))) ^^ (_.mkString)

  protected def sqlIdent: Parser[String] =
    "" ~> // handle whitespace
      opt("`") ~> normalIdent <~ opt("`") ^^ (_.mkString)

  protected def comment: Parser[Comment] = ("/*" | "--+".r) ~> ident <~ opt("*/") ^^ Comment.apply

  protected def customError[A](parser: Parser[A], msg: String): Parser[A] = Parser[A] { input =>
    parser(input) match
      case Failure(_, in) => Failure(msg, in)
      case result         => result
  }
