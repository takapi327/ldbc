/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

import org.specs2.mutable.Specification

import cats.Id
import cats.data.NonEmptyList

object KeyTest extends Specification:

  private val column1 = column[Long]("id", BIGINT(64))
  private val column2 = column[String]("name", VARCHAR(255))

  "IndexKey Test" should {
    "The query string of the generated IndexKey model matches the specified string." in {
      val key = IndexKey(None, None, NonEmptyList.of(column1, column2), None)
      key.queryString === "INDEX (`id`, `name`)"
    }

    "The query string of the generated IndexKey model matches the specified string." in {
      val key = IndexKey(Some("key_id"), None, NonEmptyList.of(column1, column2), None)
      key.queryString === "INDEX `key_id` (`id`, `name`)"
    }

    "The query string of the generated IndexKey model matches the specified string." in {
      val key = IndexKey(Some("key_id"), Some(Index.Type.BTREE), NonEmptyList.of(column1, column2), None)
      key.queryString === "INDEX `key_id` (`id`, `name`) USING BTREE"
    }

    "The query string of the generated IndexKey model matches the specified string." in {
      val key = IndexKey(
        Some("key_id"),
        Some(Index.Type.BTREE),
        NonEmptyList.of(column1, column2),
        Some(Index.IndexOption(Some(1), None, None, None, None, None))
      )
      key.queryString === "INDEX `key_id` (`id`, `name`) USING BTREE KEY_BLOCK_SIZE = 1"
    }

    "The query string of the generated IndexKey model matches the specified string." in {
      val key = IndexKey(
        Some("key_id"),
        Some(Index.Type.BTREE),
        NonEmptyList.of(column1, column2),
        Some(Index.IndexOption(Some(1), Some(Index.Type.BTREE), None, None, None, None))
      )
      key.queryString === "INDEX `key_id` (`id`, `name`) USING BTREE KEY_BLOCK_SIZE = 1 USING BTREE"
    }

    "The query string of the generated IndexKey model matches the specified string." in {
      val key = IndexKey(
        Some("key_id"),
        Some(Index.Type.BTREE),
        NonEmptyList.of(column1, column2),
        Some(Index.IndexOption(Some(1), Some(Index.Type.BTREE), Some("parser"), None, None, None))
      )
      key.queryString === "INDEX `key_id` (`id`, `name`) USING BTREE KEY_BLOCK_SIZE = 1 USING BTREE WITH PARSER parser"
    }

    "The query string of the generated IndexKey model matches the specified string." in {
      val key = IndexKey(
        Some("key_id"),
        Some(Index.Type.BTREE),
        NonEmptyList.of(column1, column2),
        Some(Index.IndexOption(Some(1), Some(Index.Type.BTREE), Some("parser"), Some("comment"), None, None))
      )
      key.queryString === "INDEX `key_id` (`id`, `name`) USING BTREE KEY_BLOCK_SIZE = 1 USING BTREE WITH PARSER parser COMMENT 'comment'"
    }

    "The query string of the generated IndexKey model matches the specified string." in {
      val key = IndexKey(
        Some("key_id"),
        Some(Index.Type.BTREE),
        NonEmptyList.of(column1, column2),
        Some(Index.IndexOption(Some(1), Some(Index.Type.BTREE), Some("parser"), Some("comment"), Some("InnoDB"), None))
      )
      key.queryString === "INDEX `key_id` (`id`, `name`) USING BTREE KEY_BLOCK_SIZE = 1 USING BTREE WITH PARSER parser COMMENT 'comment' ENGINE_ATTRIBUTE = 'InnoDB'"
    }
  }
