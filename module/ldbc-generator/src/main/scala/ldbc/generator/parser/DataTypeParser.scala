/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.parser

import ldbc.generator.model.DataType

/** Parser for parsing data type definitions.
  *
  * Please refer to the official documentation for MySQL data type definitions. SEE:
  * https://dev.mysql.com/doc/refman/8.0/en/create-table.html
  */
trait DataTypeParser extends SqlParser:

  protected def digit: Parser[Int] = """-?\d+""".r ^^ (_.toInt)

  private def unsigned: Parser[String] = "(?i)unsigned".r ^^ (_.toUpperCase)
  private def zerofill: Parser[String] = "(?i)zerofill".r ^^ (_.toUpperCase)

  private def argument(name: String, min: Int, max: Int, default: Int): Parser[Int] =
    customError(
      "(" ~> digit.filter(n => n >= min && n <= max) <~ ")",
      s"M in $name[(M)] is the number of bits per value ($min to $max); if M is omitted, the default is $default."
    )

  private def argument(name: String, min: Int, max: Int | Long): Parser[Int] =
    customError(
      "(" ~> digit.filter(n =>
        n >= min && (max match
          case m: Int  => n <= m
          case m: Long => n <= m
        )
      ) <~ ")",
      s"M in $name[(M)] is the number of bits per value ($min to $max)"
    )

  protected def dataType: Parser[DataType] =
    bitType | tinyintType | smallintType | mediumintType | bigIntType | intType | decimalType | floatType | doubleType |
      charType | varcharType | binaryType | varbinaryType | tinyblobType | tinytextType | blobType | textType | mediumblobType |
      mediumtextType | longblobType | longtextType | datetimeType | dateType | timestampType | timeType | yearType

  /** Numeric data type parsing
    */
  private[ldbc] def bitType: Parser[DataType] =
    customError(
      caseSensitivity("bit") ~> opt(argument("BIT", 1, 64, 1)) ^^ { n =>
        DataType.BIT(n.getOrElse(1))
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

  private[ldbc] def tinyintType: Parser[DataType] =
    customError(
      caseSensitivity("tinyint") ~> opt(argument("TINYINT", 1, 255, 3)) ~
        opt(unsigned) ~ opt(zerofill) ^^ {
          case n ~ unsigned ~ zerofill => DataType.TINYINT(n.getOrElse(3), unsigned.isDefined, zerofill.isDefined)
        },
      """
        |===============================================================================
        |Failed to parse tinyint data type.
        |The tinyint Data type must be defined as follows
        |※ tinyint strings are case-insensitive.
        |
        |M is the number of bits per value (0 to 255). If M is omitted, the default is 3.
        |
        |SEE: https://man.plustar.jp/mysql/numeric-type-syntax.html
        |
        |example: TINYINT[(M)] [UNSIGNED] [ZEROFILL]
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def smallintType: Parser[DataType] =
    customError(
      caseSensitivity("smallint") ~> opt(argument("SMALLINT", 1, 255, 5)) ~
        opt(unsigned) ~ opt(zerofill) ^^ {
          case n ~ unsigned ~ zerofill => DataType.SMALLINT(n.getOrElse(5), unsigned.isDefined, zerofill.isDefined)
        },
      """
        |===============================================================================
        |Failed to parse smallint data type.
        |The smallint Data type must be defined as follows
        |※ smallint strings are case-insensitive.
        |
        |M is the number of bits per value (0 to 255). If M is omitted, the default is 5.
        |
        |SEE: https://man.plustar.jp/mysql/numeric-type-syntax.html
        |
        |example: SMALLINT[(M)] [UNSIGNED] [ZEROFILL]
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def mediumintType: Parser[DataType] =
    customError(
      caseSensitivity("mediumint") ~> opt(argument("MEDIUMINT", 1, 255, 8)) ~
        opt(unsigned) ~ opt(zerofill) ^^ {
          case n ~ unsigned ~ zerofill => DataType.MEDIUMINT(n.getOrElse(8), unsigned.isDefined, zerofill.isDefined)
        },
      """
        |===============================================================================
        |Failed to parse mediumint data type.
        |The mediumint Data type must be defined as follows
        |※ mediumint strings are case-insensitive.
        |
        |M is the number of bits per value (0 to 255). If M is omitted, the default is 8.
        |
        |SEE: https://man.plustar.jp/mysql/numeric-type-syntax.html
        |
        |example: MEDIUMINT[(M)] [UNSIGNED] [ZEROFILL]
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def intType: Parser[DataType] =
    customError(
      (caseSensitivity("int") ||| caseSensitivity("integer")) ~>
        opt(argument("INT", 1, 255, 10)) ~
        opt(unsigned) ~ opt(zerofill) ^^ {
          case n ~ unsigned ~ zerofill => DataType.INT(n.getOrElse(10), unsigned.isDefined, zerofill.isDefined)
        },
      """
        |===============================================================================
        |Failed to parse int data type.
        |The int Data type must be defined as follows
        |※ int strings are case-insensitive.
        |
        |M is the number of bits per value (0 to 255). If M is omitted, the default is 10.
        |
        |SEE: https://man.plustar.jp/mysql/numeric-type-syntax.html
        |
        |example: INT[(M)] [UNSIGNED] [ZEROFILL]
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def bigIntType: Parser[DataType] =
    customError(
      caseSensitivity("bigint") ~> opt(argument("BIGINT", 1, 255, 20)) ~
        opt(unsigned) ~ opt(zerofill) ^^ {
          case n ~ unsigned ~ zerofill => DataType.BIGINT(n.getOrElse(20), unsigned.isDefined, zerofill.isDefined)
        },
      """
        |===============================================================================
        |Failed to parse bigint data type.
        |The bigint Data type must be defined as follows
        |※ bigint strings are case-insensitive.
        |
        |M is the number of bits per value (0 to 255). If M is omitted, the default is 20.
        |
        |SEE: https://man.plustar.jp/mysql/numeric-type-syntax.html
        |
        |example: BIGINT[(M)] [UNSIGNED] [ZEROFILL]
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def decimalType: Parser[DataType] =
    customError(
      (caseSensitivity("decimal") | caseSensitivity("dec")) ~>
        opt("(" ~> digit.filter(n => n >= 0 && n <= 65) ~ opt("," ~> digit.filter(n => n >= 0 && n <= 30)) <~ ")") ~
        opt(unsigned) ~ opt(zerofill) ^^ {
          case n ~ unsigned ~ zerofill =>
            n match
              case Some(m ~ d) => DataType.DECIMAL(m, d.getOrElse(0), unsigned.isDefined, zerofill.isDefined)
              case None        => DataType.DECIMAL(10, 0, unsigned.isDefined, zerofill.isDefined)
        },
      """
        |===============================================================================
        |Failed to parse decimal data type.
        |The decimal Data type must be defined as follows
        |※ decimal strings are case-insensitive.
        |
        |M is the sum of the digits (precision) and D is the number of digits after the decimal point (scale). The decimal point and the - sign for negative numbers are not counted in M. If D is 0, there is no decimal point or fractional part. The maximum number of digits (M) for DECIMAL is 65. The maximum number of decimal places supported (D) is 30. If D is omitted, the default is 0. If M is omitted, the default is 10.
        |
        |SEE: https://man.plustar.jp/mysql/numeric-type-syntax.html
        |
        |example: DECIMAL[(M[,D])] [UNSIGNED] [ZEROFILL]
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def floatType: Parser[DataType] =
    customError(
      caseSensitivity("float") ~> "(" ~> digit.filter(n => n >= 0 && n <= 24) ~ ")" ~
        opt(unsigned) ~ opt(zerofill) ^^ {
          case n ~ _ ~ unsigned ~ zerofill => DataType.FLOAT(n, unsigned.isDefined, zerofill.isDefined)
        },
      """
        |===============================================================================
        |Failed to parse float data type.
        |The float Data type must be defined as follows
        |※ float strings are case-insensitive.
        |
        |Although p expresses precision in bits, MySQL uses this value only to determine whether to use FLOAT or DOUBLE for the resulting data type. If p is 0 to 24, the data type is FLOAT with no M or D values. If p is 25 to 53, the data type is DOUBLE with no M or D values. The resulting column range is the same as for the single-precision FLOAT or double-precision DOUBLE data types described earlier in this section.
        |
        |SEE: https://man.plustar.jp/mysql/numeric-type-syntax.html
        |
        |example: FLOAT(p) [UNSIGNED] [ZEROFILL]
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def doubleType: Parser[DataType] =
    customError(
      (caseSensitivity("double") | caseSensitivity("real")) ~>
        opt("(" ~> digit.filter(n => n >= 24 && n <= 53) ~ "," ~ digit.filter(n => n >= 24 && n <= 53) <~ ")") ~
        opt(unsigned) ~ opt(zerofill) ^^ {
          case n ~ unsigned ~ zerofill =>
            n match
              case Some(m ~ _ ~ d) => DataType.FLOAT(m, unsigned.isDefined, zerofill.isDefined)
              case None            => DataType.FLOAT(10, unsigned.isDefined, zerofill.isDefined)
        },
      """
        |===============================================================================
        |Failed to parse double data type.
        |The double Data type must be defined as follows
        |※ double strings are case-insensitive.
        |
        |M is the sum of the digits and D is the number of decimal places. If M and D are omitted, the value is stored up to the limit allowed by the hardware. Double precision decimal numbers are accurate approximately to the 15th decimal place.
        |
        |SEE: https://man.plustar.jp/mysql/numeric-type-syntax.html
        |
        |example: DOUBLE[(M,D)] [UNSIGNED] [ZEROFILL]
        |==============================================================================
        |""".stripMargin
    )

  /** String data type parsing
    */
  private[ldbc] def charType: Parser[DataType] =
    customError(
      opt(caseSensitivity("national")) ~> (caseSensitivity("char") ||| caseSensitivity("character")) ~>
        opt(argument("CHAR", 0, 255, 1)) ~ opt(character ~ opt(collate)) ^^ {
          case n ~ Some(character ~ collate) => DataType.CHAR(n.getOrElse(1), Some(character), collate)
          case n ~ None                      => DataType.CHAR(n.getOrElse(1), None, None)
        },
      """
        |===============================================================================
        |Failed to parse char data type.
        |The char Data type must be defined as follows
        |※ char strings are case-insensitive.
        |
        |M is the length of the column in characters. M ranges from 0 to 255. If M is omitted, the length is 1.
        |
        |SEE: https://man.plustar.jp/mysql/string-type-syntax.html
        |
        |example: [NATIONAL] CHAR[(M)] [CHARACTER SET charset_name] [COLLATE collation_name]
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def varcharType: Parser[DataType] =
    customError(
      opt(caseSensitivity("national")) ~> caseSensitivity("varchar") ~>
        argument("VARCHAR", 0, 65535) ~ opt(character ~ opt(collate)) ^^ {
          case n ~ Some(character ~ collate) => DataType.VARCHAR(n, Some(character), collate)
          case n ~ None                      => DataType.VARCHAR(n, None, None)
        },
      """
        |===============================================================================
        |Failed to parse varchar data type.
        |The varchar Data type must be defined as follows
        |※ varchar strings are case-insensitive.
        |
        |M is the length of the column in characters. M ranges from 0 to 65,535.
        |
        |SEE: https://man.plustar.jp/mysql/string-type-syntax.html
        |
        |example: [NATIONAL] VARCHAR(M) [CHARACTER SET charset_name] [COLLATE collation_name]
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def binaryType: Parser[DataType] =
    customError(
      caseSensitivity("binary") ~> opt(argument("BINARY", 0, 255, 1)) ^^ { n =>
        DataType.BINARY(n.getOrElse(1))
      },
      """
        |===============================================================================
        |Failed to parse binary data type.
        |The binary Data type must be defined as follows
        |※ binary strings are case-insensitive.
        |
        |M is the number of bits per value (1 to 255). If M is omitted, the default is 1.
        |
        |SEE: https://man.plustar.jp/mysql/string-type-syntax.html
        |
        |example: BINARY[(M)]
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def varbinaryType: Parser[DataType] =
    customError(
      caseSensitivity("varbinary") ~> argument("VARBINARY", 0, Int.MaxValue) ^^ { n =>
        DataType.VARBINARY(n)
      },
      """
        |===============================================================================
        |Failed to parse varbinary data type.
        |The varbinary Data type must be defined as follows
        |※ varbinary strings are case-insensitive.
        |
        |M is the number of bits per value (1 to max).
        |
        |SEE: https://man.plustar.jp/mysql/string-type-syntax.html
        |
        |example: VARBINARY(M)
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def tinyblobType: Parser[DataType] =
    customError(
      caseSensitivity("tinyblob") ^^ (_ => DataType.TINYBLOB()),
      """
        |===============================================================================
        |Failed to parse tinyblob data type.
        |The tinyblob Data type must be defined as follows
        |※ tinyblob strings are case-insensitive.
        |
        |SEE: https://man.plustar.jp/mysql/string-type-syntax.html
        |
        |example: TINYBLOB
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def tinytextType: Parser[DataType] =
    customError(
      caseSensitivity("tinytext") ~> opt(character ~ opt(collate)) ^^ {
        case Some(character ~ collate) => DataType.TINYTEXT(Some(character), collate)
        case None                      => DataType.TINYTEXT(None, None)
      },
      """
        |===============================================================================
        |Failed to parse tinytext data type.
        |The tinytext Data type must be defined as follows
        |※ tinytext strings are case-insensitive.
        |
        |SEE: https://man.plustar.jp/mysql/string-type-syntax.html
        |
        |example: TINYTEXT [CHARACTER SET charset_name] [COLLATE collation_name]
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def blobType: Parser[DataType] =
    customError(
      caseSensitivity("blob") ~> opt(argument("BLOB", 0, 4294967295L)) ^^ { n =>
        DataType.BLOB(n)
      },
      """
        |===============================================================================
        |Failed to parse blob data type.
        |The blob Data type must be defined as follows
        |※ blob strings are case-insensitive.
        |
        |SEE: https://man.plustar.jp/mysql/string-type-syntax.html
        |
        |example: BLOB[(M)]
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def textType: Parser[DataType] =
    customError(
      caseSensitivity("text") ~> opt(argument("TEXT", 0, 255)) ~ opt(character ~ opt(collate)) ^^ {
        case n ~ Some(character ~ collate) => DataType.TEXT(n, Some(character), collate)
        case n ~ None                      => DataType.TEXT(n, None, None)
      },
      """
        |===============================================================================
        |Failed to parse text data type.
        |The text Data type must be defined as follows
        |※ text strings are case-insensitive.
        |
        |SEE: https://man.plustar.jp/mysql/string-type-syntax.html
        |
        |example: TEXT[(M)] [CHARACTER SET charset_name] [COLLATE collation_name]
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def mediumblobType: Parser[DataType] =
    customError(
      caseSensitivity("mediumblob") ^^ (_ => DataType.TINYBLOB()),
      """
        |===============================================================================
        |Failed to parse mediumblob data type.
        |The mediumblob Data type must be defined as follows
        |※ mediumblob strings are case-insensitive.
        |
        |SEE: https://man.plustar.jp/mysql/string-type-syntax.html
        |
        |example: MEDIUMBLOB
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def mediumtextType: Parser[DataType] =
    customError(
      caseSensitivity("mediumtext") ~> opt(character ~ opt(collate)) ^^ {
        case Some(character ~ collate) => DataType.MEDIUMTEXT(Some(character), collate)
        case None                      => DataType.MEDIUMTEXT(None, None)
      },
      """
        |===============================================================================
        |Failed to parse mediumtext data type.
        |The mediumtext Data type must be defined as follows
        |※ mediumtext strings are case-insensitive.
        |
        |SEE: https://man.plustar.jp/mysql/string-type-syntax.html
        |
        |example: MEDIUMTEXT [CHARACTER SET charset_name] [COLLATE collation_name]
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def longblobType: Parser[DataType] =
    customError(
      caseSensitivity("longblob") ^^ (_ => DataType.LONGBLOB()),
      """
        |===============================================================================
        |Failed to parse longblob data type.
        |The longblob Data type must be defined as follows
        |※ longblob strings are case-insensitive.
        |
        |SEE: https://man.plustar.jp/mysql/string-type-syntax.html
        |
        |example: LONGBLOB
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def longtextType: Parser[DataType] =
    customError(
      caseSensitivity("longtext") ~> opt(character ~ opt(collate)) ^^ {
        case Some(character ~ collate) => DataType.LONGTEXT(Some(character), collate)
        case None                      => DataType.LONGTEXT(None, None)
      },
      """
        |===============================================================================
        |Failed to parse longtext data type.
        |The longtext Data type must be defined as follows
        |※ longtext strings are case-insensitive.
        |
        |SEE: https://man.plustar.jp/mysql/string-type-syntax.html
        |
        |example: LONGTEXT [CHARACTER SET charset_name] [COLLATE collation_name]
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def dateType: Parser[DataType] =
    customError(
      caseSensitivity("date") ^^ (_ => DataType.DATE()),
      """
        |===============================================================================
        |Failed to parse date data type.
        |The date Data type must be defined as follows
        |※ date strings are case-insensitive.
        |
        |SEE: https://man.plustar.jp/mysql/date-and-time-type-syntax.html
        |
        |example: DATE
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def datetimeType: Parser[DataType] =
    customError(
      caseSensitivity("datetime") ~> opt(argument("DATETIME", 0, 6)) ^^ { fsp =>
        DataType.DATETIME(fsp)
      },
      """
        |===============================================================================
        |Failed to parse datetime data type.
        |The datetime Data type must be defined as follows
        |※ datetime strings are case-insensitive.
        |
        |If an fsp value is specified, it must be in the range of 0 to 6.
        |
        |SEE: https://man.plustar.jp/mysql/date-and-time-type-syntax.html
        |
        |example: DATETIME[(fsp)]
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def timestampType: Parser[DataType] =
    customError(
      caseSensitivity("timestamp") ~> opt(argument("TIMESTAMP", 0, 6)) ^^ { fsp =>
        DataType.TIMESTAMP(fsp)
      },
      """
        |===============================================================================
        |Failed to parse timestamp data type.
        |The timestamp Data type must be defined as follows
        |※ timestamp strings are case-insensitive.
        |
        |If an fsp value is specified, it must be in the range of 0 to 6.
        |
        |SEE: https://man.plustar.jp/mysql/date-and-time-type-syntax.html
        |
        |example: TIMESTAMP[(fsp)]
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def timeType: Parser[DataType] =
    customError(
      caseSensitivity("time") ~> opt(argument("TIME", 0, 6)) ^^ { fsp =>
        DataType.TIME(fsp)
      },
      """
        |===============================================================================
        |Failed to parse time data type.
        |The time Data type must be defined as follows
        |※ time strings are case-insensitive.
        |
        |If an fsp value is specified, it must be in the range of 0 to 6.
        |
        |SEE: https://man.plustar.jp/mysql/date-and-time-type-syntax.html
        |
        |example: TIME[(fsp)]
        |==============================================================================
        |""".stripMargin
    )

  private[ldbc] def yearType: Parser[DataType] =
    customError(
      caseSensitivity("year") ~> opt("(" ~> "4" ~> ")") ^^ (_ => DataType.YEAR()),
      """
        |===============================================================================
        |Failed to parse year data type.
        |The year Data type must be defined as follows
        |※ year strings are case-insensitive.
        |
        |SEE: https://man.plustar.jp/mysql/date-and-time-type-syntax.html
        |
        |example: YEAR[(4)]
        |==============================================================================
        |""".stripMargin
    )
