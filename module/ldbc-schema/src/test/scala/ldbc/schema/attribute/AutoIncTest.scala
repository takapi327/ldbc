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

  it should "be an Attribute" in {
    val autoInc = AutoInc[Double]()
    autoInc shouldBe a[Attribute[_]]
  }
}
