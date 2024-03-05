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
}
