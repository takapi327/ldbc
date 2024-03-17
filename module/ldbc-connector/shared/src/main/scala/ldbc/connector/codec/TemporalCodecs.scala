/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Year
import java.time.temporal.TemporalAccessor

import cats.syntax.all.*

import ldbc.connector.data.Type
import ldbc.connector.data.Formatter.*

trait TemporalCodecs:

  private def temporal[A <: TemporalAccessor](
    formatter: DateTimeFormatter,
    parse:     (String, DateTimeFormatter) => A,
    tpe:       Type
  ): Codec[A] =
    Codec.simple(
      a => formatter.format(a),
      s => Either.catchOnly[DateTimeParseException](parse(s, formatter)).leftMap(_.toString),
      tpe
    )

  val date: Codec[LocalDate] =
    temporal(localDateFormatter, LocalDate.parse, Type.date)

  def datetime(fsp: 0 | 1 | 2 | 3 | 4 | 5 | 6): Codec[LocalDateTime] =
    temporal(localDateTimeFormatter(fsp), LocalDateTime.parse, Type.datetime(fsp))
  val datetime: Codec[LocalDateTime] = datetime(0)

  def timestamp(fsp: 0 | 1 | 2 | 3 | 4 | 5 | 6): Codec[LocalDateTime] =
    temporal(localDateTimeFormatter(fsp), LocalDateTime.parse, Type.timestamp(fsp))
  val timestamp: Codec[LocalDateTime] = timestamp(0)

  def timestamptz(fsp: 0 | 1 | 2 | 3 | 4 | 5 | 6): Codec[OffsetDateTime] =
    temporal(offsetDateTimeFormatter(fsp), OffsetDateTime.parse, Type.varchar(255))
  val timestamptz: Codec[OffsetDateTime] = timestamptz(0)

  def time(fsp: 0 | 1 | 2 | 3 | 4 | 5 | 6): Codec[LocalTime] =
    temporal(timeFormatter(fsp), LocalTime.parse, Type.time(fsp))
  val time: Codec[LocalTime] = time(0)

  def timetz(fsp: 0 | 1 | 2 | 3 | 4 | 5 | 6): Codec[OffsetTime] =
    temporal(offsetTimeFormatter(fsp), OffsetTime.parse, Type.varchar(255))
  val timetz: Codec[OffsetTime] = timetz(0)

  @deprecated(
    "As of MySQL 8.0.19, specifying the number of digits for the YEAR data type is deprecated. It will not be supported in future MySQL versions.",
    "0.3.0"
  )
  def year(digit: 4): Codec[Year] =
    Codec.simple(
      _.toString,
      str => Either.catchOnly[DateTimeParseException](Year.parse(str)).leftMap(_.toString),
      Type.year(digit)
    )
  val year: Codec[Year] =
    Codec.simple(
      _.toString,
      str =>
        (
          for
            int <- Either.catchOnly[NumberFormatException](str.toInt)
            year <- Either.catchOnly[DateTimeParseException] {
                      val int = str.toInt
                      if (1901 <= int && int <= 2156) || str === "0000" then Year.of(int)
                      else
                        throw new DateTimeParseException(
                          s"Year is out of range: $int. Year must be in the range 1901 to 2155.",
                          str,
                          0
                        )
                    }
          yield year
        ).leftMap(_.toString),
      Type.year
    )

object temporal extends TemporalCodecs
