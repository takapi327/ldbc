/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import ldbc.schema.Character.*
import ldbc.schema.DataType.*
import ldbc.schema.attribute.AutoInc

import org.scalatest.flatspec.AnyFlatSpec

class ColumnImplTest extends AnyFlatSpec:

  it should "The query string of the Column model generated with only label and DataType matches the specified string." in {
    column[Long]("id", BIGINT(64)).queryString === "`id` BIGINT(64) NOT NULL" &&
    column[String](
      "name",
      VARCHAR(255).CHARACTER_SET(Character.ascii)
    ).queryString === "`name` VARCHAR(255) CHARACTER SET ascii NOT NULL"
  }

  it should "The query string of the Column model generated with only label and DataType and comment matches the specified string." in {
    column[Long](
      "id",
      BIGINT(64),
      COMMENT("identifier")
    ).queryString === "`id` BIGINT(64) NOT NULL COMMENT 'identifier'" &&
    column[String](
      "name",
      VARCHAR(255).CHARACTER_SET(Character.ascii),
      COMMENT("name")
    ).queryString === "`name` VARCHAR(255) CHARACTER SET ascii NOT NULL COMMENT 'name'" &&
    column[String](
      "name",
      VARCHAR(255).CHARACTER_SET(Character.ascii).COLLATE(Collate.ascii_bin),
      COMMENT("name")
    ).queryString === "`name` VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'name'" &&
    column[BigInt]("id", SERIAL).queryString === "`id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE KEY"
  }

  it should "The query string of the Column model generated with only label and DataType and attributes matches the specified string." in {
    column[Long]("id", BIGINT(64), AutoInc[Long]()).queryString === "`id` BIGINT(64) NOT NULL AUTO_INCREMENT"
  }

  it should "The query string of the Column model generated with only label and DataType and attributes and comment matches the specified string." in {
    column[Long](
      "id",
      BIGINT(64),
      AUTO_INCREMENT,
      COMMENT("identifier")
    ).queryString === "`id` BIGINT(64) NOT NULL AUTO_INCREMENT COMMENT 'identifier'"
  }

  it should "The query string of the column with all Attributes set matches the specified string." in {
    column[String](
      "name",
      VARCHAR(255).CHARACTER_SET(Character.ascii),
      COMMENT("name"),
      UNIQUE_KEY,
      VISIBLE,
      COLUMN_FORMAT.FIXED,
      Collate.ascii_bin,
      STORAGE.MEMORY
    ).queryString === "`name` VARCHAR(255) CHARACTER SET ascii NOT NULL COMMENT 'name' UNIQUE KEY /*!80023 VISIBLE */ /*!50606 COLUMN_FORMAT FIXED */ COLLATE ascii_bin /*!50606 STORAGE MEMORY */"
  }
