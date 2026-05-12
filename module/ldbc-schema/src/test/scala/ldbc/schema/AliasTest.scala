/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

private case class AliasTestModel(id: Long, subId: String, status: Int)

class AliasTest extends munit.FunSuite:

  class TestTable extends Table[AliasTestModel]("test"):
    def id:     Column[Long]   = column[Long]("id", BIGINT)
    def subId:  Column[String] = column[String]("sub_id", VARCHAR(255))
    def status: Column[Int]    = column[Int]("status", INT)

    override def * = (id *: subId *: status).to[AliasTestModel]

  val testTable = new TestTable

  test("PRIMARY_KEY call succeeds") {
    val p1 = PRIMARY_KEY[Long]
    val p2 = PRIMARY_KEY(testTable.subId)
    val p3 = PRIMARY_KEY(testTable.id *: testTable.subId)
    val p4 = PRIMARY_KEY(Index.Type.BTREE, testTable.id *: testTable.subId)
    val p5 = PRIMARY_KEY(
      Index.Type.BTREE,
      Index.IndexOption(Some(1), None, None, None, None, None),
      testTable.id *: testTable.subId
    )

    assertEquals(p1.queryString, "PRIMARY KEY")
    assertEquals(p2.queryString, "PRIMARY KEY (`sub_id`)")
    assertEquals(p3.queryString, "PRIMARY KEY (`id`, `sub_id`)")
    assertEquals(p4.queryString, "PRIMARY KEY (`id`, `sub_id`) USING BTREE")
    assertEquals(p5.queryString, "PRIMARY KEY (`id`, `sub_id`) USING BTREE KEY_BLOCK_SIZE = 1")
  }

  test("UNIQUE_KEY call succeeds") {
    val p1 = UNIQUE_KEY[Long]
    val p2 = UNIQUE_KEY(testTable.subId)
    val p3 = UNIQUE_KEY(testTable.id *: testTable.subId)
    val p4 = UNIQUE_KEY("index", testTable.id *: testTable.subId)
    val p5 =
      UNIQUE_KEY("index", Index.Type.BTREE, testTable.id *: testTable.subId)
    val p6 = UNIQUE_KEY(
      "index",
      Index.Type.BTREE,
      Index.IndexOption(Some(1), None, None, None, None, None),
      testTable.id *: testTable.subId
    )
    val p7 = UNIQUE_KEY(None, None, None, testTable.id *: testTable.subId)

    assertEquals(p1.queryString, "UNIQUE KEY")
    assertEquals(p2.queryString, "UNIQUE KEY (`sub_id`)")
    assertEquals(p3.queryString, "UNIQUE KEY (`id`, `sub_id`)")
    assertEquals(p4.queryString, "UNIQUE KEY `index` (`id`, `sub_id`)")
    assertEquals(p5.queryString, "UNIQUE KEY `index` (`id`, `sub_id`) USING BTREE")
    assertEquals(p6.queryString, "UNIQUE KEY `index` (`id`, `sub_id`) USING BTREE KEY_BLOCK_SIZE = 1")
    assertEquals(p7.queryString, "UNIQUE KEY (`id`, `sub_id`)")
  }

  test("INDEX_KEY call succeeds") {
    val p1 = INDEX_KEY(testTable.subId)
    val p2 = INDEX_KEY(testTable.id *: testTable.subId)
    val p3 = INDEX_KEY(None, None, None, testTable.id *: testTable.subId)
    val p4 =
      INDEX_KEY(Some("index"), None, None, testTable.id *: testTable.subId)
    val p5 =
      INDEX_KEY(
        Some("index"),
        Some(Index.Type.BTREE),
        None,
        testTable.id *: testTable.subId
      )
    val p6 = INDEX_KEY(
      Some("index"),
      Some(Index.Type.BTREE),
      Some(Index.IndexOption(Some(1), None, None, None, None, None)),
      testTable.id *: testTable.subId
    )

    assertEquals(p1.queryString, "INDEX (`sub_id`)")
    assertEquals(p2.queryString, "INDEX (`id`, `sub_id`)")
    assertEquals(p3.queryString, "INDEX (`id`, `sub_id`)")
    assertEquals(p4.queryString, "INDEX `index` (`id`, `sub_id`)")
    assertEquals(p5.queryString, "INDEX `index` (`id`, `sub_id`) USING BTREE")
    assertEquals(p6.queryString, "INDEX `index` (`id`, `sub_id`) USING BTREE KEY_BLOCK_SIZE = 1")
  }

  test("FOREIGN_KEY call succeeds") {

    case class SubTest(id: String, status: Int)

    class SubTestTable extends Table[SubTest]("sub_test"):
      def id:     Column[String] = column[String]("id", VARCHAR(255))
      def status: Column[Int]    = column[Int]("status", INT)

      override def * = (id *: status).to[SubTest]

    val subTest = TableQuery[SubTestTable]

    val p1 = FOREIGN_KEY(testTable.subId, REFERENCE(subTest)(_.id))
    val p2 = FOREIGN_KEY(
      testTable.subId *: testTable.status,
      REFERENCE(subTest)(s => s.id *: s.status)
    )

    assertEquals(p1.queryString, "FOREIGN KEY (`sub_id`) REFERENCES `sub_test` (`id`)")
    assertEquals(p2.queryString, "FOREIGN KEY (`sub_id`, `status`) REFERENCES `sub_test` (`id`, `status`)")
  }
