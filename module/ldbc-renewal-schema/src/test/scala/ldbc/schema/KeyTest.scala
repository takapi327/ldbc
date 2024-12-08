/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import org.scalatest.flatspec.AnyFlatSpec

class KeyTest extends AnyFlatSpec:

  case class Test(id: Long, subId: Long)
  class TestTable extends Table[Test]("test"):
    def id:    Column[Long] = column[Long]("id", BIGINT)
    def subId: Column[Long] = column[Long]("sub_id", BIGINT)

    override def * = (id *: subId).to[Test]

  val testTable = new TestTable

  it should "[1] The query string of the generated IndexKey model matches the specified string." in {
    val key = IndexKey(None, None, testTable.id *: testTable.subId, None)
    key.queryString === "INDEX (id, sub_id)"
  }

  it should "[2] The query string of the generated IndexKey model matches the specified string." in {
    val key = IndexKey(Some("key_id"), None, testTable.id *: testTable.subId, None)
    key.queryString === "INDEX `key_id` (id, sub_id)"
  }

  it should "[3] The query string of the generated IndexKey model matches the specified string." in {
    val key = IndexKey(Some("key_id"), Some(Index.Type.BTREE), testTable.id *: testTable.subId, None)
    key.queryString === "INDEX `key_id` (id, sub_id) USING BTREE"
  }

  it should "[4] The query string of the generated IndexKey model matches the specified string." in {
    val key = IndexKey(
      Some("key_id"),
      Some(Index.Type.BTREE),
      testTable.id *: testTable.subId,
      Some(Index.IndexOption(Some(1), None, None, None, None, None))
    )
    key.queryString === "INDEX `key_id` (id, sub_id) USING BTREE KEY_BLOCK_SIZE = 1"
  }

  it should "[5] The query string of the generated IndexKey model matches the specified string." in {
    val key = IndexKey(
      Some("key_id"),
      Some(Index.Type.BTREE),
      testTable.id *: testTable.subId,
      Some(Index.IndexOption(Some(1), Some(Index.Type.BTREE), None, None, None, None))
    )
    key.queryString === "INDEX `key_id` (id, sub_id) USING BTREE KEY_BLOCK_SIZE = 1 USING BTREE"
  }

  it should "[6] The query string of the generated IndexKey model matches the specified string." in {
    val key = IndexKey(
      Some("key_id"),
      Some(Index.Type.BTREE),
      testTable.id *: testTable.subId,
      Some(Index.IndexOption(Some(1), Some(Index.Type.BTREE), Some("parser"), None, None, None))
    )
    key.queryString === "INDEX `key_id` (id, sub_id) USING BTREE KEY_BLOCK_SIZE = 1 USING BTREE WITH PARSER parser"
  }

  it should "[7] The query string of the generated IndexKey model matches the specified string." in {
    val key = IndexKey(
      Some("key_id"),
      Some(Index.Type.BTREE),
      testTable.id *: testTable.subId,
      Some(Index.IndexOption(Some(1), Some(Index.Type.BTREE), Some("parser"), Some("comment"), None, None))
    )
    key.queryString === "INDEX `key_id` (id, sub_id) USING BTREE KEY_BLOCK_SIZE = 1 USING BTREE WITH PARSER parser COMMENT 'comment'"
  }

  it should "[8] The query string of the generated IndexKey model matches the specified string." in {
    val key = IndexKey(
      Some("key_id"),
      Some(Index.Type.BTREE),
      testTable.id *: testTable.subId,
      Some(Index.IndexOption(Some(1), Some(Index.Type.BTREE), Some("parser"), Some("comment"), Some("InnoDB"), None))
    )
    key.queryString === "INDEX `key_id` (id, sub_id) USING BTREE KEY_BLOCK_SIZE = 1 USING BTREE WITH PARSER parser COMMENT 'comment' ENGINE_ATTRIBUTE = 'InnoDB'"
  }
