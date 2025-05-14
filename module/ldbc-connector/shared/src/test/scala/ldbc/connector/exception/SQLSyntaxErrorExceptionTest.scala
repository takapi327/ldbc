/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

import org.typelevel.otel4s.Attribute

import ldbc.connector.FTestPlatform

class SQLSyntaxErrorExceptionTest extends FTestPlatform:

  test("SQLSyntaxErrorException should have correct basic properties") {
    val errorMessage = "SQL syntax error in query"
    val sqlState     = "42000"
    val vendorCode   = 1064

    val exception = new SQLSyntaxErrorException(
      message    = errorMessage,
      sqlState   = Some(sqlState),
      vendorCode = Some(vendorCode)
    )

    assertEquals(exception.getMessage.contains(errorMessage), true)
    assertEquals(exception.getSQLState, sqlState)
    assertEquals(exception.getErrorCode, vendorCode)
  }

  test("SQLSyntaxErrorException should return default values when optional fields are not provided") {
    val exception = new SQLSyntaxErrorException("Simple syntax error")

    assertEquals(exception.getSQLState, "")
    assertEquals(exception.getErrorCode, 0)
  }

  test("SQLSyntaxErrorException.fields should return correct attributes") {
    val exception = new SQLSyntaxErrorException(
      message    = "Syntax error in query",
      sqlState   = Some("42000"),
      vendorCode = Some(1064),
      sql        = Some("SELECT * FROM users WHERE;"),
      detail     = Some("Unexpected semicolon after WHERE"),
      hint       = Some("Add a condition after WHERE clause")
    )

    val fields = exception.fields

    assertEquals(fields.contains(Attribute("error.message", "Syntax error in query")), true)
    assertEquals(fields.contains(Attribute("error.sqlstate", "42000")), true)
    assertEquals(fields.contains(Attribute("error.vendorCode", 1064L)), true)
    assertEquals(fields.contains(Attribute("error.sql", "SELECT * FROM users WHERE;")), true)
    assertEquals(fields.contains(Attribute("error.detail", "Unexpected semicolon after WHERE")), true)
    assertEquals(fields.contains(Attribute("error.hint", "Add a condition after WHERE clause")), true)
  }

  test("labeled method should format messages correctly") {
    val exception = new SQLSyntaxErrorException("Test syntax error")

    val empty = exception.labeled("Label: ", "")
    assertEquals(empty, "")

    val nonEmpty = exception.labeled("Label: ", "Message")
    assert(nonEmpty.contains("Label: "), "Should contain the label")
    assert(nonEmpty.contains("Message"), "Should contain the message")
  }

  test("getMessage should include all relevant information") {
    val exception = new SQLSyntaxErrorException(
      message    = "Syntax error in query",
      sqlState   = Some("42000"),
      vendorCode = Some(1064),
      sql        = Some("SELECT * FROM users ORDER BY;"),
      detail     = Some("Missing column name after ORDER BY"),
      hint       = Some("Specify a column to sort by")
    )

    val message = exception.getMessage

    // Check if message contains all relevant parts
    assert(message.contains("Syntax error in query"), "Message should contain the error description")
    assert(message.contains("42000"), "Message should contain the SQL state")
    assert(message.contains("1064"), "Message should contain the vendor code")
    assert(message.contains("SELECT * FROM users ORDER BY;"), "Message should contain the SQL query")
    assert(message.contains("Missing column name"), "Message should contain the detail")
    assert(message.contains("Specify a column"), "Message should contain the hint")
  }