/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.attribute

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AutoIncTest extends AnyFlatSpec with Matchers {

  "AutoInc" should "be instantiated with type parameter" in {
    val autoIncInt    = AutoInc[Int]()
    val autoIncString = AutoInc[String]()
    val autoIncLong   = AutoInc[Long]()

    autoIncInt shouldBe a[AutoInc[_]]
    autoIncString shouldBe a[AutoInc[_]]
    autoIncLong shouldBe a[AutoInc[_]]
  }

  it should "return 'AUTO_INCREMENT' from queryString method" in {
    val autoInc = AutoInc[Int]()
    autoInc.queryString should be("AUTO_INCREMENT")
  }

  it should "return 'AUTO_INCREMENT' from toString method" in {
    val autoInc = AutoInc[String]()
    autoInc.toString should be("AUTO_INCREMENT")
  }

  it should "maintain its type parameter" in {
    val autoIncInt    = AutoInc[Int]()
    val autoIncString = AutoInc[String]()

    // Type checking using pattern matching
    autoIncInt match {
      case _: AutoInc[Int]    => succeed
      case _: AutoInc[String] => fail("AutoInc[Int] was incorrectly matched as AutoInc[String]")
      case _                  => fail("AutoInc[Int] was not matched correctly")
    }

    autoIncString match {
      case _: AutoInc[String] => succeed
      case _: AutoInc[Int]    => fail("AutoInc[String] was incorrectly matched as AutoInc[Int]")
      case _                  => fail("AutoInc[String] was not matched correctly")
    }
  }

  it should "be an Attribute" in {
    val autoInc = AutoInc[Double]()
    autoInc shouldBe a[Attribute[_]]
  }
}
