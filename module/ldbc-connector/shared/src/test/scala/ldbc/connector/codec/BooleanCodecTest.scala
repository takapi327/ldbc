/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

import munit.FunSuite

import ldbc.connector.codec.boolean.boolean
import ldbc.connector.data.*

class BooleanCodecTest extends FunSuite {
  test("boolean encode successfully") {
    assertEquals(boolean.encode(true), List(Some(Encoded("true", false))))
    assertEquals(boolean.encode(false), List(Some(Encoded("false", false))))
  }

  test("boolean decode successfully") {
    assertEquals(boolean.decode(0, List(Some("true"))), Right(true))
    assertEquals(boolean.decode(0, List(Some("false"))), Right(false))
    assertEquals(boolean.decode(0, List(Some("1"))), Right(true))
    assertEquals(boolean.decode(0, List(Some("0"))), Right(false))
    assertEquals(boolean.opt.decode(0, List(Some("true"))), Right(Some(true)))
    assertEquals(boolean.opt.decode(0, List(Some("false"))), Right(Some(false)))
    assertEquals(boolean.opt.decode(0, List(Some("1"))), Right(Some(true)))
    assertEquals(boolean.opt.decode(0, List(Some("0"))), Right(Some(false)))
    assertEquals(boolean.opt.decode(0, List(None)), Right(None))
  }

  test("boolean decode error") {
    assertEquals(boolean.decode(0, List(Some(""))), Left(Decoder.Error(0, 1, "Invalid boolean value: ", Type.boolean)))
    assertEquals(
      boolean.decode(0, List(Some("invalid"))),
      Left(Decoder.Error(0, 1, "Invalid boolean value: invalid", Type.boolean))
    )
    assertEquals(
      boolean.decode(0, List(Some("-1"))),
      Left(Decoder.Error(0, 1, "Invalid boolean value: -1", Type.boolean))
    )
    assertEquals(
      boolean.decode(0, List(Some("2"))),
      Left(Decoder.Error(0, 1, "Invalid boolean value: 2", Type.boolean))
    )
    assertEquals(
      boolean.decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.boolean))
    )
  }
}
