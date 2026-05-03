/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

private case class KeyTestModel(id: Long, subId: Long)
private class KeyTestTable extends Table[KeyTestModel]("test"):
  def id:    Column[Long] = column[Long]("id", BIGINT)
  def subId: Column[Long] = column[Long]("sub_id", BIGINT)

  override def * = (id *: subId).to[KeyTestModel]

private val keyTestTable = new KeyTestTable

class KeyTest extends munit.FunSuite:

  val testTable = keyTestTable

  test("[1] The query string of the generated IndexKey model matches the specified string.") {
    val key = IndexKey(None, None, testTable.id *: testTable.subId, None)
    assertEquals(key.queryString, "INDEX (`id`, `sub_id`)")
  }

  test("[2] The query string of the generated IndexKey model matches the specified string.") {
    val key = IndexKey(Some("key_id"), None, testTable.id *: testTable.subId, None)
    assertEquals(key.queryString, "INDEX `key_id` (`id`, `sub_id`)")
  }

  test("[3] The query string of the generated IndexKey model matches the specified string.") {
    val key = IndexKey(Some("key_id"), Some(Index.Type.BTREE), testTable.id *: testTable.subId, None)
    assertEquals(key.queryString, "INDEX `key_id` (`id`, `sub_id`) USING BTREE")
  }

  test("[4] The query string of the generated IndexKey model matches the specified string.") {
    val key = IndexKey(
      Some("key_id"),
      Some(Index.Type.BTREE),
      testTable.id *: testTable.subId,
      Some(Index.IndexOption(Some(1), None, None, None, None, None))
    )
    assertEquals(key.queryString, "INDEX `key_id` (`id`, `sub_id`) USING BTREE KEY_BLOCK_SIZE = 1")
  }

  test("[5] The query string of the generated IndexKey model matches the specified string.") {
    val key = IndexKey(
      Some("key_id"),
      Some(Index.Type.BTREE),
      testTable.id *: testTable.subId,
      Some(Index.IndexOption(Some(1), Some(Index.Type.BTREE), None, None, None, None))
    )
    assertEquals(key.queryString, "INDEX `key_id` (`id`, `sub_id`) USING BTREE KEY_BLOCK_SIZE = 1 USING BTREE")
  }

  test("[6] The query string of the generated IndexKey model matches the specified string.") {
    val key = IndexKey(
      Some("key_id"),
      Some(Index.Type.BTREE),
      testTable.id *: testTable.subId,
      Some(Index.IndexOption(Some(1), Some(Index.Type.BTREE), Some("parser"), None, None, None))
    )
    assertEquals(
      key.queryString,
      "INDEX `key_id` (`id`, `sub_id`) USING BTREE KEY_BLOCK_SIZE = 1 USING BTREE WITH PARSER parser"
    )
  }

  test("[7] The query string of the generated IndexKey model matches the specified string.") {
    val key = IndexKey(
      Some("key_id"),
      Some(Index.Type.BTREE),
      testTable.id *: testTable.subId,
      Some(Index.IndexOption(Some(1), Some(Index.Type.BTREE), Some("parser"), Some("comment"), None, None))
    )
    assertEquals(
      key.queryString,
      "INDEX `key_id` (`id`, `sub_id`) USING BTREE KEY_BLOCK_SIZE = 1 USING BTREE WITH PARSER parser COMMENT 'comment'"
    )
  }

  test("[8] The query string of the generated IndexKey model matches the specified string.") {
    val key = IndexKey(
      Some("key_id"),
      Some(Index.Type.BTREE),
      testTable.id *: testTable.subId,
      Some(Index.IndexOption(Some(1), Some(Index.Type.BTREE), Some("parser"), Some("comment"), Some("InnoDB"), None))
    )
    assertEquals(
      key.queryString,
      "INDEX `key_id` (`id`, `sub_id`) USING BTREE KEY_BLOCK_SIZE = 1 USING BTREE WITH PARSER parser COMMENT 'comment' ENGINE_ATTRIBUTE = 'InnoDB'"
    )
  }

  test("[1] The query string of the generated Fulltext model matches the specified string.") {
    val key = Fulltext(None, testTable.id *: testTable.subId, None)
    assertEquals(key.queryString, "FULLTEXT (`id`, `sub_id`)")
  }

  test("[2] The query string of the generated Fulltext model matches the specified string.") {
    val key = Fulltext(Some("key_id"), testTable.id *: testTable.subId, None)
    assertEquals(key.queryString, "FULLTEXT `key_id` (`id`, `sub_id`)")
  }

  test("[3] The query string of the generated Fulltext model matches the specified string.") {
    val key = Fulltext(
      Some("key_id"),
      testTable.id *: testTable.subId,
      Some(Index.IndexOption(Some(1), None, None, None, None, None))
    )
    assertEquals(key.queryString, "FULLTEXT `key_id` (`id`, `sub_id`) KEY_BLOCK_SIZE = 1")
  }

  test("[4] The query string of the generated Fulltext model matches the specified string.") {
    val key = Fulltext(
      Some("key_id"),
      testTable.id *: testTable.subId,
      Some(Index.IndexOption(Some(1), Some(Index.Type.BTREE), None, None, None, None))
    )
    assertEquals(key.queryString, "FULLTEXT `key_id` (`id`, `sub_id`) KEY_BLOCK_SIZE = 1 USING BTREE")
  }

  test("[5] The query string of the generated Fulltext model matches the specified string.") {
    val key = Fulltext(
      Some("key_id"),
      testTable.id *: testTable.subId,
      Some(Index.IndexOption(Some(1), Some(Index.Type.BTREE), Some("parser"), None, None, None))
    )
    assertEquals(key.queryString, "FULLTEXT `key_id` (`id`, `sub_id`) KEY_BLOCK_SIZE = 1 USING BTREE WITH PARSER parser")
  }

  test("[6] The query string of the generated Fulltext model matches the specified string.") {
    val key = Fulltext(
      Some("key_id"),
      testTable.id *: testTable.subId,
      Some(Index.IndexOption(Some(1), Some(Index.Type.BTREE), Some("parser"), Some("comment"), None, None))
    )
    assertEquals(
      key.queryString,
      "FULLTEXT `key_id` (`id`, `sub_id`) KEY_BLOCK_SIZE = 1 USING BTREE WITH PARSER parser COMMENT 'comment'"
    )
  }

  test("[7] The query string of the generated Fulltext model matches the specified string.") {
    val key = Fulltext(
      Some("key_id"),
      testTable.id *: testTable.subId,
      Some(Index.IndexOption(Some(1), Some(Index.Type.BTREE), Some("parser"), Some("comment"), Some("InnoDB"), None))
    )
    assertEquals(
      key.queryString,
      "FULLTEXT `key_id` (`id`, `sub_id`) KEY_BLOCK_SIZE = 1 USING BTREE WITH PARSER parser COMMENT 'comment' ENGINE_ATTRIBUTE = 'InnoDB'"
    )
  }

  test("[8] The query string of the generated Fulltext model matches the specified string.") {
    val key = Fulltext(
      Some("key_id"),
      testTable.id *: testTable.subId,
      Some(
        Index.IndexOption(
          Some(1),
          Some(Index.Type.BTREE),
          Some("parser"),
          Some("comment"),
          Some("InnoDB"),
          Some("secondary")
        )
      )
    )
    assertEquals(
      key.queryString,
      "FULLTEXT `key_id` (`id`, `sub_id`) KEY_BLOCK_SIZE = 1 USING BTREE WITH PARSER parser COMMENT 'comment' ENGINE_ATTRIBUTE = 'InnoDB' SECONDARY_ENGINE_ATTRIBUTE = 'secondary'"
    )
  }

  test("[1] The query string of the generated Constraint PrimaryKey model matches the specified string.") {
    val primaryKey = PrimaryKey(None, testTable.id, None)
    val key        = Constraint(None, primaryKey)
    assertEquals(key.queryString, "CONSTRAINT PRIMARY KEY (`id`)")
  }

  test("[2] The query string of the generated Constraint PrimaryKey model matches the specified string.") {
    val primaryKey = PrimaryKey(Some(Index.Type.BTREE), testTable.id, None)
    val key        = Constraint(None, primaryKey)
    assertEquals(key.queryString, "CONSTRAINT PRIMARY KEY (`id`) USING BTREE")
  }

  test("[1] The query string of the generated Constraint UniqueKey model matches the specified string.") {
    val uniqueKey = UniqueKey(None, None, testTable.id, None)
    val key       = Constraint(None, uniqueKey)
    assertEquals(key.queryString, "CONSTRAINT UNIQUE KEY (`id`)")
  }

  test("[2] The query string of the generated Constraint UniqueKey model matches the specified string.") {
    val uniqueKey = UniqueKey(None, None, testTable.id, None)
    val key       = Constraint(Some("key_id"), uniqueKey)
    assertEquals(key.queryString, "CONSTRAINT `key_id` UNIQUE KEY (`id`)")
  }

  test("[1] The query string of the generated Constraint ForeignKey model matches the specified string.") {
    val reference  = Reference(testTable, testTable.id, None, None)
    val foreignKey = ForeignKey(None, testTable.id, reference)
    val key        = Constraint(None, foreignKey)
    assertEquals(key.queryString, "CONSTRAINT FOREIGN KEY (`id`) REFERENCES `test` (`id`)")
  }

  test("[2] The query string of the generated Constraint ForeignKey model matches the specified string.") {
    val reference  = Reference(testTable, testTable.id, None, None)
    val foreignKey = ForeignKey(None, testTable.id, reference)
    val key        = Constraint(Some("key_id"), foreignKey)
    assertEquals(key.queryString, "CONSTRAINT `key_id` FOREIGN KEY (`id`) REFERENCES `test` (`id`)")
  }

  test("[3] The query string of the generated Constraint ForeignKey model matches the specified string.") {
    val reference = Reference(testTable, testTable.id, None, None)
      .onDelete(Reference.ReferenceOption.CASCADE)
    val foreignKey = ForeignKey(None, testTable.id, reference)
    val key        = Constraint(Some("key_id"), foreignKey)
    assertEquals(key.queryString, "CONSTRAINT `key_id` FOREIGN KEY (`id`) REFERENCES `test` (`id`) ON DELETE CASCADE")
  }
