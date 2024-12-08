/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AliasTest extends AnyFlatSpec, Matchers:

  case class Test(id: Long, subId: String, status: Int)

  class TestTable extends Table[Test]("test"):
    def id:     Column[Long]   = column[Long]("id", BIGINT)
    def subId:  Column[String] = column[String]("sub_id", VARCHAR(255))
    def status: Column[Int]    = column[Int]("status", INT)

    override def * = (id *: subId *: status).to[Test]

  val testTable = new TestTable

  it should "PRIMARY_KEY call succeeds" in {
    val p1 = PRIMARY_KEY[Long]
    val p2 = PRIMARY_KEY(testTable.subId)
    val p3 = PRIMARY_KEY(testTable.id *: testTable.subId)
    val p4 = PRIMARY_KEY(Index.Type.BTREE, testTable.id *: testTable.subId)
    val p5 = PRIMARY_KEY(
      Index.Type.BTREE,
      Index.IndexOption(Some(1), None, None, None, None, None),
      testTable.id *: testTable.subId
    )

    p1.queryString === "PRIMARY KEY" &&
    p2.queryString === "PRIMARY KEY (id)" &&
    p3.queryString === "PRIMARY KEY (id, sub_id)" &&
    p4.queryString === "PRIMARY KEY (id, sub_id) USING BTREE" &&
    p5.queryString === "PRIMARY KEY (id, sub_id) USING BTREE KEY_BLOCK_SIZE = 1"
  }

  it should "UNIQUE_KEY call succeeds" in {
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

    p1.queryString === "UNIQUE KEY" &&
    p2.queryString === "UNIQUE KEY (id)" &&
    p3.queryString === "UNIQUE KEY (id, sub_id)" &&
    p4.queryString === "UNIQUE KEY `index` (id, sub_id)" &&
    p5.queryString === "UNIQUE KEY `index` (id, sub_id) USING BTREE" &&
    p6.queryString === "UNIQUE KEY `index` (id, sub_id) USING BTREE KEY_BLOCK_SIZE = 1" &&
    p7.queryString === "UNIQUE KEY (id, sub_id)"
  }

  it should "INDEX_KEY call succeeds" in {
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

    p1.queryString === "INDEX (id)" &&
    p2.queryString === "INDEX (id, sub_id)" &&
    p3.queryString === "INDEX (id, sub_id)" &&
    p4.queryString === "INDEX `index` (id, sub_id)" &&
    p5.queryString === "INDEX `index` (id, sub_id) USING BTREE" &&
    p6.queryString === "INDEX `index` (id, sub_id) USING BTREE KEY_BLOCK_SIZE = 1"
  }

  it should "FOREIGN_KEY call succeeds" in {

    case class SubTest(id: String, status: Int)

    class SubTestTable extends Table[SubTest]("sub_test"):
      def id:     Column[String] = column[String]("id", VARCHAR(255))
      def status: Column[Int]    = column[Int]("status", INT)

      override def * = (id *: status).to[SubTest]

    val subTestTable = new SubTestTable

    val p1 = FOREIGN_KEY(testTable.subId, REFERENCE(subTestTable, subTestTable.id))
    val p2 = FOREIGN_KEY(
      testTable.subId *: testTable.status,
      REFERENCE(subTestTable, subTestTable.id *: subTestTable.status)
    )

    p1.queryString === "FOREIGN KEY (sub_id) REFERENCES sub_test (id)" &&
    p2.queryString === "FOREIGN KEY (sub_id, status) REFERENCES sub_test (id, status)"
  }
