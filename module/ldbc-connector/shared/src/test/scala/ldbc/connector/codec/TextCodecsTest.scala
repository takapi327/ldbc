/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

import munit.FunSuite

import ldbc.connector.data.*
import ldbc.connector.codec.text.*

class TextCodecsTest extends FunSuite {
  test("char encode successfully") {
    assertEquals(char(255).encode("char"), List(Some(Encoded("char", false))))
    assertEquals(char(255).encode(""), List(Some(Encoded("", false))))
    assertEquals(char(255).encode("🔥 and 🌈"), List(Some(Encoded("🔥 and 🌈", false))))
    assertEquals(char(255).encode("\"שלום"), List(Some(Encoded("\"שלום", false))))
    assertEquals(char(255).encode("مرحب"), List(Some(Encoded("مرحب", false))))
    assertEquals(char(255).encode("你好"), List(Some(Encoded("你好", false))))
    assertEquals(char(255).encode("';--'"), List(Some(Encoded("';--'", false))))
  }

  test("char decode successfully") {
    assertEquals(char(255).decode(0, List(Some("varchar"))), Right("varchar"))
    assertEquals(char(255).decode(0, List(Some(""))), Right(""))
    assertEquals(char(255).decode(0, List(Some("🔥 and 🌈"))), Right("🔥 and 🌈"))
    assertEquals(char(255).decode(0, List(Some("שלום"))), Right("שלום"))
    assertEquals(char(255).decode(0, List(Some("مرحب"))), Right("مرحب"))
    assertEquals(char(255).decode(0, List(Some("你好"))), Right("你好"))
    assertEquals(char(255).decode(0, List(Some("';--'"))), Right("';--'"))
    assertEquals(char(255).opt.decode(0, List(None)), Right(None))
  }

  test("varchar encode successfully") {
    assertEquals(varchar(255).encode("varchar"), List(Some(Encoded("varchar", false))))
    assertEquals(varchar(255).encode(""), List(Some(Encoded("", false))))
    assertEquals(varchar(255).encode("🔥 and 🌈"), List(Some(Encoded("🔥 and 🌈", false))))
    assertEquals(varchar(255).encode("\"שלום"), List(Some(Encoded("\"שלום", false))))
    assertEquals(varchar(255).encode("مرحب"), List(Some(Encoded("مرحب", false))))
    assertEquals(varchar(255).encode("你好"), List(Some(Encoded("你好", false))))
    assertEquals(varchar(255).encode("';--'"), List(Some(Encoded("';--'", false))))
  }

  test("varchar decode successfully") {
    assertEquals(varchar(255).decode(0, List(Some("varchar"))), Right("varchar"))
    assertEquals(varchar(255).decode(0, List(Some(""))), Right(""))
    assertEquals(varchar(255).decode(0, List(Some("🔥 and 🌈"))), Right("🔥 and 🌈"))
    assertEquals(varchar(255).decode(0, List(Some("שלום"))), Right("שלום"))
    assertEquals(varchar(255).decode(0, List(Some("مرحب"))), Right("مرحب"))
    assertEquals(varchar(255).decode(0, List(Some("你好"))), Right("你好"))
    assertEquals(varchar(255).decode(0, List(Some("';--'"))), Right("';--'"))
    assertEquals(varchar(255).opt.decode(0, List(None)), Right(None))
  }

  test("binary encode successfully") {
    assertEquals(binary(255).encode(Array[Byte](98, 105, 110, 97, 114, 121)), List(Some(Encoded("binary", false))))
    assertEquals(binary(255).encode(Array.emptyByteArray), List(Some(Encoded("", false))))
    assertEquals(binary(255).encode("🔥 and 🌈".getBytes), List(Some(Encoded("🔥 and 🌈", false))))
    assertEquals(binary(255).encode("\"שלום".getBytes), List(Some(Encoded("\"שלום", false))))
    assertEquals(binary(255).encode("مرحب".getBytes), List(Some(Encoded("مرحب", false))))
    assertEquals(binary(255).encode("你好".getBytes), List(Some(Encoded("你好", false))))
    assertEquals(binary(255).encode("';--'".getBytes), List(Some(Encoded("';--'", false))))
  }

  test("binary decode successfully") {
    assertEquals(
      binary(255).decode(0, List(Some("binary"))).map(_.mkString(":")),
      Right("binary".getBytes().mkString(":"))
    )
    assertEquals(binary(255).decode(0, List(Some(""))).map(_.mkString(":")), Right(""))
    assertEquals(
      binary(255).decode(0, List(Some("🔥 and 🌈"))).map(_.mkString(":")),
      Right("🔥 and 🌈".getBytes().mkString(":"))
    )
    assertEquals(binary(255).decode(0, List(Some("שלום"))).map(_.mkString(":")), Right("שלום".getBytes().mkString(":")))
    assertEquals(binary(255).decode(0, List(Some("مرحب"))).map(_.mkString(":")), Right("مرحب".getBytes().mkString(":")))
    assertEquals(binary(255).decode(0, List(Some("你好"))).map(_.mkString(":")), Right("你好".getBytes().mkString(":")))
    assertEquals(
      binary(255).decode(0, List(Some("';--'"))).map(_.mkString(":")),
      Right("';--'".getBytes().mkString(":"))
    )
    assertEquals(binary(255).opt.decode(0, List(None)), Right(None))
  }

  test("varbinary encode successfully") {
    assertEquals(varbinary(255).encode("varbinary"), List(Some(Encoded("varbinary", false))))
    assertEquals(varbinary(255).encode(""), List(Some(Encoded("", false))))
    assertEquals(varbinary(255).encode("🔥 and 🌈"), List(Some(Encoded("🔥 and 🌈", false))))
    assertEquals(varbinary(255).encode("\"שלום"), List(Some(Encoded("\"שלום", false))))
    assertEquals(varbinary(255).encode("مرحب"), List(Some(Encoded("مرحب", false))))
    assertEquals(varbinary(255).encode("你好"), List(Some(Encoded("你好", false))))
    assertEquals(varbinary(255).encode("';--'"), List(Some(Encoded("';--'", false))))
  }

  test("varbinary decode successfully") {
    assertEquals(varbinary(255).decode(0, List(Some("varbinary"))), Right("varbinary"))
    assertEquals(varbinary(255).decode(0, List(Some(""))), Right(""))
    assertEquals(varbinary(255).decode(0, List(Some("🔥 and 🌈"))), Right("🔥 and 🌈"))
    assertEquals(varbinary(255).decode(0, List(Some("שלום"))), Right("שלום"))
    assertEquals(varbinary(255).decode(0, List(Some("مرحب"))), Right("مرحب"))
    assertEquals(varbinary(255).decode(0, List(Some("你好"))), Right("你好"))
    assertEquals(varbinary(255).decode(0, List(Some("';--'"))), Right("';--'"))
    assertEquals(varbinary(255).opt.decode(0, List(None)), Right(None))
  }
}
