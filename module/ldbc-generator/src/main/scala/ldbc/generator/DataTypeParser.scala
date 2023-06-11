/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.generator

import scala.util.parsing.combinator.*

trait DataTypeParser extends LdbcParser:
  self: RegexParsers & JavaTokenParsers =>

  // デジット（0以上の数字）のパーサー
  protected def digit: Parser[Int] = """\d+""".r ^^ (_.toInt)

  private def unsigned: Parser[String] = "(?i)unsigned".r ^^ (_.toUpperCase)
  private def zerofill: Parser[String] = "(?i)zerofill".r ^^ (_.toUpperCase)

  protected def dataType: Parser[DataTypeParseResult] = bitType | tinyintType

  /**
   * ==========================================
   * Numeric data type parsing
   * ==========================================
   */
  private def bitType: Parser[DataTypeParseResult] =
    customError(
      "(?i)bit".r ~> "(" ~> digit.filter(n => n >= 1 && n <= 64) <~ ")" ^^ { n =>
        DataTypeParseResult.Bit(n)
      },
      """
        |===============================================================================
        |Failed to parse Bit data type.
        |The Bit Data type must be defined as follows
        |※ Bit strings are case-insensitive.
        |
        |M is the number of bits per value (1 to 64). If M is omitted, the default is 1.
        |
        |SEE: https://man.plustar.jp/mysql/numeric-type-syntax.html
        |
        |example: BIT[(M)]
        |==============================================================================
        |""".stripMargin
    )

  private def tinyintType: Parser[DataTypeParseResult] =
    customError(
      "(?i)tinyint".r ~> "(" ~> digit.filter(n => n >= -128 && n <= 255) ~ ")" ~ opt(unsigned) ~ opt(zerofill) ^^ {
        case n ~ _ ~ unsigned ~ zerofill => DataTypeParseResult.Tinyint(n, unsigned.isDefined, zerofill.isDefined)
      },
      """
        |===============================================================================
        |Failed to parse tinyint data type.
        |The tinyint Data type must be defined as follows
        |※ tinyint strings are case-insensitive.
        |
        |M, the signed range is -128 to 127. The unsigned range is 0 to 255.
        |
        |SEE: https://man.plustar.jp/mysql/numeric-type-syntax.html
        |
        |example: TINYINT[(M)] [UNSIGNED] [ZEROFILL]
        |==============================================================================
        |""".stripMargin
    )
