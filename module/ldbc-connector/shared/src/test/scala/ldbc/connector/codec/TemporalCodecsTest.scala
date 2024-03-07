/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

import java.time.*

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
    assertEquals(
      timestamp(0).decode(0, List(Some("1000-01-01 00:00:00"))),
      Right(LocalDateTime.of(1000, 1, 1, 0, 0, 0))
    )
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

  test("timestamptz encode successfully") {
    assertEquals(
      timestamptz(0).encode(OffsetDateTime.of(2024, 3, 7, 11, 55, 55, 0, ZoneOffset.UTC)),
      List(Some(Encoded("2024-03-07 11:55:55Z", false)))
    )
    assertEquals(
      timestamptz(0).encode(OffsetDateTime.of(1000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)),
      List(Some(Encoded("1000-01-01 00:00:00Z", false)))
    )
    assertEquals(
      timestamptz(0).encode(OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)),
      List(Some(Encoded("9999-12-31 23:59:59Z", false)))
    )
    assertEquals(
      timestamptz(1).encode(OffsetDateTime.of(2024, 3, 7, 11, 55, 55, 1, ZoneOffset.UTC)),
      List(Some(Encoded("2024-03-07 11:55:55.0Z", false)))
    )
    assertEquals(
      timestamptz(2).encode(OffsetDateTime.of(2024, 3, 7, 11, 55, 55, 1, ZoneOffset.UTC)),
      List(Some(Encoded("2024-03-07 11:55:55.00Z", false)))
    )
    assertEquals(
      timestamptz(3).encode(OffsetDateTime.of(2024, 3, 7, 11, 55, 55, 1, ZoneOffset.UTC)),
      List(Some(Encoded("2024-03-07 11:55:55.000Z", false)))
    )
    assertEquals(
      timestamptz(4).encode(OffsetDateTime.of(2024, 3, 7, 11, 55, 55, 1, ZoneOffset.UTC)),
      List(Some(Encoded("2024-03-07 11:55:55.0000Z", false)))
    )
    assertEquals(
      timestamptz(5).encode(OffsetDateTime.of(2024, 3, 7, 11, 55, 55, 1, ZoneOffset.UTC)),
      List(Some(Encoded("2024-03-07 11:55:55.00000Z", false)))
    )
    assertEquals(
      timestamptz(6).encode(OffsetDateTime.of(2024, 3, 7, 11, 55, 55, 1, ZoneOffset.UTC)),
      List(Some(Encoded("2024-03-07 11:55:55.000000Z", false)))
    )
  }

  test("timestamptz decode successfully") {
    assertEquals(
      timestamptz(0).decode(0, List(Some("2024-03-07 11:55:55Z"))),
      Right(OffsetDateTime.of(2024, 3, 7, 11, 55, 55, 0, ZoneOffset.UTC))
    )
    assertEquals(
      timestamptz(0).decode(0, List(Some("1000-01-01 00:00:00Z"))),
      Right(OffsetDateTime.of(1000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
    )
    assertEquals(
      timestamptz(0).decode(0, List(Some("9999-12-31 23:59:59Z"))),
      Right(OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC))
    )
    assertEquals(
      timestamptz(1).decode(0, List(Some("2024-03-07 11:55:55.0Z"))),
      Right(OffsetDateTime.of(2024, 3, 7, 11, 55, 55, 0, ZoneOffset.UTC))
    )
    assertEquals(
      timestamptz(2).decode(0, List(Some("2024-03-07 11:55:55.00Z"))),
      Right(OffsetDateTime.of(2024, 3, 7, 11, 55, 55, 0, ZoneOffset.UTC))
    )
    assertEquals(
      timestamptz(3).decode(0, List(Some("2024-03-07 11:55:55.000Z"))),
      Right(OffsetDateTime.of(2024, 3, 7, 11, 55, 55, 0, ZoneOffset.UTC))
    )
    assertEquals(
      timestamptz(4).decode(0, List(Some("2024-03-07 11:55:55.0000Z"))),
      Right(OffsetDateTime.of(2024, 3, 7, 11, 55, 55, 0, ZoneOffset.UTC))
    )
    assertEquals(
      timestamptz(5).decode(0, List(Some("2024-03-07 11:55:55.10000Z"))),
      Right(OffsetDateTime.of(2024, 3, 7, 11, 55, 55, 100000000, ZoneOffset.UTC))
    )
    assertEquals(
      timestamptz(6).decode(0, List(Some("2024-03-07 11:55:55.100000Z"))),
      Right(OffsetDateTime.of(2024, 3, 7, 11, 55, 55, 100000000, ZoneOffset.UTC))
    )
  }

  test("timestamptz decode error") {
    assertEquals(
      timestamptz(0).decode(0, List(Some(""))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text '' could not be parsed at index 0",
          Type.varchar(255)
        )
      )
    )
    assertEquals(
      timestamptz(0).decode(0, List(Some("invalid"))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text 'invalid' could not be parsed at index 0",
          Type.varchar(255)
        )
      )
    )
    assertEquals(
      timestamptz(0).decode(0, List(Some("-1"))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text '-1' could not be parsed at index 0",
          Type.varchar(255)
        )
      )
    )
    assertEquals(
      timestamptz(0).decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.varchar(255)))
    )
  }

  test("time encode successfully") {
    assertEquals(
      time(0).encode(LocalTime.of(11, 55, 55)),
      List(Some(Encoded("11:55:55", false)))
    )
    assertEquals(
      time(0).encode(LocalTime.of(0, 0, 0)),
      List(Some(Encoded("00:00:00", false)))
    )
    assertEquals(
      time(0).encode(LocalTime.of(23, 59, 59)),
      List(Some(Encoded("23:59:59", false)))
    )
    assertEquals(
      time(1).encode(LocalTime.of(11, 55, 55, 1)),
      List(Some(Encoded("11:55:55.0", false)))
    )
    assertEquals(
      time(2).encode(LocalTime.of(11, 55, 55, 1)),
      List(Some(Encoded("11:55:55.00", false)))
    )
    assertEquals(
      time(3).encode(LocalTime.of(11, 55, 55, 1)),
      List(Some(Encoded("11:55:55.000", false)))
    )
    assertEquals(
      time(4).encode(LocalTime.of(11, 55, 55, 1)),
      List(Some(Encoded("11:55:55.0000", false)))
    )
    assertEquals(
      time(5).encode(LocalTime.of(11, 55, 55, 1)),
      List(Some(Encoded("11:55:55.00000", false)))
    )
    assertEquals(
      time(6).encode(LocalTime.of(11, 55, 55, 1)),
      List(Some(Encoded("11:55:55.000000", false)))
    )
  }

  test("time decode successfully") {
    assertEquals(
      time(0).decode(0, List(Some("11:55:55"))),
      Right(LocalTime.of(11, 55, 55))
    )
    assertEquals(
      time(0).decode(0, List(Some("00:00:00"))),
      Right(LocalTime.of(0, 0, 0))
    )
    assertEquals(
      time(0).decode(0, List(Some("23:59:59"))),
      Right(LocalTime.of(23, 59, 59))
    )
    assertEquals(
      time(1).decode(0, List(Some("11:55:55.0"))),
      Right(LocalTime.of(11, 55, 55))
    )
    assertEquals(
      time(2).decode(0, List(Some("11:55:55.00"))),
      Right(LocalTime.of(11, 55, 55))
    )
    assertEquals(
      time(3).decode(0, List(Some("11:55:55.000"))),
      Right(LocalTime.of(11, 55, 55))
    )
    assertEquals(
      time(4).decode(0, List(Some("11:55:55.0000"))),
      Right(LocalTime.of(11, 55, 55))
    )
    assertEquals(
      time(5).decode(0, List(Some("11:55:55.10000"))),
      Right(LocalTime.of(11, 55, 55, 100000000))
    )
    assertEquals(
      time(6).decode(0, List(Some("11:55:55.100000"))),
      Right(LocalTime.of(11, 55, 55, 100000000))
    )
  }

  test("time decode error") {
    assertEquals(
      time(0).decode(0, List(Some(""))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text '' could not be parsed at index 0",
          Type.time(0)
        )
      )
    )
    assertEquals(
      time(0).decode(0, List(Some("invalid"))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text 'invalid' could not be parsed at index 0",
          Type.time(0)
        )
      )
    )
    assertEquals(
      time(0).decode(0, List(Some("-1"))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text '-1' could not be parsed at index 0",
          Type.time(0)
        )
      )
    )
    assertEquals(
      time(0).decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.time(0)))
    )
  }

  test("timetz encode successfully") {
    assertEquals(
      timetz(0).encode(OffsetTime.of(11, 55, 55, 0, ZoneOffset.UTC)),
      List(Some(Encoded("11:55:55Z", false)))
    )
    assertEquals(
      timetz(0).encode(OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC)),
      List(Some(Encoded("00:00:00Z", false)))
    )
    assertEquals(
      timetz(0).encode(OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC)),
      List(Some(Encoded("23:59:59Z", false)))
    )
    assertEquals(
      timetz(1).encode(OffsetTime.of(11, 55, 55, 1, ZoneOffset.UTC)),
      List(Some(Encoded("11:55:55.0Z", false)))
    )
    assertEquals(
      timetz(2).encode(OffsetTime.of(11, 55, 55, 1, ZoneOffset.UTC)),
      List(Some(Encoded("11:55:55.00Z", false)))
    )
    assertEquals(
      timetz(3).encode(OffsetTime.of(11, 55, 55, 1, ZoneOffset.UTC)),
      List(Some(Encoded("11:55:55.000Z", false)))
    )
    assertEquals(
      timetz(4).encode(OffsetTime.of(11, 55, 55, 1, ZoneOffset.UTC)),
      List(Some(Encoded("11:55:55.0000Z", false)))
    )
    assertEquals(
      timetz(5).encode(OffsetTime.of(11, 55, 55, 1, ZoneOffset.UTC)),
      List(Some(Encoded("11:55:55.00000Z", false)))
    )
    assertEquals(
      timetz(6).encode(OffsetTime.of(11, 55, 55, 1, ZoneOffset.UTC)),
      List(Some(Encoded("11:55:55.000000Z", false)))
    )
  }

  test("timetz decode successfully") {
    assertEquals(
      timetz(0).decode(0, List(Some("11:55:55Z"))),
      Right(OffsetTime.of(11, 55, 55, 0, ZoneOffset.UTC))
    )
    assertEquals(
      timetz(0).decode(0, List(Some("00:00:00Z"))),
      Right(OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC))
    )
    assertEquals(
      timetz(0).decode(0, List(Some("23:59:59Z"))),
      Right(OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC))
    )
    assertEquals(
      timetz(1).decode(0, List(Some("11:55:55.0Z"))),
      Right(OffsetTime.of(11, 55, 55, 0, ZoneOffset.UTC))
    )
    assertEquals(
      timetz(2).decode(0, List(Some("11:55:55.00Z"))),
      Right(OffsetTime.of(11, 55, 55, 0, ZoneOffset.UTC))
    )
    assertEquals(
      timetz(3).decode(0, List(Some("11:55:55.000Z"))),
      Right(OffsetTime.of(11, 55, 55, 0, ZoneOffset.UTC))
    )
    assertEquals(
      timetz(4).decode(0, List(Some("11:55:55.0000Z"))),
      Right(OffsetTime.of(11, 55, 55, 0, ZoneOffset.UTC))
    )
    assertEquals(
      timetz(5).decode(0, List(Some("11:55:55.10000Z"))),
      Right(OffsetTime.of(11, 55, 55, 100000000, ZoneOffset.UTC))
    )
    assertEquals(
      timetz(6).decode(0, List(Some("11:55:55.100000Z"))),
      Right(OffsetTime.of(11, 55, 55, 100000000, ZoneOffset.UTC))
    )
  }

  test("timetz decode error") {
    assertEquals(
      timetz(0).decode(0, List(Some(""))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text '' could not be parsed at index 0",
          Type.varchar(255)
        )
      )
    )
    assertEquals(
      timetz(0).decode(0, List(Some("invalid"))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text 'invalid' could not be parsed at index 0",
          Type.varchar(255)
        )
      )
    )
    assertEquals(
      timetz(0).decode(0, List(Some("-1"))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text '-1' could not be parsed at index 0",
          Type.varchar(255)
        )
      )
    )
    assertEquals(
      timetz(0).decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.varchar(255)))
    )
  }

  test("year encode successfully") {
    assertEquals(
      year.encode(Year.of(2024)),
      List(Some(Encoded("2024", false)))
    )
    assertEquals(
      year.encode(Year.of(1000)),
      List(Some(Encoded("1000", false)))
    )
    assertEquals(
      year.encode(Year.of(9999)),
      List(Some(Encoded("9999", false)))
    )
  }

  test("year decode successfully") {
    assertEquals(
      year.decode(0, List(Some("2024"))),
      Right(Year.of(2024))
    )
    assertEquals(
      year.decode(0, List(Some("1000"))),
      Right(Year.of(1000))
    )
    assertEquals(
      year.decode(0, List(Some("9999"))),
      Right(Year.of(9999))
    )
  }

  test("year decode error") {
    assertEquals(
      year.decode(0, List(Some(""))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text '' could not be parsed at index 0",
          Type.year
        )
      )
    )
    assertEquals(
      year.decode(0, List(Some("invalid"))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text 'invalid' could not be parsed at index 0",
          Type.year
        )
      )
    )
    assertEquals(
      year.decode(0, List(Some("-1"))),
      Left(
        Decoder.Error(
          0,
          1,
          "java.time.format.DateTimeParseException: Text '-1' could not be parsed at index 1",
          Type.year
        )
      )
    )
    assertEquals(
      year.decode(0, List(None)),
      Left(Decoder.Error(0, 1, "Unexpected NULL value in non-optional column.", Type.year))
    )
  }
