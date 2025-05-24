/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.model

import munit.CatsEffectSuite

class ColumnDefinitionTest extends CatsEffectSuite:

  test("ColumnDefinition should have correct isOptional value when Condition attribute is present") {
    val notNullColumn = ColumnDefinition(
      "id",
      DataType.BIGINT(None, false, false),
      Some(List(ColumnDefinition.Attribute.Condition(false)))
    )

    val nullableColumn = ColumnDefinition(
      "name",
      DataType.VARCHAR(255, None, None),
      Some(List(ColumnDefinition.Attribute.Condition(true)))
    )

    assertEquals(notNullColumn.isOptional, false)
    assertEquals(nullableColumn.isOptional, true)
  }

  test("ColumnDefinition should default to isOptional=true when no Condition attribute is provided") {
    val column = ColumnDefinition(
      "description",
      DataType.TEXT(None, None, None),
      None
    )

    assertEquals(column.isOptional, true)
  }

  test("ColumnDefinition should format _attributes string correctly") {
    val column = ColumnDefinition(
      "email",
      DataType.VARCHAR(100, None, None),
      Some(
        List(
          ColumnDefinition.Attribute.Condition(false),
          CommentSet("User email address"),
          ColumnDefinition.Attribute.Key("PRIMARY KEY"),
          ColumnDefinition.Attribute.Collate("utf8mb4_unicode_ci")
        )
      )
    )

    val expected = ", COMMENT(\"User email address\"), PRIMARY KEY, Collate.utf8mb4_unicode_ci"
    assertEquals(column._attributes, expected)
  }

  test("ColumnDefinition should return empty string for _attributes when no attributes provided") {
    val column = ColumnDefinition("created_at", DataType.DATETIME(None), None)
    assertEquals(column._attributes, "")
  }

  test("ColumnDefinition should handle all attribute types correctly") {
    val column = ColumnDefinition(
      "status",
      DataType.ENUM(List("active", "inactive", "suspended"), None, None),
      Some(
        List(
          ColumnDefinition.Attribute.Condition(false),
          CommentSet("User status"),
          ColumnDefinition.Attribute.Visible("VISIBLE"),
          ColumnDefinition.Attribute.ColumnFormat("DYNAMIC"),
          ColumnDefinition.Attribute.Storage("DISK")
        )
      )
    )

    val expected = ", COMMENT(\"User status\"), VISIBLE, COLUMN_FORMAT.DYNAMIC, STORAGE.DISK"
    assertEquals(column._attributes, expected)
  }

  test("ColumnDefinition should filter out unsupported attributes in _attributes string") {
    val defaultAttr = ColumnDefinition.Attribute.Default.Value("active")
    val column = ColumnDefinition(
      "status",
      DataType.ENUM(List("active", "inactive", "suspended"), None, None),
      Some(
        List(
          ColumnDefinition.Attribute.Condition(false),
          defaultAttr,
          CommentSet("User status")
        )
      )
    )

    val expected = ", COMMENT(\"User status\")"
    assertEquals(column._attributes, expected)
  }
