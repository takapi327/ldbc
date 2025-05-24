/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import ldbc.sql.ResultSetMetaData

import ldbc.connector.data.*
import ldbc.connector.net.packet.response.*
import ldbc.connector.util.Version

class ResultSetMetaDataTest extends FTestPlatform:

  // Helper method to create a test ColumnDefinition41Packet
  private def createTestColumn(
    name:       String,
    columnType: ColumnDataType,
    flags:      Seq[ColumnDefinitionFlags] = Seq.empty,
    decimals:   Int = 0,
    length:     Long = 10,
    table:      String = "test_table",
    orgName:    String = "",
    orgTable:   String = "",
    schema:     String = "test_schema",
    catalog:    String = "test_catalog"
  ): ColumnDefinitionPacket =
    new ColumnDefinition41Packet(
      catalog      = catalog,
      schema       = schema,
      table        = table,
      orgTable     = orgTable,
      name         = name,
      orgName      = if orgName.isEmpty then name else orgName,
      length       = 0,
      characterSet = 8,
      columnLength = length,
      columnType   = columnType,
      flags        = flags,
      decimals     = decimals
    )

  test("getColumnCount should return correct number of columns") {
    val columns = Vector(
      createTestColumn("id", ColumnDataType.MYSQL_TYPE_LONG),
      createTestColumn("name", ColumnDataType.MYSQL_TYPE_VARCHAR),
      createTestColumn("price", ColumnDataType.MYSQL_TYPE_DECIMAL)
    )

    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))
    assertEquals(metaData.getColumnCount(), 3)
  }

  test("isAutoIncrement should return correct value based on flags") {
    val autoIncColumn = createTestColumn(
      "id",
      ColumnDataType.MYSQL_TYPE_LONG,
      Seq(ColumnDefinitionFlags.AUTO_INCREMENT_FLAG)
    )
    val normalColumn = createTestColumn("name", ColumnDataType.MYSQL_TYPE_VARCHAR)

    val columns  = Vector(autoIncColumn, normalColumn)
    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))

    assert(metaData.isAutoIncrement(1))
    assert(!metaData.isAutoIncrement(2))
  }

  test("isCaseSensitive should return correct value based on column type") {
    val columns = Vector(
      createTestColumn("id", ColumnDataType.MYSQL_TYPE_LONG),     // Numeric, not case sensitive
      createTestColumn("name", ColumnDataType.MYSQL_TYPE_VARCHAR) // String, potentially case sensitive
    )

    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))

    assert(!metaData.isCaseSensitive(1)) // Numeric types are not case sensitive
    // For string types, it depends on the collation, but we're not testing the specific behavior here
  }

  test("isSearchable should return true for all columns") {
    val columns = Vector(
      createTestColumn("id", ColumnDataType.MYSQL_TYPE_LONG),
      createTestColumn("name", ColumnDataType.MYSQL_TYPE_VARCHAR)
    )

    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))

    assert(metaData.isSearchable(1))
    assert(metaData.isSearchable(2))
  }

  test("isCurrency should return false for all columns") {
    val columns = Vector(
      createTestColumn("id", ColumnDataType.MYSQL_TYPE_LONG),
      createTestColumn("price", ColumnDataType.MYSQL_TYPE_DECIMAL)
    )

    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))

    assert(!metaData.isCurrency(1))
    assert(!metaData.isCurrency(2))
  }

  test("isNullable should return correct value based on NOT_NULL_FLAG") {
    val notNullColumn = createTestColumn(
      "id",
      ColumnDataType.MYSQL_TYPE_LONG,
      Seq(ColumnDefinitionFlags.NOT_NULL_FLAG)
    )
    val nullableColumn = createTestColumn("name", ColumnDataType.MYSQL_TYPE_VARCHAR)

    val columns  = Vector(notNullColumn, nullableColumn)
    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))

    assertEquals(metaData.isNullable(1), ResultSetMetaData.columnNoNulls)
    assertEquals(metaData.isNullable(2), ResultSetMetaData.columnNullable)
  }

  test("isSigned should return correct value based on UNSIGNED_FLAG") {
    val unsignedColumn = createTestColumn(
      "id",
      ColumnDataType.MYSQL_TYPE_LONG,
      Seq(ColumnDefinitionFlags.UNSIGNED_FLAG)
    )
    val signedColumn = createTestColumn("value", ColumnDataType.MYSQL_TYPE_LONG)

    val columns  = Vector(unsignedColumn, signedColumn)
    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))

    assert(metaData.isSigned(1))  // Has UNSIGNED_FLAG
    assert(!metaData.isSigned(2)) // Does not have UNSIGNED_FLAG
  }

  test("getColumnDisplaySize should return clamped length") {
    val column1 = createTestColumn("short", ColumnDataType.MYSQL_TYPE_VARCHAR)
    val column2 = createTestColumn("long", ColumnDataType.MYSQL_TYPE_VARCHAR, length = 1000)

    val columns  = Vector(column1, column2)
    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))

    assertEquals(metaData.getColumnDisplaySize(1), 0)
    assertEquals(metaData.getColumnDisplaySize(2), 0)
  }

  test("getColumnLabel and getColumnName should return correct values") {
    val column1 = createTestColumn("id", ColumnDataType.MYSQL_TYPE_LONG, orgName = "user_id")
    val column2 = createTestColumn("name", ColumnDataType.MYSQL_TYPE_VARCHAR)

    val columns  = Vector(column1, column2)
    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))

    assertEquals(metaData.getColumnLabel(1), "id")     // Column alias
    assertEquals(metaData.getColumnName(1), "user_id") // Original column name

    assertEquals(metaData.getColumnLabel(2), "name")
    assertEquals(metaData.getColumnName(2), "name") // Same as label when no alias
  }

  test("getSchemaName should return correct schema") {
    val column = createTestColumn("id", ColumnDataType.MYSQL_TYPE_LONG, schema = "test_schema")

    val columns  = Vector(column)
    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))

    assertEquals(metaData.getSchemaName(1), "test_schema")
  }

  test("getPrecision should return correct precision based on column type") {
    val decimalColumn = createTestColumn("amount", ColumnDataType.MYSQL_TYPE_DECIMAL)
    val textColumn    = createTestColumn("description", ColumnDataType.MYSQL_TYPE_BLOB, length = 100)

    val columns  = Vector(decimalColumn, textColumn)
    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))

    assertEquals(metaData.getPrecision(1), 0)
    assertEquals(metaData.getPrecision(2), 0)
  }

  test("getScale should return correct decimal places") {
    val decimalColumn = createTestColumn("amount", ColumnDataType.MYSQL_TYPE_DECIMAL, decimals = 2)
    val integerColumn = createTestColumn("count", ColumnDataType.MYSQL_TYPE_LONG)

    val columns  = Vector(decimalColumn, integerColumn)
    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))

    assertEquals(metaData.getScale(1), 2)
    assertEquals(metaData.getScale(2), 0)
  }

  test("getTableName should return original table name") {
    val column = createTestColumn("id", ColumnDataType.MYSQL_TYPE_LONG, table = "view_users", orgTable = "users")

    val columns  = Vector(column)
    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))

    assertEquals(metaData.getTableName(1), "users")
  }

  test("getCatalogName should return catalog") {
    val column = createTestColumn("id", ColumnDataType.MYSQL_TYPE_LONG, catalog = "test_db")

    val columns  = Vector(column)
    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))

    assertEquals(metaData.getCatalogName(1), "test_db")
  }

  test("getColumnType should return SQL type") {
    val intColumn  = createTestColumn("id", ColumnDataType.MYSQL_TYPE_LONG)
    val yearColumn = createTestColumn("year", ColumnDataType.MYSQL_TYPE_YEAR)

    val columns  = Vector(intColumn, yearColumn)
    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))

    assertEquals(metaData.getColumnType(1), ColumnDataType.MYSQL_TYPE_LONG.code.toInt)
    assertEquals(metaData.getColumnType(2), ColumnDataType.MYSQL_TYPE_SHORT.code.toInt) // YEAR is represented as SHORT
  }

  test("getColumnTypeName should return type name") {
    val intColumn     = createTestColumn("id", ColumnDataType.MYSQL_TYPE_LONG)
    val varcharColumn = createTestColumn("name", ColumnDataType.MYSQL_TYPE_VARCHAR)

    val columns  = Vector(intColumn, varcharColumn)
    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))

    assertEquals(metaData.getColumnTypeName(1), "INT")
    assertEquals(metaData.getColumnTypeName(2), "VARCHAR")
  }

  test("isReadOnly should identify read-only columns") {
    val readWriteColumn = createTestColumn("id", ColumnDataType.MYSQL_TYPE_LONG, table = "users", orgName = "id")
    val readOnlyColumn  = createTestColumn("", ColumnDataType.MYSQL_TYPE_VARCHAR, table = "")

    val columns  = Vector(readWriteColumn, readOnlyColumn)
    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))

    assert(!metaData.isReadOnly(1))
    assert(metaData.isReadOnly(2))
  }

  test("isWritable should be inverse of isReadOnly") {
    val readWriteColumn = createTestColumn("id", ColumnDataType.MYSQL_TYPE_LONG, table = "users", orgName = "id")
    val readOnlyColumn  = createTestColumn("", ColumnDataType.MYSQL_TYPE_VARCHAR, table = "")

    val columns  = Vector(readWriteColumn, readOnlyColumn)
    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))

    assert(metaData.isWritable(1))
    assert(!metaData.isWritable(2))
  }

  test("isDefinitelyWritable should match isWritable") {
    val readWriteColumn = createTestColumn("id", ColumnDataType.MYSQL_TYPE_LONG, table = "users", orgName = "id")
    val readOnlyColumn  = createTestColumn("", ColumnDataType.MYSQL_TYPE_VARCHAR, table = "")

    val columns  = Vector(readWriteColumn, readOnlyColumn)
    val metaData = ResultSetMetaDataImpl(columns, Map("character_set_client" -> "utf8mb4"), Version(8, 0, 25))

    assert(metaData.isDefinitelyWritable(1))
    assert(!metaData.isDefinitelyWritable(2))
    assertEquals(metaData.isDefinitelyWritable(1), metaData.isWritable(1))
    assertEquals(metaData.isDefinitelyWritable(2), metaData.isWritable(2))
  }
