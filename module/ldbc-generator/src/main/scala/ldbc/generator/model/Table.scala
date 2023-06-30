/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.model

object Table:

  trait Options

  object Options:

    case class AutoExtendSize(value: Int)                extends Options
    case class AutoIncrement(value: Int)                    extends Options
    case class AVGRowLength(value: Int)                  extends Options
    case class Character(value: String)                     extends Options
    case class CheckSum(value: 0 | 1)                       extends Options
    case class Collate(value: String)                       extends Options
    case class Comment(value: String)                       extends Options
    case class Compression(value: "ZLIB" | "LZ4" | "NONE")  extends Options
    case class Connection(value: String)                    extends Options
    case class Directory(value: String)                     extends Options
    case class DelayKeyWrite(value: 0 | 1)                  extends Options
    case class Encryption(value: "Y" | "N")                 extends Options
    case class Engine(value: "InnoDB" | "MyISAM" | "MEMORY" | "CSV" | "ARCHIVE" | "EXAMPLE" | "FEDERATED" | "HEAP" | "MERGE" | "NDB")                        extends Options
    case class EngineAttribute(value: String)               extends Options
    case class InsertMethod(value: "NO" | "FIRST" | "LAST") extends Options
    case class KeyBlockSize(value: Int)                     extends Options
    case class MaxRows(value: Long)                          extends Options
    case class MinRows(value: Long)                          extends Options
    case class PackKeys(value: "0" | "1" | "DEFAULT")       extends Options
    case class RowFormat(value: "DEFAULT" | "DYNAMIC" | "FIXED" | "COMPRESSED" | "REDUNDANT" | "COMPACT")
      extends Options
    case class SecondaryEngineAttribute(value: String)                      extends Options
    case class StatsAutoRecalc(value: "0" | "1" | "DEFAULT")                extends Options
    case class StatsPersistent(value: "0" | "1" | "DEFAULT")                extends Options
    case class StatsSamplePages(value: Int)                              extends Options
    case class Tablespace(name: String, storage: Option["DISK" | "MEMORY"]) extends Options
    case class Union(tableNames: List[String])                              extends Options

  case class CreateStatement(
    tableName:         String,
    columnDefinitions: List[ColumnDefinition],
    keyDefinitions:    List[Key],
    options:           Option[List[Options]]
  )

  case class DropStatement(tableName: String)
