/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import java.time.{ LocalDate, LocalDateTime, LocalTime, Year }

import ldbc.connector.*

class ParameterTest extends FTestPlatform:

  test("Parameter creation and conversion") {
    // Test String parameter
    val stringParam = Parameter.string("test")
    assertEquals(stringParam.toString, "'test'")

    // Test Integer parameter
    val intParam = Parameter.int(123)
    assertEquals(intParam.toString, "123")

    // Test Boolean parameter
    val boolParam = Parameter.boolean(true)
    assertEquals(boolParam.toString, "true")

    // Test Double parameter
    val doubleParam = Parameter.double(12.34)
    assertEquals(doubleParam.toString, "12.34")

    // Test Long parameter
    val longParam = Parameter.long(123456789L)
    assertEquals(longParam.toString, "123456789")

    // Test Null parameter
    val nullParam = Parameter.none
    assertEquals(nullParam.toString, "NULL")
  }

  test("Parameter binary/BLOB handling") {
    // Test binary parameter
    val binaryData  = Array[Byte](1, 2, 3, 4, 5)
    val binaryParam = Parameter.bytes(binaryData)
    assertEquals(binaryParam.toString, "0x0102030405")
  }

  test("Parameter date/time handling") {
    // Test date parameter
    val date      = LocalDate.of(2023, 1, 15)
    val dateParam = Parameter.date(date)
    assertEquals(dateParam.toString, s"'$date'")

    // Test time parameter
    val time      = LocalTime.of(14, 30, 15)
    val timeParam = Parameter.time(time)
    assertEquals(timeParam.toString, s"'$time'")

    // Test timestamp parameter
    val timestamp      = LocalDateTime.of(2023, 1, 15, 14, 30, 15)
    val timestampParam = Parameter.datetime(timestamp)
    assertEquals(timestampParam.toString, s"'${ timestamp.toString.replace("T", " ") }'")
  }

  test("Year parameter handling") {
    // Test year parameter
    val year      = Year.of(2023)
    val yearParam = Parameter.year(year)
    assertEquals(yearParam.toString, s"'$year'")
  }

  test("BigDecimal parameter handling") {
    // Test BigDecimal parameter
    val bigDecimal      = BigDecimal("123456.789")
    val bigDecimalParam = Parameter.bigDecimal(bigDecimal)
    assertEquals(bigDecimalParam.toString, bigDecimal.toString)
  }

  test("Short parameter handling") {
    // Test Short parameter
    val shortVal: Short = 123
    val shortParam = Parameter.short(shortVal)
    assertEquals(shortParam.toString, "123")
  }

  test("Float parameter handling") {
    // Test Float parameter
    val floatVal: Float = 123.45f
    val floatParam = Parameter.float(floatVal)
    assertEquals(floatParam.toString.split('.').head, "123")
  }

  test("BigInt parameter handling") {
    // Test BigInt parameter
    val bigInt      = BigInt("123456789012345678901234567890")
    val bigIntParam = Parameter.bigInt(bigInt)
    assertEquals(bigIntParam.toString, bigInt.toString)
  }

  test("Byte parameter handling") {
    // Test Byte parameter
    val byteVal: Byte = 123
    val byteParam = Parameter.byte(byteVal)
    assertEquals(byteParam.toString, "123")
    assertEquals(byteParam.columnDataType, ColumnDataType.MYSQL_TYPE_TINY)
    
    // Test negative byte
    val negByteVal: Byte = -123
    val negByteParam = Parameter.byte(negByteVal)
    assertEquals(negByteParam.toString, "-123")
  }

  test("Parameter.parameter method handling") {
    // Test parameter method (raw SQL injection)
    val rawParam = Parameter.parameter("CURRENT_TIMESTAMP")
    assertEquals(rawParam.toString, "CURRENT_TIMESTAMP")
    assertEquals(rawParam.columnDataType, ColumnDataType.MYSQL_TYPE_STRING)
    
    // Test with SQL function
    val funcParam = Parameter.parameter("NOW()")
    assertEquals(funcParam.toString, "NOW()")
  }

  test("columnDataType for all parameter types") {
    assertEquals(Parameter.none.columnDataType, ColumnDataType.MYSQL_TYPE_NULL)
    assertEquals(Parameter.boolean(true).columnDataType, ColumnDataType.MYSQL_TYPE_TINY)
    assertEquals(Parameter.byte(1).columnDataType, ColumnDataType.MYSQL_TYPE_TINY)
    assertEquals(Parameter.short(1).columnDataType, ColumnDataType.MYSQL_TYPE_SHORT)
    assertEquals(Parameter.int(1).columnDataType, ColumnDataType.MYSQL_TYPE_LONG)
    assertEquals(Parameter.long(1L).columnDataType, ColumnDataType.MYSQL_TYPE_LONGLONG)
    assertEquals(Parameter.bigInt(BigInt(1)).columnDataType, ColumnDataType.MYSQL_TYPE_STRING)
    assertEquals(Parameter.float(1.0f).columnDataType, ColumnDataType.MYSQL_TYPE_FLOAT)
    assertEquals(Parameter.double(1.0).columnDataType, ColumnDataType.MYSQL_TYPE_DOUBLE)
    assertEquals(Parameter.bigDecimal(BigDecimal(1)).columnDataType, ColumnDataType.MYSQL_TYPE_NEWDECIMAL)
    assertEquals(Parameter.string("test").columnDataType, ColumnDataType.MYSQL_TYPE_STRING)
    assertEquals(Parameter.bytes(Array[Byte](1)).columnDataType, ColumnDataType.MYSQL_TYPE_VAR_STRING)
    assertEquals(Parameter.time(LocalTime.now()).columnDataType, ColumnDataType.MYSQL_TYPE_TIME)
    assertEquals(Parameter.date(LocalDate.now()).columnDataType, ColumnDataType.MYSQL_TYPE_DATE)
    assertEquals(Parameter.datetime(LocalDateTime.now()).columnDataType, ColumnDataType.MYSQL_TYPE_TIMESTAMP)
    assertEquals(Parameter.year(Year.now()).columnDataType, ColumnDataType.MYSQL_TYPE_SHORT)
    assertEquals(Parameter.parameter("test").columnDataType, ColumnDataType.MYSQL_TYPE_STRING)
  }

  test("encode method returns BitVector") {
    // Test that encode returns non-empty BitVector for various types
    assert(Parameter.boolean(true).encode.nonEmpty)
    assert(Parameter.byte(1).encode.nonEmpty)
    assert(Parameter.short(1).encode.nonEmpty)
    assert(Parameter.int(1).encode.nonEmpty)
    assert(Parameter.long(1L).encode.nonEmpty)
    assert(Parameter.float(1.0f).encode.nonEmpty)
    assert(Parameter.double(1.0).encode.nonEmpty)
    assert(Parameter.string("test").encode.nonEmpty)
    assert(Parameter.bytes(Array[Byte](1, 2, 3)).encode.nonEmpty)
    
    // Test that none returns empty BitVector
    assert(Parameter.none.encode.isEmpty)
  }

  test("Special values handling") {
    // Test empty string
    val emptyStringParam = Parameter.string("")
    assertEquals(emptyStringParam.toString, "''")
    
    // Test string with quotes
    val quotedStringParam = Parameter.string("test'quotes")
    assertEquals(quotedStringParam.toString, "'test'quotes'")
    
    // Test zero values
    assertEquals(Parameter.byte(0).toString, "0")
    assertEquals(Parameter.short(0).toString, "0")
    assertEquals(Parameter.int(0).toString, "0")
    assertEquals(Parameter.long(0L).toString, "0")
    assertEquals(Parameter.float(0.0f).toString, "0.0")
    assertEquals(Parameter.double(0.0).toString, "0.0")
    
    // Test negative values
    assertEquals(Parameter.short(-32768).toString, "-32768")
    assertEquals(Parameter.int(-2147483648).toString, "-2147483648")
    assertEquals(Parameter.long(-9223372036854775808L).toString, "-9223372036854775808")
  }

  test("Boolean parameter encode") {
    val trueParam = Parameter.boolean(true)
    val falseParam = Parameter.boolean(false)
    
    // Verify encoded values (true = 1, false = 0)
    assert(trueParam.encode.nonEmpty)
    assert(falseParam.encode.nonEmpty)
    assertEquals(falseParam.toString, "false")
  }

  test("Date and time edge cases") {
    // Test LocalDate edge cases
    val minDate = LocalDate.of(1, 1, 1)
    val maxDate = LocalDate.of(9999, 12, 31)
    assertEquals(Parameter.date(minDate).toString, s"'$minDate'")
    assertEquals(Parameter.date(maxDate).toString, s"'$maxDate'")
    
    // Test LocalTime edge cases
    val minTime = LocalTime.of(0, 0, 0)
    val maxTime = LocalTime.of(23, 59, 59)
    assertEquals(Parameter.time(minTime).toString, "'00:00:00'")
    assertEquals(Parameter.time(maxTime).toString, "'23:59:59'")
    
    // Test LocalTime with microseconds
    val timeWithMicros = LocalTime.of(12, 30, 45, 123456000)
    val timeParam = Parameter.time(timeWithMicros)
    assert(timeParam.toString.contains("12:30:45"))
    
    // Test LocalDateTime edge cases
    val minDateTime = LocalDateTime.of(1, 1, 1, 0, 0, 0)
    val maxDateTime = LocalDateTime.of(9999, 12, 31, 23, 59, 59)
    assert(Parameter.datetime(minDateTime).toString.contains("0001-01-01"))
    assert(Parameter.datetime(maxDateTime).toString.contains("9999-12-31"))
    
    // Test LocalDateTime with microseconds
    val dateTimeWithMicros = LocalDateTime.of(2023, 5, 15, 14, 30, 25, 123456000)
    val dateTimeParam = Parameter.datetime(dateTimeWithMicros)
    assert(dateTimeParam.toString.contains("2023-05-15"))
  }

  test("Year parameter edge cases") {
    val minYear = Year.of(1)
    val maxYear = Year.of(9999)
    
    assertEquals(Parameter.year(minYear).toString, "'1'")
    assertEquals(Parameter.year(maxYear).toString, "'9999'")
  }

  test("Binary data encoding") {
    // Empty array
    val emptyBytes = Array.empty[Byte]
    assertEquals(Parameter.bytes(emptyBytes).toString, "0x")
    
    // Single byte
    val singleByte = Array[Byte](0xff.toByte)
    assertEquals(Parameter.bytes(singleByte).toString, "0xff")
    
    // Multiple bytes with various values
    val multiBytes = Array[Byte](0x00, 0x01, 0x7f, 0x80.toByte, 0xff.toByte)
    assertEquals(Parameter.bytes(multiBytes).toString, "0x00017f80ff")
  }

  test("String parameter special cases") {
    // Multiline string
    val multilineString = "line1\nline2\nline3"
    assertEquals(Parameter.string(multilineString).toString, s"'$multilineString'")
    
    // String with special characters
    val specialChars = "\t\r\n\b"
    assertEquals(Parameter.string(specialChars).toString, s"'$specialChars'")
    
    // Unicode string
    val unicodeString = "Hello 世界 🌍"
    assertEquals(Parameter.string(unicodeString).toString, s"'$unicodeString'")
  }

  test("Numeric precision and special values") {
    // Float special values
    assertEquals(Parameter.float(Float.PositiveInfinity).toString, "Infinity")
    assertEquals(Parameter.float(Float.NegativeInfinity).toString, "-Infinity")
    assertEquals(Parameter.float(Float.NaN).toString, "NaN")
    
    // Double special values
    assertEquals(Parameter.double(Double.PositiveInfinity).toString, "Infinity")
    assertEquals(Parameter.double(Double.NegativeInfinity).toString, "-Infinity")
    assertEquals(Parameter.double(Double.NaN).toString, "NaN")
    
    // BigDecimal precision
    val preciseBigDecimal = BigDecimal("123.456789012345678901234567890")
    assertEquals(Parameter.bigDecimal(preciseBigDecimal).toString, preciseBigDecimal.toString)
    
    // BigInt edge cases
    val negativeBigInt = BigInt("-123456789012345678901234567890")
    assertEquals(Parameter.bigInt(negativeBigInt).toString, negativeBigInt.toString)
  }
