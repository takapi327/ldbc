/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import java.time.*
import java.time.temporal.ChronoField

import ldbc.connector.*

class FormatterTest extends FTestPlatform:

  test("localDateFormatter should format dates correctly") {
    val date = LocalDate.of(2023, 5, 15)
    val formattedDate = date.format(Formatter.localDateFormatter)
    
    assertEquals(formattedDate, "2023-05-15")
  }

  test("localDateFormatterWithoutEra should format dates correctly") {
    val date = LocalDate.of(2023, 5, 15)
    val formattedDate = date.format(Formatter.localDateFormatterWithoutEra)
    
    assertEquals(formattedDate, "2023-05-15")
  }

  test("timeFormatter should format times correctly with different precisions") {
    val time = LocalTime.of(14, 30, 25, 123456789)
    
    // Precision 0: no fractional seconds
    val formatted0 = time.format(Formatter.timeFormatter(0))
    assertEquals(formatted0, "14:30:25")
    
    // Precision 3: milliseconds
    val formatted3 = time.format(Formatter.timeFormatter(3))
    assertEquals(formatted3, "14:30:25.123")
    
    // Precision 6: microseconds
    val formatted6 = time.format(Formatter.timeFormatter(6))
    assertEquals(formatted6, "14:30:25.123456")
    
    // Precision 9: nanoseconds
    val formatted9 = time.format(Formatter.timeFormatter(9))
    assertEquals(formatted9, "14:30:25.123456789")
  }

  test("localDateTimeFormatter should format datetime correctly with different precisions") {
    val dateTime = LocalDateTime.of(2023, 5, 15, 14, 30, 25, 123456789)
    
    // Precision 0: no fractional seconds
    val formatted0 = dateTime.format(Formatter.localDateTimeFormatter(0))
    assertEquals(formatted0, "2023-05-15 14:30:25")
    
    // Precision 3: milliseconds
    val formatted3 = dateTime.format(Formatter.localDateTimeFormatter(3))
    assertEquals(formatted3, "2023-05-15 14:30:25.123")
    
    // Precision 6: microseconds
    val formatted6 = dateTime.format(Formatter.localDateTimeFormatter(6))
    assertEquals(formatted6, "2023-05-15 14:30:25.123456")
  }

  test("offsetTimeFormatter should format offset time correctly") {
    val offsetTime = OffsetTime.of(14, 30, 25, 123456789, ZoneOffset.ofHours(9))
    
    // Precision 0: no fractional seconds
    val formatted0 = offsetTime.format(Formatter.offsetTimeFormatter(0))
    assertEquals(formatted0, "14:30:25+09")
    
    // Precision 3: milliseconds
    val formatted3 = offsetTime.format(Formatter.offsetTimeFormatter(3))
    assertEquals(formatted3, "14:30:25.123+09")
  }

  test("offsetDateTimeFormatter should format offset datetime correctly") {
    val dateTime = LocalDateTime.of(2023, 5, 15, 14, 30, 25, 123456789)
    val offsetDateTime = OffsetDateTime.of(dateTime, ZoneOffset.ofHours(9))
    
    // Precision 0: no fractional seconds
    val formatted0 = offsetDateTime.format(Formatter.offsetDateTimeFormatter(0))
    assertEquals(formatted0, "2023-05-15 14:30:25+09")
    
    // Precision 3: milliseconds
    val formatted3 = offsetDateTime.format(Formatter.offsetDateTimeFormatter(3))
    assertEquals(formatted3, "2023-05-15 14:30:25.123+09")
    
    // Precision 6: microseconds
    val formatted6 = offsetDateTime.format(Formatter.offsetDateTimeFormatter(6))
    assertEquals(formatted6, "2023-05-15 14:30:25.123456+09")
  }

  test("parsing with formatters should work correctly") {
    val dateStr = "2023-05-15"
    val parsedDate = LocalDate.parse(dateStr, Formatter.localDateFormatter)
    assertEquals(parsedDate.getYear, 2023)
    assertEquals(parsedDate.getMonthValue, 5)
    assertEquals(parsedDate.getDayOfMonth, 15)
    
    val timeStr = "14:30:25.123"
    val parsedTime = LocalTime.parse(timeStr, Formatter.timeFormatter(3))
    assertEquals(parsedTime.getHour, 14)
    assertEquals(parsedTime.getMinute, 30)
    assertEquals(parsedTime.getSecond, 25)
    assertEquals(parsedTime.get(ChronoField.MILLI_OF_SECOND), 123)
  }
