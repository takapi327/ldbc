/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.attribute

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PackageTest extends AnyFlatSpec, Matchers:

  it should "generate proper query string for Comment attribute" in {
    val comment = Comment[String]("This is a comment")
    comment.queryString should be("COMMENT 'This is a comment'")
  }

  it should "generate proper query string for Visible attribute" in {
    val visible = Visible[String]()
    visible.queryString should be("/*!80023 VISIBLE */")
  }

  it should "generate proper query string for InVisible attribute" in {
    val invisible = InVisible[String]()
    invisible.queryString should be("/*!80023 INVISIBLE */")
  }

  it should "generate proper query string for ColumnFormat.Fixed attribute" in {
    val fixed = ColumnFormat.Fixed[String]()
    fixed.queryString should be("/*!50606 COLUMN_FORMAT FIXED */")
  }

  it should "generate proper query string for ColumnFormat.Dynamic attribute" in {
    val dynamic = ColumnFormat.Dynamic[String]()
    dynamic.queryString should be("/*!50606 COLUMN_FORMAT DYNAMIC */")
  }

  it should "generate proper query string for ColumnFormat.Default attribute" in {
    val default = ColumnFormat.Default[String]()
    default.queryString should be("/*!50606 COLUMN_FORMAT DEFAULT */")
  }

  it should "generate proper query string for Storage.Disk attribute" in {
    val disk = Storage.Disk[String]()
    disk.queryString should be("/*!50606 STORAGE DISK */")
  }

  it should "generate proper query string for Storage.Memory attribute" in {
    val memory = Storage.Memory[String]()
    memory.queryString should be("/*!50606 STORAGE MEMORY */")
  }
