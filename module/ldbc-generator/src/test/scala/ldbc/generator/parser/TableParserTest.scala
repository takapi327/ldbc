/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.parser

import org.scalatest.flatspec.AnyFlatSpec

class TableParserTest extends AnyFlatSpec, TableParser:

  it should "Table create statement parsing test succeeds." in {
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |);
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """/* comment */ CREATE /* comment */ temporary /* comment */ TABLE /* comment */ IF NOT EXISTS /* comment */ `table` /* comment */ (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |);
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) autoextend_size=8M;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) auto_increment=8;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) avg_row_length=8;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) default character set = utf8mb4;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) checksum 0;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) default collate = utf8mb4_unicode_ci;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) COMMENT='NDB_TABLE=READ_BACKUP=0,PARTITION_BALANCE=FOR_RP_BY_NODE';
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) compression lz4;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) CONNECTION='mysql://fed_user@remote_host:9306/federated/test_table';
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) DATA DIRECTORY = 'test';
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) delay_key_write 1;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) encryption y;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) ENGINE InnoDB;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) INSERT_METHOD FIRST;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) MAX_ROWS 4294967295;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) MIN_ROWS 4294967295;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) PACK_KEYS 0;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) row_format default;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) STATS_AUTO_RECALC 1;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) STATS_PERSISTENT 1;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) STATS_SAMPLE_PAGES 1;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) TABLESPACE innodb_system;
        |""".stripMargin
      ).successful
    )
    assert(
      parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) ENGINE MERGE UNION (t1, t2);
        |""".stripMargin
      ).successful
    )
  }

  it should "Table create statement parsing test fails." in {
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) autoextend_size=2M;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) auto_increment=d;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) avg_row_length=d;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) default character set;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) checksum 2;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) default collate;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) COMMENT="NDB_TABLE=READ_BACKUP=0,PARTITION_BALANCE=FOR_RP_BY_NODE";
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) compression hoge;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) CONNECTION;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) DATA DIRECTORY;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) delay_key_write 2;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) encryption m;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) ENGINE failed;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) INSERT_METHOD failed;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) MAX_ROWS 4294967296;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) MIN_ROWS d;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) PACK_KEYS failed;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) row_format failed;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) STATS_AUTO_RECALC failed;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) STATS_PERSISTENT failed;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) stats_sample_pages failed;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) TABLESPACE innodb_system STORAGE failed;
        |""".stripMargin
      ).successful
    )
    assert(
      !parseAll(
        tableStatements,
        """CREATE TABLE `table` (
        |  `id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT
        |) ENGINE MERGE UNION;
        |""".stripMargin
      ).successful
    )
  }

  it should "Table drop statement parsing test succeeds." in {
    assert(parseAll(tableStatements,
      """
        |DROP IF NOT EXISTS `table`;
        |""".stripMargin).successful)
    assert(parseAll(tableStatements,
      """
        |DROP `table`;
        |""".stripMargin).successful)
    assert(parseAll(tableStatements,
      """
        |/* comment */ DROP /* comment */ IF NOT EXISTS /* comment */ `table`;
        |""".stripMargin).successful)
  }

  it should "Table drop statement parsing test fails" in {
    assert(!parseAll(tableStatements,
      """
        |DROP TABLE `table`;
        |""".stripMargin).successful)
    assert(!parseAll(tableStatements,
      """
        |DROP IF EXISTS `table`;
        |""".stripMargin).successful)
    assert(!parseAll(tableStatements,
      """
        |DROP IF NOT EXISTS;
        |""".stripMargin).successful)
  }
