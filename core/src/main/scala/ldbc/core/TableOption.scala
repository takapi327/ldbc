/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
  */

package ldbc.core

trait TableOption:

  def queryString: String

object TableOption:
  case class AutoExtendSize(value: Int) extends TableOption:
    override def queryString: String = s"AUTOEXTEND_SIZE=$value"

  case class AutoIncrement(value: Int) extends TableOption:
    override def queryString: String = s"AUTO_INCREMENT=$value"

  case class AVGRowLength(value: Int) extends TableOption:
    override def queryString: String = s"AVG_ROW_LENGTH=$value"

  case class CheckSum(value: 0 | 1) extends TableOption:
    override def queryString: String = s"CHECKSUM=$value"

  case class Comment(value: String) extends TableOption:
    override def queryString: String = s"COMMENT='$value'"

  case class Compression(value: "ZLIB" | "LZ4" | "NONE") extends TableOption:
    override def queryString: String = s"COMPRESSION='$value'"

  case class Connection(value: String) extends TableOption:
    override def queryString: String = s"CONNECTION='$value'"

  case class Directory(`type`: "DATA" | "INDEX", value: String) extends TableOption:
    override def queryString: String = s"${ `type` } DIRECTORY='$value'"

  case class DelayKeyWrite(value: 0 | 1) extends TableOption:
    override def queryString: String = s"DELAY_KEY_WRITE=$value"

  case class Encryption(value: "Y" | "N") extends TableOption:
    override def queryString: String = s"ENCRYPTION='$value'"

  case class Engine(
    value: "InnoDB" | "MyISAM" | "MEMORY" | "CSV" | "ARCHIVE" | "EXAMPLE" | "FEDERATED" | "HEAP" | "MERGE" | "NDB"
  ) extends TableOption:
    override def queryString: String = s"ENGINE=$value"

  case class EngineAttribute(value: String) extends TableOption:
    override def queryString: String = s"ENGINE_ATTRIBUTE='$value'"

  case class InsertMethod(value: "NO" | "FIRST" | "LAST") extends TableOption:
    override def queryString: String = s"INSERT_METHOD=$value"

  case class KeyBlockSize(value: 1 | 2 | 4 | 8 | 16) extends TableOption:
    override def queryString: String = s"KEY_BLOCK_SIZE=$value"

  case class MaxRows(value: Long) extends TableOption:
    override def queryString: String = s"MAX_ROWS=$value"

  case class MinRows(value: Long) extends TableOption:
    override def queryString: String = s"MIN_ROWS=$value"

  case class PackKeys(value: "0" | "1" | "DEFAULT") extends TableOption:
    override def queryString: String = s"PACK_KEYS=$value"

  case class RowFormat(value: "DEFAULT" | "DYNAMIC" | "FIXED" | "COMPRESSED" | "REDUNDANT" | "COMPACT")
    extends TableOption:
    override def queryString: String = s"ROW_FORMAT=$value"

  case class SecondaryEngineAttribute(value: String) extends TableOption:
    override def queryString: String = s"SECONDARY_ENGINE_ATTRIBUTE='$value'"

  case class StatsAutoRecalc(value: "0" | "1" | "DEFAULT") extends TableOption:
    override def queryString: String = s"STATS_AUTO_RECALC=$value"

  case class StatsPersistent(value: "0" | "1" | "DEFAULT") extends TableOption:
    override def queryString: String = s"STATS_PERSISTENT=$value"

  case class StatsSamplePages(value: Int) extends TableOption:
    override def queryString: String = s"STATS_SAMPLE_PAGES=$value"

  case class Tablespace(name: String, storage: Option["DISK" | "MEMORY"]) extends TableOption:
    override def queryString: String = storage.fold(s"TABLESPACE $name")(v => s"TABLESPACE $name STORAGE $v")

  case class Union(tableNames: List[String]) extends TableOption:
    override def queryString: String = s"UNION ${ tableNames.mkString(",") }"

  private[ldbc] case class Character(value: String) extends TableOption:
    override def queryString: String = s"DEFAULT CHARACTER SET=$value"

  private[ldbc] case class Collate(value: String) extends TableOption:
    override def queryString: String = s"DEFAULT COLLATE=$value"
