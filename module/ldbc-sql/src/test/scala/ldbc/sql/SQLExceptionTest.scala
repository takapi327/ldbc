/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

class SQLExceptionTest extends munit.FunSuite:

  test("getSQLState and getErrorCode return the provided values, or defaults when unknown") {
    val full = new SQLException("boom", sqlState = Some("42000"), vendorCode = Some(1064))
    assertEquals(full.getSQLState, "42000")
    assertEquals(full.getErrorCode, 1064)

    val bare = new SQLException("boom")
    assertEquals(bare.getSQLState, "")
    assertEquals(bare.getErrorCode, 0)
  }

  test("fields summarises the standard diagnostics as attributes (no parameter values)") {
    val exception = new SQLException(
      "Invalid data",
      sqlState   = Some("22007"),
      vendorCode = Some(1292),
      sql        = Some("INSERT INTO t VALUES (?)"),
      detail     = Some("bad date"),
      hint       = Some("use ISO-8601")
    )
    val fields = exception.fields
    assert(fields.contains(Attribute("error.message", "Invalid data")))
    assert(fields.contains(Attribute("error.sqlstate", "22007")))
    assert(fields.contains(Attribute("error.vendorCode", 1292L)))
    assert(fields.contains(Attribute("error.sql", "INSERT INTO t VALUES (?)")))
    assert(fields.contains(Attribute("error.detail", "bad date")))
    assert(fields.contains(Attribute("error.hint", "use ISO-8601")))
    assert(!fields.exists(_.key.startsWith("error.parameter")), "bound parameter values must never appear in fields")
  }

  test("getMessage includes the diagnostics and honours the vendor label") {
    val message = new SQLException(
      "Data validation error",
      sqlState   = Some("22007"),
      vendorCode = Some(1292),
      sql        = Some("SELECT 1"),
      vendor     = "MySQL"
    ).getMessage
    assert(message.contains("Data validation error"))
    assert(message.contains("22007"))
    assert(message.contains("1292"))
    assert(message.contains("SELECT 1"))
    assert(message.contains("MySQL ERROR"), "the vendor label should brand the rendered title")

    val default = new SQLException("boom").getMessage
    assert(default.contains("SQL ERROR"), "the default vendor label is SQL")
  }

  test("BatchUpdateException adds the update counts to fields") {
    val fields = new BatchUpdateException("batch failed", updateCounts = List(1L, 1L, -3L)).fields
    assert(fields.contains(Attribute("error.updateCounts", "[1,1,-3]")))
    assert(fields.contains(Attribute("error.message", "batch failed")))
  }

  test("the JDBC category hierarchy is preserved") {
    assert(new SQLDataException("x").isInstanceOf[SQLNonTransientException])
    assert(new SQLDataException("x").isInstanceOf[SQLException])
    assert(new SQLTimeoutException("x").isInstanceOf[SQLTransientException])
    assert(new SQLTransactionRollbackException("x").isInstanceOf[SQLTransientException])
  }
