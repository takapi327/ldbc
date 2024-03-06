/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

//import java.time.format.DateTimeFormatter
//import java.time.format.DateTimeParseException
import java.time.LocalDate
//import java.time.LocalDateTime
//import java.time.LocalTime
//import java.time.OffsetDateTime
//import java.time.OffsetTime
//import java.time.Year
//import java.time.temporal.ChronoField.*
//import java.time.temporal.TemporalAccessor
//import java.time.format.DateTimeFormatterBuilder
//import java.time.format.SignStyle
//import java.util.Locale

import munit.FunSuite

import ldbc.connector.data.*
import ldbc.connector.codec.temporal.*

class TemporalCodecsTest extends FunSuite:

  test("date encode successfully") {
    assertEquals(date.encode(LocalDate.of(2024, 3, 7)), List(Some(Encoded("2024-03-07", false))))
    assertEquals(date.encode(LocalDate.of(1000, 1, 1)), List(Some(Encoded("1000-01-01", false))))
    assertEquals(date.encode(LocalDate.of(9999, 12, 31)), List(Some(Encoded("9999-12-31", false))))
  }

  test("date decode successfully") {
    assertEquals(date.decode(0, List(Some("2024-03-07"))), Right(LocalDate.of(2024, 3, 7)))
    assertEquals(date.decode(0, List(Some("1000-01-01"))), Right(LocalDate.of(1000, 1, 1)))
    assertEquals(date.decode(0, List(Some("9999-12-31"))), Right(LocalDate.of(9999, 12, 31)))
  }

  test("date decode error") {
    assertEquals(date.decode(0, List(Some(""))), Left(Decoder.Error(0, 1, "java.time.format.DateTimeParseException: Text '' could not be parsed at index 0", Type.date)))
    assertEquals(date.decode(0, List(Some("invalid"))), Left(Decoder.Error(0, 1, "java.time.format.DateTimeParseException: Text 'invalid' could not be parsed at index 0", Type.date)))
    assertEquals(date.decode(0, List(Some("-1"))), Left(Decoder.Error(0, 1, "java.time.format.DateTimeParseException: Text '-1' could not be parsed at index 0", Type.date)))
    assertEquals(date.decode(0, List(None)), Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.date)))
  }
