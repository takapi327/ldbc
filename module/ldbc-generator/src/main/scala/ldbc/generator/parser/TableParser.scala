/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.parser

import ldbc.generator.model.*

trait TableParser extends KeyParser:

  private def temporary: Parser[String] = caseSensitivity("temporary") ^^ (_.toUpperCase)
  private def table:     Parser[String] = caseSensitivity("table") ^^ (_.toUpperCase)

  private def keyValue[T](keyParser: Parser[String], valueParser: Parser[T]): Parser[T] =
    keyParser ~> opt("=") ~> valueParser

  /**
   * The AUTOEXTEND_SIZE option is a feature added in MySQL database version 8.0.23 and later. This is an option to set whether the database file size is automatically extended.
   *
   * The AUTOEXTEND_SIZE option is a feature to automatically increase the size of the database, allowing for flexibility in accommodating data growth.
   *
   * SEE: https://dev.mysql.com/doc/refman/8.0/ja/innodb-tablespace-autoextend-size.html
   */
  private def autoextendSize: Parser[Table.Options] =
    customError(
      keyValue(
        caseSensitivity("autoextend_size"),
        """(4|8|12|16|20|24|28|32|36|40|44|48|52|56|60|64)""".r <~ caseSensitivity("m")
      ) ^^ (v => Table.Options.AutoExtendSize(v.toInt)),
      """
        |======================================================
        |There is an error in the autoextend_size format.
        |Please correct the format according to the following.
        |
        |The value set for AUTOEXTEND_SIZE must be a multiple of 4.
        |The minimum is 4 and the maximum is 64.
        |
        |example: AUTOEXTEND_SIZE[=]'size'M
        |======================================================
        |""".stripMargin
    )

  /**
   * The initial AUTO_INCREMENT value for the table.
   *
   * In MySQL 8.0, this works for MyISAM, MEMORY, InnoDB, and ARCHIVE tables. To set the initial auto-increment value for engines that do not support the AUTO_INCREMENT table option,
   * insert a "dummy" row with a value one less than the desired value after creating the table, then delete the dummy row.
   */
  private def autoIncrement: Parser[Table.Options] =
    customError(
      keyValue(caseSensitivity("auto_increment"), digit) ^^ Table.Options.AutoIncrement.apply,
      """
        |======================================================
        |There is an error in the auto_increment format.
        |Please correct the format according to the following.
        |
        |Only numbers can be set for size.
        |It cannot be set smaller than the maximum value currently in the column.
        |
        |example: AUTO_INCREMENT[=]'size'
        |======================================================
        |""".stripMargin
    )

  /**
   * An approximation of the average row length of the table. You only need to set this for large tables with variable size rows.
   *
   * The AVG_ROW_LENGTH option is a setting to specify the average length of a single row. This setting allows for efficient use of database space.
   */
  private def avgRowLength: Parser[Table.Options] =
    customError(
      keyValue(caseSensitivity("avg_row_length"), digit) ^^ Table.Options.AVGRowLength.apply,
      """
        |======================================================
        |There is an error in the avg_row_length format.
        |Please correct the format according to the following.
        |
        |Only numbers can be set for size.
        |
        |example: AVG_ROW_LENGTH[=]'size'
        |======================================================
        |""".stripMargin
    )

  /**
   * Specifies the default character set for the table. CHARSET is a synonym for CHARACTER SET. If the character set name is DEFAULT, the database character set is used.
   */
  private def characterSet: Parser[Table.Options] =
    customError(
      opt(caseSensitivity("default")) ~> character ^^ Table.Options.Character.apply,
      """
        |======================================================
        |There is an error in the character format.
        |Please correct the format according to the following.
        |
        |Only numbers can be set for size.
        |
        |example: [DEFAULT] {CHARACTER [SET] | CHARSET} [=] 'string'
        |======================================================
        |""".stripMargin
    )

  private def tableOption: Parser[Table.Options] =
    autoextendSize | autoIncrement | avgRowLength | characterSet |
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
      engineAttribute ^^ Table.Options.EngineAttribute.apply |
      keyValue(caseSensitivity("insert_method"), sqlIdent) ^^ {
        case value: ("NO" | "FIRST" | "LAST") => Table.Options.InsertMethod(value)
        case unknown =>
          throw new IllegalArgumentException(
            s"$unknown is not a value that can be set in the insert_method; the checksum must be one of the values NO, FIRST or LAST."
          )
      } |
      keyBlockSize ^^ Table.Options.KeyBlockSize.apply |
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
