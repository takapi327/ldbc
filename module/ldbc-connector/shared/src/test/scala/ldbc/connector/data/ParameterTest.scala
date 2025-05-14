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
    println(binaryParam.toString)
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
    assertEquals(floatParam.toString, "123.45")
  }

  test("BigInt parameter handling") {
    // Test BigInt parameter
    val bigInt      = BigInt("123456789012345678901234567890")
    val bigIntParam = Parameter.bigInt(bigInt)
    assertEquals(bigIntParam.toString, bigInt.toString)
  }
