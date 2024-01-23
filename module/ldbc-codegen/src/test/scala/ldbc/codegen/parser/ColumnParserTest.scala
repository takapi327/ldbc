/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.parser

import org.scalatest.flatspec.AnyFlatSpec

class ColumnParserTest extends AnyFlatSpec, ColumnParser:

  override def fileName: String = "test.sql"

  it should "Column parsing test succeeds." in {
    assert(parseAll(columnDefinition, "`id` BIGINT(64)").successful)
    assert(parseAll(columnDefinition, "id BIGINT").successful)
    assert(
      parseAll(columnDefinition, "/* Comment */ `id` /* Comment */ BIGINT(64) /* Comment */ COMMENT 'test'").successful
    )
    assert(parseAll(columnDefinition, "`id` BIGINT(64) PRIMARY KEY").successful)
    assert(parseAll(columnDefinition, "`id` BIGINT(64) PRIMARY").successful)
    assert(parseAll(columnDefinition, "`id` BIGINT(64) UNIQUE").successful)
    assert(parseAll(columnDefinition, "`id` BIGINT(64) UNIQUE KEY").successful)
    assert(parseAll(columnDefinition, "`id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT").successful)
    assert(parseAll(columnDefinition, "`id` BIGINT(64) NULL DEFAULT NULL AUTO_INCREMENT").successful)
    assert(parseAll(columnDefinition, "`id` BIGINT(64) UNSIGNED NULL DEFAULT 1 PRIMARY KEY").successful)
    assert(parseAll(columnDefinition, "`id` BIGINT(64) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY").successful)
    assert(parseAll(columnDefinition, "`id` BIGINT(64) visible").successful)
    assert(parseAll(columnDefinition, "`id` BIGINT(64) INVISIBLE").successful)
    assert(parseAll(columnDefinition, "`id` BIGINT(64) storage DISK").successful)
    assert(parseAll(columnDefinition, "`id` BIGINT(64) STORAGE memory").successful)
    assert(parseAll(columnDefinition, "`id` BIGINT(64) column_format FIXED").successful)
    assert(parseAll(columnDefinition, "`id` BIGINT(64) Column_format dynamic").successful)
    assert(parseAll(columnDefinition, "`id` BIGINT(64) COLUMN_FORMAT default").successful)
    assert(parseAll(columnDefinition, "`id` DOUBLE DEFAULT 5.4").successful)
    assert(
      parseAll(
        columnDefinition,
        "`id` BIGINT(64) UNSIGNED NOT NULL VISIBLE PRIMARY KEY COMMENT 'test' COLLATE ascii_bin COLUMN_FORMAT FIXED ENGINE_ATTRIBUTE InnoDB SECONDARY_ENGINE_ATTRIBUTE InnoDB STORAGE DISK"
      ).successful
    )
    assert(parseAll(columnDefinition, "`created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP").successful)
    assert(
      parseAll(
        columnDefinition,
        "`created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
      ).successful
    )
  }

  it should "Column parsing test fails." in {
    assert(!parseAll(columnDefinition, "Column parsing test fails.").successful)
    assert(!parseAll(columnDefinition, "`id`").successful)
    assert(!parseAll(columnDefinition, "'id' BIGINT").successful)
    assert(!parseAll(columnDefinition, "`id` failed").successful)
    assert(!parseAll(columnDefinition, "`id` DOUBLE DEFAULT 5.").successful)
    assert(!parseAll(columnDefinition, "`id` DOUBLE DEFAULT .5").successful)
  }
