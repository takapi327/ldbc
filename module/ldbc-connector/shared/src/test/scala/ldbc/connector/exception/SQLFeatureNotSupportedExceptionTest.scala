/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

import org.typelevel.otel4s.Attribute

import ldbc.connector.FTestPlatform

class SQLFeatureNotSupportedExceptionTest extends FTestPlatform:

  test("SQLFeatureNotSupportedException should have correct basic properties") {
    val errorMessage = "Feature not supported"
    val sqlState     = "0A000"
    val vendorCode   = 5432

    val exception = new SQLFeatureNotSupportedException(
      message    = errorMessage,
      sqlState   = Some(sqlState),
      vendorCode = Some(vendorCode)
    )

    assertEquals(exception.getMessage.contains(errorMessage), true)
    assertEquals(exception.getSQLState, sqlState)
    assertEquals(exception.getErrorCode, vendorCode)
  }

  test("SQLFeatureNotSupportedException should return default values when optional fields are not provided") {
    val exception = new SQLFeatureNotSupportedException("Simple not supported error")

    assertEquals(exception.getSQLState, "")
    assertEquals(exception.getErrorCode, 0)
  }

  test("SQLFeatureNotSupportedException.fields should return correct attributes") {
    val exception = new SQLFeatureNotSupportedException(
      message    = "Feature not supported error",
      sqlState   = Some("0A000"),
      vendorCode = Some(1234),
      sql        = Some("SELECT ST_AsGeoJSON(geom) FROM spatial_table"),
      detail     = Some("GeoJSON conversion not supported"),
      hint       = Some("Use a different format or upgrade driver")
    )

    val fields = exception.fields

    assertEquals(fields.contains(Attribute("error.message", "Feature not supported error")), true)
    assertEquals(fields.contains(Attribute("error.sqlstate", "0A000")), true)
    assertEquals(fields.contains(Attribute("error.vendorCode", 1234L)), true)
    assertEquals(fields.contains(Attribute("error.sql", "SELECT ST_AsGeoJSON(geom) FROM spatial_table")), true)
    assertEquals(fields.contains(Attribute("error.detail", "GeoJSON conversion not supported")), true)
    assertEquals(fields.contains(Attribute("error.hint", "Use a different format or upgrade driver")), true)
  }

  test("getMessage should include all relevant information") {
    val exception = new SQLFeatureNotSupportedException(
      message    = "Feature not available",
      sqlState   = Some("0A000"),
      vendorCode = Some(9876),
      sql        = Some("CALL sp_some_unsupported_procedure()"),
      detail     = Some("This procedure requires newer version"),
      hint       = Some("Upgrade to the latest version")
    )

    val message = exception.getMessage

    // Check if message contains all relevant parts
    assert(message.contains("Feature not available"), "Message should contain the error description")
    assert(message.contains("0A000"), "Message should contain the SQL state")
    assert(message.contains("9876"), "Message should contain the vendor code")
    assert(message.contains("CALL sp_some_unsupported_procedure()"), "Message should contain the SQL query")
    assert(message.contains("This procedure requires newer version"), "Message should contain the detail")
    assert(message.contains("Upgrade to the latest version"), "Message should contain the hint")
  }

  test("submitIssues should create exception with proper hint") {
    val errorMessage = "Batch operations not supported"
    val detail       = Some("This driver doesn't support batch operations")
    val exception    = SQLFeatureNotSupportedException.submitIssues(errorMessage, detail)

    assertEquals(exception.getMessage.contains(errorMessage), true)
    assertEquals(exception.getMessage.contains(detail.getOrElse("")), true)
  }
