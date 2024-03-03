/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

import munit.FunSuite

import ldbc.connector.data.*
import ldbc.connector.codec.all.*

class NumericCodecsTest extends FunSuite:

  test("bit encode successfully") {
    assertEquals(bit.encode(Byte.MinValue), List(Some(Encoded("-128", false))))
    assertEquals(bit.encode(Byte.MaxValue), List(Some(Encoded("127", false))))
  }

  test("bit decode successfully") {
    assertEquals(bit.decode(0, List(Some("-128"))), Right(Byte.MinValue))
    assertEquals(bit.decode(0, List(Some("127"))), Right(Byte.MaxValue))
    assertEquals(bit.opt.decode(0, List(Some("-128"))), Right(Some(Byte.MinValue)))
    assertEquals(bit.opt.decode(0, List(Some("-128"))), Right(Some(Byte.MinValue)))
    assertEquals(bit.opt.decode(0, List(None)), Right(None))
  }

  test("bit decode error") {
    assertEquals(bit.decode(0, List(Some(""))), Left(Decoder.Error(0, 1, "Invalid bit  For input string: \"\"", Type.bit)))
    assertEquals(bit.decode(0, List(Some("invalid"))), Left(Decoder.Error(0, 1, "Invalid bit invalid For input string: \"invalid\"", Type.bit)))
    assertEquals(bit.decode(0, List(Some("-129"))), Left(Decoder.Error(0, 1, "Invalid bit -129 Value out of range. Value:\"-129\" Radix:10", Type.bit)))
    assertEquals(bit.decode(0, List(Some("128"))), Left(Decoder.Error(0, 1, "Invalid bit 128 Value out of range. Value:\"128\" Radix:10", Type.bit)))
    assertEquals(bit.decode(0, List(None)), Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.bit)))
  }
