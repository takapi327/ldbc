/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.parser

import ldbc.codegen.model.*

/**
 * Parser for parsing Table definitions.
 */
trait TableParser extends KeyParser:

  private def temporary: Parser[String] = caseSensitivity("temporary") ^^ (_.toUpperCase)
  private def table:     Parser[String] = caseSensitivity("table") ^^ (_.toUpperCase)

  /**
   * The AUTOEXTEND_SIZE option is a feature added in MySQL database version 8.0.23 and later. This is an option to set
   * whether the database file size is automatically extended.
   *
   * The AUTOEXTEND_SIZE option is a feature to automatically increase the size of the database, allowing for
   * flexibility in accommodating data growth.
   *
   * SEE: https://dev.mysql.com/doc/refman/8.0/ja/innodb-tablespace-autoextend-size.html
   */
  private def autoextendSize: Parser[TableOption] =
    customError(
      keyValue(
        caseSensitivity("autoextend_size"),
        """(4|8|12|16|20|24|28|32|36|40|44|48|52|56|60|64)""".r <~ caseSensitivity("m")
      ) ^^ (v => TableOption.AutoExtendSize(v.toInt)),
      input => s"""
        |======================================================
        |There is an error in the autoextend_size format.
        |Please correct the format according to the following.
        |
        |The value set for AUTOEXTEND_SIZE must be a multiple of 4.
        |The minimum is 4 and the maximum is 64.
        |
        |${ input.pos.longString } ($fileName:${ input.pos.line }:${ input.pos.column })
        |example: AUTOEXTEND_SIZE[=]'size'M
        |======================================================
        |""".stripMargin
    )

  /**
   * The initial AUTO_INCREMENT value for the table.
   *
   * In MySQL 8.0, this works for MyISAM, MEMORY, InnoDB, and ARCHIVE tables. To set the initial auto-increment value
   * for engines that do not support the AUTO_INCREMENT table option, insert a "dummy" row with a value one less than
   * the desired value after creating the table, then delete the dummy row.
   */
  private def autoIncrement: Parser[TableOption] =
    customError(
      keyValue(caseSensitivity("auto_increment"), digit) ^^ TableOption.AutoIncrement.apply,
      input => s"""
        |======================================================
        |There is an error in the auto_increment format.
        |Please correct the format according to the following.
        |
        |Only numbers can be set for size.
        |It cannot be set smaller than the maximum value currently in the column.
        |
        |${ input.pos.longString } ($fileName:${ input.pos.line }:${ input.pos.column })
        |example: AUTO_INCREMENT[=]'size'
        |======================================================
        |""".stripMargin
    )

  /**
   * An approximation of the average row length of the table. You only need to set this for large tables with variable
   * size rows.
   *
   * The AVG_ROW_LENGTH option is a setting to specify the average length of a single row. This setting allows for
   * efficient use of database space.
   */
  private def avgRowLength: Parser[TableOption] =
    customError(
      keyValue(caseSensitivity("avg_row_length"), digit) ^^ TableOption.AVGRowLength.apply,
      failureMessage("avg_row_length", "AVG_ROW_LENGTH[=]'number'")
    )

  /**
   * Specifies the default character set for the table. CHARSET is a synonym for CHARACTER SET. If the character set
   * name is DEFAULT, the database character set is used.
   */
  private def characterSet: Parser[TableOption] =
    customError(
      opt(caseSensitivity("default")) ~> character ^^ TableOption.Character.apply,
      failureMessage("character", " [DEFAULT] {CHARACTER [SET] | CHARSET} [=] 'string'")
    )

  /**
   * The CHECKSUM option is one of the features used in the MySQL database. It is used to check data integrity.
   *
   * The CHECKSUM option has two setting values, 0 and 1. If set to 0, no checksum calculation is performed. When set to
   * 0, no checksum calculation is performed, i.e., data integrity is not checked. On the other hand, when set to 1, the
   * checksum is calculated and data integrity is checked.
   */
  private def checksum: Parser[TableOption] =
    customError(
      keyValue(caseSensitivity("checksum"), """(0|1)""".r) ^^ {
        case "0" => TableOption.CheckSum(0)
        case "1" => TableOption.CheckSum(1)
      },
      failureMessage("checksum", "CHECKSUM [=] {0 | 1}")
    )

  /**
   * Specifies the default collation for the table.
   */
  private def collateSet: Parser[TableOption] =
    customError(
      opt(caseSensitivity("default")) ~> collate ^^ TableOption.Collate.apply,
      failureMessage("collate", "[DEFAULT] COLLATE[=]'string'")
    )

  /**
   * It is a comment of the table and can be up to 2048 characters in length.
   */
  private def commentOption: Parser[TableOption] =
    customError(
      keyValue(caseSensitivity("comment"), stringLiteral) ^^ TableOption.Comment.apply,
      failureMessage("comment", "COMMENT[=]'string'")
    )

  /**
   * The COMPRESSION option is a feature for compressing and storing data. This reduces the size of the database and
   * saves space. Different setting values will change the compression algorithm, resulting in different compression
   * speeds and efficiencies.
   */
  private def compression: Parser[TableOption] =
    customError(
      keyValue(
        caseSensitivity("compression"),
        caseSensitivity("zlib") | caseSensitivity("lz4") | caseSensitivity("none")
      ) ^^ {
        case str if str.toUpperCase == "ZLIB" => TableOption.Compression("ZLIB")
        case str if str.toUpperCase == "LZ4"  => TableOption.Compression("LZ4")
        case str if str.toUpperCase == "NONE" => TableOption.Compression("NONE")
      },
      failureMessage("compression", "COMPRESSION[=]{ZLIB | LZ4 | NONE}")
    )

  /**
   * The CONNECTION option is one of the settings used by the MySQL database. It is used to configure settings related
   * to the connection to the database.
   */
  private def connection: Parser[TableOption] =
    customError(
      keyValue(caseSensitivity("connection"), stringLiteral) ^^ TableOption.Connection.apply,
      input =>
        s"""
        |======================================================
        |There is an error in the connection format.
        |Please correct the format according to the following.
        |
        |scheme: The recognized connection protocol. At this time, only mysql is supported as a scheme value.
        |
        |user_name: User name for the connection. This user must have been created on the remote server and must have the appropriate privileges to perform the required actions (SELECT, INSERT, UPDATE, etc.) on the remote table.
        |
        |password: (optional) Password corresponding to user_name.
        |
        |host_name: Host name or IP address of the remote server.
        |
        |port_num: (Optional) Port number of the remote server. Default is 3306.
        |
        |db_name: Name of the database holding the remote table.
        |
        |tbl_name: Name of the remote table. The names of the local and remote tables do not need to match.
        |
        |${ input.pos.longString } ($fileName:${ input.pos.line }:${ input.pos.column })
        |example: CONNECTION [=] 'scheme://user_name[:password]@host_name[:port_num]/db_name/tbl_name'
        |======================================================
        |""".stripMargin
    )

  /**
   * DATA DIRECTORY and INDEX DIRECTORY are options used in the MySQL database. These options are used to specify where
   * data and indexes are stored.
   */
  private def directory: Parser[TableOption] =
    customError(
      (caseSensitivity("data") | caseSensitivity("index")) ~ keyValue(
        caseSensitivity("directory"),
        stringLiteral
      ) ^^ {
        case str ~ value if str.toUpperCase == "DATA"  => TableOption.Directory("DATA", value)
        case str ~ value if str.toUpperCase == "INDEX" => TableOption.Directory("INDEX", value)
      },
      failureMessage("directory", "{DATA | INDEX} DIRECTORY[=]'string'")
    )

  /**
   * Specifies how to use delayed key writing. This applies only to MyISAM tables. Delayed key writes do not flush the
   * key buffer between writes.
   */
  private def delayKeyWrite: Parser[TableOption] =
    customError(
      keyValue(caseSensitivity("delay_key_write"), """(0|1)""".r) ^^ {
        case "0" => TableOption.DelayKeyWrite(0)
        case "1" => TableOption.DelayKeyWrite(1)
      },
      failureMessage("delay_key_write", "DELAY_KEY_WRITE[=]{0 | 1}")
    )

  /**
   * The ENCRYPTION option is one of the settings used in the MySQL database. It is used to encrypt (encode) data.
   */
  protected def encryption: Parser[TableOption.Encryption] =
    customError(
      keyValue(caseSensitivity("encryption"), caseSensitivity("y") | caseSensitivity("n")) ^^ {
        case str if str.toUpperCase == "Y" => TableOption.Encryption("Y")
        case str if str.toUpperCase == "N" => TableOption.Encryption("N")
      },
      failureMessage("encryption", "ENCRYPTION[=]{Y | N}")
    )

  /**
   * Option to specify storage engine for table
   */
  private def engine: Parser[TableOption] =
    customError(
      keyValue(
        caseSensitivity("engine"),
        "InnoDB" | "MyISAM" | "MEMORY" | "CSV" | "ARCHIVE" | "EXAMPLE" | "FEDERATED" | "HEAP" | "MERGE" | "NDB"
      ) ^^ {
        case "InnoDB"    => TableOption.Engine("InnoDB")
        case "MyISAM"    => TableOption.Engine("MyISAM")
        case "MEMORY"    => TableOption.Engine("MEMORY")
        case "CSV"       => TableOption.Engine("CSV")
        case "ARCHIVE"   => TableOption.Engine("ARCHIVE")
        case "EXAMPLE"   => TableOption.Engine("EXAMPLE")
        case "FEDERATED" => TableOption.Engine("FEDERATED")
        case "HEAP"      => TableOption.Engine("HEAP")
        case "MERGE"     => TableOption.Engine("MERGE")
        case "NDB"       => TableOption.Engine("NDB")
      },
      failureMessage(
        "engine",
        "ENGINE[=]{InnoDB | MyISAM | MEMORY | CSV | ARCHIVE | EXAMPLE | FEDERATED | HEAP | MERGE | NDB}"
      )
    )

  /**
   * When inserting data into a MERGE table, INSERT_METHOD must be used to specify the table into which the rows are to
   * be inserted. INSERT_METHOD is a useful option only for MERGE tables.
   */
  private def insertMethod: Parser[TableOption] =
    customError(
      keyValue(
        caseSensitivity("insert_method"),
        caseSensitivity("NO") | caseSensitivity("FIRST") | caseSensitivity("LAST")
      ) ^^ {
        case str if str.toUpperCase == "NO"    => TableOption.InsertMethod("NO")
        case str if str.toUpperCase == "FIRST" => TableOption.InsertMethod("FIRST")
        case str if str.toUpperCase == "LAST"  => TableOption.InsertMethod("LAST")
      },
      failureMessage("insert_method", "INSERT_METHOD[=]{NO | FIRST | LAST}")
    )

  /**
   * The maximum number of rows you plan to store in the table. This is not a strong limit, but rather a hint to the
   * storage engine that the table must be able to store at least this number of rows.
   */
  private def maxRows: Parser[TableOption] =
    customError(
      keyValue(caseSensitivity("max_rows"), """-?\d+""".r.filter(_.toLong < 4294967296L)) ^^ { digit =>
        TableOption.MaxRows(digit.toLong)
      },
      failureMessage("max_rows", "MAX_ROWS[=]'size'")
    )

  /**
   * The minimum number of rows you plan to store in the table. MEMORY The storage engine uses this option as a hint
   * regarding memory usage.
   */
  private def minRows: Parser[TableOption] =
    customError(
      keyValue(caseSensitivity("min_rows"), """-?\d+""".r) ^^ { digit =>
        TableOption.MinRows(digit.toLong)
      },
      failureMessage("min_rows", "MIN_ROWS[=]'size'")
    )

  /**
   * Valid only for MyISAM tables. Set this option to 1 for smaller indexes. This usually results in slower updates and
   * faster reads. Setting this option to 0 disables all packing of keys. Setting it to DEFAULT tells the storage engine
   * to pack only long CHAR, VARCHAR, BINARY, or VARBINARY columns.
   */
  private def packKeys: Parser[TableOption] =
    customError(
      keyValue(
        caseSensitivity("pack_keys"),
        "0" | "1" | caseSensitivity("default")
      ) ^^ {
        case "0"                                 => TableOption.PackKeys("0")
        case "1"                                 => TableOption.PackKeys("1")
        case str if str.toUpperCase == "DEFAULT" => TableOption.PackKeys("DEFAULT")
      },
      failureMessage("pack_keys", "PACK_KEYS[=]{0 | 1 | DEFAULT}")
    )

  /**
   * Defines the physical format in which the rows will be stored.
   */
  private def rowFormat: Parser[TableOption] =
    customError(
      keyValue(
        caseSensitivity("row_format"),
        caseSensitivity("default") | caseSensitivity("dynamic") | caseSensitivity("fixed") | caseSensitivity(
          "redundant"
        ) | caseSensitivity("compact")
      ) ^^ {
        case str if str.toUpperCase == "DEFAULT"    => TableOption.RowFormat("DEFAULT")
        case str if str.toUpperCase == "DYNAMIC"    => TableOption.RowFormat("DYNAMIC")
        case str if str.toUpperCase == "FIXED"      => TableOption.RowFormat("FIXED")
        case str if str.toUpperCase == "COMPRESSED" => TableOption.RowFormat("COMPRESSED")
        case str if str.toUpperCase == "REDUNDANT"  => TableOption.RowFormat("REDUNDANT")
        case str if str.toUpperCase == "COMPACT"    => TableOption.RowFormat("COMPACT")
      },
      failureMessage("row_format", "ROW_FORMAT[=]{DEFAULT | DYNAMIC | FIXED | COMPRESSED | REDUNDANT | COMPACT}")
    )

  /**
   * Specifies whether the persistent statistics for InnoDB tables should be automatically recalculated. With the value
   * DEFAULT, the table's persistent statistics settings are determined by the innodb_stats_auto_recalc configuration
   * option. A value of 1 specifies that the statistics will be recalculated when 10% of the data in the table has
   * changed. A value of 0 prevents automatic recalculation of this table. With this setting, to recalculate the
   * statistics after making significant changes to the table, issue an ANALYZE TABLE statement.
   */
  private def statsAutoRecalc: Parser[TableOption] =
    customError(
      keyValue(
        caseSensitivity("stats_auto_recalc"),
        "0" | "1" | caseSensitivity("default")
      ) ^^ {
        case "0"                                 => TableOption.StatsAutoRecalc("0")
        case "1"                                 => TableOption.StatsAutoRecalc("1")
        case str if str.toUpperCase == "DEFAULT" => TableOption.StatsAutoRecalc("DEFAULT")
      },
      failureMessage("stats_auto_recalc", "STATS_AUTO_RECALC[=]{0 | 1 | DEFAULT}")
    )

  /**
   * Specifies whether to enable persistent statistics for InnoDB tables. With the value DEFAULT, the table persistent
   * statistics setting is determined by the innodb_stats_persistent configuration option. A value of 1 enables
   * persistent statistics for the table, while a value of 0 disables this feature.
   */
  private def statsPersistent: Parser[TableOption] =
    customError(
      keyValue(
        caseSensitivity("stats_persistent"),
        "0" | "1" | caseSensitivity("default")
      ) ^^ {
        case "0"                                 => TableOption.StatsPersistent("0")
        case "1"                                 => TableOption.StatsPersistent("1")
        case str if str.toUpperCase == "DEFAULT" => TableOption.StatsPersistent("DEFAULT")
      },
      failureMessage("stats_persistent", "STATS_PERSISTENT[=]{0 | 1 | DEFAULT}")
    )

  /**
   * Number of index pages to sample when estimating cardinality and other statistics (such as those computed by ANALYZE
   * TABLE) for indexed columns.
   */
  private def statsSamplePages: Parser[TableOption] =
    customError(
      keyValue(caseSensitivity("stats_sample_pages"), digit) ^^ TableOption.StatsSamplePages.apply,
      failureMessage("stats_sample_pages", "STATS_SAMPLE_PAGES[=]'size'")
    )

  /**
   * The TABLESPACE clause can be used to create tables in an existing general tablespace, file-per-table tablespace, or
   * system tablespace.
   */
  private def tablespace: Parser[TableOption] =
    customError(
      caseSensitivity("tablespace") ~> sqlIdent ~ opt(
        caseSensitivity("storage") ~> caseSensitivity("disk") | caseSensitivity("memory")
      ) ^^ {
        case name ~ storage =>
          TableOption.Tablespace(
            name,
            storage.map {
              case "DISK"   => "DISK"
              case "MEMORY" => "MEMORY"
            }
          )
      },
      failureMessage("tablespace", "TABLESPACE[=]'string' [STORAGE {DISK | MEMORY}]")
    )

  /**
   * Used to access collections of identical MyISAM tables. This only works with MERGE tables.
   */
  private def union: Parser[TableOption] =
    customError(
      keyValue(caseSensitivity("union"), "(" ~> repsep(sqlIdent, ",") <~ ")") ^^ TableOption.Union.apply,
      failureMessage("union", "UNION[=](table_name, table_name, ...)")
    )

  private def engineAttributeOption: Parser[TableOption] =
    engineAttribute ^^ { v => TableOption.EngineAttribute(v.value) }

  private def keyBlockSizeOption: Parser[TableOption] =
    keyBlockSize ^^ { v => TableOption.KeyBlockSize(v.value) }

  private def tableOption: Parser[TableOption] =
    autoextendSize | autoIncrement | avgRowLength | characterSet | checksum | collateSet |
      commentOption | compression | connection | directory | delayKeyWrite | encryption |
      engine | engineAttributeOption | insertMethod |
      keyBlockSizeOption | maxRows | minRows | packKeys |
      rowFormat | keyValue(
        caseSensitivity("secondary_engine_attribute"),
        sqlIdent
      ) ^^ TableOption.SecondaryEngineAttribute.apply |
      statsAutoRecalc | statsPersistent | statsSamplePages | tablespace | union

  protected def tableStatements: Parser[Table.CreateStatement | Table.DropStatement] =
    createTableStatement | dropTableStatement

  /**
   * Parser for parsing Table create statement.
   *
   * Please refer to the official documentation for MySQL Table create statement. SEE:
   * https://dev.mysql.com/doc/refman/8.0/en/create-table.html
   */
  private[ldbc] def createTableStatement: Parser[Table.CreateStatement] =
    opt(comment) ~> create ~> opt(comment) ~> opt(temporary) ~> opt(comment) ~> table ~>
      opt(comment) ~> opt(ifNotExists) ~> opt(comment) ~> sqlIdent ~ opt(comment) ~
      "(" ~ repsep(columnDefinition | keyDefinitions, ",") ~ opt(comment) ~ ")" ~ opt(rep1(tableOption)) <~ ";" ^^ {
        case tableName ~ _ ~ _ ~ objects ~ _ ~ _ ~ options =>
          val columnDefs = objects.filter(_.isInstanceOf[ColumnDefinition]).asInstanceOf[List[ColumnDefinition]]
          val keyDefs    = objects.filter(_.isInstanceOf[Key]).asInstanceOf[List[Key]]
          Table.CreateStatement(tableName, columnDefs, keyDefs, options)
      }

  /**
   * Parser for parsing Table drop statement.
   *
   * Please refer to the official documentation for MySQL Table drop statement. SEE:
   * https://dev.mysql.com/doc/refman/8.0/en/drop-table.html
   */
  private[ldbc] def dropTableStatement: Parser[Table.DropStatement] =
    customError(
      opt(comment) ~> drop ~> opt(comment) ~> opt(temporary) ~> opt(comment) ~> table ~> opt(comment) ~> opt(
        ifNotExists
      ) ~> opt(comment) ~> sqlIdent
        <~ opt(comment) <~ opt(caseSensitivity("restrict") | caseSensitivity("cascade")) <~ ";" ^^ { tableName =>
          Table.DropStatement(tableName)
        },
      failureMessage("drop statement", "DROP [TEMPORARY] TABLE [IF [NOT] EXISTS] `table_name` [RESTRICT | CASCADE];")
    )
