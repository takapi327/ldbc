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
import java.time.temporal.ChronoField.*
import java.time.temporal.TemporalAccessor
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.util.Locale

import cats.syntax.all.*

import ldbc.connector.data.Type

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

  private val localDateFormatterWithoutEra: DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .appendValue(YEAR_OF_ERA, 4, 19, SignStyle.NOT_NEGATIVE)
      .appendLiteral('-')
      .appendValue(MONTH_OF_YEAR, 2)
      .appendLiteral('-')
      .appendValue(DAY_OF_MONTH, 2)
      .toFormatter(Locale.US)

  private val localDateFormatter: DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .append(localDateFormatterWithoutEra)
      .toFormatter(Locale.US)

  private def localDateTimeFormatter(precision: Int): DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .append(localDateFormatterWithoutEra)
      .appendLiteral(' ')
      .append(timeFormatter(precision))
      .toFormatter(Locale.US)

  private def offsetTimeFormatter(precision: Int): DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .append(timeFormatter(precision))
      .appendOffset("+HH:mm", "Z")
      .toFormatter(Locale.US)

  private def offsetDateTimeFormatter(precision: Int): DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .append(localDateFormatterWithoutEra)
      .appendLiteral(' ')
      .append(timeFormatter(precision))
      .appendOffset("+HH:mm", "Z")
      .toFormatter(Locale.US)

  private def timeFormatter(precision: Int): DateTimeFormatter =

    val requiredPart: DateTimeFormatterBuilder =
      new DateTimeFormatterBuilder()
        .appendValue(HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(MINUTE_OF_HOUR, 2)
        .appendLiteral(':')
        .appendValue(SECOND_OF_MINUTE, 2)

    if precision > 0 then
      requiredPart.optionalStart
        .appendFraction(NANO_OF_SECOND, 0, precision, true)
        .optionalEnd
      ()

    requiredPart.toFormatter(Locale.US)

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
    temporal(offsetTimeFormatter(fsp), OffsetTime.parse, Type.time(fsp))
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
      str => Either.catchOnly[DateTimeParseException](Year.parse(str)).leftMap(_.toString),
      Type.year
    )

object temporal extends TemporalCodecs
