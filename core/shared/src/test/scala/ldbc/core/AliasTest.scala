/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.core

import org.specs2.mutable.Specification

object AliasTest extends Specification:

  case class Test(id: Long, status: Int)
  private val table = Table[Test]("test")(column("id", BIGINT), column("status", INT))

  "PRIMARY_KEY call succeeds" in {
    val p1 = PRIMARY_KEY
    val p2 = PRIMARY_KEY(column("p1", VARCHAR(255)))
    val p3 = PRIMARY_KEY(column("p1", VARCHAR(255)), column("p2", VARCHAR(255)))
    val p4 = PRIMARY_KEY(Index.Type.BTREE, column("p1", VARCHAR(255)), column("p2", VARCHAR(255)))
    val p5 = PRIMARY_KEY(
      Index.Type.BTREE,
      Index.IndexOption(Some(1), None, None, None, None, None),
      column("p1", VARCHAR(255)),
      column("p2", VARCHAR(255))
    )

    p1.queryString == "PRIMARY KEY" and
      p2.queryString == "PRIMARY KEY (`p1`)" and
      p3.queryString == "PRIMARY KEY (`p1`, `p2`)" and
      p4.queryString == "PRIMARY KEY (`p1`, `p2`) USING BTREE" and
      p5.queryString == "PRIMARY KEY (`p1`, `p2`) USING BTREE KEY_BLOCK_SIZE = 1"
  }

  "PRIMARY_KEY call failed" in {
    PRIMARY_KEY() must throwAn[IllegalArgumentException] and
      (PRIMARY_KEY(Index.Type.BTREE) must throwAn[IllegalArgumentException]) and
      (PRIMARY_KEY(Index.Type.BTREE, Index.IndexOption(Some(1), None, None, None, None, None)) must throwAn[
        IllegalArgumentException
      ])
  }

  "UNIQUE_KEY call succeeds" in {
    val p1 = UNIQUE_KEY
    val p2 = UNIQUE_KEY(column("p1", VARCHAR(255)))
    val p3 = UNIQUE_KEY(column("p1", VARCHAR(255)), column("p2", VARCHAR(255)))
    val p4 = UNIQUE_KEY("index", column("p1", VARCHAR(255)), column("p2", VARCHAR(255)))
    val p5 = UNIQUE_KEY("index", Index.Type.BTREE, column("p1", VARCHAR(255)), column("p2", VARCHAR(255)))
    val p6 = UNIQUE_KEY(
      "index",
      Index.Type.BTREE,
      Index.IndexOption(Some(1), None, None, None, None, None),
      column("p1", VARCHAR(255)),
      column("p2", VARCHAR(255))
    )
    val p7 = UNIQUE_KEY(None, None, None, column("p1", VARCHAR(255)), column("p2", VARCHAR(255)))

    p1.queryString == "UNIQUE KEY" and
      p2.queryString == "UNIQUE KEY (`p1`)" and
      p3.queryString == "UNIQUE KEY (`p1`, `p2`)" and
      p4.queryString == "UNIQUE KEY `index` (`p1`, `p2`)" and
      p5.queryString == "UNIQUE KEY `index` (`p1`, `p2`) USING BTREE" and
      p6.queryString == "UNIQUE KEY `index` (`p1`, `p2`) USING BTREE KEY_BLOCK_SIZE = 1" and
      p7.queryString == "UNIQUE KEY (`p1`, `p2`)"
  }

  "UNIQUE_KEY call failed" in {
    UNIQUE_KEY() must throwAn[IllegalArgumentException] and
      (UNIQUE_KEY("index") must throwAn[IllegalArgumentException]) and
      (UNIQUE_KEY("index", Index.Type.BTREE) must throwAn[IllegalArgumentException])
  }

  "INDEX_KEY call succeeds" in {
    val p1 = INDEX_KEY(column("p1", VARCHAR(255)))
    val p2 = INDEX_KEY(column("p1", VARCHAR(255)), column("p2", VARCHAR(255)))
    val p3 = INDEX_KEY(None, None, None, column("p1", VARCHAR(255)), column("p2", VARCHAR(255)))
    val p4 = INDEX_KEY(Some("index"), None, None, column("p1", VARCHAR(255)), column("p2", VARCHAR(255)))
    val p5 =
      INDEX_KEY(Some("index"), Some(Index.Type.BTREE), None, column("p1", VARCHAR(255)), column("p2", VARCHAR(255)))
    val p6 = INDEX_KEY(
      Some("index"),
      Some(Index.Type.BTREE),
      Some(Index.IndexOption(Some(1), None, None, None, None, None)),
      column("p1", VARCHAR(255)),
      column("p2", VARCHAR(255))
    )

    p1.queryString == "INDEX (`p1`)" and
      p2.queryString == "INDEX (`p1`, `p2`)" and
      p3.queryString == "INDEX (`p1`, `p2`)" and
      p4.queryString == "INDEX `index` (`p1`, `p2`)" and
      p5.queryString == "INDEX `index` (`p1`, `p2`) USING BTREE" and
      p6.queryString == "INDEX `index` (`p1`, `p2`) USING BTREE KEY_BLOCK_SIZE = 1"
  }

  "INDEX_KEY call failed" in {
    INDEX_KEY() must throwAn[IllegalArgumentException] and
      (INDEX_KEY(None, None, None) must throwAn[IllegalArgumentException]) and
      (INDEX_KEY(Some("index"), None, None) must throwAn[IllegalArgumentException]) and
      (INDEX_KEY(Some("index"), Some(Index.Type.BTREE), None) must throwAn[IllegalArgumentException]) and
      (INDEX_KEY(
        Some("index"),
        Some(Index.Type.BTREE),
        Some(Index.IndexOption(Some(1), None, None, None, None, None))
      ) must throwAn[IllegalArgumentException])
  }

  "FOREIGN_KEY call succeeds" in {
    val p1 = FOREIGN_KEY(column("test_id", BIGINT), REFERENCE(table, table.id))
    val p2 = FOREIGN_KEY(
      (column("test_id", BIGINT), column("test_status", INT)),
      REFERENCE(table, (table.id, table.status))
    )

    p1.queryString == "FOREIGN KEY (`test_id`) REFERENCES `test` (`id`)" and
      p2.queryString == "FOREIGN KEY (`test_id`, `test_status`) REFERENCES `test` (`id`, `status`)"
  }
