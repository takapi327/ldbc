/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.attribute

class PackageTest extends munit.FunSuite:

  test("generate proper query string for Comment attribute") {
    val comment = Comment[String]("This is a comment")
    assertEquals(comment.queryString, "COMMENT 'This is a comment'")
  }

  test("generate proper query string for Visible attribute") {
    val visible = Visible[String]()
    assertEquals(visible.queryString, "/*!80023 VISIBLE */")
  }

  test("generate proper query string for InVisible attribute") {
    val invisible = InVisible[String]()
    assertEquals(invisible.queryString, "/*!80023 INVISIBLE */")
  }

  test("generate proper query string for ColumnFormat.Fixed attribute") {
    val fixed = ColumnFormat.Fixed[String]()
    assertEquals(fixed.queryString, "/*!50606 COLUMN_FORMAT FIXED */")
  }

  test("generate proper query string for ColumnFormat.Dynamic attribute") {
    val dynamic = ColumnFormat.Dynamic[String]()
    assertEquals(dynamic.queryString, "/*!50606 COLUMN_FORMAT DYNAMIC */")
  }

  test("generate proper query string for ColumnFormat.Default attribute") {
    val default = ColumnFormat.Default[String]()
    assertEquals(default.queryString, "/*!50606 COLUMN_FORMAT DEFAULT */")
  }

  test("generate proper query string for Storage.Disk attribute") {
    val disk = Storage.Disk[String]()
    assertEquals(disk.queryString, "/*!50606 STORAGE DISK */")
  }

  test("generate proper query string for Storage.Memory attribute") {
    val memory = Storage.Memory[String]()
    assertEquals(memory.queryString, "/*!50606 STORAGE MEMORY */")
  }
