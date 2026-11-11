/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net.packet

import ldbc.sql.*

import ldbc.mysql.data.SQLState
import ldbc.mysql.net.packet.response.ERRPacket

class SQLStateMappingTest extends munit.FunSuite:

  private def exceptionFor(state: String): SQLException =
    ERRPacket(
      status         = ERRPacket.STATUS,
      errorCode      = 1234,
      sqlStateMarker = '#'.toInt,
      sqlState       = Some(state),
      errorMessage   = "boom"
    ).toException

  private def errorTypeFor(state: String): String =
    ERRPacket(
      status         = ERRPacket.STATUS,
      errorCode      = 1234,
      sqlStateMarker = '#'.toInt,
      sqlState       = Some(state),
      errorMessage   = "boom"
    ).attributes
      .collectFirst { case a if a.key == "error.type" => a.value.toString }
      .getOrElse(fail("no error.type attribute"))

  test("classOf takes the first two characters"):
    assertEquals(SQLState.classOf("08S01"), "08")
    assertEquals(SQLState.classOf("08000"), "08")
    assertEquals(SQLState.classOf("22"), "22")
    assertEquals(SQLState.classOf("0"), "0")
    assertEquals(SQLState.classOf(""), "")

  test("a connection state maps to the non-transient connection exception"):
    assert(exceptionFor("08000").isInstanceOf[SQLNonTransientConnectionException])
    assert(exceptionFor("08S01").isInstanceOf[SQLNonTransientConnectionException])
    assert(exceptionFor("08004").isInstanceOf[SQLNonTransientConnectionException])

  test("the subclass characters do not change the mapping"):
    assert(exceptionFor("22000").isInstanceOf[SQLDataException])
    assert(exceptionFor("22007").isInstanceOf[SQLDataException])
    assert(exceptionFor("23000").isInstanceOf[SQLIntegrityConstraintViolationException])
    assert(exceptionFor("23505").isInstanceOf[SQLIntegrityConstraintViolationException])
    assert(exceptionFor("28000").isInstanceOf[SQLInvalidAuthorizationSpecException])
    assert(exceptionFor("40001").isInstanceOf[SQLTransactionRollbackException])
    assert(exceptionFor("42000").isInstanceOf[SQLSyntaxErrorException])
    assert(exceptionFor("42S02").isInstanceOf[SQLSyntaxErrorException])
    assert(exceptionFor("0A000").isInstanceOf[SQLFeatureNotSupportedException])

  test("an unmapped class falls back to the base exception"):
    val fallback = exceptionFor("HY000")
    assertEquals(fallback.getClass.getSimpleName, "SQLException")

  test("the original state is preserved on the exception"):
    assertEquals(exceptionFor("08S01").getSQLState, "08S01")
    assertEquals(exceptionFor("42S02").getSQLState, "42S02")

  test("the telemetry attribute follows the same classification"):
    assertEquals(errorTypeFor("08S01"), "NonTransientConnectionException")
    assertEquals(errorTypeFor("22007"), "DataException")
    assertEquals(errorTypeFor("HY000"), "SQLException")
