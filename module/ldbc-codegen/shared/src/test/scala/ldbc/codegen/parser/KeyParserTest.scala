/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.parser

import munit.CatsEffectSuite

class KeyParserTest extends CatsEffectSuite, KeyParser:

  override def fileName: String = "test.sql"

  test("Key definitions parsing test succeeds.") {
    assert(parseAll(keyDefinitions, "INDEX (`column`)").successful)
    assert(parseAll(keyDefinitions, "KEY (`column`)").successful)
    assert(parseAll(keyDefinitions, "INDEX `index` (`column1`, `column2`)").successful)
    assert(parseAll(keyDefinitions, "FULLTEXT INDEX (`column1`, `column2`)").successful)
    assert(parseAll(keyDefinitions, "SPATIAL KEY (`column1`)").successful)
    assert(parseAll(keyDefinitions, "PRIMARY KEY (`column1`)").successful)
    assert(parseAll(keyDefinitions, "constraint PRIMARY KEY (`column1`)").successful)
    assert(parseAll(keyDefinitions, "CONSTRAINT `index` PRIMARY KEY (`column1`)").successful)
    assert(parseAll(keyDefinitions, "constraint unique KEY (`column1`)").successful)
    assert(parseAll(keyDefinitions, "CONSTRAINT `index` unique `index` (`column1`)").successful)
    assert(parseAll(keyDefinitions, "CONSTRAINT `index` unique KEY `index` (`column1`)").successful)
    assert(parseAll(keyDefinitions, "CONSTRAINT `index` unique Index `index` (`column1`)").successful)
    assert(parseAll(keyDefinitions, "constraint foreign key (`column1`) references table_name (`column1`)").successful)
    assert(
      parseAll(
        keyDefinitions,
        "constraint `index` foreign key (`column1`) references table_name (`column1`) match full on delete restrict"
      ).successful
    )
    assert(
      parseAll(
        keyDefinitions,
        "constraint foreign key (`column1`) references table_name (`column1`) on update set null"
      ).successful
    )
    assert(
      parseAll(
        keyDefinitions,
        "foreign key (`column1`) references table_name (`column1`) match full on delete restrict on update cascade"
      ).successful
    )
    assert(parseAll(keyDefinitions, "constraint check (column1 > 1)").successful)
    assert(
      parseAll(
        keyDefinitions,
        "INDEX `index` USING HASH (`column`) KEY_BLOCK_SIZE=1 USING HASH WITH PARSER ngram COMMENT 'comment' VISIBLE ENGINE_ATTRIBUTE=InnoDB SECONDARY_ENGINE_ATTRIBUTE=InnoDB"
      ).successful
    )
  }

  test("Key definitions parsing test fails.") {
    assert(!parseAll(keyDefinitions, "failed").successful)
    assert(!parseAll(keyDefinitions, "INDEX USING failed (`column`)").successful)
    assert(!parseAll(keyDefinitions, "INDEX (`column` failed)").successful)
    assert(!parseAll(keyDefinitions, "INDEX KEY").successful)
    assert(!parseAll(keyDefinitions, "INDEX (`column`) key_block_size=3").successful)
    assert(!parseAll(keyDefinitions, "FULLTEXT (title,body) WITH ngram").successful)
    assert(!parseAll(keyDefinitions, "INDEX (`column`) engine_attribute ==InnoDB").successful)
    assert(!parseAll(keyDefinitions, "INDEX (`column`) secondary_engine_attribute ==InnoDB").successful)
    assert(!parseAll(keyDefinitions, "FULLTEXT index key `index` (title,body) WITH ngram").successful)
    assert(
      !parseAll(
        keyDefinitions,
        "foreign key `index` (title,body) references table_name (title,body) match failed"
      ).successful
    )
    assert(
      !parseAll(
        keyDefinitions,
        "foreign key `index` (title,body) references table_name (title,body) match FULL on delete failed"
      ).successful
    )
    assert(
      !parseAll(
        keyDefinitions,
        "foreign key `index` (title,body) references table_name (title,body) match FULL on failed failed"
      ).successful
    )
    assert(
      !parseAll(
        keyDefinitions,
        "foreign key `index` (title,body) references table_name (title,body) match FULL on failed SET NULL"
      ).successful
    )
    assert(!parseAll(keyDefinitions, "check (column1 > 1").successful)
  }
