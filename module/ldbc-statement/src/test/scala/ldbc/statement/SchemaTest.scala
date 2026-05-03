/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

class SchemaTest extends munit.FunSuite:

  test("Schema.DDL should store a single statement") {
    val ddl = Schema.DDL("CREATE TABLE test")
    assertEquals(ddl.statements, List("CREATE TABLE test"))
  }

  test("Schema.DDL should concatenate two DDLs") {
    val ddl1     = Schema.DDL("CREATE TABLE test1")
    val ddl2     = Schema.DDL("CREATE TABLE test2")
    val combined = ddl1 ++ ddl2
    assertEquals(combined.statements, List("CREATE TABLE test1", "CREATE TABLE test2"))
  }

  test("Schema.empty should create an empty schema with empty statements") {
    val schema = Schema.empty
    assertEquals(schema.create.statements, List(""))
    assertEquals(schema.createIfNotExists.statements, List(""))
    assertEquals(schema.drop.statements, List(""))
    assertEquals(schema.dropIfExists.statements, List(""))
    assertEquals(schema.truncate.statements, List(""))
  }

  test("Schema.apply should create a schema with provided DDL statements") {
    val create            = Schema.DDL("CREATE TABLE test")
    val createIfNotExists = Schema.DDL("CREATE TABLE IF NOT EXISTS test")
    val drop              = Schema.DDL("DROP TABLE test")
    val dropIfExists      = Schema.DDL("DROP TABLE IF EXISTS test")
    val truncate          = Schema.DDL("TRUNCATE TABLE test")

    val schema = Schema(create, createIfNotExists, drop, dropIfExists, truncate)

    assertEquals(schema.create.statements, List("CREATE TABLE test"))
    assertEquals(schema.createIfNotExists.statements, List("CREATE TABLE IF NOT EXISTS test"))
    assertEquals(schema.drop.statements, List("DROP TABLE test"))
    assertEquals(schema.dropIfExists.statements, List("DROP TABLE IF EXISTS test"))
    assertEquals(schema.truncate.statements, List("TRUNCATE TABLE test"))
  }

  test("Schema++ should concatenate two schemas correctly") {
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

    assertEquals(combined.create.statements, List("CREATE TABLE test1", "CREATE TABLE test2"))
    assertEquals(
      combined.createIfNotExists.statements,
      List("CREATE TABLE IF NOT EXISTS test1", "CREATE TABLE IF NOT EXISTS test2")
    )
    assertEquals(combined.drop.statements, List("DROP TABLE test1", "DROP TABLE test2"))
    assertEquals(combined.dropIfExists.statements, List("DROP TABLE IF EXISTS test1", "DROP TABLE IF EXISTS test2"))
    assertEquals(combined.truncate.statements, List("TRUNCATE TABLE test1", "TRUNCATE TABLE test2"))
  }

  test("Schema methods should return the correct DDL statements") {
    val schema = Schema(
      Schema.DDL("CREATE TABLE test"),
      Schema.DDL("CREATE TABLE IF NOT EXISTS test"),
      Schema.DDL("DROP TABLE test"),
      Schema.DDL("DROP TABLE IF EXISTS test"),
      Schema.DDL("TRUNCATE TABLE test")
    )

    assertEquals(schema.create.statements, List("CREATE TABLE test"))
    assertEquals(schema.createIfNotExists.statements, List("CREATE TABLE IF NOT EXISTS test"))
    assertEquals(schema.drop.statements, List("DROP TABLE test"))
    assertEquals(schema.dropIfExists.statements, List("DROP TABLE IF EXISTS test"))
    assertEquals(schema.truncate.statements, List("TRUNCATE TABLE test"))
  }

  test("Multiple DDL concatenation should work correctly") {
    val ddl1 = Schema.DDL("CREATE TABLE test1")
    val ddl2 = Schema.DDL("CREATE TABLE test2")
    val ddl3 = Schema.DDL("CREATE TABLE test3")

    val combined = ddl1 ++ ddl2 ++ ddl3
    assertEquals(combined.statements, List("CREATE TABLE test1", "CREATE TABLE test2", "CREATE TABLE test3"))
  }
