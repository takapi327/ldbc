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

class SQLTransactionRollbackExceptionTest extends FTestPlatform:

  test("SQLTransactionRollbackException should have correct basic properties") {
    val errorMessage = "Transaction was rolled back due to deadlock"
    val sqlState     = "40001"
    val vendorCode   = 1213

    val exception = new SQLTransactionRollbackException(
      message    = errorMessage,
      sqlState   = Some(sqlState),
      vendorCode = Some(vendorCode)
    )

    assertEquals(exception.getMessage.contains(errorMessage), true)
    assertEquals(exception.getSQLState, sqlState)
    assertEquals(exception.getErrorCode, vendorCode)
  }

  test("SQLTransactionRollbackException should return default values when optional fields are not provided") {
    val exception = new SQLTransactionRollbackException("Simple rollback error")

    assertEquals(exception.getSQLState, "")
    assertEquals(exception.getErrorCode, 0)
  }

  test("SQLTransactionRollbackException.fields should return correct attributes") {
    val params = SortedMap(
      1 -> Parameter.string("product_id"),
      2 -> Parameter.int(10)
    )

    val exception = new SQLTransactionRollbackException(
      message    = "Deadlock found when trying to get lock",
      sqlState   = Some("40001"),
      vendorCode = Some(1213),
      sql        = Some("UPDATE inventory SET quantity = quantity - ? WHERE product_id = ?"),
      detail     = Some("Transaction was rolled back automatically"),
      hint       = Some("Try restarting the transaction"),
      params     = params
    )

    val fields = exception.fields

    assertEquals(fields.contains(Attribute("error.message", "Deadlock found when trying to get lock")), true)
    assertEquals(fields.contains(Attribute("error.sqlstate", "40001")), true)
    assertEquals(fields.contains(Attribute("error.vendorCode", 1213L)), true)
    assertEquals(fields.contains(Attribute("error.sql", "UPDATE inventory SET quantity = quantity - ? WHERE product_id = ?")), true)
    assertEquals(fields.contains(Attribute("error.detail", "Transaction was rolled back automatically")), true)
    assertEquals(fields.contains(Attribute("error.hint", "Try restarting the transaction")), true)

    // Check parameter attributes
    assertEquals(fields.contains(Attribute("error.parameter.1.type", "CHAR")), true)
    assertEquals(fields.contains(Attribute("error.parameter.1.value", "'product_id'")), true)
    assertEquals(fields.contains(Attribute("error.parameter.2.type", "INT")), true)
    assertEquals(fields.contains(Attribute("error.parameter.2.value", "10")), true)
  }

  test("getMessage should include all relevant information") {
    val exception = new SQLTransactionRollbackException(
      message    = "Transaction rolled back",
      sqlState   = Some("40001"),
      vendorCode = Some(1213),
      sql        = Some("UPDATE accounts SET balance = balance - ? WHERE id = ?"),
      params     = SortedMap(
        1 -> Parameter.int(100),
        2 -> Parameter.int(42)
      )
    )

    val message = exception.getMessage

    // Check if message contains all relevant parts
    assert(message.contains("Transaction rolled back"), "Message should contain the error description")
    assert(message.contains("40001"), "Message should contain the SQL state")
    assert(message.contains("1213"), "Message should contain the vendor code")
    assert(message.contains("UPDATE accounts SET balance = balance - ? WHERE id = ?"), "Message should contain the SQL query")
    assert(message.contains("100"), "Message should contain the first parameter value")
    assert(message.contains("42"), "Message should contain the second parameter value")
  }

  test("SQLTransactionRollbackException should be a subclass of SQLTransientException") {
    val exception = new SQLTransactionRollbackException("Deadlock detected")
    assert(exception.isInstanceOf[SQLTransientException], "Should be an instance of SQLTransientException")
  }

