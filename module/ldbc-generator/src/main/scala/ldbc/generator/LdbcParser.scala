/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.generator

import scala.util.parsing.combinator.RegexParsers

case class Comment(message: String)

trait LdbcParser:
  self: RegexParsers =>

  protected def customError[A](parser: Parser[A], msg: String): Parser[A] = Parser[A] { input =>
    parser(input) match
      case Failure(_, in) => Failure(msg, in)
      case result => result
  }
