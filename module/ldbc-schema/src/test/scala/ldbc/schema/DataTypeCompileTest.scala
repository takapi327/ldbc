/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

class DataTypeCompileTest extends munit.FunSuite:

  test("Successful TINYBLOB compile") {
    assertEquals(compileErrors("""
        import ldbc.schema.*
        import ldbc.schema.DataType.*

        val p1: Tinyblob[Option[Array[Byte]]] = TINYBLOB[Option[Array[Byte]]]().DEFAULT(None)
        """), "")
  }

  test("Fails TINYBLOB compile") {
    assert(compileErrors("""
        import ldbc.schema.*
        import ldbc.schema.DataType.*

        val p1: Tinyblob[Array[Byte]] = TINYBLOB[Array[Byte]]().DEFAULT(Array.emptyByteArray)
        val p2: Tinyblob[Option[Array[Byte]]] = TINYBLOB[Option[Array[Byte]]]().DEFAULT(Some(Array.emptyByteArray))
        """).nonEmpty)
  }

  test("Successful BLOB compile") {
    assertEquals(compileErrors("""
        import ldbc.schema.*
        import ldbc.schema.DataType.*

        val p1: Blob[Option[Array[Byte]]] = BLOB[Option[Array[Byte]]]().DEFAULT(None)
        """), "")
  }

  test("Fails BLOB compile") {
    assert(compileErrors("""
        import ldbc.schema.*
        import ldbc.schema.DataType.*

        val p1: Blob[Array[Byte]] = BLOB[Array[Byte]]().DEFAULT(Array.emptyByteArray)
        val p2: Blob[Option[Array[Byte]]] = BLOB[Option[Array[Byte]]]().DEFAULT(Some(Array.emptyByteArray))
        """).nonEmpty)
  }

  test("Successful MEDIUMBLOB compile") {
    assertEquals(compileErrors("""
        import ldbc.schema.*
        import ldbc.schema.DataType.*

        val p1: Mediumblob[Option[Array[Byte]]] = MEDIUMBLOB[Option[Array[Byte]]]().DEFAULT(None)
        """), "")
  }

  test("Fails MEDIUMBLOB compile") {
    assert(compileErrors("""
        import ldbc.schema.*
        import ldbc.schema.DataType.*

        val p1: Mediumblob[Array[Byte]] = MEDIUMBLOB[Array[Byte]]().DEFAULT(Array.emptyByteArray)
        val p2: Mediumblob[Option[Array[Byte]]] = MEDIUMBLOB[Option[Array[Byte]]]().DEFAULT(Some(Array.emptyByteArray))
        """).nonEmpty)
  }

  test("Successful LONGBLOB compile") {
    assertEquals(compileErrors("""
        import ldbc.schema.*
        import ldbc.schema.DataType.*

        val p1: LongBlob[Option[Array[Byte]]] = LONGBLOB[Option[Array[Byte]]]().DEFAULT(None)
        """), "")
  }

  test("Fails LONGBLOB compile") {
    assert(compileErrors("""
        import ldbc.schema.*
        import ldbc.schema.DataType.*

        val p1: LongBlob[Array[Byte]] = LONGBLOB[Array[Byte]]().DEFAULT(Array.emptyByteArray)
        val p2: LongBlob[Option[Array[Byte]]] = LONGBLOB[Option[Array[Byte]]]().DEFAULT(Some(Array.emptyByteArray))
        """).nonEmpty)
  }

  test("Successful TINYTEXT compile") {
    assertEquals(compileErrors("""
        import ldbc.schema.*
        import ldbc.schema.DataType.*

        val p1: TinyText[Option[String]] = TINYTEXT[Option[String]]().DEFAULT(None)
        """), "")
  }

  test("Fails TINYTEXT compile") {
    assert(compileErrors("""
        import ldbc.schema.*
        import ldbc.schema.DataType.*

        val p1: TinyText[String] = TINYTEXT[String]().DEFAULT("value")
        val p2: TinyText[Option[String]] = TINYTEXT[Option[String]]().DEFAULT(Some("value"))
        """).nonEmpty)
  }

  test("Successful TEXT compile") {
    assertEquals(compileErrors("""
        import ldbc.schema.*
        import ldbc.schema.DataType.*

        val p1: Text[Option[String]] = TEXT[Option[String]]().DEFAULT(None)
        """), "")
  }

  test("Fails TEXT compile") {
    assert(compileErrors("""
        import ldbc.schema.*
        import ldbc.schema.DataType.*

        val p1: Text[String] = TEXT[String]().DEFAULT("value")
        val p2: Text[Option[String]] = TEXT[Option[String]]().DEFAULT(Some("value"))
        """).nonEmpty)
  }

  test("Successful MEDIUMTEXT compile") {
    assertEquals(compileErrors("""
        import ldbc.schema.*
        import ldbc.schema.DataType.*

        val p1: MediumText[Option[String]] = MEDIUMTEXT[Option[String]]().DEFAULT(None)
        """), "")
  }

  test("Fails MEDIUMTEXT compile") {
    assert(compileErrors("""
        import ldbc.schema.*
        import ldbc.schema.DataType.*

        val p1: MediumText[String] = MEDIUMTEXT[String]().DEFAULT("value")
        val p2: MediumText[Option[String]] = MEDIUMTEXT[Option[String]]().DEFAULT(Some("value"))
        """).nonEmpty)
  }

  test("Successful LONGTEXT compile") {
    assertEquals(compileErrors("""
        import ldbc.schema.*
        import ldbc.schema.DataType.*

        val p1: LongText[Option[String]] = LONGTEXT[Option[String]]().DEFAULT(None)
        """), "")
  }

  test("Fails LONGTEXT compile") {
    assert(compileErrors("""
        import ldbc.schema.*
        import ldbc.schema.DataType.*

        val p1: LongText[String] = LONGTEXT[String]().DEFAULT("value")
        val p2: LongText[Option[String]] = LONGTEXT[Option[String]]().DEFAULT(Some("value"))
        """).nonEmpty)
  }
