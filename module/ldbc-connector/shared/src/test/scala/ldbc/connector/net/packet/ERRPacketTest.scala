/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scodec.bits.BitVector
import scodec.Attempt

import ldbc.connector.*
import ldbc.connector.data.*
import ldbc.connector.exception.*
import ldbc.connector.net.packet.response.ERRPacket

class ERRPacketTest extends FTestPlatform:

  test("ERRPacket creation and properties") {
    val errPacket = ERRPacket(
      status         = ERRPacket.STATUS,
      errorCode      = 1045,
      sqlStateMarker = '#'.toInt,
      sqlState       = Some("28000"),
      errorMessage   = "Access denied for user"
    )

    assertEquals(errPacket.status, 0xff)
    assertEquals(errPacket.errorCode, 1045)
    assertEquals(errPacket.sqlStateMarker, '#'.toInt) // Charをintに変換
    assertEquals(errPacket.sqlState, Some("28000"))
    assertEquals(errPacket.errorMessage, "Access denied for user")
    assertEquals(errPacket.toString, "ERR_Packet")
  }

  test("ERRPacket decoder with CLIENT_PROTOCOL_41") {
    // Create sample packet data that would be received from server
    val packetBytes = Array[Byte](
      0x15, 0x04,                   // error code (1045)
      '#',                          // SQL state marker
      '2', '8', '0', '0', '0',      // SQL state
      'A', 'c', 'c', 'e', 's', 's', // error message
      ' ', 'd', 'e', 'n', 'i', 'e', 'd'
    )

    val bitVector       = BitVector(packetBytes)
    val capabilityFlags = Set(CapabilitiesFlags.CLIENT_PROTOCOL_41)

    val result = ERRPacket.decoder(capabilityFlags).decode(bitVector)

    assert(result.isSuccessful)
    result match {
      case Attempt.Successful(decoded) =>
        val errPacket = decoded.value
        assertEquals(errPacket.status, 0xff)
        assertEquals(errPacket.errorCode, 1045)
        assertEquals(errPacket.sqlStateMarker, '#'.toInt)
        assertEquals(errPacket.sqlState, Some("28000"))
        assertEquals(errPacket.errorMessage, "Access denied")
      case _ => fail("Decoding failed")
    }
  }

  test("ERRPacket decoder without CLIENT_PROTOCOL_41") {
    // Create sample packet without SQL state information
    val packetBytes = Array[Byte](
      0x15, 0x04,                   // error code (1045)
      'A', 'c', 'c', 'e', 's', 's', // error message
      ' ', 'd', 'e', 'n', 'i', 'e', 'd'
    )

    val bitVector       = BitVector(packetBytes)
    val capabilityFlags = Set.empty[CapabilitiesFlags]

    val result = ERRPacket.decoder(capabilityFlags).decode(bitVector)

    assert(result.isSuccessful)
    result match {
      case Attempt.Successful(decoded) =>
        val errPacket = decoded.value
        assertEquals(errPacket.status, 0xff)
        assertEquals(errPacket.errorCode, 1045)
        assertEquals(errPacket.sqlStateMarker, 0)
        assertEquals(errPacket.sqlState, None)
        assertEquals(errPacket.errorMessage, "Access denied")
      case _ => fail("Decoding failed")
    }
  }

  test("Convert ERRPacket to appropriate exception") {
    // Test basic SQLException conversion
    val basicErrPacket = ERRPacket(
      status         = ERRPacket.STATUS,
      errorCode      = 1000,
      sqlStateMarker = '#'.toInt,
      sqlState       = Some("42000"),
      errorMessage   = "Generic SQL error"
    )

    val basicException = basicErrPacket.toException
    assert(basicException.isInstanceOf[SQLException])
    assert(basicException.getMessage.contains("Generic SQL error"))
    assertEquals(basicException.getSQLState, "42000")
    assertEquals(basicException.getErrorCode, 1000)

    // Test authorization exception
    val authErrPacket = ERRPacket(
      status         = ERRPacket.STATUS,
      errorCode      = 1045,
      sqlStateMarker = '#'.toInt,
      sqlState       = Some("28000"),
      errorMessage   = "Access denied for user"
    )

    val authException = authErrPacket.toException
    assert(authException.isInstanceOf[SQLInvalidAuthorizationSpecException])
    assertEquals(authException.getSQLState, "28000")

    // Test SQLTransientConnectionException
    val connectionErrPacket = ERRPacket(
      status         = ERRPacket.STATUS,
      errorCode      = 2003,
      sqlStateMarker = '#'.toInt,
      sqlState       = Some("08S01"),
      errorMessage   = "Connection refused"
    )

    val connectionException = connectionErrPacket.toException
    assertEquals(connectionException.getSQLState, "08S01")

    // Test SQLDataException
    val dataErrPacket = ERRPacket(
      status         = ERRPacket.STATUS,
      errorCode      = 1264,
      sqlStateMarker = '#'.toInt,
      sqlState       = Some("22000"),
      errorMessage   = "Data truncation error"
    )

    val dataException = dataErrPacket.toException
    assert(dataException.isInstanceOf[SQLDataException])
    assertEquals(dataException.getSQLState, "22000")
    assertEquals(dataException.getErrorCode, 1264)

    // Test SQLIntegrityConstraintViolationException
    val integrityErrPacket = ERRPacket(
      status         = ERRPacket.STATUS,
      errorCode      = 1062,
      sqlStateMarker = '#'.toInt,
      sqlState       = Some("23000"),
      errorMessage   = "Duplicate entry for key 'PRIMARY'"
    )

    val integrityException = integrityErrPacket.toException
    assert(integrityException.isInstanceOf[SQLIntegrityConstraintViolationException])
    assertEquals(integrityException.getSQLState, "23000")
    assertEquals(integrityException.getErrorCode, 1062)

    // Test SQLTransactionRollbackException
    val rollbackErrPacket = ERRPacket(
      status         = ERRPacket.STATUS,
      errorCode      = 1205,
      sqlStateMarker = '#'.toInt,
      sqlState       = Some("40000"),
      errorMessage   = "Lock wait timeout exceeded; try restarting transaction"
    )

    val rollbackException = rollbackErrPacket.toException
    assert(rollbackException.isInstanceOf[SQLTransactionRollbackException])
    assertEquals(rollbackException.getSQLState, "40000")
    assertEquals(rollbackException.getErrorCode, 1205)

    // Test SQLFeatureNotSupportedException
    val featureErrPacket = ERRPacket(
      status         = ERRPacket.STATUS,
      errorCode      = 1289,
      sqlStateMarker = '#'.toInt,
      sqlState       = Some("0A000"),
      errorMessage   = "The 'OPTION' feature is not supported"
    )

    val featureException = featureErrPacket.toException
    assert(featureException.isInstanceOf[SQLFeatureNotSupportedException])
    assertEquals(featureException.getSQLState, "0A000")
    assertEquals(featureException.getErrorCode, 1289)

    // Test generic SQLException with no specific SQL state
    val noStateErrPacket = ERRPacket(
      status         = ERRPacket.STATUS,
      errorCode      = 1234,
      sqlStateMarker = 0,
      sqlState       = None,
      errorMessage   = "Unknown error occurred"
    )

    val noStateException = noStateErrPacket.toException
    assert(noStateException.isInstanceOf[SQLException])
    assertEquals(noStateException.getErrorCode, 1234)

    // Test with SQL query
    val sqlSyntaxErrPacket = ERRPacket(
      status         = ERRPacket.STATUS,
      errorCode      = 1054,
      sqlStateMarker = '#'.toInt,
      sqlState       = Some("42000"),
      errorMessage   = "Unknown column 'invalid_column'"
    )

    val sql          = "SELECT invalid_column FROM table"
    val detail       = "Syntax error"
    val sqlException = sqlSyntaxErrPacket.toException(detail, sql)
    assert(sqlException.isInstanceOf[SQLException])

    // メッセージに含まれているかを確認する方法でテスト
    val exceptionMessage = sqlException.getMessage
    assert(exceptionMessage.contains("invalid_column"), s"Exception message should contain SQL: $exceptionMessage")
    assert(exceptionMessage.contains("Syntax error"), s"Exception message should contain detail: $exceptionMessage")

    // Test batch update exception
    val batchErrPacket = ERRPacket(
      status         = ERRPacket.STATUS,
      errorCode      = 1062,
      sqlStateMarker = '#'.toInt,
      sqlState       = Some("23000"),
      errorMessage   = "Duplicate entry"
    )

    // 型を明示的に指定して変換する
    val longValues: Vector[Long] = Vector(1L, 2L, -3L)
    val batchException = batchErrPacket.toException("Batch failed", longValues)
    assert(batchException.isInstanceOf[BatchUpdateException])

    // BatchUpdateExceptionのメッセージにUpdateCountsの情報が含まれているかを確認
    val batchExMessage = batchException.getMessage
    assert(
      batchExMessage.contains("Duplicate entry"),
      s"Batch exception message should contain error message: $batchExMessage"
    )
    assert(batchExMessage.contains("Batch failed"), s"Batch exception message should contain detail: $batchExMessage")

    // fieldsメソッドを通じて間接的にupdateCountsをテスト
    val attributesList        = batchException.fields
    val updateCountsAttribute = attributesList.find(_.key.name == "error.updateCounts")
    assert(updateCountsAttribute.isDefined, "Should have updateCounts attribute")
    assertEquals(updateCountsAttribute.get.value.toString, "[1,2,-3]")
  }
