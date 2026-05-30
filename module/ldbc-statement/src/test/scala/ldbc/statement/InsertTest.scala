/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import cats.data.NonEmptyList

import ldbc.dsl.*

class InsertTest extends munit.FunSuite:

  case class Table()

  test("construct basic insert statement correctly") {
    val insert = Insert.Impl[Table](
      Table(),
      "INSERT INTO table_name (column1, column2) VALUES (?, ?)",
      List(Parameter.Dynamic.Success("value1"), Parameter.Dynamic.Success("value2"))
    )
    assertEquals(insert.statement, "INSERT INTO table_name (column1, column2) VALUES (?, ?)")
    assertEquals(insert.params.length, 2)
  }

  test("combine insert statement with SQL using ++ operator") {
    val insert = Insert.Impl[Table](
      Table(),
      "INSERT INTO table_name (column1, column2) VALUES (?, ?)",
      List(Parameter.Dynamic.Success("value1"), Parameter.Dynamic.Success("value2"))
    )
    val combined = insert ++ sql" WHERE id = ${ 1 }"
    assertEquals(combined.statement, "INSERT INTO table_name (column1, column2) VALUES (?, ?) WHERE id = ?")
    assertEquals(combined.params.length, 3)
  }

  test("construct onDuplicateKeyUpdate with column reference") {
    val insert = Insert.Impl[Table](
      Table(),
      "INSERT INTO table_name (column1, column2) VALUES (?, ?)",
      List(Parameter.Dynamic.Success("value1"), Parameter.Dynamic.Success("value2"))
    )
    // Use the Column.apply(name) constructor which takes only a name
    val column             = Column[String]("column1")
    val duplicateKeyUpdate = insert.onDuplicateKeyUpdate(_ => column)
    val combined           = duplicateKeyUpdate ++ sql" AND WHERE is_active = ${ true }"

    assertEquals(
      duplicateKeyUpdate.statement,
      "INSERT INTO table_name (column1, column2) VALUES (?, ?) ON DUPLICATE KEY UPDATE `column1` = VALUES(`column1`)"
    )
    assertEquals(duplicateKeyUpdate.params.length, 2)
    assertEquals(
      combined.statement,
      "INSERT INTO table_name (column1, column2) VALUES (?, ?) ON DUPLICATE KEY UPDATE `column1` = VALUES(`column1`) AND WHERE is_active = ?"
    )
    assertEquals(combined.params.length, 3)
  }

  test("construct onDuplicateKeyUpdate with explicit value") {
    val insert = Insert.Impl[Table](
      Table(),
      "INSERT INTO table_name (column1, column2) VALUES (?, ?)",
      List(Parameter.Dynamic.Success("value1"), Parameter.Dynamic.Success("value2"))
    )
    // Use the Column.apply(name) constructor which takes only a name
    val column             = Column[String]("column1")
    val duplicateKeyUpdate = insert.onDuplicateKeyUpdate(_ => column, "new_value")

    assertEquals(
      duplicateKeyUpdate.statement,
      "INSERT INTO table_name (column1, column2) VALUES (?, ?) ON DUPLICATE KEY UPDATE `column1` = ?"
    )
    assertEquals(duplicateKeyUpdate.params.length, 3)
    assertEquals(duplicateKeyUpdate.params.last.asInstanceOf[Parameter.Dynamic.Success].value, "new_value")
  }

  test("construct values statement with single value") {
    case class TestTable()
    val column: Column[(Int, String)] = Column[Int]("id").product(Column[String]("name"))
    val insertInto = Insert.Into[TestTable, (Int, String)](TestTable(), "INSERT INTO test_table", column)

    val values = insertInto.values((1, "Test"))

    assertEquals(values.statement, "INSERT INTO test_table (`id`, `name`) VALUES (?,?)")
    assertEquals(values.params.length, 2)
  }

  test("construct values statement with multiple values") {
    case class TestTable()
    val column: Column[(Int, String)] = Column[Int]("id").product(Column[String]("name"))
    val insertInto = Insert.Into[TestTable, (Int, String)](TestTable(), "INSERT INTO test_table", column)

    val values = insertInto.values((1, "Test1"), (2, "Test2"), (3, "Test3"))

    assertEquals(values.statement, "INSERT INTO test_table (`id`, `name`) VALUES (?,?),(?,?),(?,?)")
    assertEquals(values.params.length, 6)
  }

  test("construct values statement with NonEmptyList") {
    case class TestTable()
    val column: Column[(Int, String)] = Column[Int]("id").product(Column[String]("name"))
    val insertInto = Insert.Into[TestTable, (Int, String)](TestTable(), "INSERT INTO test_table", column)

    val valuesList = NonEmptyList.of((1, "Test1"), (2, "Test2"))
    val values     = insertInto.values(valuesList)

    assertEquals(values.statement, "INSERT INTO test_table (`id`, `name`) VALUES (?,?),(?,?)")
    assertEquals(values.params.length, 4)
  }

  test("support onDuplicateKeyUpdate on Values") {
    case class TestTable()
    val column: Column[(Int, String)] = Column[Int]("id").product(Column[String]("name"))
    val updateColumn = Column[String]("name")
    val insertInto   = Insert.Into[TestTable, (Int, String)](TestTable(), "INSERT INTO test_table", column)

    val values = insertInto.values((1, "Test"))

    val duplicateKeyUpdate = values.onDuplicateKeyUpdate(_ => updateColumn)

    assertEquals(
      duplicateKeyUpdate.statement,
      "INSERT INTO test_table (`id`, `name`) VALUES (?,?) ON DUPLICATE KEY UPDATE `name` = VALUES(`name`)"
    )
    assertEquals(duplicateKeyUpdate.params.length, 2)
  }

  test("support onDuplicateKeyUpdate with explicit value on Values") {
    case class TestTable()
    val column: Column[(Int, String)] = Column[Int]("id").product(Column[String]("name"))
    val updateColumn = Column[String]("name")
    val insertInto   = Insert.Into[TestTable, (Int, String)](TestTable(), "INSERT INTO test_table", column)

    val values = insertInto.values((1, "Test"))

    val duplicateKeyUpdate = values.onDuplicateKeyUpdate(_ => updateColumn, "updated")

    assertEquals(
      duplicateKeyUpdate.statement,
      "INSERT INTO test_table (`id`, `name`) VALUES (?,?) ON DUPLICATE KEY UPDATE `name` = ?"
    )
    assertEquals(duplicateKeyUpdate.params.length, 3)
    assertEquals(duplicateKeyUpdate.params.last.asInstanceOf[Parameter.Dynamic.Success].value, "updated")
  }

  test("combine DuplicateKeyUpdate with additional SQL") {
    val insert = Insert.Impl[Table](
      Table(),
      "INSERT INTO table_name (column1, column2) VALUES (?, ?)",
      List(Parameter.Dynamic.Success("value1"), Parameter.Dynamic.Success("value2"))
    )
    val column             = Column[String]("column1")
    val duplicateKeyUpdate = insert.onDuplicateKeyUpdate(_ => column)

    val combined = duplicateKeyUpdate ++ sql" AND WHERE is_active = ${ true }"

    assertEquals(
      combined.statement,
      "INSERT INTO table_name (column1, column2) VALUES (?, ?) ON DUPLICATE KEY UPDATE `column1` = VALUES(`column1`) AND WHERE is_active = ?"
    )
    assertEquals(combined.params.length, 3)
  }
