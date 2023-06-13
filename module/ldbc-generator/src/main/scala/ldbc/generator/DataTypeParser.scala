/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator

import ldbc.generator.model.DataType

trait DataTypeParser extends LdbcParser:

  // Parser for digits (numbers greater than or equal to 0)
  protected def digit: Parser[Int] = """\d+""".r ^^ (_.toInt)

  private def unsigned: Parser[String] = "(?i)unsigned".r ^^ (_.toUpperCase)
  private def zerofill: Parser[String] = "(?i)zerofill".r ^^ (_.toUpperCase)

  protected def dataType: Parser[DataType] = bitType | tinyintType | bigIntType

  /** Numeric data type parsing
    */
  private def bitType: Parser[DataType] =
    customError(
      "(?i)bit".r ~> "(" ~> digit.filter(n => n >= 1 && n <= 64) <~ ")" ^^ { n =>
        DataType.Bit(n)
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

  private def tinyintType: Parser[DataType] =
    customError(
      "(?i)tinyint".r ~> "(" ~> digit.filter(n => n >= -128 && n <= 255) ~ ")" ~ opt(unsigned) ~ opt(zerofill) ^^ {
        case n ~ _ ~ unsigned ~ zerofill => DataType.Tinyint(n, unsigned.isDefined, zerofill.isDefined)
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

  private def bigIntType: Parser[DataType] =
    customError(
      "(?i)bigint".r ~> "(" ~> digit.filter(n => n >= -128 && n <= 255) ~ ")" ~ opt(unsigned) ~ opt(zerofill) ^^ {
        case n ~ _ ~ unsigned ~ zerofill => DataType.BigInt(n, unsigned.isDefined, zerofill.isDefined)
      },
      """
        |===============================================================================
        |Failed to parse bigint data type.
        |The bigint Data type must be defined as follows
        |※ bigint strings are case-insensitive.
        |
        |M, the signed range is -128 to 127. The unsigned range is 0 to 255.
        |
        |SEE: https://man.plustar.jp/mysql/numeric-type-syntax.html
        |
        |example: BIGINT[(M)] [UNSIGNED] [ZEROFILL]
        |==============================================================================
        |""".stripMargin
    )
