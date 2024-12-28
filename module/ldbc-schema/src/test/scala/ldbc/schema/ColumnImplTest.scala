/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import org.scalatest.flatspec.AnyFlatSpec

import ldbc.dsl.codec.{Decoder, Encoder}
import ldbc.schema.DataType.*
import ldbc.schema.attribute.*

class ColumnImplTest extends AnyFlatSpec:

  private def column[A](name: String, dataType: DataType[A], attributes: Attribute[A]*)(using
    elem: Decoder.Elem[A],
                                                                                        encoder: Encoder[A]
  ): Column[A] =
    val decoder = new Decoder[A]((resultSet, prefix) => elem.decode(resultSet, prefix.getOrElse(name)))
    ColumnImpl[A](name, None, decoder, encoder, Some(dataType), attributes.toList)

  it should "The query string of the Column model generated with only label and DataType matches the specified string." in {
    assert(column[Long]("id", BIGINT).statement === "`id` BIGINT NOT NULL")
    assert(
      column[String](
        "name",
        VARCHAR(255).CHARACTER_SET(Character.ascii)
      ).statement === "`name` VARCHAR(255) CHARACTER SET ascii NOT NULL"
    )
  }

  it should "The query string of the Column model generated with only label and DataType and comment matches the specified string." in {
    assert(
      column[Long](
        "id",
        BIGINT,
        COMMENT("identifier")
      ).statement === "`id` BIGINT NOT NULL COMMENT 'identifier'"
    )
    assert(
      column[String](
        "name",
        VARCHAR(255).CHARACTER_SET(Character.ascii),
        COMMENT("name")
      ).statement === "`name` VARCHAR(255) CHARACTER SET ascii NOT NULL COMMENT 'name'"
    )
    assert(
      column[String](
        "name",
        VARCHAR(255).CHARACTER_SET(Character.ascii).COLLATE(Collate.ascii_bin),
        COMMENT("name")
      ).statement === "`name` VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'name'"
    )
    assert(column[BigInt]("id", SERIAL).statement === "`id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE KEY")
  }

  it should "The query string of the Column model generated with only label and DataType and attributes matches the specified string." in {
    assert(column[Long]("id", BIGINT, AutoInc[Long]()).statement === "`id` BIGINT NOT NULL AUTO_INCREMENT")
  }

  it should "The query string of the Column model generated with only label and DataType and attributes and comment matches the specified string." in {
    assert(
      column[Long](
        "id",
        BIGINT,
        AUTO_INCREMENT,
        COMMENT("identifier")
      ).statement === "`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'identifier'"
    )
  }

  it should "The query string of the column with all Attributes set matches the specified string." in {
    assert(
      column[String](
        "name",
        VARCHAR(255).CHARACTER_SET(Character.ascii),
        COMMENT("name"),
        UNIQUE_KEY,
        VISIBLE,
        COLUMN_FORMAT.FIXED,
        Collate.ascii_bin,
        STORAGE.MEMORY
      ).statement === "`name` VARCHAR(255) CHARACTER SET ascii NOT NULL COMMENT 'name' UNIQUE KEY /*!80023 VISIBLE */ /*!50606 COLUMN_FORMAT FIXED */ COLLATE ascii_bin /*!50606 STORAGE MEMORY */"
    )
  }
