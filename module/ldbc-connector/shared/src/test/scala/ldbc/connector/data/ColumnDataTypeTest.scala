/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import ldbc.connector.*

class ColumnDataTypeTest extends FTestPlatform:

  test("apply(code: Long) should return the correct data type") {
    assertEquals(ColumnDataType(0x00), ColumnDataType.MYSQL_TYPE_DECIMAL)
    assertEquals(ColumnDataType(0x01), ColumnDataType.MYSQL_TYPE_TINY)
    assertEquals(ColumnDataType(0x03), ColumnDataType.MYSQL_TYPE_LONG)
    assertEquals(ColumnDataType(0xff), ColumnDataType.MYSQL_TYPE_GEOMETRY)
  }

  test("apply(code: Long) should throw IllegalArgumentException for unknown code") {
    val exception = intercept[IllegalArgumentException] {
      ColumnDataType(0x100) // Non-existent code
    }
    assert(exception.getMessage.contains("Unknown column data type code"))
  }

  test("toString should return the correct data type name") {
    assertEquals(ColumnDataType.MYSQL_TYPE_DECIMAL.toString, "DECIMAL")
    assertEquals(ColumnDataType.MYSQL_TYPE_TINY.toString, "TINYINT")
    assertEquals(ColumnDataType.MYSQL_TYPE_LONG.toString, "INT")
    assertEquals(ColumnDataType.MYSQL_TYPE_VARCHAR.toString, "VARCHAR")
  }

  test("code field should return the correct numeric value") {
    assertEquals(ColumnDataType.MYSQL_TYPE_DECIMAL.code, 0x00L)
    assertEquals(ColumnDataType.MYSQL_TYPE_TINY.code, 0x01L)
    assertEquals(ColumnDataType.MYSQL_TYPE_JSON.code, 0xf5L)
    assertEquals(ColumnDataType.MYSQL_TYPE_GEOMETRY.code, 0xffL)
  }

  test("name field should return the correct type name") {
    assertEquals(ColumnDataType.MYSQL_TYPE_DECIMAL.name, "DECIMAL")
    assertEquals(ColumnDataType.MYSQL_TYPE_TINY.name, "TINYINT")
    assertEquals(ColumnDataType.MYSQL_TYPE_JSON.name, "JSON")
    assertEquals(ColumnDataType.MYSQL_TYPE_GEOMETRY.name, "GEOMETRY")
  }

  test("values should include all MySQL data types") {
    val values = ColumnDataType.values

    // Verify presence of all MySQL data types
    assert(values.contains(ColumnDataType.MYSQL_TYPE_DECIMAL))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_TINY))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_SHORT))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_LONG))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_FLOAT))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_DOUBLE))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_NULL))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_TIMESTAMP))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_LONGLONG))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_INT24))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_DATE))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_TIME))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_DATETIME))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_YEAR))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_NEWDATE))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_VARCHAR))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_BIT))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_TIMESTAMP2))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_DATETIME2))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_TIME2))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_TYPED_ARRAY))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_VECTOR))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_INVALID))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_BOOL))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_JSON))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_NEWDECIMAL))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_ENUM))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_SET))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_TINY_BLOB))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_MEDIUM_BLOB))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_LONG_BLOB))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_BLOB))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_VAR_STRING))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_STRING))
    assert(values.contains(ColumnDataType.MYSQL_TYPE_GEOMETRY))

    assertEquals(values.length, 35)
  }
