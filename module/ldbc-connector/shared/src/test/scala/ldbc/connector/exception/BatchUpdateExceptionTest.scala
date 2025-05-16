/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

import org.typelevel.otel4s.Attribute

import ldbc.connector.FTestPlatform

class BatchUpdateExceptionTest extends FTestPlatform:

  test("BatchUpdateException should have correct basic properties") {
    val errorMessage = "Batch update failed"
    val sqlState     = "40001"
    val vendorCode   = 5678
    val updateCounts = List(1L, 2L, -3L)

    val exception = new BatchUpdateException(
      message      = errorMessage,
      updateCounts = updateCounts,
      sqlState     = Some(sqlState),
      vendorCode   = Some(vendorCode)
    )

    assertEquals(exception.getMessage.contains(errorMessage), true)
    assertEquals(exception.getSQLState, sqlState)
    assertEquals(exception.getErrorCode, vendorCode)
  }

  test("BatchUpdateException should inherit from SQLException") {
    val exception = new BatchUpdateException(
      message      = "Batch error",
      updateCounts = List(1L, 2L)
    )

    assert(exception.isInstanceOf[SQLException], "BatchUpdateException should be a subclass of SQLException")
  }

  test("BatchUpdateException.fields should return correct attributes including updateCounts") {
    val updateCounts = List(1L, 0L, 1L, -3L)

    val exception = new BatchUpdateException(
      message      = "Batch insert failed",
      updateCounts = updateCounts,
      sqlState     = Some("23000"),
      vendorCode   = Some(1062),
      sql          = Some("INSERT INTO users VALUES (?, ?)"),
      detail       = Some("Duplicate entry"),
      hint         = Some("Remove duplicate records")
    )

    val fields = exception.fields

    assertEquals(fields.contains(Attribute("error.message", "Batch insert failed")), true)
    assertEquals(fields.contains(Attribute("error.updateCounts", "[1,0,1,-3]")), true)
    assertEquals(fields.contains(Attribute("error.sqlstate", "23000")), true)
    assertEquals(fields.contains(Attribute("error.vendorCode", 1062L)), true)
    assertEquals(fields.contains(Attribute("error.sql", "INSERT INTO users VALUES (?, ?)")), true)
    assertEquals(fields.contains(Attribute("error.detail", "Duplicate entry")), true)
    assertEquals(fields.contains(Attribute("error.hint", "Remove duplicate records")), true)
  }

  test("getMessage should include all relevant information") {
    val updateCounts = List(1L, 1L, -3L)

    val exception = new BatchUpdateException(
      message      = "Batch operation partially failed",
      updateCounts = updateCounts,
      sqlState     = Some("01S01"),
      vendorCode   = Some(1200),
      sql          = Some("INSERT INTO logs (message) VALUES (?)"),
      detail       = Some("Some records were not inserted"),
      hint         = Some("Check the error records")
    )

    val message = exception.getMessage

    // Check if message contains all relevant parts
    assert(message.contains("Batch operation partially failed"), "Message should contain the error description")
    assert(message.contains("01S01"), "Message should contain the SQL state")
    assert(message.contains("1200"), "Message should contain the vendor code")
    assert(message.contains("INSERT INTO logs"), "Message should contain the SQL query")
    assert(message.contains("Some records were not inserted"), "Message should contain the detail")
    assert(message.contains("Check the error records"), "Message should contain the hint")
  }

  test("BatchUpdateException with empty updateCounts should handle correctly") {
    val exception = new BatchUpdateException(
      message      = "Empty batch operation",
      updateCounts = List.empty,
      sqlState     = Some("01000")
    )

    val fields = exception.fields
    assertEquals(fields.contains(Attribute("error.updateCounts", "[]")), true)
  }
