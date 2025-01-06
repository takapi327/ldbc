/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField.*
import java.util.Locale

object Formatter:

  val localDateFormatterWithoutEra: DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .appendValue(YEAR_OF_ERA, 4, 19, SignStyle.NOT_NEGATIVE)
      .appendLiteral('-')
      .appendValue(MONTH_OF_YEAR, 2)
      .appendLiteral('-')
      .appendValue(DAY_OF_MONTH, 2)
      .toFormatter(Locale.US)

  val localDateFormatter: DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .append(localDateFormatterWithoutEra)
      .toFormatter(Locale.US)

  def localDateTimeFormatter(precision: Int): DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .append(localDateFormatterWithoutEra)
      .appendLiteral(' ')
      .append(timeFormatter(precision))
      .toFormatter(Locale.US)

  def offsetTimeFormatter(precision: Int): DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .append(timeFormatter(precision))
      .appendOffset("+HH:mm", "Z")
      .toFormatter(Locale.US)

  def offsetDateTimeFormatter(precision: Int): DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .append(localDateFormatterWithoutEra)
      .appendLiteral(' ')
      .append(timeFormatter(precision))
      .appendOffset("+HH:mm", "Z")
      .toFormatter(Locale.US)

  def timeFormatter(precision: Int): DateTimeFormatter =

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
