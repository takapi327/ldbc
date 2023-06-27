/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator

import ldbc.generator.model.*
import ldbc.generator.parser.KeyParser

trait TableParser extends KeyParser:

  private def temporary: Parser[String] = caseSensitivity("temporary") ^^ (_.toUpperCase)
  private def table:     Parser[String] = caseSensitivity("table") ^^ (_.toUpperCase)

  private def keyValue[T](keyParser: Parser[String], valueParser: Parser[T]): Parser[T] =
    keyParser ~> opt("=") ~> valueParser

  private def tableOption: Parser[Table.Options] =
    keyValue(caseSensitivity("autoextend_size"), sqlIdent) ^^ Table.Options.AutoExtendSize.apply |
      keyValue(caseSensitivity("auto_increment"), digit) ^^ Table.Options.AutoIncrement.apply |
      keyValue(caseSensitivity("avg_row_length"), sqlIdent) ^^ Table.Options.AVGRowLength.apply |
      opt(caseSensitivity("default")) ~> character ^^ Table.Options.Character.apply |
      keyValue(caseSensitivity("checksum"), digit) ^^ {
        case value: (0 | 1) => Table.Options.CheckSum(value)
        case unknown =>
          throw new IllegalArgumentException(
            s"$unknown is not a value that can be set in the checksum; the checksum must be one of the values 0 or 1."
          )
      } |
      opt(caseSensitivity("default")) ~> collate ^^ Table.Options.Collate.apply |
      keyValue(caseSensitivity("comment"), stringLiteral) ^^ Table.Options.Comment.apply |
      keyValue(caseSensitivity("compression"), sqlIdent) ^^ {
        case value: ("ZLIB" | "LZ4" | "NONE") => Table.Options.Compression(value)
        case unknown =>
          throw new IllegalArgumentException(
            s"$unknown is not a value that can be set in the compression; the checksum must be one of the values ZLIB, LZ4 or NONE."
          )
      } |
      keyValue(caseSensitivity("connection"), sqlIdent) ^^ Table.Options.Connection.apply |
      keyValue(
        caseSensitivity("data") | caseSensitivity("index") ~> caseSensitivity("directory"),
        sqlIdent
      ) ^^ Table.Options.Directory.apply |
      keyValue(caseSensitivity("delay_key_write"), digit) ^^ {
        case value: (0 | 1) => Table.Options.DelayKeyWrite(value)
        case unknown =>
          throw new IllegalArgumentException(
            s"$unknown is not a value that can be set in the delay_key_write; the checksum must be one of the values 0 or 1."
          )
      } |
      keyValue(caseSensitivity("encryption"), sqlIdent) ^^ {
        case value: ("Y" | "N") => Table.Options.Encryption(value)
        case unknown =>
          throw new IllegalArgumentException(
            s"$unknown is not a value that can be set in the encryption; the checksum must be one of the values Y or N."
          )
      } |
      keyValue(caseSensitivity("engine"), sqlIdent) ^^ Table.Options.Engine.apply |
      keyValue(caseSensitivity("engine_attribute"), sqlIdent) ^^ Table.Options.EngineAttribute.apply |
      keyValue(caseSensitivity("insert_method"), sqlIdent) ^^ {
        case value: ("NO" | "FIRST" | "LAST") => Table.Options.InsertMethod(value)
        case unknown =>
          throw new IllegalArgumentException(
            s"$unknown is not a value that can be set in the insert_method; the checksum must be one of the values NO, FIRST or LAST."
          )
      } |
      keyValue(caseSensitivity("key_block_size"), sqlIdent) ^^ Table.Options.KeyBlockSize.apply |
      keyValue(caseSensitivity("max_rows"), digit) ^^ Table.Options.MaxRows.apply |
      keyValue(caseSensitivity("min_rows"), digit) ^^ Table.Options.MinRows.apply |
      keyValue(caseSensitivity("pack_keys"), sqlIdent) ^^ {
        case value: ("0" | "1" | "DEFAULT") => Table.Options.PackKeys(value)
        case unknown =>
          throw new IllegalArgumentException(
            s"$unknown is not a value that can be set in the pack_keys; the checksum must be one of the values 0, 1 or DEFAULT."
          )
      } |
      keyValue(caseSensitivity("password"), sqlIdent) ^^ Table.Options.Password.apply |
      keyValue(caseSensitivity("row_format"), sqlIdent) ^^ {
        case value: ("DEFAULT" | "DYNAMIC" | "FIXED" | "COMPRESSED" | "REDUNDANT" | "COMPACT") =>
          Table.Options.RowFormat(value)
        case unknown =>
          throw new IllegalArgumentException(
            s"$unknown is not a value that can be set in the pack_keys; the checksum must be one of the values DEFAULT, DYNAMIC, FIXED, COMPRESSED, REDUNDANT, or COMPACT."
          )
      } |
      keyValue(
        caseSensitivity("secondary_engine_attribute"),
        sqlIdent
      ) ^^ Table.Options.SecondaryEngineAttribute.apply |
      keyValue(caseSensitivity("stats_auto_recalc"), sqlIdent) ^^ {
        case value: ("0" | "1" | "DEFAULT") => Table.Options.StatsAutoRecalc(value)
        case unknown =>
          throw new IllegalArgumentException(
            s"$unknown is not a value that can be set in the stats_auto_recalc; the checksum must be one of the values 0, 1 or DEFAULT."
          )
      } |
      keyValue(caseSensitivity("stats_persistent"), sqlIdent) ^^ {
        case value: ("0" | "1" | "DEFAULT") => Table.Options.StatsPersistent(value)
        case unknown =>
          throw new IllegalArgumentException(
            s"$unknown is not a value that can be set in the stats_persistent; the checksum must be one of the values 0, 1 or DEFAULT."
          )
      } |
      keyValue(caseSensitivity("stats_sample_pages"), sqlIdent) ^^ Table.Options.StatsSamplePages.apply |
      caseSensitivity("tablespace") ~> sqlIdent ~ opt(
        caseSensitivity("storage") ~> caseSensitivity("disk") | caseSensitivity("memory")
      ) ^^ {
        case name ~ storage =>
          Table.Options.Tablespace(
            name,
            storage.map {
              case "DISK"   => "DISK"
              case "MEMORY" => "MEMORY"
              case unknown =>
                throw new IllegalArgumentException(
                  s"$unknown is not a value that can be set in the tablespace storage; the checksum must be one of the values DISK or MEMORY."
                )
            }
          )
      } |
      keyValue(caseSensitivity("union"), repsep(sqlIdent, ",")) ^^ Table.Options.Union.apply

  protected def tableStatements: Parser[Table.CreateStatement | Table.DropStatement] = createStatement | dropStatement

  private def createStatement: Parser[Table.CreateStatement] =
    opt(comment) ~> create ~> opt(comment) ~> opt(temporary) ~> opt(comment) ~> table ~>
      opt(comment) ~> opt(ifNotExists) ~> opt(comment) ~> sqlIdent ~ opt(comment) ~
      "(" ~ repsep(columnDefinition | keyDefinitions, ",") ~ opt(comment) ~ ")" ~ opt(rep1(tableOption)) <~ ";" ^^ {
        case tableName ~ _ ~ _ ~ objects ~ _ ~ _ ~ options =>
          val columnDefs = objects.filter(_.isInstanceOf[ColumnDefinition]).asInstanceOf[List[ColumnDefinition]]
          val keyDefs    = objects.filter(_.isInstanceOf[Key]).asInstanceOf[List[Key]]
          Table.CreateStatement(tableName, columnDefs, keyDefs, options)
      }

  private def dropStatement: Parser[Table.DropStatement] =
    opt(comment) ~> drop ~> opt(comment) ~> opt(ifNotExists) ~> opt(comment) ~> sqlIdent <~ opt(comment) <~ ";" ^^ {
      tableName => Table.DropStatement(tableName)
    }
