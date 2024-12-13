/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AliasTest extends AnyFlatSpec, Matchers:

  case class Test(id: Long, status: Int)
  private val table = Table[Test]("test")(column("id", BIGINT), column("status", INT))

  it should "PRIMARY_KEY call succeeds" in {
    val p1 = PRIMARY_KEY
    val p2 = PRIMARY_KEY(column[String]("p1", VARCHAR(255)))
    val p3 = PRIMARY_KEY(column[String]("p1", VARCHAR(255)), column[String]("p2", VARCHAR(255)))
    val p4 = PRIMARY_KEY(Index.Type.BTREE, column[String]("p1", VARCHAR(255)), column[String]("p2", VARCHAR(255)))
    val p5 = PRIMARY_KEY(
      Index.Type.BTREE,
      Index.IndexOption(Some(1), None, None, None, None, None),
      column[String]("p1", VARCHAR(255)),
      column[String]("p2", VARCHAR(255))
    )

    p1.queryString === "PRIMARY KEY" &&
    p2.queryString === "PRIMARY KEY (`p1`)" &&
    p3.queryString === "PRIMARY KEY (`p1`, `p2`)" &&
    p4.queryString === "PRIMARY KEY (`p1`, `p2`) USING BTREE" &&
    p5.queryString === "PRIMARY KEY (`p1`, `p2`) USING BTREE KEY_BLOCK_SIZE = 1"
  }

  it should "PRIMARY_KEY call failed" in {
    an[IllegalArgumentException] must be thrownBy PRIMARY_KEY()
    an[IllegalArgumentException] must be thrownBy PRIMARY_KEY(Index.Type.BTREE)
    an[IllegalArgumentException] must be thrownBy PRIMARY_KEY(
      Index.Type.BTREE,
      Index.IndexOption(Some(1), None, None, None, None, None)
    )
  }

  it should "UNIQUE_KEY call succeeds" in {
    val p1 = UNIQUE_KEY
    val p2 = UNIQUE_KEY(column[String]("p1", VARCHAR(255)))
    val p3 = UNIQUE_KEY(column[String]("p1", VARCHAR(255)), column[String]("p2", VARCHAR(255)))
    val p4 = UNIQUE_KEY("index", column[String]("p1", VARCHAR(255)), column[String]("p2", VARCHAR(255)))
    val p5 =
      UNIQUE_KEY("index", Index.Type.BTREE, column[String]("p1", VARCHAR(255)), column[String]("p2", VARCHAR(255)))
    val p6 = UNIQUE_KEY(
      "index",
      Index.Type.BTREE,
      Index.IndexOption(Some(1), None, None, None, None, None),
      column[String]("p1", VARCHAR(255)),
      column[String]("p2", VARCHAR(255))
    )
    val p7 = UNIQUE_KEY(None, None, None, column[String]("p1", VARCHAR(255)), column[String]("p2", VARCHAR(255)))

    p1.queryString === "UNIQUE KEY" &&
    p2.queryString === "UNIQUE KEY (`p1`)" &&
    p3.queryString === "UNIQUE KEY (`p1`, `p2`)" &&
    p4.queryString === "UNIQUE KEY `index` (`p1`, `p2`)" &&
    p5.queryString === "UNIQUE KEY `index` (`p1`, `p2`) USING BTREE" &&
    p6.queryString === "UNIQUE KEY `index` (`p1`, `p2`) USING BTREE KEY_BLOCK_SIZE = 1" &&
    p7.queryString === "UNIQUE KEY (`p1`, `p2`)"
  }

  it should "UNIQUE_KEY call failed" in {
    an[IllegalArgumentException] must be thrownBy UNIQUE_KEY()
    an[IllegalArgumentException] must be thrownBy UNIQUE_KEY("index")
    an[IllegalArgumentException] must be thrownBy UNIQUE_KEY("index", Index.Type.BTREE)
  }

  it should "INDEX_KEY call succeeds" in {
    val p1 = INDEX_KEY(column[String]("p1", VARCHAR(255)))
    val p2 = INDEX_KEY(column[String]("p1", VARCHAR(255)), column[String]("p2", VARCHAR(255)))
    val p3 = INDEX_KEY(None, None, None, column[String]("p1", VARCHAR(255)), column[String]("p2", VARCHAR(255)))
    val p4 =
      INDEX_KEY(Some("index"), None, None, column[String]("p1", VARCHAR(255)), column[String]("p2", VARCHAR(255)))
    val p5 =
      INDEX_KEY(
        Some("index"),
        Some(Index.Type.BTREE),
        None,
        column[String]("p1", VARCHAR(255)),
        column[String]("p2", VARCHAR(255))
      )
    val p6 = INDEX_KEY(
      Some("index"),
      Some(Index.Type.BTREE),
      Some(Index.IndexOption(Some(1), None, None, None, None, None)),
      column[String]("p1", VARCHAR(255)),
      column[String]("p2", VARCHAR(255))
    )

    p1.queryString === "INDEX (`p1`)" &&
    p2.queryString === "INDEX (`p1`, `p2`)" &&
    p3.queryString === "INDEX (`p1`, `p2`)" &&
    p4.queryString === "INDEX `index` (`p1`, `p2`)" &&
    p5.queryString === "INDEX `index` (`p1`, `p2`) USING BTREE" &&
    p6.queryString === "INDEX `index` (`p1`, `p2`) USING BTREE KEY_BLOCK_SIZE = 1"
  }

  it should "INDEX_KEY call failed" in {
    an[IllegalArgumentException] must be thrownBy INDEX_KEY()
    an[IllegalArgumentException] must be thrownBy INDEX_KEY(None, None, None)
    an[IllegalArgumentException] must be thrownBy INDEX_KEY(Some("index"), None, None)
    an[IllegalArgumentException] must be thrownBy INDEX_KEY(Some("index"), Some(Index.Type.BTREE), None)
    an[IllegalArgumentException] must be thrownBy INDEX_KEY(
      Some("index"),
      Some(Index.Type.BTREE),
      Some(Index.IndexOption(Some(1), None, None, None, None, None))
    )
  }

  it should "FOREIGN_KEY call succeeds" in {
    val p1 = FOREIGN_KEY(column[Long]("test_id", BIGINT), REFERENCE(table, table.id))
    val p2 = FOREIGN_KEY(
      (column[Long]("test_id", BIGINT), column[Int]("test_status", INT)),
      REFERENCE(table, (table.id, table.status))
    )

    p1.queryString === "FOREIGN KEY (`test_id`) REFERENCES `test` (`id`)" &&
    p2.queryString === "FOREIGN KEY (`test_id`, `test_status`) REFERENCES `test` (`id`, `status`)"
  }
