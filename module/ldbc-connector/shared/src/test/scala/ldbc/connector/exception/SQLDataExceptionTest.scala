/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

import scala.collection.immutable.SortedMap

import org.typelevel.otel4s.Attribute

import ldbc.connector.data.Parameter
import ldbc.connector.FTestPlatform

class SQLDataExceptionTest extends FTestPlatform:

  test("SQLDataException should have correct basic properties") {
    val errorMessage = "Data conversion error"
    val sqlState     = "22003" // Value out of range
    val vendorCode   = 1264

    val exception = new SQLDataException(
      message    = errorMessage,
      sqlState   = Some(sqlState),
      vendorCode = Some(vendorCode)
    )

    assertEquals(exception.getMessage.contains(errorMessage), true)
    assertEquals(exception.getSQLState, sqlState)
    assertEquals(exception.getErrorCode, vendorCode)
  }

  test("SQLDataException should return default values when optional fields are not provided") {
    val exception = new SQLDataException("Simple data error")

    assertEquals(exception.getSQLState, "")
    assertEquals(exception.getErrorCode, 0)
  }

  test("SQLDataException.fields should return correct attributes") {
    val params = SortedMap(
      1 -> Parameter.string("test_value"),
      2 -> Parameter.int(42)
    )

    val exception = new SQLDataException(
      message    = "Data type mismatch",
      sqlState   = Some("22005"),
      vendorCode = Some(1366),
      sql        = Some("INSERT INTO numbers (int_column) VALUES (?)"),
      detail     = Some("Cannot convert string to integer"),
      hint       = Some("Provide a valid numeric value"),
      params     = params
    )

    val fields = exception.fields

    assertEquals(fields.contains(Attribute("error.message", "Data type mismatch")), true)
    assertEquals(fields.contains(Attribute("error.sqlstate", "22005")), true)
    assertEquals(fields.contains(Attribute("error.vendorCode", 1366L)), true)
    assertEquals(fields.contains(Attribute("error.sql", "INSERT INTO numbers (int_column) VALUES (?)")), true)
    assertEquals(fields.contains(Attribute("error.detail", "Cannot convert string to integer")), true)
    assertEquals(fields.contains(Attribute("error.hint", "Provide a valid numeric value")), true)

    // Check parameter attributes
    assertEquals(fields.contains(Attribute("error.parameter.1.type", "CHAR")), true)
    assertEquals(fields.contains(Attribute("error.parameter.1.value", "'test_value'")), true)
    assertEquals(fields.contains(Attribute("error.parameter.2.type", "INT")), true)
    assertEquals(fields.contains(Attribute("error.parameter.2.value", "42")), true)
  }

  test("labeled method should format messages correctly for SQLDataException") {
    val exception = new SQLDataException("Data validation error")

    val empty = exception.labeled("Label: ", "")
    assertEquals(empty, "")

    val nonEmpty = exception.labeled("Label: ", "Message")
    assert(nonEmpty.contains("Label: "), "Should contain the label")
    assert(nonEmpty.contains("Message"), "Should contain the message")
  }

  test("getMessage should include all relevant information for SQLDataException") {
    val exception = new SQLDataException(
      message    = "Invalid data format",
      sqlState   = Some("22007"),
      vendorCode = Some(1292),
      sql        = Some("INSERT INTO dates (created_at) VALUES (?)"),
      params     = SortedMap(1 -> Parameter.string("not-a-date"))
    )

    val message = exception.getMessage

    // Check if message contains all relevant parts
    assert(message.contains("Invalid data format"), "Message should contain the error description")
    assert(message.contains("22007"), "Message should contain the SQL state")
    assert(message.contains("1292"), "Message should contain the vendor code")
    assert(message.contains("INSERT INTO dates (created_at) VALUES (?)"), "Message should contain the SQL query")
    assert(message.contains("not-a-date"), "Message should contain the parameter value")
  }