/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

import java.time.LocalDate
import java.time.LocalDateTime
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
    assertEquals(
      date.decode(0, List(Some(""))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text '' could not be parsed at index 0",
          Type.date
        )
      )
    )
    assertEquals(
      date.decode(0, List(Some("invalid"))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text 'invalid' could not be parsed at index 0",
          Type.date
        )
      )
    )
    assertEquals(
      date.decode(0, List(Some("-1"))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text '-1' could not be parsed at index 0",
          Type.date
        )
      )
    )
    assertEquals(
      date.decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.date))
    )
  }

  test("datetime encode successfully") {
    assertEquals(
      datetime(0).encode(LocalDateTime.of(2024, 3, 7, 11, 55, 55)),
      List(Some(Encoded("2024-03-07 11:55:55", false)))
    )
    assertEquals(
      datetime(0).encode(LocalDateTime.of(1000, 1, 1, 0, 0, 0)),
      List(Some(Encoded("1000-01-01 00:00:00", false)))
    )
    assertEquals(
      datetime(0).encode(LocalDateTime.of(9999, 12, 31, 23, 59, 59)),
      List(Some(Encoded("9999-12-31 23:59:59", false)))
    )
    assertEquals(
      datetime(1).encode(LocalDateTime.of(2024, 3, 7, 11, 55, 55, 1)),
      List(Some(Encoded("2024-03-07 11:55:55.0", false)))
    )
    assertEquals(
      datetime(2).encode(LocalDateTime.of(2024, 3, 7, 11, 55, 55, 1)),
      List(Some(Encoded("2024-03-07 11:55:55.00", false)))
    )
    assertEquals(
      datetime(3).encode(LocalDateTime.of(2024, 3, 7, 11, 55, 55, 1)),
      List(Some(Encoded("2024-03-07 11:55:55.000", false)))
    )
    assertEquals(
      datetime(4).encode(LocalDateTime.of(2024, 3, 7, 11, 55, 55, 1)),
      List(Some(Encoded("2024-03-07 11:55:55.0000", false)))
    )
    assertEquals(
      datetime(5).encode(LocalDateTime.of(2024, 3, 7, 11, 55, 55, 1)),
      List(Some(Encoded("2024-03-07 11:55:55.00000", false)))
    )
    assertEquals(
      datetime(6).encode(LocalDateTime.of(2024, 3, 7, 11, 55, 55, 1)),
      List(Some(Encoded("2024-03-07 11:55:55.000000", false)))
    )
  }

  test("datetime decode successfully") {
    assertEquals(
      datetime(0).decode(0, List(Some("2024-03-07 11:55:55"))),
      Right(LocalDateTime.of(2024, 3, 7, 11, 55, 55))
    )
    assertEquals(datetime(0).decode(0, List(Some("1000-01-01 00:00:00"))), Right(LocalDateTime.of(1000, 1, 1, 0, 0, 0)))
    assertEquals(
      datetime(0).decode(0, List(Some("9999-12-31 23:59:59"))),
      Right(LocalDateTime.of(9999, 12, 31, 23, 59, 59))
    )
    assertEquals(
      datetime(1).decode(0, List(Some("2024-03-07 11:55:55.0"))),
      Right(LocalDateTime.of(2024, 3, 7, 11, 55, 55))
    )
    assertEquals(
      datetime(2).decode(0, List(Some("2024-03-07 11:55:55.00"))),
      Right(LocalDateTime.of(2024, 3, 7, 11, 55, 55))
    )
    assertEquals(
      datetime(3).decode(0, List(Some("2024-03-07 11:55:55.000"))),
      Right(LocalDateTime.of(2024, 3, 7, 11, 55, 55))
    )
    assertEquals(
      datetime(4).decode(0, List(Some("2024-03-07 11:55:55.0000"))),
      Right(LocalDateTime.of(2024, 3, 7, 11, 55, 55))
    )
    assertEquals(
      datetime(5).decode(0, List(Some("2024-03-07 11:55:55.10000"))),
      Right(LocalDateTime.of(2024, 3, 7, 11, 55, 55, 100000000))
    )
    assertEquals(
      datetime(6).decode(0, List(Some("2024-03-07 11:55:55.100000"))),
      Right(LocalDateTime.of(2024, 3, 7, 11, 55, 55, 100000000))
    )
  }

  test("datetime decode error") {
    assertEquals(
      datetime(0).decode(0, List(Some(""))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text '' could not be parsed at index 0",
          Type.datetime(0)
        )
      )
    )
    assertEquals(
      datetime(0).decode(0, List(Some("invalid"))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text 'invalid' could not be parsed at index 0",
          Type.datetime(0)
        )
      )
    )
    assertEquals(
      datetime(0).decode(0, List(Some("-1"))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text '-1' could not be parsed at index 0",
          Type.datetime(0)
        )
      )
    )
    assertEquals(
      datetime(0).decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.datetime(0)))
    )
  }

  test("timestamp encode successfully") {
    assertEquals(
      timestamp(0).encode(LocalDateTime.of(2024, 3, 7, 11, 55, 55)),
      List(Some(Encoded("2024-03-07 11:55:55", false)))
    )
    assertEquals(
      timestamp(0).encode(LocalDateTime.of(1000, 1, 1, 0, 0, 0)),
      List(Some(Encoded("1000-01-01 00:00:00", false)))
    )
    assertEquals(
      timestamp(0).encode(LocalDateTime.of(9999, 12, 31, 23, 59, 59)),
      List(Some(Encoded("9999-12-31 23:59:59", false)))
    )
    assertEquals(
      timestamp(1).encode(LocalDateTime.of(2024, 3, 7, 11, 55, 55, 1)),
      List(Some(Encoded("2024-03-07 11:55:55.0", false)))
    )
    assertEquals(
      timestamp(2).encode(LocalDateTime.of(2024, 3, 7, 11, 55, 55, 1)),
      List(Some(Encoded("2024-03-07 11:55:55.00", false)))
    )
    assertEquals(
      timestamp(3).encode(LocalDateTime.of(2024, 3, 7, 11, 55, 55, 1)),
      List(Some(Encoded("2024-03-07 11:55:55.000", false)))
    )
    assertEquals(
      timestamp(4).encode(LocalDateTime.of(2024, 3, 7, 11, 55, 55, 1)),
      List(Some(Encoded("2024-03-07 11:55:55.0000", false)))
    )
    assertEquals(
      timestamp(5).encode(LocalDateTime.of(2024, 3, 7, 11, 55, 55, 1)),
      List(Some(Encoded("2024-03-07 11:55:55.00000", false)))
    )
    assertEquals(
      timestamp(6).encode(LocalDateTime.of(2024, 3, 7, 11, 55, 55, 1)),
      List(Some(Encoded("2024-03-07 11:55:55.000000", false)))
    )
  }

  test("timestamp decode successfully") {
    assertEquals(
      timestamp(0).decode(0, List(Some("2024-03-07 11:55:55"))),
      Right(LocalDateTime.of(2024, 3, 7, 11, 55, 55))
    )
    assertEquals(datetime(0).decode(0, List(Some("1000-01-01 00:00:00"))), Right(LocalDateTime.of(1000, 1, 1, 0, 0, 0)))
    assertEquals(
      timestamp(0).decode(0, List(Some("9999-12-31 23:59:59"))),
      Right(LocalDateTime.of(9999, 12, 31, 23, 59, 59))
    )
    assertEquals(
      timestamp(1).decode(0, List(Some("2024-03-07 11:55:55.0"))),
      Right(LocalDateTime.of(2024, 3, 7, 11, 55, 55))
    )
    assertEquals(
      timestamp(2).decode(0, List(Some("2024-03-07 11:55:55.00"))),
      Right(LocalDateTime.of(2024, 3, 7, 11, 55, 55))
    )
    assertEquals(
      timestamp(3).decode(0, List(Some("2024-03-07 11:55:55.000"))),
      Right(LocalDateTime.of(2024, 3, 7, 11, 55, 55))
    )
    assertEquals(
      timestamp(4).decode(0, List(Some("2024-03-07 11:55:55.0000"))),
      Right(LocalDateTime.of(2024, 3, 7, 11, 55, 55))
    )
    assertEquals(
      timestamp(5).decode(0, List(Some("2024-03-07 11:55:55.10000"))),
      Right(LocalDateTime.of(2024, 3, 7, 11, 55, 55, 100000000))
    )
    assertEquals(
      timestamp(6).decode(0, List(Some("2024-03-07 11:55:55.100000"))),
      Right(LocalDateTime.of(2024, 3, 7, 11, 55, 55, 100000000))
    )
  }

  test("timestamp decode error") {
    assertEquals(
      timestamp(0).decode(0, List(Some(""))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text '' could not be parsed at index 0",
          Type.timestamp(0)
        )
      )
    )
    assertEquals(
      timestamp(0).decode(0, List(Some("invalid"))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text 'invalid' could not be parsed at index 0",
          Type.timestamp(0)
        )
      )
    )
    assertEquals(
      timestamp(0).decode(0, List(Some("-1"))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text '-1' could not be parsed at index 0",
          Type.timestamp(0)
        )
      )
    )
    assertEquals(
      timestamp(0).decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.timestamp(0)))
    )
  }

