/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

import ldbc.connector.codec.text.*
import ldbc.connector.data.*

import munit.FunSuite

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

  test("tinyblob encode successfully") {
    assertEquals(tinyblob.encode("tinyblob"), List(Some(Encoded("74696e79626c6f62", false))))
    assertEquals(tinyblob.encode(""), List(Some(Encoded("", false))))
    assertEquals(tinyblob.encode("🔥 and 🌈"), List(Some(Encoded("f09f94a520616e6420f09f8c88", false))))
    assertEquals(tinyblob.encode("שלום"), List(Some(Encoded("d7a9d79cd795d79d", false))))
    assertEquals(tinyblob.encode("مرحب"), List(Some(Encoded("d985d8b1d8add8a8", false))))
    assertEquals(tinyblob.encode("你好"), List(Some(Encoded("e4bda0e5a5bd", false))))
    assertEquals(tinyblob.encode("';--'"), List(Some(Encoded("273b2d2d27", false))))
  }

  test("tinyblob decode successfully") {
    assertEquals(tinyblob.decode(0, List(Some("74696e79626c6f62"))), Right("tinyblob"))
    assertEquals(tinyblob.decode(0, List(Some(""))), Right(""))
    assertEquals(tinyblob.decode(0, List(Some("f09f94a520616e6420f09f8c88"))), Right("🔥 and 🌈"))
    assertEquals(tinyblob.decode(0, List(Some("d7a9d79cd795d79d"))), Right("שלום"))
    assertEquals(tinyblob.decode(0, List(Some("d985d8b1d8add8a8"))), Right("مرحب"))
    assertEquals(tinyblob.decode(0, List(Some("e4bda0e5a5bd"))), Right("你好"))
    assertEquals(tinyblob.decode(0, List(Some("273b2d2d27"))), Right("';--'"))
    assertEquals(tinyblob.opt.decode(0, List(None)), Right(None))
  }

  test("blob encode successfully") {
    assertEquals(blob.encode("blob"), List(Some(Encoded("626c6f62", false))))
    assertEquals(blob.encode(""), List(Some(Encoded("", false))))
    assertEquals(blob.encode("🔥 and 🌈"), List(Some(Encoded("f09f94a520616e6420f09f8c88", false))))
    assertEquals(blob.encode("שלום"), List(Some(Encoded("d7a9d79cd795d79d", false))))
    assertEquals(blob.encode("مرحب"), List(Some(Encoded("d985d8b1d8add8a8", false))))
    assertEquals(blob.encode("你好"), List(Some(Encoded("e4bda0e5a5bd", false))))
    assertEquals(blob.encode("';--'"), List(Some(Encoded("273b2d2d27", false))))
  }

  test("blob decode successfully") {
    assertEquals(blob.decode(0, List(Some("626c6f62"))), Right("blob"))
    assertEquals(blob.decode(0, List(Some(""))), Right(""))
    assertEquals(blob.decode(0, List(Some("f09f94a520616e6420f09f8c88"))), Right("🔥 and 🌈"))
    assertEquals(blob.decode(0, List(Some("d7a9d79cd795d79d"))), Right("שלום"))
    assertEquals(blob.decode(0, List(Some("d985d8b1d8add8a8"))), Right("مرحب"))
    assertEquals(blob.decode(0, List(Some("e4bda0e5a5bd"))), Right("你好"))
    assertEquals(blob.decode(0, List(Some("273b2d2d27"))), Right("';--'"))
    assertEquals(blob.opt.decode(0, List(None)), Right(None))
  }

  test("mediumblob encode successfully") {
    assertEquals(mediumblob.encode("mediumblob"), List(Some(Encoded("6d656469756d626c6f62", false))))
    assertEquals(mediumblob.encode(""), List(Some(Encoded("", false))))
    assertEquals(mediumblob.encode("🔥 and 🌈"), List(Some(Encoded("f09f94a520616e6420f09f8c88", false))))
    assertEquals(mediumblob.encode("שלום"), List(Some(Encoded("d7a9d79cd795d79d", false))))
    assertEquals(mediumblob.encode("مرحب"), List(Some(Encoded("d985d8b1d8add8a8", false))))
    assertEquals(mediumblob.encode("你好"), List(Some(Encoded("e4bda0e5a5bd", false))))
    assertEquals(mediumblob.encode("';--'"), List(Some(Encoded("273b2d2d27", false))))
  }

  test("mediumblob decode successfully") {
    assertEquals(mediumblob.decode(0, List(Some("6d656469756d626c6f62"))), Right("mediumblob"))
    assertEquals(mediumblob.decode(0, List(Some(""))), Right(""))
    assertEquals(mediumblob.decode(0, List(Some("f09f94a520616e6420f09f8c88"))), Right("🔥 and 🌈"))
    assertEquals(mediumblob.decode(0, List(Some("d7a9d79cd795d79d"))), Right("שלום"))
    assertEquals(mediumblob.decode(0, List(Some("d985d8b1d8add8a8"))), Right("مرحب"))
    assertEquals(mediumblob.decode(0, List(Some("e4bda0e5a5bd"))), Right("你好"))
    assertEquals(mediumblob.decode(0, List(Some("273b2d2d27"))), Right("';--'"))
    assertEquals(mediumblob.opt.decode(0, List(None)), Right(None))
  }

  test("longblob encode successfully") {
    assertEquals(longblob.encode("longblob"), List(Some(Encoded("6c6f6e67626c6f62", false))))
    assertEquals(longblob.encode(""), List(Some(Encoded("", false))))
    assertEquals(longblob.encode("🔥 and 🌈"), List(Some(Encoded("f09f94a520616e6420f09f8c88", false))))
    assertEquals(longblob.encode("שלום"), List(Some(Encoded("d7a9d79cd795d79d", false))))
    assertEquals(longblob.encode("مرحب"), List(Some(Encoded("d985d8b1d8add8a8", false))))
    assertEquals(longblob.encode("你好"), List(Some(Encoded("e4bda0e5a5bd", false))))
    assertEquals(longblob.encode("';--'"), List(Some(Encoded("273b2d2d27", false))))
  }

  test("longblob decode successfully") {
    assertEquals(longblob.decode(0, List(Some("6c6f6e67626c6f62"))), Right("longblob"))
    assertEquals(longblob.decode(0, List(Some(""))), Right(""))
    assertEquals(longblob.decode(0, List(Some("f09f94a520616e6420f09f8c88"))), Right("🔥 and 🌈"))
    assertEquals(longblob.decode(0, List(Some("d7a9d79cd795d79d"))), Right("שלום"))
    assertEquals(longblob.decode(0, List(Some("d985d8b1d8add8a8"))), Right("مرحب"))
    assertEquals(longblob.decode(0, List(Some("e4bda0e5a5bd"))), Right("你好"))
    assertEquals(longblob.decode(0, List(Some("273b2d2d27"))), Right("';--'"))
    assertEquals(longblob.opt.decode(0, List(None)), Right(None))
  }

  test("tinytext encode successfully") {
    assertEquals(tinytext.encode("tinytext"), List(Some(Encoded("tinytext", false))))
    assertEquals(tinytext.encode(""), List(Some(Encoded("", false))))
    assertEquals(tinytext.encode("🔥 and 🌈"), List(Some(Encoded("🔥 and 🌈", false))))
    assertEquals(tinytext.encode("\"שלום"), List(Some(Encoded("\"שלום", false))))
    assertEquals(tinytext.encode("مرحب"), List(Some(Encoded("مرحب", false))))
    assertEquals(tinytext.encode("你好"), List(Some(Encoded("你好", false))))
    assertEquals(tinytext.encode("';--'"), List(Some(Encoded("';--'", false))))
  }

  test("tinytext decode successfully") {
    assertEquals(tinytext.decode(0, List(Some("tinytext"))), Right("tinytext"))
    assertEquals(tinytext.decode(0, List(Some(""))), Right(""))
    assertEquals(tinytext.decode(0, List(Some("🔥 and 🌈"))), Right("🔥 and 🌈"))
    assertEquals(tinytext.decode(0, List(Some("שלום"))), Right("שלום"))
    assertEquals(tinytext.decode(0, List(Some("مرحب"))), Right("مرحب"))
    assertEquals(tinytext.decode(0, List(Some("你好"))), Right("你好"))
    assertEquals(tinytext.decode(0, List(Some("';--'"))), Right("';--'"))
    assertEquals(tinytext.opt.decode(0, List(None)), Right(None))
  }

  test("text encode successfully") {
    assertEquals(text.encode("text"), List(Some(Encoded("text", false))))
    assertEquals(text.encode(""), List(Some(Encoded("", false))))
    assertEquals(text.encode("🔥 and 🌈"), List(Some(Encoded("🔥 and 🌈", false))))
    assertEquals(text.encode("\"שלום"), List(Some(Encoded("\"שלום", false))))
    assertEquals(text.encode("مرحب"), List(Some(Encoded("مرحب", false))))
    assertEquals(text.encode("你好"), List(Some(Encoded("你好", false))))
    assertEquals(text.encode("';--'"), List(Some(Encoded("';--'", false))))
  }

  test("text decode successfully") {
    assertEquals(text.decode(0, List(Some("text"))), Right("text"))
    assertEquals(text.decode(0, List(Some(""))), Right(""))
    assertEquals(text.decode(0, List(Some("🔥 and 🌈"))), Right("🔥 and 🌈"))
    assertEquals(text.decode(0, List(Some("שלום"))), Right("שלום"))
    assertEquals(text.decode(0, List(Some("مرحب"))), Right("مرحب"))
    assertEquals(text.decode(0, List(Some("你好"))), Right("你好"))
    assertEquals(text.decode(0, List(Some("';--'"))), Right("';--'"))
    assertEquals(text.opt.decode(0, List(None)), Right(None))
  }

  test("mediumtext encode successfully") {
    assertEquals(mediumtext.encode("mediumtext"), List(Some(Encoded("mediumtext", false))))
    assertEquals(mediumtext.encode(""), List(Some(Encoded("", false))))
    assertEquals(mediumtext.encode("🔥 and 🌈"), List(Some(Encoded("🔥 and 🌈", false))))
    assertEquals(mediumtext.encode("\"שלום"), List(Some(Encoded("\"שלום", false))))
    assertEquals(mediumtext.encode("مرحب"), List(Some(Encoded("مرحب", false))))
    assertEquals(mediumtext.encode("你好"), List(Some(Encoded("你好", false))))
    assertEquals(mediumtext.encode("';--'"), List(Some(Encoded("';--'", false))))
  }

  test("mediumtext decode successfully") {
    assertEquals(mediumtext.decode(0, List(Some("mediumtext"))), Right("mediumtext"))
    assertEquals(mediumtext.decode(0, List(Some(""))), Right(""))
    assertEquals(mediumtext.decode(0, List(Some("🔥 and 🌈"))), Right("🔥 and 🌈"))
    assertEquals(mediumtext.decode(0, List(Some("שלום"))), Right("שלום"))
    assertEquals(mediumtext.decode(0, List(Some("مرحب"))), Right("مرحب"))
    assertEquals(mediumtext.decode(0, List(Some("你好"))), Right("你好"))
    assertEquals(mediumtext.decode(0, List(Some("';--'"))), Right("';--'"))
    assertEquals(mediumtext.opt.decode(0, List(None)), Right(None))
  }

  test("longtext encode successfully") {
    assertEquals(longtext.encode("longtext"), List(Some(Encoded("longtext", false))))
    assertEquals(longtext.encode(""), List(Some(Encoded("", false))))
    assertEquals(longtext.encode("🔥 and 🌈"), List(Some(Encoded("🔥 and 🌈", false))))
    assertEquals(longtext.encode("\"שלום"), List(Some(Encoded("\"שלום", false))))
    assertEquals(longtext.encode("مرحب"), List(Some(Encoded("مرحب", false))))
    assertEquals(longtext.encode("你好"), List(Some(Encoded("你好", false))))
    assertEquals(longtext.encode("';--'"), List(Some(Encoded("';--'", false))))
  }

  test("longtext decode successfully") {
    assertEquals(longtext.decode(0, List(Some("longtext"))), Right("longtext"))
    assertEquals(longtext.decode(0, List(Some(""))), Right(""))
    assertEquals(longtext.decode(0, List(Some("🔥 and 🌈"))), Right("🔥 and 🌈"))
    assertEquals(longtext.decode(0, List(Some("שלום"))), Right("שלום"))
    assertEquals(longtext.decode(0, List(Some("مرحب"))), Right("مرحب"))
    assertEquals(longtext.decode(0, List(Some("你好"))), Right("你好"))
    assertEquals(longtext.decode(0, List(Some("';--'"))), Right("';--'"))
    assertEquals(longtext.opt.decode(0, List(None)), Right(None))
  }
}
