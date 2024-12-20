/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.builder

import org.specs2.mutable.Specification

import ldbc.codegen.formatter.Naming
import ldbc.codegen.model.*
import ldbc.codegen.model.ColumnDefinition.*

object ColumnCodeBuilderTest extends Specification:

  private val builder = ColumnCodeBuilder(Naming.PASCAL)

  "Testing the ColumnCodeBuilder" should {
    "The construction of Column into a code string matches the specified string." in {
      val column = ColumnDefinition("p1", DataType.VARCHAR(255, None, None), None)
      builder.build(
        column,
        None
      ) === "def p1: Column[Option[String]] = column[Option[String]](\"p1\", VARCHAR[Option[String]](255))"
    }

    "The construction of Column into a code string matches the specified string." in {
      val column = ColumnDefinition("p1", DataType.VARCHAR(255, None, None), Some(List(Attribute.Condition(false))))
      builder.build(column, None) === "def p1: Column[String] = column[String](\"p1\", VARCHAR[String](255))"
    }

    "The construction of Column into a code string matches the specified string." in {
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
      builder.build(
        column,
        None
      ) === "def p1: Column[Long] = column[Long](\"p1\", BIGINT[Long], AUTO_INCREMENT, PRIMARY_KEY)"
    }

    "The construction of Column into a code string matches the specified string." in {
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
      builder.build(
        column,
        None
      ) === "def p1: Column[Long] = column[Long](\"p1\", BIGINT[Long], COMMENT(\"identifier\"))"
    }

    "The construction of Column into a code string matches the specified string." in {
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
      builder.build(
        column,
        None
      ) === "def p1: Column[String] = column[String](\"p1\", VARCHAR[String](255), Collate.utf8mb4_bin)"
    }

    "The construction of Column into a code string matches the specified string." in {
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
      builder.build(column, None) === "def p1: Column[String] = column[String](\"p1\", VARCHAR[String](255), VISIBLE)"
    }

    "The construction of Column into a code string matches the specified string." in {
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
      builder.build(
        column,
        None
      ) === "def p1: Column[String] = column[String](\"p1\", VARCHAR[String](255), COLUMN_FORMAT.FIXED)"
    }

    "The construction of Column into a code string matches the specified string." in {
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
      builder.build(
        column,
        None
      ) === "def p1: Column[String] = column[String](\"p1\", VARCHAR[String](255), STORAGE.DISK)"
    }

    "The construction of Column into a code string matches the specified string." in {
      val column = ColumnDefinition("p1", DataType.SERIAL(), None)
      builder.build(column, None) === "def p1: Column[BigInt] = column[BigInt](\"p1\", SERIAL[BigInt])"
    }
  }
