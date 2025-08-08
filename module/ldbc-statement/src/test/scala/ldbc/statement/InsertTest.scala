/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import cats.data.NonEmptyList

import org.scalatest.flatspec.AnyFlatSpec

import ldbc.dsl.*

class InsertTest extends AnyFlatSpec:

  case class Table()

  it should "construct basic insert statement correctly" in {
    val insert = Insert.Impl[Table](
      Table(),
      "INSERT INTO table_name (column1, column2) VALUES (?, ?)",
      List(Parameter.Dynamic.Success("value1"), Parameter.Dynamic.Success("value2"))
    )
    assert(insert.statement === "INSERT INTO table_name (column1, column2) VALUES (?, ?)")
    assert(insert.params.length === 2)
  }

  it should "combine insert statement with SQL using ++ operator" in {
    val insert = Insert.Impl[Table](
      Table(),
      "INSERT INTO table_name (column1, column2) VALUES (?, ?)",
      List(Parameter.Dynamic.Success("value1"), Parameter.Dynamic.Success("value2"))
    )
    val combined = insert ++ sql" WHERE id = ${ 1 }"
    assert(combined.statement === "INSERT INTO table_name (column1, column2) VALUES (?, ?) WHERE id = ?")
    assert(combined.params.length === 3)
  }

  it should "construct onDuplicateKeyUpdate with column reference" in {
    val insert = Insert.Impl[Table](
      Table(),
      "INSERT INTO table_name (column1, column2) VALUES (?, ?)",
      List(Parameter.Dynamic.Success("value1"), Parameter.Dynamic.Success("value2"))
    )
    // Use the Column.apply(name) constructor which takes only a name
    val column             = Column[String]("column1")
    val duplicateKeyUpdate = insert.onDuplicateKeyUpdate(_ => column)
    val combined           = duplicateKeyUpdate ++ sql" AND WHERE is_active = ${ true }"

    assert(
      duplicateKeyUpdate.statement === "INSERT INTO table_name (column1, column2) VALUES (?, ?) ON DUPLICATE KEY UPDATE `column1` = VALUES(`column1`)"
    )
    assert(duplicateKeyUpdate.params.length === 2)
    assert(
      combined.statement === "INSERT INTO table_name (column1, column2) VALUES (?, ?) ON DUPLICATE KEY UPDATE `column1` = VALUES(`column1`) AND WHERE is_active = ?"
    )
    assert(combined.params.length === 3)
  }

  it should "construct onDuplicateKeyUpdate with explicit value" in {
    val insert = Insert.Impl[Table](
      Table(),
      "INSERT INTO table_name (column1, column2) VALUES (?, ?)",
      List(Parameter.Dynamic.Success("value1"), Parameter.Dynamic.Success("value2"))
    )
    // Use the Column.apply(name) constructor which takes only a name
    val column             = Column[String]("column1")
    val duplicateKeyUpdate = insert.onDuplicateKeyUpdate(_ => column, "new_value")

    assert(
      duplicateKeyUpdate.statement === "INSERT INTO table_name (column1, column2) VALUES (?, ?) ON DUPLICATE KEY UPDATE `column1` = ?"
    )
    assert(duplicateKeyUpdate.params.length === 3)
    assert(duplicateKeyUpdate.params.last.asInstanceOf[Parameter.Dynamic.Success].value === "new_value")
  }

  it should "construct values statement with single value" in {
    case class TestTable()
    val column: Column[(Int, String)] = Column[Int]("id").product(Column[String]("name"))
    val insertInto = Insert.Into[TestTable, (Int, String)](TestTable(), "INSERT INTO test_table", column)

    val values = insertInto.values((1, "Test"))

    assert(values.statement === "INSERT INTO test_table (`id`, `name`) VALUES (?,?)")
    assert(values.params.length === 2)
  }

  it should "construct values statement with multiple values" in {
    case class TestTable()
    val column: Column[(Int, String)] = Column[Int]("id").product(Column[String]("name"))
    val insertInto = Insert.Into[TestTable, (Int, String)](TestTable(), "INSERT INTO test_table", column)

    val values = insertInto.values((1, "Test1"), (2, "Test2"), (3, "Test3"))

    assert(values.statement === "INSERT INTO test_table (`id`, `name`) VALUES (?,?),(?,?),(?,?)")
    assert(values.params.length === 6)
  }

  it should "construct values statement with NonEmptyList" in {
    case class TestTable()
    val column: Column[(Int, String)] = Column[Int]("id").product(Column[String]("name"))
    val insertInto = Insert.Into[TestTable, (Int, String)](TestTable(), "INSERT INTO test_table", column)

    val valuesList = NonEmptyList.of((1, "Test1"), (2, "Test2"))
    val values     = insertInto.values(valuesList)

    assert(values.statement === "INSERT INTO test_table (`id`, `name`) VALUES (?,?),(?,?)")
    assert(values.params.length === 4)
  }

  it should "support onDuplicateKeyUpdate on Values" in {
    case class TestTable()
    val column: Column[(Int, String)] = Column[Int]("id").product(Column[String]("name"))
    val updateColumn = Column[String]("name")
    val insertInto   = Insert.Into[TestTable, (Int, String)](TestTable(), "INSERT INTO test_table", column)

    val values = insertInto.values((1, "Test"))

    val duplicateKeyUpdate = values.onDuplicateKeyUpdate(_ => updateColumn)

    assert(
      duplicateKeyUpdate.statement === "INSERT INTO test_table (`id`, `name`) VALUES (?,?) ON DUPLICATE KEY UPDATE `name` = VALUES(`name`)"
    )
    assert(duplicateKeyUpdate.params.length === 2)
  }

  it should "support onDuplicateKeyUpdate with explicit value on Values" in {
    case class TestTable()
    val column: Column[(Int, String)] = Column[Int]("id").product(Column[String]("name"))
    val updateColumn = Column[String]("name")
    val insertInto   = Insert.Into[TestTable, (Int, String)](TestTable(), "INSERT INTO test_table", column)

    val values = insertInto.values((1, "Test"))

    val duplicateKeyUpdate = values.onDuplicateKeyUpdate(_ => updateColumn, "updated")

    assert(
      duplicateKeyUpdate.statement === "INSERT INTO test_table (`id`, `name`) VALUES (?,?) ON DUPLICATE KEY UPDATE `name` = ?"
    )
    assert(duplicateKeyUpdate.params.length === 3)
    assert(duplicateKeyUpdate.params.last.asInstanceOf[Parameter.Dynamic.Success].value === "updated")
  }

  it should "combine DuplicateKeyUpdate with additional SQL" in {
    val insert = Insert.Impl[Table](
      Table(),
      "INSERT INTO table_name (column1, column2) VALUES (?, ?)",
      List(Parameter.Dynamic.Success("value1"), Parameter.Dynamic.Success("value2"))
    )
    val column             = Column[String]("column1")
    val duplicateKeyUpdate = insert.onDuplicateKeyUpdate(_ => column)

    val combined = duplicateKeyUpdate ++ sql" AND WHERE is_active = ${ true }"

    assert(
      combined.statement === "INSERT INTO table_name (column1, column2) VALUES (?, ?) ON DUPLICATE KEY UPDATE `column1` = VALUES(`column1`) AND WHERE is_active = ?"
    )
    assert(combined.params.length === 3)
  }
