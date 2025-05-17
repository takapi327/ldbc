/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SchemaTest extends AnyFlatSpec with Matchers:

  "Schema.DDL" should "store a single statement" in {
    val ddl = Schema.DDL("CREATE TABLE test")
    ddl.statements should be(List("CREATE TABLE test"))
  }

  it should "concatenate two DDLs" in {
    val ddl1 = Schema.DDL("CREATE TABLE test1")
    val ddl2 = Schema.DDL("CREATE TABLE test2")
    val combined = ddl1 ++ ddl2
    combined.statements should be(List("CREATE TABLE test1", "CREATE TABLE test2"))
  }

  "Schema.empty" should "create an empty schema with empty statements" in {
    val schema = Schema.empty
    schema.create.statements should be(List(""))
    schema.createIfNotExists.statements should be(List(""))
    schema.drop.statements should be(List(""))
    schema.dropIfExists.statements should be(List(""))
    schema.truncate.statements should be(List(""))
  }

  "Schema.apply" should "create a schema with provided DDL statements" in {
    val create = Schema.DDL("CREATE TABLE test")
    val createIfNotExists = Schema.DDL("CREATE TABLE IF NOT EXISTS test")
    val drop = Schema.DDL("DROP TABLE test")
    val dropIfExists = Schema.DDL("DROP TABLE IF EXISTS test")
    val truncate = Schema.DDL("TRUNCATE TABLE test")

    val schema = Schema(create, createIfNotExists, drop, dropIfExists, truncate)

    schema.create.statements should be(List("CREATE TABLE test"))
    schema.createIfNotExists.statements should be(List("CREATE TABLE IF NOT EXISTS test"))
    schema.drop.statements should be(List("DROP TABLE test"))
    schema.dropIfExists.statements should be(List("DROP TABLE IF EXISTS test"))
    schema.truncate.statements should be(List("TRUNCATE TABLE test"))
  }

  "Schema++" should "concatenate two schemas correctly" in {
    val schema1 = Schema(
      Schema.DDL("CREATE TABLE test1"),
      Schema.DDL("CREATE TABLE IF NOT EXISTS test1"),
      Schema.DDL("DROP TABLE test1"),
      Schema.DDL("DROP TABLE IF EXISTS test1"),
      Schema.DDL("TRUNCATE TABLE test1")
    )

    val schema2 = Schema(
      Schema.DDL("CREATE TABLE test2"),
      Schema.DDL("CREATE TABLE IF NOT EXISTS test2"),
      Schema.DDL("DROP TABLE test2"),
      Schema.DDL("DROP TABLE IF EXISTS test2"),
      Schema.DDL("TRUNCATE TABLE test2")
    )

    val combined = schema1 ++ schema2

    combined.create.statements should be(List("CREATE TABLE test1", "CREATE TABLE test2"))
    combined.createIfNotExists.statements should be(List("CREATE TABLE IF NOT EXISTS test1", "CREATE TABLE IF NOT EXISTS test2"))
    combined.drop.statements should be(List("DROP TABLE test1", "DROP TABLE test2"))
    combined.dropIfExists.statements should be(List("DROP TABLE IF EXISTS test1", "DROP TABLE IF EXISTS test2"))
    combined.truncate.statements should be(List("TRUNCATE TABLE test1", "TRUNCATE TABLE test2"))
  }

  "Schema methods" should "return the correct DDL statements" in {
    val schema = Schema(
      Schema.DDL("CREATE TABLE test"),
      Schema.DDL("CREATE TABLE IF NOT EXISTS test"),
      Schema.DDL("DROP TABLE test"),
      Schema.DDL("DROP TABLE IF EXISTS test"),
      Schema.DDL("TRUNCATE TABLE test")
    )

    schema.create.statements should be(List("CREATE TABLE test"))
    schema.createIfNotExists.statements should be(List("CREATE TABLE IF NOT EXISTS test"))
    schema.drop.statements should be(List("DROP TABLE test"))
    schema.dropIfExists.statements should be(List("DROP TABLE IF EXISTS test"))
    schema.truncate.statements should be(List("TRUNCATE TABLE test"))
  }

  "Multiple DDL concatenation" should "work correctly" in {
    val ddl1 = Schema.DDL("CREATE TABLE test1")
    val ddl2 = Schema.DDL("CREATE TABLE test2")
    val ddl3 = Schema.DDL("CREATE TABLE test3")
    
    val combined = ddl1 ++ ddl2 ++ ddl3
    combined.statements should be(List("CREATE TABLE test1", "CREATE TABLE test2", "CREATE TABLE test3"))
  }