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

class SQLIntegrityConstraintViolationExceptionTest extends FTestPlatform:

  test("SQLIntegrityConstraintViolationException should have correct basic properties") {
    val errorMessage = "Duplicate entry '1' for key 'PRIMARY'"
    val sqlState     = "23000"
    val vendorCode   = 1062

    val exception = new SQLIntegrityConstraintViolationException(
      message    = errorMessage,
      sqlState   = Some(sqlState),
      vendorCode = Some(vendorCode)
    )

    assertEquals(exception.getMessage.contains(errorMessage), true)
    assertEquals(exception.getSQLState, sqlState)
    assertEquals(exception.getErrorCode, vendorCode)
  }

  test("SQLIntegrityConstraintViolationException should return default values when optional fields are not provided") {
    val exception = new SQLIntegrityConstraintViolationException("Foreign key violation")

    assertEquals(exception.getSQLState, "")
    assertEquals(exception.getErrorCode, 0)
  }

  test("SQLIntegrityConstraintViolationException.fields should return correct attributes") {
    val params = SortedMap(
      1 -> Parameter.int(101),
      2 -> Parameter.string("test_user")
    )

    val exception = new SQLIntegrityConstraintViolationException(
      message    = "Violation of unique constraint",
      sqlState   = Some("23505"),
      vendorCode = Some(1063),
      sql        = Some("INSERT INTO users (id, name) VALUES (?, ?)"),
      detail     = Some("Key (id)=(101) already exists"),
      hint       = Some("Use a different primary key value"),
      params     = params
    )

    val fields = exception.fields

    assertEquals(fields.contains(Attribute("error.message", "Violation of unique constraint")), true)
    assertEquals(fields.contains(Attribute("error.sqlstate", "23505")), true)
    assertEquals(fields.contains(Attribute("error.vendorCode", 1063L)), true)
    assertEquals(fields.contains(Attribute("error.sql", "INSERT INTO users (id, name) VALUES (?, ?)")), true)
    assertEquals(fields.contains(Attribute("error.detail", "Key (id)=(101) already exists")), true)
    assertEquals(fields.contains(Attribute("error.hint", "Use a different primary key value")), true)

    // Check parameter attributes
    assertEquals(fields.contains(Attribute("error.parameter.1.type", "INT")), true)
    assertEquals(fields.contains(Attribute("error.parameter.1.value", "101")), true)
    assertEquals(fields.contains(Attribute("error.parameter.2.type", "CHAR")), true)
    assertEquals(fields.contains(Attribute("error.parameter.2.value", "'test_user'")), true)
  }

  test("getMessage should include all relevant information") {
    val exception = new SQLIntegrityConstraintViolationException(
      message    = "Foreign key constraint violated",
      sqlState   = Some("23503"),
      vendorCode = Some(1452),
      sql        = Some("INSERT INTO orders (user_id, product_id) VALUES (?, ?)"),
      params = SortedMap(
        1 -> Parameter.int(999),
        2 -> Parameter.int(101)
      )
    )

    val message = exception.getMessage

    // Check if message contains all relevant parts
    assert(message.contains("Foreign key constraint violated"), "Message should contain the error description")
    assert(message.contains("23503"), "Message should contain the SQL state")
    assert(message.contains("1452"), "Message should contain the vendor code")
    assert(
      message.contains("INSERT INTO orders (user_id, product_id) VALUES (?, ?)"),
      "Message should contain the SQL query"
    )
    assert(message.contains("999"), "Message should contain the first parameter value")
    assert(message.contains("101"), "Message should contain the second parameter value")
  }
