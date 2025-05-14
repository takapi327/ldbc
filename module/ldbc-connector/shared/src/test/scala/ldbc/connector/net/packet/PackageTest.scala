/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import java.time.*

import scodec.bits.BitVector

import ldbc.connector.*
import ldbc.connector.data.ColumnDataType

class PackageTest extends FTestPlatform:

  test("nullTerminatedStringCodec should encode and decode correctly") {
    val testString = "Hello World"
    
    // Encode the string
    val encoded = nullTerminatedStringCodec.encode(testString)
    assert(encoded.isSuccessful)
    
    // Decode and verify
    val decoded = nullTerminatedStringCodec.decode(encoded.require)
    assert(decoded.isSuccessful)
    
    assertEquals(decoded.require.value, testString)
  }
  
  test("nullTerminatedStringCodec should handle empty string") {
    val emptyString = ""
    
    val encoded = nullTerminatedStringCodec.encode(emptyString)
    assert(encoded.isSuccessful)
    
    val decoded = nullTerminatedStringCodec.decode(encoded.require)
    assert(decoded.isSuccessful)
    
    assertEquals(decoded.require.value, emptyString)
  }
  
  test("lengthEncodedIntDecoder should decode small integers (<=251)") {
    val smallInt = 42
    val bitVector = BitVector(Array[Byte](smallInt.toByte))
    
    val result = lengthEncodedIntDecoder.decode(bitVector)
    assert(result.isSuccessful)
    assertEquals(result.require.value, smallInt.toLong)
  }
  
  test("lengthEncodedIntDecoder should decode 2-byte integers (252-65535)") {
    val value = 500
    val bitVector = BitVector(Array[Byte](
      0xFC.toByte, // 252 indicates 2-byte integer
      (value & 0xFF).toByte, 
      ((value >> 8) & 0xFF).toByte
    ))
    
    val result = lengthEncodedIntDecoder.decode(bitVector)
    assert(result.isSuccessful)
    assertEquals(result.require.value, value.toLong)
  }
  
  test("lengthEncodedIntDecoder should decode 3-byte integers") {
    val value = 100000
    val bitVector = BitVector(Array[Byte](
      0xFD.toByte, // 253 indicates 3-byte integer
      (value & 0xFF).toByte,
      ((value >> 8) & 0xFF).toByte,
      ((value >> 16) & 0xFF).toByte
    ))
    
    val result = lengthEncodedIntDecoder.decode(bitVector)
    assert(result.isSuccessful)
    assertEquals(result.require.value, value.toLong)
  }
  
  test("lengthEncodedIntDecoder should decode 4-byte integers") {
    val value = 16777216 // 2^24
    val bitVector = BitVector(Array[Byte](
      0xFE.toByte, // 254 indicates 4-byte integer
      (value & 0xFF).toByte,
      ((value >> 8) & 0xFF).toByte,
      ((value >> 16) & 0xFF).toByte,
      ((value >> 24) & 0xFF).toByte
    ))
    
    val result = lengthEncodedIntDecoder.decode(bitVector)
    assert(result.isSuccessful)
    assertEquals(result.require.value, value.toLong)
  }
  
  test("spaceDelimitedStringDecoder should decode until space") {
    val testString = "TEST"
    val bitVector = BitVector((testString + " remaining").getBytes)
    
    val result = spaceDelimitedStringDecoder.decode(bitVector)
    assert(result.isSuccessful)
    assertEquals(result.require.value, testString)
    
    // Verify remaining bits are preserved
    val remainingString = new String(result.require.remainder.toByteArray).trim
    assertEquals(remainingString, "remaining")
  }
  
  test("nullBitmap should create proper bitmap for mixed column types") {
    val columns = List(
      ColumnDataType.MYSQL_TYPE_NULL,
      ColumnDataType.MYSQL_TYPE_LONG,
      ColumnDataType.MYSQL_TYPE_NULL,
      ColumnDataType.MYSQL_TYPE_VARCHAR
    )
    
    val bitmap = nullBitmap(columns)
    
    assertEquals(bitmap.bytes.toArray(0).toInt & 0xFF, 5)
  }
  
  test("time decoder should handle different time formats") {
    // Test for 8-byte time format (zero microseconds)
    val time8bytes = BitVector(Array[Byte](
      8, // Length of time value
      0, // Is negative (0 = positive)
      0, 0, 0, 0, // Days
      10, // Hour
      30, // Minute
      45  // Second
    ))
    
    val time8result = time.decode(time8bytes)
    assert(time8result.isSuccessful)
    time8result.require.value match {
      case Some(t) => 
        assertEquals(t.getHour, 10)
        assertEquals(t.getMinute, 30)
        assertEquals(t.getSecond, 45)
      case None => fail("Expected Some(LocalTime) but got None")
    }
    
    // Test for 12-byte time format (with microseconds)
    val time12bytes = BitVector(Array[Byte](
      12, // Length of time value
      0,  // Is negative (0 = positive)
      0, 0, 0, 0, // Days
      15, // Hour
      20, // Minute
      30, // Second
      40, 0, 0, 0  // Microseconds (40)
    ))
    
    val time12result = time.decode(time12bytes)
    assert(time12result.isSuccessful)
    time12result.require.value match {
      case Some(t) => 
        assertEquals(t.getHour, 15)
        assertEquals(t.getMinute, 20)
        assertEquals(t.getSecond, 30)
        assertEquals(t.getNano, 40000)
      case None => fail("Expected Some(LocalTime) but got None")
    }
    
    // Test for zero length (NULL value)
    val timeNullBytes = BitVector(Array[Byte](0))
    val timeNullResult = time.decode(timeNullBytes)
    assert(timeNullResult.isSuccessful)
    assertEquals(timeNullResult.require.value, None)
  }
  
  test("timestamp decoder should handle different timestamp formats") {
    // Test for 4-byte timestamp (date only)
    val ts4bytes = BitVector(Array[Byte](
      4,  // Length of timestamp value
      (2023 & 0xFF).toByte, ((2023 >> 8) & 0xFF).toByte, // Year
      12, // Month
      25  // Day
    ))
    
    val ts4Result = timestamp.decode(ts4bytes)
    assert(ts4Result.isSuccessful)
    ts4Result.require.value match {
      case Some(dt) =>
        assertEquals(dt.getYear, 2023)
        assertEquals(dt.getMonthValue, 12)
        assertEquals(dt.getDayOfMonth, 25)
        assertEquals(dt.getHour, 0)
      case None => fail("Expected Some(LocalDateTime) but got None")
    }
    
    // Test for 7-byte timestamp (date and time without microseconds)
    val ts7bytes = BitVector(Array[Byte](
      7,  // Length of timestamp value
      (2023 & 0xFF).toByte, ((2023 >> 8) & 0xFF).toByte, // Year
      12, // Month
      25, // Day
      14, // Hour
      30, // Minute
      45  // Second
    ))
    
    val ts7Result = timestamp.decode(ts7bytes)
    assert(ts7Result.isSuccessful)
    ts7Result.require.value match {
      case Some(dt) =>
        assertEquals(dt.getYear, 2023)
        assertEquals(dt.getMonthValue, 12)
        assertEquals(dt.getDayOfMonth, 25)
        assertEquals(dt.getHour, 14)
        assertEquals(dt.getMinute, 30)
        assertEquals(dt.getSecond, 45)
        assertEquals(dt.getNano, 0)
      case None => fail("Expected Some(LocalDateTime) but got None")
    }
    
    // Test for 11-byte timestamp (date and time with microseconds)
    val ts11bytes = BitVector(Array[Byte](
      11, // Length of timestamp value
      (2023 & 0xFF).toByte, ((2023 >> 8) & 0xFF).toByte, // Year
      12, // Month
      25, // Day
      14, // Hour
      30, // Minute
      45, // Second
      100, 0, 0, 0  // Microseconds
    ))
    
    val ts11Result = timestamp.decode(ts11bytes)
    assert(ts11Result.isSuccessful)
    ts11Result.require.value match {
      case Some(dt) =>
        assertEquals(dt.getYear, 2023)
        assertEquals(dt.getMonthValue, 12)
        assertEquals(dt.getDayOfMonth, 25)
        assertEquals(dt.getHour, 14)
        assertEquals(dt.getMinute, 30)
        assertEquals(dt.getSecond, 45)
        assertEquals(dt.getNano, 100000)
      case None => fail("Expected Some(LocalDateTime) but got None")
    }
    
    // Test for zero length (NULL value)
    val tsNullBytes = BitVector(Array[Byte](0))
    val tsNullResult = timestamp.decode(tsNullBytes)
    assert(tsNullResult.isSuccessful)
    assertEquals(tsNullResult.require.value, None)
  }
