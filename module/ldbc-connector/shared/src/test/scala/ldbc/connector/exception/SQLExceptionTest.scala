/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

import scala.collection.immutable.SortedMap

import org.typelevel.otel4s.Attribute

import ldbc.connector.FTestPlatform
import ldbc.connector.data.Parameter

class SQLExceptionTest extends FTestPlatform {
  
  test("SQLException should have correct basic properties") {
    val errorMessage = "Database connection failed"
    val sqlState = "08001"
    val vendorCode = 1234
    
    val exception = new SQLException(
      message = errorMessage,
      sqlState = Some(sqlState),
      vendorCode = Some(vendorCode)
    )
    
    assertEquals(exception.getMessage.contains(errorMessage), true)
    assertEquals(exception.getSQLState, sqlState)
    assertEquals(exception.getErrorCode, vendorCode)
  }
  
  test("SQLException should return default values when optional fields are not provided") {
    val exception = new SQLException("Simple error")
    
    assertEquals(exception.getSQLState, "")
    assertEquals(exception.getErrorCode, 0)
  }
  
  test("SQLException.fields should return correct attributes") {
    val params = SortedMap(
      1 -> Parameter.string("test_value"),
      2 -> Parameter.int(42)
    )
    
    val exception = new SQLException(
      message = "Query error",
      sqlState = Some("42S02"),
      vendorCode = Some(1146),
      sql = Some("SELECT * FROM non_existent_table"),
      detail = Some("Table does not exist"),
      hint = Some("Create the table first"),
      params = params
    )
    
    val fields = exception.fields
    
    assertEquals(fields.contains(Attribute("error.message", "Query error")), true)
    assertEquals(fields.contains(Attribute("error.sqlstate", "42S02")), true)
    assertEquals(fields.contains(Attribute("error.vendorCode", 1146L)), true)
    assertEquals(fields.contains(Attribute("error.sql", "SELECT * FROM non_existent_table")), true)
    assertEquals(fields.contains(Attribute("error.detail", "Table does not exist")), true)
    assertEquals(fields.contains(Attribute("error.hint", "Create the table first")), true)

    // Check parameter attributes
    assertEquals(fields.contains(Attribute("error.parameter.1.type", "CHAR")), true)
    assertEquals(fields.contains(Attribute("error.parameter.1.value", "'test_value'")), true)
    assertEquals(fields.contains(Attribute("error.parameter.2.type", "INT")), true)
    assertEquals(fields.contains(Attribute("error.parameter.2.value", "42")), true)
  }
  
  test("labeled method should format messages correctly") {
    val exception = new SQLException("Test error")
    
    val empty = exception.labeled("Label: ", "")
    assertEquals(empty, "")
    
    val nonEmpty = exception.labeled("Label: ", "Message")
    assert(nonEmpty.contains("Label: "), "Should contain the label")
    assert(nonEmpty.contains("Message"), "Should contain the message")
  }
  
  test("getMessage should include all relevant information") {
    val exception = new SQLException(
      message = "Query failed",
      sqlState = Some("HY000"),
      vendorCode = Some(9999),
      sql = Some("SELECT * FROM users WHERE id = ?"),
      params = SortedMap(1 -> Parameter.int(123))
    )
    
    val message = exception.getMessage
    
    // Check if message contains all relevant parts
    assert(message.contains("Query failed"), "Message should contain the error description")
    assert(message.contains("HY000"), "Message should contain the SQL state")
    assert(message.contains("9999"), "Message should contain the vendor code")
    assert(message.contains("SELECT * FROM users WHERE id = ?"), "Message should contain the SQL query")
    assert(message.contains("123"), "Message should contain the parameter value")
  }
}
