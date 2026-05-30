/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.attribute

class AutoIncTest extends munit.FunSuite:

  test("AutoInc should be instantiated with type parameter") {
    val autoIncInt    = AutoInc[Int]()
    val autoIncString = AutoInc[String]()
    val autoIncLong   = AutoInc[Long]()

    assert(autoIncInt.isInstanceOf[AutoInc[_]])
    assert(autoIncString.isInstanceOf[AutoInc[_]])
    assert(autoIncLong.isInstanceOf[AutoInc[_]])
  }

  test("AutoInc should return 'AUTO_INCREMENT' from queryString method") {
    val autoInc = AutoInc[Int]()
    assertEquals(autoInc.queryString, "AUTO_INCREMENT")
  }

  test("AutoInc should return 'AUTO_INCREMENT' from toString method") {
    val autoInc = AutoInc[String]()
    assertEquals(autoInc.toString, "AUTO_INCREMENT")
  }

  test("AutoInc should be an Attribute") {
    val autoInc = AutoInc[Double]()
    assert(autoInc.isInstanceOf[Attribute[_]])
  }
