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
      Left(Decoder.Error(0, 1, "Invalid bit -129 For input string: \"-129\"", Type.bit))
    )
    assertEquals(
      bit.decode(0, List(Some("128"))),
      Left(Decoder.Error(0, 1, "Invalid bit 128 For input string: \"128\"", Type.bit))
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
      Left(Decoder.Error(0, 1, "Invalid tinyint -129 For input string: \"-129\"", Type.tinyint))
    )
    assertEquals(
      tinyint.decode(0, List(Some("128"))),
      Left(Decoder.Error(0, 1, "Invalid tinyint 128 For input string: \"128\"", Type.tinyint))
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
      Left(Decoder.Error(0, 1, "Invalid smallint -32769 For input string: \"-32769\"", Type.smallint))
    )
    assertEquals(
      smallint.decode(0, List(Some("32768"))),
      Left(Decoder.Error(0, 1, "Invalid smallint 32768 For input string: \"32768\"", Type.smallint))
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

  test("mediumint encode successfully") {
    assertEquals(mediumint.encode(-8388608), List(Some(Encoded("-8388608", false))))
    assertEquals(mediumint.encode(8388607), List(Some(Encoded("8388607", false))))
  }

  test("mediumint decode successfully") {
    assertEquals(mediumint.decode(0, List(Some("-8388608"))), Right(-8388608))
    assertEquals(mediumint.decode(0, List(Some("8388607"))), Right(8388607))
    assertEquals(mediumint.opt.decode(0, List(Some("-8388608"))), Right(Some(-8388608)))
    assertEquals(mediumint.opt.decode(0, List(Some("8388607"))), Right(Some(8388607)))
    assertEquals(mediumint.opt.decode(0, List(None)), Right(None))
  }

  test("mediumint decode error") {
    assertEquals(
      mediumint.decode(0, List(Some(""))),
      Left(Decoder.Error(0, 1, "Invalid mediumint  For input string: \"\"", Type.mediumint))
    )
    assertEquals(
      mediumint.decode(0, List(Some("invalid"))),
      Left(Decoder.Error(0, 1, "Invalid mediumint invalid For input string: \"invalid\"", Type.mediumint))
    )
    assertEquals(
      mediumint.decode(0, List(Some("-8388609"))),
      Left(
        Decoder.Error(0, 1, "Invalid mediumint -8388609 can only handle the range -8388608 ~ 8388607", Type.mediumint)
      )
    )
    assertEquals(
      mediumint.decode(0, List(Some("8388608"))),
      Left(
        Decoder.Error(0, 1, "Invalid mediumint 8388608 can only handle the range -8388608 ~ 8388607", Type.mediumint)
      )
    )
    assertEquals(
      mediumint.decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.mediumint))
    )
  }

  test("unsigned mediumint encode successfully") {
    assertEquals(umediumint.encode(0), List(Some(Encoded("0", false))))
    assertEquals(umediumint.encode(16777215), List(Some(Encoded("16777215", false))))
  }

  test("unsigned mediumint decode successfully") {
    assertEquals(umediumint.decode(0, List(Some("0"))), Right(0))
    assertEquals(umediumint.decode(0, List(Some("16777215"))), Right(16777215))
    assertEquals(umediumint.opt.decode(0, List(Some("0"))), Right(Some(0)))
    assertEquals(umediumint.opt.decode(0, List(Some("16777215"))), Right(Some(16777215)))
    assertEquals(umediumint.opt.decode(0, List(None)), Right(None))
  }

  test("unsigned mediumint decode error") {
    assertEquals(
      umediumint.decode(0, List(Some(""))),
      Left(Decoder.Error(0, 1, "Invalid mediumint unsigned  For input string: \"\"", Type.umediumint))
    )
    assertEquals(
      umediumint.decode(0, List(Some("invalid"))),
      Left(Decoder.Error(0, 1, "Invalid mediumint unsigned invalid For input string: \"invalid\"", Type.umediumint))
    )
    assertEquals(
      umediumint.decode(0, List(Some("-1"))),
      Left(Decoder.Error(0, 1, "Invalid mediumint unsigned -1 can only handle the range 0 ~ 16777215", Type.umediumint))
    )
    assertEquals(
      umediumint.decode(0, List(Some("16777216"))),
      Left(
        Decoder.Error(
          0,
          1,
          "Invalid mediumint unsigned 16777216 can only handle the range 0 ~ 16777215",
          Type.umediumint
        )
      )
    )
    assertEquals(
      umediumint.decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.umediumint))
    )
  }

  test("int encode successfully") {
    assertEquals(int.encode(Int.MinValue), List(Some(Encoded("-2147483648", false))))
    assertEquals(int.encode(Int.MaxValue), List(Some(Encoded("2147483647", false))))
  }

  test("int decode successfully") {
    assertEquals(int.decode(0, List(Some("-2147483648"))), Right(Int.MinValue))
    assertEquals(int.decode(0, List(Some("2147483647"))), Right(Int.MaxValue))
    assertEquals(int.opt.decode(0, List(Some("-2147483648"))), Right(Some(Int.MinValue)))
    assertEquals(int.opt.decode(0, List(Some("2147483647"))), Right(Some(Int.MaxValue)))
    assertEquals(int.opt.decode(0, List(None)), Right(None))
  }

  test("int decode error") {
    assertEquals(
      int.decode(0, List(Some(""))),
      Left(Decoder.Error(0, 1, "Invalid int  For input string: \"\"", Type.int))
    )
    assertEquals(
      int.decode(0, List(Some("invalid"))),
      Left(Decoder.Error(0, 1, "Invalid int invalid For input string: \"invalid\"", Type.int))
    )
    assertEquals(
      int.decode(0, List(Some("-2147483649"))),
      Left(Decoder.Error(0, 1, "Invalid int -2147483649 For input string: \"-2147483649\"", Type.int))
    )
    assertEquals(
      int.decode(0, List(Some("2147483648"))),
      Left(Decoder.Error(0, 1, "Invalid int 2147483648 For input string: \"2147483648\"", Type.int))
    )
    assertEquals(
      int.decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.int))
    )
  }

  test("unsigned int encode successfully") {
    assertEquals(uint.encode(0), List(Some(Encoded("0", false))))
    assertEquals(uint.encode(4294967295L), List(Some(Encoded("4294967295", false))))
  }

  test("unsigned int decode successfully") {
    assertEquals(uint.decode(0, List(Some("0"))), Right(0L))
    assertEquals(uint.decode(0, List(Some("4294967295"))), Right(4294967295L))
    assertEquals(uint.opt.decode(0, List(Some("0"))), Right(Some(0L)))
    assertEquals(uint.opt.decode(0, List(Some("4294967295"))), Right(Some(4294967295L)))
    assertEquals(uint.opt.decode(0, List(None)), Right(None))
  }

  test("unsigned int decode error") {
    assertEquals(
      uint.decode(0, List(Some(""))),
      Left(Decoder.Error(0, 1, "Invalid int unsigned  For input string: \"\"", Type.uint))
    )
    assertEquals(
      uint.decode(0, List(Some("invalid"))),
      Left(Decoder.Error(0, 1, "Invalid int unsigned invalid For input string: \"invalid\"", Type.uint))
    )
    assertEquals(
      uint.decode(0, List(Some("-1"))),
      Left(Decoder.Error(0, 1, "Invalid int unsigned -1 can only handle the range 0 ~ 4294967295", Type.uint))
    )
    assertEquals(
      uint.decode(0, List(Some("4294967296"))),
      Left(Decoder.Error(0, 1, "Invalid int unsigned 4294967296 can only handle the range 0 ~ 4294967295", Type.uint))
    )
    assertEquals(
      uint.decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.uint))
    )
  }

  test("bigint encode successfully") {
    assertEquals(bigint.encode(Long.MinValue), List(Some(Encoded("-9223372036854775808", false))))
    assertEquals(bigint.encode(Long.MaxValue), List(Some(Encoded("9223372036854775807", false))))
  }

  test("bigint decode successfully") {
    assertEquals(bigint.decode(0, List(Some("-9223372036854775808"))), Right(Long.MinValue))
    assertEquals(bigint.decode(0, List(Some("9223372036854775807"))), Right(Long.MaxValue))
    assertEquals(bigint.opt.decode(0, List(Some("-9223372036854775808"))), Right(Some(Long.MinValue)))
    assertEquals(bigint.opt.decode(0, List(Some("9223372036854775807"))), Right(Some(Long.MaxValue)))
    assertEquals(bigint.opt.decode(0, List(None)), Right(None))
  }

  test("bigint decode error") {
    assertEquals(
      bigint.decode(0, List(Some(""))),
      Left(Decoder.Error(0, 1, "Invalid bigint  For input string: \"\"", Type.bigint))
    )
    assertEquals(
      bigint.decode(0, List(Some("invalid"))),
      Left(Decoder.Error(0, 1, "Invalid bigint invalid For input string: \"invalid\"", Type.bigint))
    )
    assertEquals(
      bigint.decode(0, List(Some("-9223372036854775809"))),
      Left(
        Decoder.Error(
          0,
          1,
          "Invalid bigint -9223372036854775809 For input string: \"-9223372036854775809\"",
          Type.bigint
        )
      )
    )
    assertEquals(
      bigint.decode(0, List(Some("9223372036854775808"))),
      Left(
        Decoder.Error(0, 1, "Invalid bigint 9223372036854775808 For input string: \"9223372036854775808\"", Type.bigint)
      )
    )
    assertEquals(
      bigint.decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.bigint))
    )
  }

  test("unsigned bigint encode successfully") {
    assertEquals(ubigint.encode(BigInt(0)), List(Some(Encoded("0", false))))
    assertEquals(ubigint.encode(BigInt("18446744073709551615")), List(Some(Encoded("18446744073709551615", false))))
  }

  test("unsigned bigint decode successfully") {
    assertEquals(ubigint.decode(0, List(Some("0"))), Right(BigInt("0")))
    assertEquals(ubigint.decode(0, List(Some("18446744073709551615"))), Right(BigInt("18446744073709551615")))
    assertEquals(ubigint.opt.decode(0, List(Some("0"))), Right(Some(BigInt("0"))))
    assertEquals(ubigint.opt.decode(0, List(Some("18446744073709551615"))), Right(Some(BigInt("18446744073709551615"))))
    assertEquals(ubigint.opt.decode(0, List(None)), Right(None))
  }

  test("unsigned bigint decode error") {
    assertEquals(
      ubigint.decode(0, List(Some(""))),
      Left(Decoder.Error(0, 1, "Invalid bigint unsigned  Zero length BigInteger", Type.ubigint))
    )
    assertEquals(
      ubigint.decode(0, List(Some("invalid"))),
      Left(Decoder.Error(0, 1, "Invalid bigint unsigned invalid For input string: \"invalid\"", Type.ubigint))
    )
    assertEquals(
      ubigint.decode(0, List(Some("-1"))),
      Left(
        Decoder.Error(
          0,
          1,
          "Invalid bigint unsigned -1 can only handle the range 0 ~ 18446744073709551615",
          Type.ubigint
        )
      )
    )
    assertEquals(
      ubigint.decode(0, List(Some("18446744073709551616"))),
      Left(
        Decoder.Error(
          0,
          1,
          "Invalid bigint unsigned 18446744073709551616 can only handle the range 0 ~ 18446744073709551615",
          Type.ubigint
        )
      )
    )
    assertEquals(
      ubigint.decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.ubigint))
    )
  }

  test("decimal encode successfully") {
    assertEquals(decimal().encode(BigDecimal(1L)), List(Some(Encoded("1", false))))
    assertEquals(decimal().encode(BigDecimal(1.1)), List(Some(Encoded("1.1", false))))
  }

  test("decimal decode successfully") {
    assertEquals(decimal().decode(0, List(Some("1"))), Right(BigDecimal(1L)))
    assertEquals(decimal().decode(0, List(Some("-1"))), Right(BigDecimal(-1.0)))
    assertEquals(decimal().decode(0, List(Some(".0"))), Right(BigDecimal(0.0)))
    assertEquals(decimal().decode(0, List(Some("1.1"))), Right(BigDecimal(1.1)))
    assertEquals(decimal().opt.decode(0, List(Some("1"))), Right(Some(BigDecimal(1L))))
    assertEquals(decimal().opt.decode(0, List(Some("-1"))), Right(Some(BigDecimal(-1.0))))
    assertEquals(decimal().opt.decode(0, List(Some(".0"))), Right(Some(BigDecimal(0.0))))
    assertEquals(decimal().opt.decode(0, List(Some("1.1"))), Right(Some(BigDecimal(1.1))))
  }

  test("decimal decode error") {
    assertEquals(
      decimal().decode(0, List(Some(""))),
      Left(Decoder.Error(0, 1, "Invalid decimal(10, 0)  Bad offset/length: offset=0 len=0 in.length=0", Type.decimal()))
    )
    assertEquals(
      decimal().decode(0, List(Some("invalid"))),
      Left(
        Decoder.Error(
          0,
          1,
          "Invalid decimal(10, 0) invalid For input string: \"invalid\"",
          Type.decimal()
        )
      )
    )
    assertEquals(
      decimal().decode(0, List(Some("1.1.1"))),
      Left(
        Decoder.Error(
          0,
          1,
          "Invalid decimal(10, 0) 1.1.1 For input string: \"11.1\"",
          Type.decimal()
        )
      )
    )
  }

  test("float encode successfully") {
    assertEquals(float.encode(Float.MinValue), List(Some(Encoded("-3.4028234663852886e+38", false))))
    assertEquals(float.encode(Float.MaxValue), List(Some(Encoded("3.4028234663852886e+38", false))))
  }

  test("float decode successfully") {
    assertEquals(float.decode(0, List(Some("-3.4028234663852886e+38"))), Right(Float.MinValue))
    assertEquals(float.decode(0, List(Some("3.4028234663852886e+38"))), Right(Float.MaxValue))
    assertEquals(float.decode(0, List(Some(".0"))), Right(0.0.toFloat))
    assertEquals(float.decode(0, List(Some("1.1"))), Right(1.1.toFloat))
    assertEquals(float.opt.decode(0, List(Some("-3.4028234663852886e+38"))), Right(Some(Float.MinValue)))
    assertEquals(float.opt.decode(0, List(Some("3.4028234663852886e+38"))), Right(Some(Float.MaxValue)))
    assertEquals(float.opt.decode(0, List(Some(".0"))), Right(Some(0.0.toFloat)))
    assertEquals(float.opt.decode(0, List(Some("1.1"))), Right(Some(1.1.toFloat)))
  }

  test("float decode error") {
    assertEquals(
      float.decode(0, List(Some(""))),
      Left(Decoder.Error(0, 1, "Invalid float  For input string: \"\"", Type.float))
    )
    assertEquals(
      float.decode(0, List(Some("invalid"))),
      Left(Decoder.Error(0, 1, "Invalid float invalid For input string: \"invalid\"", Type.float))
    )
    assertEquals(
      float.decode(0, List(Some("1.1.1"))),
      Left(Decoder.Error(0, 1, "Invalid float 1.1.1 For input string: \"1.1.1\"", Type.float))
    )
  }

  test("double encode successfully") {
    assertEquals(double.encode(Double.MinValue), List(Some(Encoded("-1.7976931348623157e+308", false))))
    assertEquals(double.encode(Double.MaxValue), List(Some(Encoded("1.7976931348623157e+308", false))))
  }

  test("double decode successfully") {
    assertEquals(double.decode(0, List(Some("-1.7976931348623157e+308"))), Right(Double.MinValue))
    assertEquals(double.decode(0, List(Some("1.7976931348623157e+308"))), Right(Double.MaxValue))
    assertEquals(double.decode(0, List(Some(".0"))), Right(0.0))
    assertEquals(double.decode(0, List(Some("1.1"))), Right(1.1))
    assertEquals(double.opt.decode(0, List(Some("-1.7976931348623157e+308"))), Right(Some(Double.MinValue)))
    assertEquals(double.opt.decode(0, List(Some("1.7976931348623157e+308"))), Right(Some(Double.MaxValue)))
    assertEquals(double.opt.decode(0, List(Some(".0"))), Right(Some(0.0)))
    assertEquals(double.opt.decode(0, List(Some("1.1"))), Right(Some(1.1)))
  }

  test("double decode error") {
    assertEquals(
      double.decode(0, List(Some(""))),
      Left(Decoder.Error(0, 1, "Invalid double  For input string: \"\"", Type.double))
    )
    assertEquals(
      double.decode(0, List(Some("invalid"))),
      Left(Decoder.Error(0, 1, "Invalid double invalid For input string: \"invalid\"", Type.double))
    )
    assertEquals(
      double.decode(0, List(Some("1.1.1"))),
      Left(Decoder.Error(0, 1, "Invalid double 1.1.1 For input string: \"1.1.1\"", Type.double))
    )
  }
