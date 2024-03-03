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
    assertEquals(bit.opt.decode(0, List(Some("127"))), Right(Some(Byte.MaxValue)))
    assertEquals(bit.opt.decode(0, List(None)), Right(None))
  }

  test("bit decode error") {
    assertEquals(bit.decode(0, List(Some(""))), Left(Decoder.Error(0, 1, "Invalid bit  For input string: \"\"", Type.bit)))
    assertEquals(bit.decode(0, List(Some("invalid"))), Left(Decoder.Error(0, 1, "Invalid bit invalid For input string: \"invalid\"", Type.bit)))
    assertEquals(bit.decode(0, List(Some("-129"))), Left(Decoder.Error(0, 1, "Invalid bit -129 Value out of range. Value:\"-129\" Radix:10", Type.bit)))
    assertEquals(bit.decode(0, List(Some("128"))), Left(Decoder.Error(0, 1, "Invalid bit 128 Value out of range. Value:\"128\" Radix:10", Type.bit)))
    assertEquals(bit.decode(0, List(None)), Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.bit)))
  }

  test("tinyint encode successfully") {
    assertEquals(tinyint.encode(Byte.MinValue), List(Some(Encoded("-128", false))))
    assertEquals(tinyint.encode(Byte.MaxValue), List(Some(Encoded("127", false))))
  }

  test("tinyint decode successfully") {
    assertEquals(tinyint.decode(0, List(Some("-128"))), Right(Byte.MinValue))
    assertEquals(tinyint.decode(0, List(Some("127"))), Right(Byte.MaxValue))
    assertEquals(tinyint.opt.decode(0, List(Some("-128"))), Right(Some(Byte.MinValue)))
    assertEquals(tinyint.opt.decode(0, List(Some("127"))), Right(Some(Byte.MaxValue)))
    assertEquals(tinyint.opt.decode(0, List(None)), Right(None))
  }

  test("tinyint decode error") {
    assertEquals(tinyint.decode(0, List(Some(""))), Left(Decoder.Error(0, 1, "Invalid tinyint  For input string: \"\"", Type.tinyint)))
    assertEquals(tinyint.decode(0, List(Some("invalid"))), Left(Decoder.Error(0, 1, "Invalid tinyint invalid For input string: \"invalid\"", Type.tinyint)))
    assertEquals(tinyint.decode(0, List(Some("-129"))), Left(Decoder.Error(0, 1, "Invalid tinyint -129 Value out of range. Value:\"-129\" Radix:10", Type.tinyint)))
    assertEquals(tinyint.decode(0, List(Some("128"))), Left(Decoder.Error(0, 1, "Invalid tinyint 128 Value out of range. Value:\"128\" Radix:10", Type.tinyint)))
    assertEquals(tinyint.decode(0, List(None)), Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.tinyint)))
  }

  test("unsigned tinyint encode successfully") {
    assertEquals(utinyint.encode(Short.MinValue), List(Some(Encoded("-32768", false))))
    assertEquals(utinyint.encode(Short.MaxValue), List(Some(Encoded("32767", false))))
  }

  test("unsigned tinyint decode successfully") {
    assertEquals(utinyint.decode(0, List(Some("-32768"))), Right(Short.MinValue))
    assertEquals(utinyint.decode(0, List(Some("32767"))), Right(Short.MaxValue))
    assertEquals(utinyint.opt.decode(0, List(Some("-32768"))), Right(Some(Short.MinValue)))
    assertEquals(utinyint.opt.decode(0, List(Some("32767"))), Right(Some(Short.MaxValue)))
    assertEquals(utinyint.opt.decode(0, List(None)), Right(None))
  }

  test("unsigned tinyint decode error") {
    assertEquals(utinyint.decode(0, List(Some(""))), Left(Decoder.Error(0, 1, "Invalid tinyint unsigned  For input string: \"\"", Type.utinyint)))
    assertEquals(utinyint.decode(0, List(Some("invalid"))), Left(Decoder.Error(0, 1, "Invalid tinyint unsigned invalid For input string: \"invalid\"", Type.utinyint)))
    assertEquals(utinyint.decode(0, List(Some("-32769"))), Left(Decoder.Error(0, 1, "Invalid tinyint unsigned -32769 Value out of range. Value:\"-32769\" Radix:10", Type.utinyint)))
    assertEquals(utinyint.decode(0, List(Some("32768"))), Left(Decoder.Error(0, 1, "Invalid tinyint unsigned 32768 Value out of range. Value:\"32768\" Radix:10", Type.utinyint)))
    assertEquals(utinyint.decode(0, List(None)), Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.utinyint)))
  }
