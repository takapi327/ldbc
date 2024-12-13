/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import org.scalatest.flatspec.AnyFlatSpec

class DataTypeCompileTest extends AnyFlatSpec:

  it should "Successful TINYBLOB compile" in {
    assertCompiles("""
        |import ldbc.schema.*
        |import ldbc.schema.DataType.*
        |
        |val p1: Tinyblob[Option[Array[Byte]]] = TINYBLOB[Option[Array[Byte]]]().DEFAULT(None)
        |""".stripMargin)
  }

  it should "Fails TINYBLOB compile" in {
    assertDoesNotCompile("""
        |import ldbc.schema.*
        |import ldbc.schema.DataType.*
        |
        |val p1: Tinyblob[Array[Byte]] = TINYBLOB[Array[Byte]]().DEFAULT(Array.emptyByteArray)
        |val p2: Tinyblob[Option[Array[Byte]]] = TINYBLOB[Option[Array[Byte]]]().DEFAULT(Some(Array.emptyByteArray))
        |""".stripMargin)
  }

  it should "Successful BLOB compile" in {
    assertCompiles("""
        |import ldbc.schema.*
        |import ldbc.schema.DataType.*
        |
        |val p1: Blob[Option[Array[Byte]]] = BLOB[Option[Array[Byte]]]().DEFAULT(None)
        |""".stripMargin)
  }

  it should "Fails BLOB compile" in {
    assertDoesNotCompile("""
        |import ldbc.schema.*
        |import ldbc.schema.DataType.*
        |
        |val p1: Blob[Array[Byte]] = BLOB[Array[Byte]]().DEFAULT(Array.emptyByteArray)
        |val p2: Blob[Option[Array[Byte]]] = BLOB[Option[Array[Byte]]]().DEFAULT(Some(Array.emptyByteArray))
        |""".stripMargin)
  }

  it should "Successful MEDIUMBLOB compile" in {
    assertCompiles("""
        |import ldbc.schema.*
        |import ldbc.schema.DataType.*
        |
        |val p1: Mediumblob[Option[Array[Byte]]] = MEDIUMBLOB[Option[Array[Byte]]]().DEFAULT(None)
        |""".stripMargin)
  }

  it should "Fails MEDIUMBLOB compile" in {
    assertDoesNotCompile("""
        |import ldbc.schema.*
        |import ldbc.schema.DataType.*
        |
        |val p1: Mediumblob[Array[Byte]] = MEDIUMBLOB[Array[Byte]]().DEFAULT(Array.emptyByteArray)
        |val p2: Mediumblob[Option[Array[Byte]]] = MEDIUMBLOB[Option[Array[Byte]]]().DEFAULT(Some(Array.emptyByteArray))
        |""".stripMargin)
  }

  it should "Successful LONGBLOB compile" in {
    assertCompiles("""
        |import ldbc.schema.*
        |import ldbc.schema.DataType.*
        |
        |val p1: LongBlob[Option[Array[Byte]]] = LONGBLOB[Option[Array[Byte]]]().DEFAULT(None)
        |""".stripMargin)
  }

  it should "Fails LONGBLOB compile" in {
    assertDoesNotCompile("""
        |import ldbc.schema.*
        |import ldbc.schema.DataType.*
        |
        |val p1: LongBlob[Array[Byte]] = LONGBLOB[Array[Byte]]().DEFAULT(Array.emptyByteArray)
        |val p2: LongBlob[Option[Array[Byte]]] = LONGBLOB[Option[Array[Byte]]]().DEFAULT(Some(Array.emptyByteArray))
        |""".stripMargin)
  }

  it should "Successful TINYTEXT compile" in {
    assertCompiles("""
        |import ldbc.schema.*
        |import ldbc.schema.DataType.*
        |
        |val p1: TinyText[Option[String]] = TINYTEXT[Option[String]]().DEFAULT(None)
        |""".stripMargin)
  }

  it should "Fails TINYTEXT compile" in {
    assertDoesNotCompile("""
        |import ldbc.schema.*
        |import ldbc.schema.DataType.*
        |
        |val p1: TinyText[String] = TINYTEXT[String]().DEFAULT("value")
        |val p2: TinyText[Option[String]] = TINYTEXT[Option[String]]().DEFAULT(Some("value"))
        |""".stripMargin)
  }

  it should "Successful TEXT compile" in {
    assertCompiles("""
        |import ldbc.schema.*
        |import ldbc.schema.DataType.*
        |
        |val p1: Text[Option[String]] = TEXT[Option[String]]().DEFAULT(None)
        |""".stripMargin)
  }

  it should "Fails TEXT compile" in {
    assertDoesNotCompile("""
        |import ldbc.schema.*
        |import ldbc.schema.DataType.*
        |
        |val p1: Text[String] = TEXT[String]().DEFAULT("value")
        |val p2: Text[Option[String]] = TEXT[Option[String]]().DEFAULT(Some("value"))
        |""".stripMargin)
  }

  it should "Successful MEDIUMTEXT compile" in {
    assertCompiles("""
        |import ldbc.schema.*
        |import ldbc.schema.DataType.*
        |
        |val p1: MediumText[Option[String]] = MEDIUMTEXT[Option[String]]().DEFAULT(None)
        |""".stripMargin)
  }

  it should "Fails MEDIUMTEXT compile" in {
    assertDoesNotCompile("""
        |import ldbc.schema.*
        |import ldbc.schema.DataType.*
        |
        |val p1: MediumText[String] = MEDIUMTEXT[String]().DEFAULT("value")
        |val p2: MediumText[Option[String]] = MEDIUMTEXT[Option[String]]().DEFAULT(Some("value"))
        |""".stripMargin)
  }

  it should "Successful LONGTEXT compile" in {
    assertCompiles("""
        |import ldbc.schema.*
        |import ldbc.schema.DataType.*
        |
        |val p1: LongText[Option[String]] = LONGTEXT[Option[String]]().DEFAULT(None)
        |""".stripMargin)
  }

  it should "Fails LONGTEXT compile" in {
    assertDoesNotCompile("""
        |import ldbc.schema.*
        |import ldbc.schema.DataType.*
        |
        |val p1: LongText[String] = LONGTEXT[String]().DEFAULT("value")
        |val p2: LongText[Option[String]] = LONGTEXT[Option[String]]().DEFAULT(Some("value"))
        |""".stripMargin)
  }
