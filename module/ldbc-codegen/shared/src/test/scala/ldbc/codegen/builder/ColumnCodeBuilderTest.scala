/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.builder

import munit.CatsEffectSuite

import ldbc.codegen.formatter.Naming
import ldbc.codegen.model.*
import ldbc.codegen.model.ColumnDefinition.*

class ColumnCodeBuilderTest extends CatsEffectSuite:

  private val builder = ColumnCodeBuilder(Naming.PASCAL)

  test("The construction of Column into a code string matches the specified string.") {
    val column = ColumnDefinition("p1", DataType.VARCHAR(255, None, None), None)
    assertEquals(
      builder.build(
        column,
        None
      ),
      "def p1: Column[Option[String]] = column[Option[String]](\"p1\", VARCHAR[Option[String]](255))"
    )
  }

  test("The construction of Column into a code string matches the specified string.") {
    val column = ColumnDefinition("p1", DataType.VARCHAR(255, None, None), Some(List(Attribute.Condition(false))))
    assertEquals(
      builder.build(column, None),
      "def p1: Column[String] = column[String](\"p1\", VARCHAR[String](255))"
    )
  }
  
  test("The construction of Column into a code string matches the specified string.") {
    val column = ColumnDefinition(
      "p1",
      DataType.BIGINT(None, false, false),
      Some(
        List(
          Attribute.Condition(false),
          Attribute.Key("AUTO_INCREMENT"),
          Attribute.Key("PRIMARY_KEY")
        )
      )
    )
    assertEquals(
      builder.build(
        column,
        None
      ),
      "def p1: Column[Long] = column[Long](\"p1\", BIGINT[Long], AUTO_INCREMENT, PRIMARY_KEY)"
    )
  }
  
  test("The construction of Column into a code string matches the specified string.") {
    val column = ColumnDefinition(
      "p1",
      DataType.BIGINT(None, false, false),
      Some(
        List(
          Attribute.Condition(false),
          CommentSet("identifier")
        )
      )
    )
    assertEquals(
      builder.build(
        column,
        None
      ),
      "def p1: Column[Long] = column[Long](\"p1\", BIGINT[Long], COMMENT(\"identifier\"))"
    )
  }
  
  test("The construction of Column into a code string matches the specified string.") {
    val column = ColumnDefinition(
      "p1",
      DataType.VARCHAR(255, None, None),
      Some(
        List(
          Attribute.Condition(false),
          Attribute.Collate("utf8mb4_bin")
        )
      )
    )
    assertEquals(
      builder.build(
        column,
        None
      ),
      "def p1: Column[String] = column[String](\"p1\", VARCHAR[String](255), Collate.utf8mb4_bin)"
    )
  }
  
  test("The construction of Column into a code string matches the specified string.") {
    val column = ColumnDefinition(
      "p1",
      DataType.VARCHAR(255, None, None),
      Some(
        List(
          Attribute.Condition(false),
          Attribute.Visible("VISIBLE")
        )
      )
    )
    assertEquals(
      builder.build(column, None),
      "def p1: Column[String] = column[String](\"p1\", VARCHAR[String](255), VISIBLE)"
    )
  }
  
  test("The construction of Column into a code string matches the specified string.") {
    val column = ColumnDefinition(
      "p1",
      DataType.VARCHAR(255, None, None),
      Some(
        List(
          Attribute.Condition(false),
          Attribute.ColumnFormat("FIXED")
        )
      )
    )
    assertEquals(
      builder.build(
        column,
        None
      ),
      "def p1: Column[String] = column[String](\"p1\", VARCHAR[String](255), COLUMN_FORMAT.FIXED)"
    )
  }
  
  test("The construction of Column into a code string matches the specified string.") {
    val column = ColumnDefinition(
      "p1",
      DataType.VARCHAR(255, None, None),
      Some(
        List(
          Attribute.Condition(false),
          Attribute.Storage("DISK")
        )
      )
    )
    assertEquals(
      builder.build(
        column,
        None
      ),
      "def p1: Column[String] = column[String](\"p1\", VARCHAR[String](255), STORAGE.DISK)"
    )
  }
  
  test("The construction of Column into a code string matches the specified string.") {
    val column = ColumnDefinition("p1", DataType.SERIAL(), None)
    assertEquals(
      builder.build(column, None),
      "def p1: Column[BigInt] = column[BigInt](\"p1\", SERIAL[BigInt])"
    )
  }
