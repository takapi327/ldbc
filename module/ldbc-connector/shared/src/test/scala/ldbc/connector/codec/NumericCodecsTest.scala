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
    assertEquals(
      bit.decode(0, List(Some(""))),
      Left(Decoder.Error(0, 1, "Invalid bit  For input string: \"\"", Type.bit))
    )
    assertEquals(
      bit.decode(0, List(Some("invalid"))),
      Left(Decoder.Error(0, 1, "Invalid bit invalid For input string: \"invalid\"", Type.bit))
    )
    assertEquals(
      bit.decode(0, List(Some("-129"))),
      Left(Decoder.Error(0, 1, "Invalid bit -129 Value out of range. Value:\"-129\" Radix:10", Type.bit))
    )
    assertEquals(
      bit.decode(0, List(Some("128"))),
      Left(Decoder.Error(0, 1, "Invalid bit 128 Value out of range. Value:\"128\" Radix:10", Type.bit))
    )
    assertEquals(
      bit.decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.bit))
    )
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
    assertEquals(
      tinyint.decode(0, List(Some(""))),
      Left(Decoder.Error(0, 1, "Invalid tinyint  For input string: \"\"", Type.tinyint))
    )
    assertEquals(
      tinyint.decode(0, List(Some("invalid"))),
      Left(Decoder.Error(0, 1, "Invalid tinyint invalid For input string: \"invalid\"", Type.tinyint))
    )
    assertEquals(
      tinyint.decode(0, List(Some("-129"))),
      Left(Decoder.Error(0, 1, "Invalid tinyint -129 Value out of range. Value:\"-129\" Radix:10", Type.tinyint))
    )
    assertEquals(
      tinyint.decode(0, List(Some("128"))),
      Left(Decoder.Error(0, 1, "Invalid tinyint 128 Value out of range. Value:\"128\" Radix:10", Type.tinyint))
    )
    assertEquals(
      tinyint.decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.tinyint))
    )
  }

  test("unsigned tinyint encode successfully") {
    assertEquals(utinyint.encode(0), List(Some(Encoded("0", false))))
    assertEquals(utinyint.encode(255), List(Some(Encoded("255", false))))
  }

  test("unsigned tinyint decode successfully") {
    assertEquals(utinyint.decode(0, List(Some("0"))), Right(0.toShort))
    assertEquals(utinyint.decode(0, List(Some("255"))), Right(255.toShort))
    assertEquals(utinyint.opt.decode(0, List(Some("0"))), Right(Some(0.toShort)))
    assertEquals(utinyint.opt.decode(0, List(Some("255"))), Right(Some(255.toShort)))
    assertEquals(utinyint.opt.decode(0, List(None)), Right(None))
  }

  test("unsigned tinyint decode error") {
    assertEquals(
      utinyint.decode(0, List(Some(""))),
      Left(Decoder.Error(0, 1, "Invalid tinyint unsigned  For input string: \"\"", Type.utinyint))
    )
    assertEquals(
      utinyint.decode(0, List(Some("invalid"))),
      Left(Decoder.Error(0, 1, "Invalid tinyint unsigned invalid For input string: \"invalid\"", Type.utinyint))
    )
    assertEquals(
      utinyint.decode(0, List(Some("-1"))),
      Left(Decoder.Error(0, 1, "Invalid tinyint unsigned -1 can only handle the range 0 ~ 255", Type.utinyint))
    )
    assertEquals(
      utinyint.decode(0, List(Some("256"))),
      Left(Decoder.Error(0, 1, "Invalid tinyint unsigned 256 can only handle the range 0 ~ 255", Type.utinyint))
    )
    assertEquals(
      utinyint.decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.utinyint))
    )
  }

  test("smallint encode successfully") {
    assertEquals(smallint.encode(Short.MinValue), List(Some(Encoded("-32768", false))))
    assertEquals(smallint.encode(Short.MaxValue), List(Some(Encoded("32767", false))))
  }

  test("smallint decode successfully") {
    assertEquals(smallint.decode(0, List(Some("-32768"))), Right(Short.MinValue))
    assertEquals(smallint.decode(0, List(Some("32767"))), Right(Short.MaxValue))
    assertEquals(smallint.opt.decode(0, List(Some("-32768"))), Right(Some(Short.MinValue)))
    assertEquals(smallint.opt.decode(0, List(Some("32767"))), Right(Some(Short.MaxValue)))
    assertEquals(smallint.opt.decode(0, List(None)), Right(None))
  }

  test("smallint decode error") {
    assertEquals(
      smallint.decode(0, List(Some(""))),
      Left(Decoder.Error(0, 1, "Invalid smallint  For input string: \"\"", Type.smallint))
    )
    assertEquals(
      smallint.decode(0, List(Some("invalid"))),
      Left(Decoder.Error(0, 1, "Invalid smallint invalid For input string: \"invalid\"", Type.smallint))
    )
    assertEquals(
      smallint.decode(0, List(Some("-32769"))),
      Left(Decoder.Error(0, 1, "Invalid smallint -32769 Value out of range. Value:\"-32769\" Radix:10", Type.smallint))
    )
    assertEquals(
      smallint.decode(0, List(Some("32768"))),
      Left(Decoder.Error(0, 1, "Invalid smallint 32768 Value out of range. Value:\"32768\" Radix:10", Type.smallint))
    )
    assertEquals(
      smallint.decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.smallint))
    )
  }

  test("unsigned smallint encode successfully") {
    assertEquals(usmallint.encode(-8388608), List(Some(Encoded("-8388608", false))))
    assertEquals(usmallint.encode(8388607), List(Some(Encoded("8388607", false))))
  }

  test("unsigned smallint decode successfully") {
    assertEquals(usmallint.decode(0, List(Some("0"))), Right(0))
    assertEquals(usmallint.decode(0, List(Some("65535"))), Right(65535))
    assertEquals(usmallint.opt.decode(0, List(Some("0"))), Right(Some(0)))
    assertEquals(usmallint.opt.decode(0, List(Some("65535"))), Right(Some(65535)))
    assertEquals(usmallint.opt.decode(0, List(None)), Right(None))
  }

  test("unsigned smallint decode error") {
    assertEquals(
      usmallint.decode(0, List(Some(""))),
      Left(Decoder.Error(0, 1, "Invalid smallint unsigned  For input string: \"\"", Type.usmallint))
    )
    assertEquals(
      usmallint.decode(0, List(Some("invalid"))),
      Left(Decoder.Error(0, 1, "Invalid smallint unsigned invalid For input string: \"invalid\"", Type.usmallint))
    )
    assertEquals(
      usmallint.decode(0, List(Some("-1"))),
      Left(Decoder.Error(0, 1, "Invalid smallint unsigned -1 can only handle the range 0 ~ 65535", Type.usmallint))
    )
    assertEquals(
      usmallint.decode(0, List(Some("65536"))),
      Left(Decoder.Error(0, 1, "Invalid smallint unsigned 65536 can only handle the range 0 ~ 65535", Type.usmallint))
    )
    assertEquals(
      usmallint.decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.usmallint))
    )
  }