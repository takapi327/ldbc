/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement.functions

import java.time.*
import java.time.format.DateTimeFormatter

import ldbc.dsl.codec.*

import ldbc.statement.Column

/**
 * Provide functions that can be used to manipulate temporal values provided by MySQL.
 *
 * @see https://dev.mysql.com/doc/refman/8.0/en/date-and-time-functions.html
 */
trait DateTime:

  /**
   * Function to perform addition on a specified date type column.
   *
   * {{{
   *   TableQuery[DateTime].select(p => DATE_ADD(p.birthDate, DateTime.Interval.YEAR(1)))
   *   // SELECT DATE_ADD(birth_date, INTERVAL 1 YEAR) FROM date_time
   * }}}
   *
   * @param column
   *   The column to which the addition is to be performed.
   * @param interval
   *   The interval to be added to the column.
   */
  def DATE_ADD[A <: LocalDate | Option[LocalDate]](column: Column[A], interval: DateTime.Interval[Int]): Column[A] =
    Column.function(s"DATE_ADD(${ column.name }, ${ interval.statement })")(using column.decoder, column.encoder)

  /**
   * Function to perform addition on a specified date type column.
   *
   * {{{
   *   TableQuery[DateTime].select(p => DATE_ADD(LocalDate.now, DateTime.Interval.YEAR(1)))
   *   // SELECT DATE_ADD('2008-02-02', INTERVAL 1 YEAR) FROM date_time
   * }}}
   *
   * @param date
   *   The date to which the addition is to be performed.
   * @param interval
   *   The interval to be added to the date.
   */
  def DATE_ADD(date: LocalDate, interval: DateTime.Interval[Int])(using
    Decoder[LocalDate],
    Encoder[LocalDate]
  ): Column[LocalDate] =
    Column.function(s"DATE_ADD('${ date.toString }', ${ interval.statement })")

  /**
   * Function to perform subtraction on a given date type column.
   *
   * {{{
   *   TableQuery[DateTime].select(p => DATE_SUB(p.birthDate, DateTime.Interval.YEAR(1)))
   *   // SELECT DATE_SUB(birth_date, INTERVAL 1 YEAR) FROM date_time
   * }}}
   *
   * @param column
   *   The column to which the subtraction is to be performed.
   * @param interval
   *   The interval to be subtraction to the column.
   */
  def DATE_SUB[A <: LocalDate | Option[LocalDate]](column: Column[A], interval: DateTime.Interval[Int]): Column[A] =
    Column.function(s"DATE_SUB(${ column.name }, ${ interval.statement })")(using column.decoder, column.encoder)

  /**
   * Function to perform subtraction on a given date.
   *
   * {{{
   *   TableQuery[DateTime].select(p => DATE_SUB(LocalDate.now, DateTime.Interval.YEAR(1)))
   *   // SELECT DATE_SUB('2008-02-02', INTERVAL 1 YEAR) FROM date_time
   * }}}
   *
   * @param date
   *   The date to which the subtraction is to be performed.
   * @param interval
   *   The interval to be subtraction to the date.
   */
  def DATE_SUB(date: LocalDate, interval: DateTime.Interval[Int])(using
    Decoder[LocalDate],
    Encoder[LocalDate]
  ): Column[LocalDate] =
    Column.function(s"DATE_SUB('${ date.toString }', ${ interval.statement })")

  /**
   * Function to perform addition on a specified date type column.
   *
   * {{{
   *   TableQuery[DateTime].select(p => ADDTIME(p.time, LocalTime.of(1, 1, 1, 1)))
   *   // SELECT ADDTIME(time, '01:01:01.000000001') FROM date_time
   * }}}
   *
   * @param column
   *   The column to which the addition is to be performed.
   * @param time
   *   The time to be added to the column.
   */
  def ADDTIME[
    A <: LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A],
    time:   LocalTime
  ): Column[A] =
    Column.function(s"ADDTIME(${ column.name }, '${ time.toString }')")(using column.decoder, column.encoder)

  /**
   * Function to perform addition on a specified date type column.
   *
   * {{{
   *   TableQuery[DateTime].select(p => ADDTIME(LocalTime.of(1, 1, 1, 1), LocalTime.of(1, 1, 1, 1)))
   *   // SELECT ADDTIME('01:01:01.000000001', '01:01:01.000000001') FROM date_time
   * }}}
   *
   * @param dateTime
   *   The date time to which the addition is to be performed.
   * @param time
   *   The time to be added to the date time.
   */
  def ADDTIME(
    dateTime: LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime,
    time:     LocalTime
  ): Column[LocalTime] =
    Column.function(s"ADDTIME('${ dateTime.toString }', '${ time.toString }')")

  /**
   * Function to convert a date-time value dt from the time zone specified by from_tz to the time zone specified by to_tz.
   *
   * {{{
   *   TableQuery[DateTime].select(p => CONVERT_TZ(p.timestampe, LocalTime.of(0, 0), LocalTime.of(9, 0)))
   *   // SELECT CONVERT_TZ(timestampe, '+00:00', '+09:00') FROM date_time
   * }}}
   *
   * @param column
   *   The column to which the addition is to be performed.
   * @param from
   *   The time zone from which the conversion is to be performed.
   * @param to
   *   The time zone to which the conversion is to be performed.
   */
  def CONVERT_TZ[
    A <: LocalDateTime | OffsetDateTime | ZonedDateTime | Option[LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A],
    from:   LocalTime,
    to:     LocalTime
  ): Column[A] =
    Column.function(
      s"CONVERT_TZ(${ column.name }, '+${ from.getHour }:${ from.getMinute }', '+${ to.getHour }:${ to.getMinute }')"
    )(using column.decoder, column.encoder)

  /**
   * Function to convert a date-time value dt from the time zone specified by from_tz to the time zone specified by to_tz.
   *
   * {{{
   *   TableQuery[DateTime].select(p => CONVERT_TZ(LocalDateTime.of(2025, 1, 1), LocalTime.of(0, 0), LocalTime.of(9, 0)))
   *   // SELECT CONVERT_TZ('2025-01-01', '+00:00', '+09:00') FROM date_time
   * }}}
   *
   * @param dateTime
   *   The date time to which the addition is to be performed.
   * @param from
   *   The time zone from which the conversion is to be performed.
   * @param to
   *   The time zone to which the conversion is to be performed.
   */
  def CONVERT_TZ(
    dateTime: LocalDateTime | OffsetDateTime | ZonedDateTime,
    from:     LocalTime,
    to:       LocalTime
  ): Column[LocalDateTime] =
    Column.function(
      s"CONVERT_TZ('${ dateTime.toString }', '+${ from.getHour }:${ from.getMinute }', '+${ to.getHour }:${ to.getMinute }')"
    )

  /**
   * Function to return the current date as 'YYYY-MM-DD' format.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => CURDATE)
   *   // SELECT CURDATE() FROM date_time
   * }}}
   */
  def CURDATE(using Decoder[LocalDate], Encoder[LocalDate]): Column[LocalDate] = Column.function("CURDATE()")

  /**
   * Function to return the current time in 'hh:mm:ss' format.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => CURTIME)
   *   // SELECT CURTIME() FROM date_time
   * }}}
   */
  def CURTIME(using Decoder[LocalTime], Encoder[LocalTime]): Column[LocalTime] = Column.function("CURTIME()")

  /**
   * Function to extract the date portion of a date or date-time expression.
   *
   * {{{
   *   TableQuery[DateTime].select(p => DATE(p.timestamp))
   *   // SELECT DATE(timestamp) FROM date_time
   * }}}
   *
   * @param column
   *   Date or date/time column from which to extract the date portion
   */
  def DATE[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](column: Column[A])(using Decoder[LocalDate], Encoder[LocalDate]): Column[LocalDate] =
    Column.function(s"DATE(${ column.name })")

  /**
   * Function to extract the date portion of a date or date-time expression.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => DATE(LocalDateTime.of(2025, 1, 1, 1, 1)))
   *   // SELECT DATE('2025-01-01T01:01') FROM date_time
   * }}}
   * @param date
   *   The date or date-time expression from which the date portion is to be extracted.
   */
  def DATE(date: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime): Column[LocalDate] =
    Column.function(s"DATE('${ date.toString }')")

  /**
   * Function to format a date value according to the format string.
   * @see https://dev.mysql.com/doc/refman/8.0/en/date-and-time-functions.html#function_date-format
   *      
   * {{{
   *   TableQuery[DateTime].select(p => DATE_FORMAT(p.timestamp, "%Y-%m-%d"))
   *   // SELECT DATE_FORMAT(timestamp, '%Y-%m-%d') FROM date_time
   * }}}
   *
   * @param column
   *   The column to be formatted.
   * @param format
   *   The format string.
   */
  def DATE_FORMAT[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A],
    format: String
  )(using Decoder[String], Encoder[String]): Column[String] =
    Column.function(s"DATE_FORMAT(${ column.name }, '$format')")

  /**
   * Function to format a date value according to the format string.
   * 
   * {{{
   *   TableQuery[DateTime].select(_ => DATE_FORMAT(LocalDate.of(2025, 1, 1), "%Y-%m-%d"))
   *   // SELECT DATE_FORMAT('2025-01-01', '%Y-%m-%d') FROM date_time
   * }}}
   * 
   * @param date
   *   The date or date-time expression to be formatted.
   * @param format
   *   The format string.
   */
  def DATE_FORMAT(date: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime, format: String): Column[String] =
    Column.function(s"DATE_FORMAT('${ date.toString }', '$format')")

  /**
   * Function to calculate the value of the number of days from one date to another.
   *
   * {{{
   *   TableQuery[DateTime].select(p => DATEDIFF(p.birthDate, p.deathDate))
   *   // SELECT DATEDIFF(birth_date, death_date) FROM date_time
   * }}}
   * 
   * @param from
   *   The starting date.
   * @param to
   *   The ending date.
   */
  def DATEDIFF[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime],
    B <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    from: Column[A],
    to:   Column[B]
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"DATEDIFF(${ from.name }, ${ to.name })")

  /**
   * Function to calculate the value of the number of days from one date to another.
   * 
   * {{{
   *  TableQuery[DateTime].select(p => DATEDIFF(p.birthDate, LocalDate.of(2021, 1, 1)))
   *  // SELECT DATEDIFF(birth_date, '2021-01-01') FROM date_time 
   * }}}
   * 
   * @param from
   *   The starting date.
   * @param to
   *   The ending date.
   */
  def DATEDIFF[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    from: Column[A],
    to:   LocalDate
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"DATEDIFF(${ from.name }, '${ to.toString }')")

  /**
   * Function to calculate the value of the number of days from one date to another.
   *
   * {{{
   *  TableQuery[DateTime].select(p => DATEDIFF(p.birthDate, LocalDate.of(2021, 1, 1)))
   *  // SELECT DATEDIFF(birth_date, '2021-01-01') FROM date_time 
   * }}}
   *
   * @param from
   * The starting date.
   * @param to
   * The ending date.
   */
  def DATEDIFF(
    from: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime,
    to:   LocalDate
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"DATEDIFF('${ from.toString }', '${ to.toString }')")

  /**
   * A function that returns the name of the day of the week corresponding to date.
   * The language used for the names is controlled by the value of the lc_time_names system variable (Section 10.16, “Locale Support in MySQL Server”).
   * @see https://dev.mysql.com/doc/refman/8.0/en/locale-support.html
   *
   * {{{
   *   TableQuery[DateTime].select(p => DAYNAME(p.birthDate))
   *   // SELECT DAYNAME(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *   The date or date-time column from which to extract the day of the week.
   */
  def DAYNAME[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[String], Encoder[String]): Column[String] =
    Column.function(s"DAYNAME(${ column.name })")

  /**
   * A function that returns the name of the day of the week corresponding to date.
   * The language used for the names is controlled by the value of the lc_time_names system variable (Section 10.16, “Locale Support in MySQL Server”).
   *
   * @see https://dev.mysql.com/doc/refman/8.0/en/locale-support.html
   *
   * {{{
   *   TableQuery[DateTime].select(_ => DAYNAME(LocalDate.of(2021, 1, 1)))
   *   // SELECT DAYNAME('2021-01-01') FROM date_time
   * }}}
   *
   * @param date
   *   The date or date-time expression from which to extract the day of the week.
   */
  def DAYNAME(
    date: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[String], Encoder[String]): Column[String] =
    Column.function(s"DAYNAME('${ date.toString }')")

  /**
   * A function that returns the day of the month for date, in the range 1 to 31, or 0 for dates such as '0000-00-00' or '2008-00-00' that have a zero day part.
   *
   * {{{
   *  TableQuery[DateTime].select(p => DAYOFMONTH(p.birthDate))
   *  // SELECT DAYOFMONTH(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *   The date or date-time column from which to extract the day of the month.
   */
  def DAYOFMONTH[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"DAYOFMONTH(${ column.name })")

  /**
   * A function that returns the day of the month for date, in the range 1 to 31, or 0 for dates such as '0000-00-00' or '2008-00-00' that have a zero day part.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => DAYOFMONTH(LocalDate.of(2021, 1, 1)))
   *   // SELECT DAYOFMONTH('2021-01-01') FROM date_time
   * }}}
   *
   * @param date
   *   The date or date-time expression from which to extract the day of the month.
   */
  def DAYOFMONTH(
    date: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"DAYOFMONTH('${ date.toString }')")

  /**
   * A function that returns the day of the week for date, in the range 1 to 7, where 1 represents Sunday.
   * Day of the week index (1 = Sunday, 2 = Monday, ..., 7 = Saturday) for date.
   *
   * {{{
   *  TableQuery[DateTime].select(p => DAYOFWEEK(p.birthDate))
   *  // SELECT DAYOFWEEK(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *   The date or date-time column from which to extract the day of the week.
   */
  def DAYOFWEEK[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"DAYOFWEEK(${ column.name })")

  /**
   * A function that returns the day of the week for date, in the range 1 to 7, where 1 represents Sunday.
   * Day of the week index (1 = Sunday, 2 = Monday, ..., 7 = Saturday) for date.
   *
   * {{{
   *  TableQuery[DateTime].select(_ => DAYOFWEEK(LocalDate.of(2021, 1, 1)))
   *  // SELECT DAYOFWEEK('2021-01-01') FROM date_time
   * }}}
   *
   * @param date
   *   The date or date-time expression from which to extract the day of the week.
   */
  def DAYOFWEEK(
    date: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"DAYOFWEEK('${ date.toString }')")

  /**
   * A function that returns the day of the year for date, in the range 1 to 366.
   * The range of DAYOFYEAR() is 1 to 366 because MySQL supports leap year.
   *
   * {{{
   *   TableQuery[DateTime].select(p => DAYOFYEAR(p.birthDate))
   *   // SELECT DAYOFYEAR(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *   The date or date-time column from which to extract the day of the year.
   */
  def DAYOFYEAR[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"DAYOFYEAR(${ column.name })")

  /**
   * A function that returns the day of the year for date, in the range 1 to 366.
   * The range of DAYOFYEAR() is 1 to 366 because MySQL supports leap year.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => DAYOFYEAR(LocalDate.of(2021, 1, 1)))
   *   // SELECT DAYOFYEAR('2021-01-01') FROM date_time
   * }}}
   *
   * @param date
   *   The date or date-time expression from which to extract the day of the year.
   */
  def DAYOFYEAR(
    date: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"DAYOFYEAR('${ date.toString }')")

  /**
   * A function that returns the time part of the expression expr as a time value.
   *
   * {{{
   *   TableQuery[DateTime].select(p => EXTRACT(p.timestamp, DateTime.TimeUnit.HOUR))
   *   // SELECT EXTRACT(HOUR FROM timestamp) FROM date_time
   * }}}
   * @param column
   *   The column from which to extract the time part.
   * @param timeUnit
   *   The time unit to be extracted.
   */
  def EXTRACT[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column:   Column[A],
    timeUnit: DateTime.TimeUnit
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"EXTRACT(${ timeUnit.toString } FROM ${ column.name })")

  /**
   * A function that returns the time part of the expression expr as a time value.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => EXTRACT(LocalDateTime.of(2021, 1, 1, 0, 0), DateTime.TimeUnit.HOUR))
   *   // SELECT EXTRACT(HOUR FROM '2021-01-01T00:00') FROM date_time
   * }}}
   * @param date
   *   The date or date-time expression from which to extract the time part.
   * @param timeUnit
   *   The time unit to be extracted.
   */
  def EXTRACT(
    date:     LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime,
    timeUnit: DateTime.TimeUnit
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"EXTRACT(${ timeUnit.toString } FROM '${ date.toString.replaceAll("T", " ") }')")

  /**
   * Function to convert a day value to a date.
   *
   * Use FROM_DAYS() carefully with older dates. It is not designed to be used with values prior to the advent of the Gregorian calendar (1582).
   *
   * @see https://dev.mysql.com/doc/refman/8.0/en/mysql-calendar.html
   *
   * {{{
   *   TableQuery[DateTime].select(p => FROM_DAYS(p.birthDate))
   *   // SELECT FROM_DAYS(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *   The column from which to extract the date.
   */
  def FROM_DAYS[A <: Int | Long | Option[Int | Long]](
    column: Column[A]
  )(using Decoder[LocalDate], Encoder[LocalDate]): Column[LocalDate] =
    Column.function(s"FROM_DAYS(${ column.name })")

  /**
   * Function to convert a day value to a date.
   *
   * Use FROM_DAYS() carefully with older dates. It is not designed to be used with values prior to the advent of the Gregorian calendar (1582).
   *
   * @see https://dev.mysql.com/doc/refman/8.0/en/mysql-calendar.html
   *
   * {{{
   *   TableQuery[DateTime].select(_ => FROM_DAYS(730669))
   *   // SELECT FROM_DAYS(730669) FROM date_time
   * }}}
   *
   * @param days
   *   The number of days from which to extract the date.
   */
  def FROM_DAYS[A <: Int | Long | Option[Int | Long]](
    days: A
  )(using Decoder[LocalDate], Encoder[LocalDate]): Column[LocalDate] =
    Column.function(s"FROM_DAYS($days)")

  /**
   * Function to generate a UTC date in YYYYY-MM-DD hh:mm:ss format from a number.
   *
   * {{{
   *   TableQuery[DateTime].select(p => FROM_UNIXTIME(p.birthDate))
   *   // SELECT FROM_UNIXTIME(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *   The column from which to extract the date.
   */
  def FROM_UNIXTIME[A <: Int | Long | Option[Int | Long]](
    column: Column[A]
  )(using Decoder[LocalDateTime], Encoder[LocalDateTime]): Column[LocalDateTime] =
    Column.function(s"FROM_UNIXTIME(${ column.name })")

  /**
   * Function to generate a UTC date in YYYYY-MM-DD hh:mm:ss format from a number.
   *
   * {{{
   *  TableQuery[DateTime].select(_ => FROM_UNIXTIME(1612137600))
   *  // SELECT FROM_UNIXTIME(1612137600) FROM date_time
   * }}}
   *
   * @param timestamp
   *   The number from which to extract the date.
   */
  def FROM_UNIXTIME(timestamp: Int | Long)(using Decoder[String], Encoder[String]): Column[String] =
    Column.function(s"FROM_UNIXTIME($timestamp)")

  /**
   * Function for extracting time-only values from date types.
   * The range of returned values is from 0 to 23 for date-time values. However, since the range of TIME values is actually much larger, HOUR can return values greater than 23.
   *
   * {{{
   *   TableQuery[DateTime].select(p => HOUR(p.birthDate))
   *   // SELECT HOUR(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *   The column from which to extract the hour.
   */
  def HOUR[
    A <: LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"HOUR(${ column.name })")

  /**
   * Function for extracting time-only values from date types.
   * The range of returned values is from 0 to 23 for date-time values. However, since the range of TIME values is actually much larger, HOUR can return values greater than 23.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => HOUR(LocalTime.of(1, 1, 1, 1)))
   *   // SELECT HOUR('01:01:01.000000001') FROM date_time
   * }}}
   * @param date
   *   The date or date-time expression from which to extract the hour.
   */
  def HOUR(
    date: LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"HOUR('${ date.toString.replaceAll("T", " ") }')")

  /**
   * Function that returns the value corresponding to the last day of the month from a date or date-time value.
   *
   * {{{
   *   TableQuery[DateTime].select(p => LAST_DAY(p.birthDate))
   *   // SELECT LAST_DAY(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *  The column from which to extract the last day of the month.
   */
  def LAST_DAY[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[LocalDate], Encoder[LocalDate]): Column[LocalDate] =
    Column.function(s"LAST_DAY(${ column.name })")

  /**
   * Function that returns the value corresponding to the last day of the month from a date or date-time value.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => LAST_DAY(LocalDate.of(2021, 1, 1)))
   *   // SELECT LAST_DAY('2021-01-01') FROM date_time
   * }}}
   *
   * @param date
   *  The date or date-time expression from which to extract the last day of the month.
   */
  def LAST_DAY(
    date: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[LocalDate], Encoder[LocalDate]): Column[LocalDate] =
    Column.function(s"LAST_DAY('${ date.toString }')")

  /**
   * Function to return a date calculated from a given year and annual total.
   *
   * {{{
   *   TableQuery[DateTime].select(p => MAKEDATE(p.birthDate, 1))
   *   // SELECT MAKEDATE(birth_date, 1) FROM date_time
   * }}}
   *
   * @param column
   *   The column from which to calculate the date.
   * @param day
   *   The annual total from which to calculate the date.
   *   The annual total must be greater than 0.
   */
  def MAKEDATE[A <: Year | Option[Year]](
    column: Column[A],
    day:    Int
  )(using Decoder[LocalDate], Encoder[LocalDate]): Column[LocalDate] =
    require(day > 0, "The annual total must be greater than 0.")
    Column.function(s"MAKEDATE(${ column.name }, $day)")

  /**
   * Function to return a date calculated from a given year and annual total.
   *
   * {{{
   *   TableQuery[DateTime].select(p => MAKEDATE(2021, 1))
   *   // SELECT MAKEDATE(2021, 1) FROM date_time
   * }}}
   *
   * @param year
   *   The year from which to calculate the date.
   * @param day
   *   The annual total from which to calculate the date.
   *   The annual total must be greater than 0.
   */
  def MAKEDATE(year: Int | Year, day: Int)(using Decoder[LocalDate], Encoder[LocalDate]): Column[LocalDate] =
    require(day > 0, "The annual total must be greater than 0.")
    Column.function(s"MAKEDATE($year, $day)")

  /**
   * Function to return a time value calculated from the given hour, minute, and second.
   * The range of the hour is 0 to 23, the range of the minute is 0 to 59, and the range of the second is 0 to 59.
   *
   * {{{
   *   TableQuery[DateTime].select(p => MAKETIME(p.hour, p.minute, p.second))
   *   // SELECT MAKETIME(hour, minute, second) FROM date_time
   * }}}
   *
   * @param hour
   *   The hour from which to calculate the time.
   * @param minute
   *   The minute from which to calculate the time.
   * @param second
   *   The second from which to calculate the time.
   */
  def MAKETIME[
    A <: Int | Long | Option[Int | Long],
    B <: Int | Long | Option[Int | Long],
    C <: Int | Long | Option[Int | Long]
  ](
    hour:   Column[A],
    minute: Column[B],
    second: Column[C]
  )(using Decoder[LocalTime], Encoder[LocalTime]): Column[LocalTime] =
    Column.function(s"MAKETIME(${ hour.name }, ${ minute.name }, ${ second.name })")

  /**
   * Function to return a time value calculated from the given hour, minute, and second.
   * The range of the hour is 0 to 23, the range of the minute is 0 to 59, and the range of the second is 0 to 59.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => MAKETIME(1, 1, 1))
   *   // SELECT MAKETIME(1, 1, 1) FROM date_time
   * }}}
   *
   * @param hour
   *   The hour from which to calculate the time.
   * @param minute
   *   The minute from which to calculate the time.
   * @param second
   *   The second from which to calculate the time.
   */
  def MAKETIME(
    hour:   Int | Long,
    minute: Int | Long,
    second: Int | Long
  )(using Decoder[LocalTime], Encoder[LocalTime]): Column[LocalTime] =
    Column.function(s"MAKETIME($hour, $minute, $second)")

  /**
   * Function that returns a format string that can be used with DATE_FORMAT() and STR_TO_DATE() functions.
   * 
   * {{{
   *   TableQuery[DateTime].select(_ => GET_FORMAT(DateTime.DateType.DATE, DateTime.FormatType.USA))
   *   // SELECT GET_FORMAT(DATE, 'USA') FROM date_time
   * }}}
   *
   * @param dateType
   *   The type of date/time format (DATE, TIME, DATETIME).
   * @param formatType
   *   The format style (EUR, USA, JIS, ISO, INTERNAL).
   */
  def GET_FORMAT(
    dateType:   DateTime.DateType,
    formatType: DateTime.FormatType
  )(using Decoder[String], Encoder[String]): Column[String] =
    Column.function(s"GET_FORMAT(${ dateType.toString }, '${ formatType.toString }')")

  /**
   * Function that returns microseconds from a time or date-time expression as a number in the range 0 to 999999.
   *
   * {{{
   *   TableQuery[DateTime].select(p => MICROSECOND(p.birthDate))
   *   // SELECT MICROSECOND(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *   The column from which to extract the microseconds.
   */
  def MICROSECOND[
    A <: LocalDateTime | OffsetDateTime | ZonedDateTime | Option[LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"MICROSECOND(${ column.name })")

  /**
   * Function that returns microseconds from a time or date-time expression as a number in the range 0 to 999999.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => MICROSECOND(LocalDateTime.of(2021, 1, 1, 0, 0)))
   *   // SELECT MICROSECOND('2021-01-01 00:00') FROM date_time
   * }}}
   *
   * @param datetime
   *   The date or date-time expression from which to extract the microseconds.
   */
  def MICROSECOND(
    datetime: LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"MICROSECOND('${ datetime.toString.replaceAll("T", " ") }')")

  /**
   * Function that returns the minute part of a date or date-time expression.
   * The range of the minute is 0 to 59.
   *
   * {{{
   *   TableQuery[DateTime].select(p => MINUTE(p.birthDate))
   *   // SELECT MINUTE(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *  The column from which to extract the minute.
   */
  def MINUTE[
    A <: LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"MINUTE(${ column.name })")

  /**
   * Function that returns the minute part of a date or date-time expression.
   * The range of the minute is 0 to 59.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => MINUTE(LocalDateTime.of(2021, 1, 1, 0, 0)))
   *   // SELECT MINUTE('2021-01-01 00:00') FROM date_time
   * }}}
   *
   * @param time
   *   The date or date-time expression from which to extract the minute.
   */
  def MINUTE(
    time: LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"MINUTE('${ time.toString.replaceAll("T", " ") }')")

  /**
   * Function that returns the month part of a date or date-time expression.
   * The range of the month is 1 to 12.
   *
   * {{{
   *   TableQuery[DateTime].select(p => MONTH(p.birthDate))
   *   // SELECT MONTH(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *  The column from which to extract the month.
   */
  def MONTH[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"MONTH(${ column.name })")

  /**
   * Function that returns the month part of a date or date-time expression.
   * The range of the month is 1 to 12.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => MONTH(LocalDateTime.of(2021, 1, 1, 0, 0)))
   *   // SELECT MONTH('2021-01-01 00:00') FROM date_time
   * }}}
   *
   * @param date
   *  The date or date-time expression from which to extract the month.
   */
  def MONTH(
    date: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"MONTH('${ date.toString.replaceAll("T", " ") }')")

  /**
   * Function to return the full name of the month corresponding to a value of type Date.
   * The language used for names is controlled by the value of the lc_time_names system variable (Section 10.16, “Locale Support in MySQL Server”).
   *
   * @see https://dev.mysql.com/doc/refman/8.0/en/locale-support.html
   *
   * {{{
   *   TableQuery[DateTime].select(p => MONTHNAME(p.birthDate))
   *   // SELECT MONTHNAME(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *   The date or date-time column from which to extract the month name.
   */
  def MONTHNAME[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[String], Encoder[String]): Column[String] =
    Column.function(s"MONTHNAME(${ column.name })")

  /**
   * Function to return the full name of the month corresponding to a value of type Date.
   * The language used for names is controlled by the value of the lc_time_names system variable (Section 10.16, “Locale Support in MySQL Server”).
   *
   * @see https://dev.mysql.com/doc/refman/8.0/en/locale-support.html
   *
   * {{{
   *   TableQuery[DateTime].select(_ => MONTHNAME(LocalDate.of(2021, 1, 1)))
   *   // SELECT MONTHNAME('2021-01-01') FROM date_time
   * }}}
   *
   * @param date
   *   The date or date-time expression from which to extract the month name.
   */
  def MONTHNAME(
    date: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[String], Encoder[String]): Column[String] =
    Column.function(s"MONTHNAME('${ date.toString }')")

  /**
   * Function to return the current date and time in 'YYYY-MM-DD hh:mm:ss' format.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => NOW)
   *   // SELECT NOW() FROM date_time
   * }}}
   */
  def NOW()(using Decoder[LocalDateTime], Encoder[LocalDateTime]): Column[LocalDateTime] = Column.function("NOW()")

  /**
   * Function to add the specified month to the yyyyMM value.
   *
   * @param period
   *   The period in yyyyMM format.
   * @param months
   *   The number of months to add.
   */
  def PERIOD_ADD(period: YearMonth, months: Int): Column[YearMonth] =
    val formatter          = DateTimeFormatter.ofPattern("yyyyMM")
    given Codec[YearMonth] = Codec[String].imap { str =>
      YearMonth.parse(str, formatter)
    } { yearMonth =>
      yearMonth.format(formatter)
    }
    Column.function(s"PERIOD_ADD(${ period.format(formatter) }, $months)")

  /**
   * Function to return the number of months between periods.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => PERIOD_DIFF(YearMonth.of(2008, 2), YearMonth.of(2007, 3)))
   *   // SELECT PERIOD_DIFF(200802, 200703) FROM date_time
   * }}}
   *
   * @param period1
   *   First period in YYYYMM format.
   * @param period2
   *   Second period in YYYYMM format.
   */
  def PERIOD_DIFF(period1: YearMonth, period2: YearMonth)(using Decoder[Int], Encoder[Int]): Column[Int] =
    val formatter = DateTimeFormatter.ofPattern("yyyyMM")
    Column.function(s"PERIOD_DIFF(${ period1.format(formatter) }, ${ period2.format(formatter) })")

  /**
   * Function to return the quarter corresponding to date in the range 1 to 4.
   *
   * {{{
   *   TableQuery[DateTime].select(p => QUARTER(p.birthDate))
   *   // SELECT QUARTER(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *   The date or date-time column from which to extract the quarter.
   */
  def QUARTER[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"QUARTER(${ column.name })")

  /**
   * Function to return the quarter corresponding to date in the range 1 to 4.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => QUARTER(LocalDate.of(2021, 1, 1)))
   *   // SELECT QUARTER('2021-01-01') FROM date_time
   * }}}
   *
   * @param date
   *   The date or date-time expression from which to extract the quarter.
   */
  def QUARTER(
    date: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"QUARTER('${ date.toString }')")

  /**
   * Function to convert TIME values to hh:mm:ss format.
   *
   * {{{
   *   TableQuery[DateTime].select(p => SEC_TO_TIME(p.seconds))
   *   // SELECT SEC_TO_TIME(seconds) FROM date_time
   * }}}
   *
   * @param column
   *   The column to be converted.
   */
  def SEC_TO_TIME[A <: Long | Int | Option[Long | Int]](column: Column[A]): Column[LocalTime] =
    given Codec[LocalTime] = Codec[Int].imap(LocalTime.ofSecondOfDay(_))(_.toSecondOfDay)
    Column.function(s"SEC_TO_TIME(${ column.name })")

  /**
   * Function to convert TIME values to hh:mm:ss format.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => SEC_TO_TIME(3600))
   *   // SELECT SEC_TO_TIME(3600) FROM date_time
   * }}}
   * @param seconds
   *   The number of seconds to be converted.
   */
  def SEC_TO_TIME(seconds: Long): Column[LocalTime] =
    given Codec[LocalTime] = Codec[Int].imap(LocalTime.ofSecondOfDay(_))(_.toSecondOfDay)
    Column.function(s"SEC_TO_TIME($seconds)")

  /**
   * Function to return the second part of a date or date-time expression.
   *
   * {{{
   *   TableQuery[DateTime].select(p => SECOND(p.birthDate))
   *   // SELECT SECOND(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *   The column from which to extract the second.
   */
  def SECOND[
    A <: LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"SECOND(${ column.name })")

  /**
   * Function to return the second part of a date or date-time expression.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => SECOND(LocalDateTime.of(2021, 1, 1, 0, 0)))
   *   // SELECT SECOND('2021-01-01 00:00') FROM date_time
   * }}}
   *
   * @param time
   *   The date or date-time expression from which to extract the second.
   */
  def SECOND(
    time: LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"SECOND('${ time.toString.replaceAll("T", " ") }')")

  /**
   * Function to calculate the difference time from a date/time or time type value.
   *
   * {{{
   *   TableQuery[DateTime].select(p => SEC_TO_TIME(p.start, p.end))
   *   // SELECT SEC_TO_TIME(start, end) FROM date_time
   * }}}
   *
   * @param time
   *   The start time.
   * @param interval
   *   The end time.
   */
  def SUBTIME[
    A <: LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime],
    B <: LocalTime | Option[LocalTime]
  ](
    time:     Column[A],
    interval: Column[B]
  )(using Decoder[LocalTime], Encoder[LocalTime]): Column[LocalTime] =
    Column.function(s"SUBTIME(${ time.name }, ${ interval.name })")

  /**
   * Function to calculate the difference time from a date/time or time type value.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => SUBTIME(LocalDateTime.of(2021, 1, 1, 0, 0), LocalDateTime.of(2021, 1, 1, 0, 0)))
   *   // SELECT SUBTIME('2021-01-01 00:00', '2021-01-01 00:00') FROM date_time
   * }}}
   *
   * @param time
   *   The start time.
   * @param interval
   *   The end time.
   */
  def SUBTIME(
    time:     LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime,
    interval: LocalTime
  )(using Decoder[LocalTime], Encoder[LocalTime]): Column[LocalTime] =
    Column.function(s"SUBTIME('${ time.toString.replaceAll("T", " ") }', '${ interval.toString }')")

  /**
   * Function to return the current date and time in 'YYYY-MM-DD hh:mm:ss' format.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => SYSDATE)
   *   // SELECT SYSDATE() FROM date_time
   * }}}
   */
  def SYSDATE()(using Decoder[LocalDateTime], Encoder[LocalDateTime]): Column[LocalDateTime] =
    Column.function("SYSDATE()")

  /**
   * Function to extract only the time portion from a time or date-time expression value.
   *
   * {{{
   *   TableQuery[DateTime].select(p => TIME(p.birthDate))
   *   // SELECT TIME(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *  The column from which to extract the time.
   */
  def TIME[
    A <: LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](column: Column[A])(using Decoder[LocalTime], Encoder[LocalTime]): Column[LocalTime] =
    Column.function(s"TIME(${ column.name })")

  /**
   * Function to extract only the time portion from a time or date-time expression value.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => TIME(LocalDateTime.of(2021, 1, 1, 0, 0)))
   *   // SELECT TIME('2021-01-01 00:00') FROM date_time
   * }}}
   * @param time
   *   The time or date-time expression from which to extract the time.
   */
  def TIME(
    time: LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[LocalTime], Encoder[LocalTime]): Column[LocalTime] =
    Column.function(s"TIME('${ time.toString.replaceAll("T", " ") }')")

  /**
   * Function to convert the time portion from a date type value to seconds.
   *
   * {{{
   *   TableQuery[DateTime].select(p => TIME_TO_SEC(p.birthDate))
   *   // SELECT TIME_TO_SEC(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *   The column from which to extract the time.
   */
  def TIME_TO_SEC[
    A <: LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](column: Column[A])(using Decoder[Long], Encoder[Long]): Column[Long] =
    Column.function(s"TIME_TO_SEC(${ column.name })")

  /**
   * Function to convert the time portion from a date type value to seconds.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => TIME_TO_SEC(LocalDateTime.of(2021, 1, 1, 0, 0)))
   *   // SELECT TIME_TO_SEC('2021-01-01 00:00') FROM date_time
   * }}}
   *
   * @param time
   *   The time or date-time expression from which to extract the time.
   */
  def TIME_TO_SEC(
    time: LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[Long], Encoder[Long]): Column[Long] =
    Column.function(s"TIME_TO_SEC('${ time.toString.replaceAll("T", " ") }')")

  /**
   * A function that calculates the difference of the time portion from a date/time type value and returns the result as a time type.
   *
   * {{{
   *   TableQuery[DateTime].select(p => TIMEDIFF(p.start, p.end))
   *   // SELECT TIMEDIFF(start, end) FROM date_time
   * }}}
   *
   * @param start
   *   The start time.
   * @param end
   *   The end time.
   */
  def TIMEDIFF[
    A <: LocalDateTime | OffsetDateTime | ZonedDateTime | Option[LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    start: Column[A],
    end:   Column[A]
  )(using Decoder[LocalTime], Encoder[LocalTime]): Column[LocalTime] =
    Column.function(s"TIMEDIFF(${ start.name }, ${ end.name })")

  /**
   * A function that calculates the difference of the time portion from a date/time type value and returns the result as a time type.
   *
   * {{{
   *  TableQuery[DateTime].select(p => TIMEDIFF(p.start, LocalDateTime.of(2021, 1, 1, 0, 0)))
   *  // SELECT TIMEDIFF(start, '2021-01-01 00:00') FROM date_time
   * }}}
   *
   * @param start
   *   The start time.
   * @param end
   *   The end time.
   */
  def TIMEDIFF[
    A <: LocalDateTime | OffsetDateTime | ZonedDateTime | Option[LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    start: Column[A],
    end:   LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[LocalTime], Encoder[LocalTime]): Column[LocalTime] =
    Column.function(s"TIMEDIFF(${ start.name }, '${ end.toString.replaceAll("T", " ") }')")

  /**
   * A function that calculates the difference of the time portion from a date/time type value and returns the result as a time type.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => TIMEDIFF(LocalDateTime.of(2021, 1, 1, 0, 0), LocalDateTime.of(2021, 1, 1, 0, 0)))
   *   // SELECT TIMEDIFF('2021-01-01 00:00', '2021-01-01 00:00') FROM date_time
   * }}}
   *
   * @param start
   *   The start time.
   * @param end
   *  The end time.
   */
  def TIMEDIFF[A <: LocalDateTime | OffsetDateTime | ZonedDateTime](
    start: A,
    end:   A
  )(using Decoder[LocalTime], Encoder[LocalTime]): Column[LocalTime] =
    Column.function(s"TIMEDIFF('${ start.toString.replaceAll("T", " ") }', '${ end.toString.replaceAll("T", " ") }')")

  /**
   * Function to return a date or date-time format value as a date-time value.
   *
   * {{{
   *   TableQuery[DateTime].select(p => TIMESTAMP(p.birthDate))
   *   // SELECT TIMESTAMP(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *   The column from which to extract the date-time value.
   */
  def TIMESTAMP[A <: LocalDate | Option[LocalDate]](
    column: Column[A]
  )(using Decoder[Option[LocalDateTime]], Encoder[Option[LocalDateTime]]): Column[Option[LocalDateTime]] =
    Column.function(s"TIMESTAMP(${ column.name })")

  /**
   * Function to return a date or date-time format value as a date-time value.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => TIMESTAMP(LocalDate.of(2021, 1, 1)))
   *   // SELECT TIMESTAMP('2021-01-01') FROM date_time
   * }}}
   *
   * @param date
   *   The date or date-time expression from which to extract the date-time value.
   */
  def TIMESTAMP[A <: LocalDate](
    date: A
  )(using Decoder[Option[LocalDateTime]], Encoder[Option[LocalDateTime]]): Column[Option[LocalDateTime]] =
    Column.function(s"TIMESTAMP('${ date.toString }')")

  /**
   * Function that adds the value of a time expression to the value of a date or date-time expression and returns the result as a date-time value.
   * 
   * {{{
   *   TableQuery[DateTime].select(p => TIMESTAMP(p.birthDate, p.birthTime))
   *   // SELECT TIMESTAMP(birth_date, birth_time) FROM date_time
   * }}}
   * 
   * @param column1
   *   The date or date-time expression.
   * @param column2
   *   The time expression.
   */
  def TIMESTAMP[A <: LocalDateTime | Option[LocalDateTime], B <: LocalTime | Option[LocalTime]](
    column1: Column[A],
    column2: Column[B]
  )(using Decoder[Option[LocalDateTime]], Encoder[Option[LocalDateTime]]): Column[Option[LocalDateTime]] =
    Column.function(s"TIMESTAMP(${ column1.name }, ${ column2.name })")

  /**
   * Function to add an interval to a date or datetime expression.
   *
   * {{{
   *   TableQuery[DateTime].select(p => TIMESTAMPADD(DateTime.TimeUnit.MINUTE, 1, p.timestamp))
   *   // SELECT TIMESTAMPADD(MINUTE, 1, timestamp) FROM date_time
   * }}}
   *
   * @param unit
   *   The time unit specifying how to add the interval.
   * @param interval
   *   The number of units to add.
   * @param column
   *   The date or datetime column to which the interval will be added.
   */
  def TIMESTAMPADD[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    unit:     DateTime.TimeUnit,
    interval: Int,
    column:   Column[A]
  ): Column[A] =
    Column
      .function(s"TIMESTAMPADD(${ unit.toString }, $interval, ${ column.name })")(using column.decoder, column.encoder)

  /**
   * Function to add an interval to a date or datetime expression.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => TIMESTAMPADD(DateTime.TimeUnit.MINUTE, 1, LocalDateTime.of(2003, 1, 2, 0, 0)))
   *   // SELECT TIMESTAMPADD(MINUTE, 1, '2003-01-02 00:00:00') FROM date_time
   * }}}
   *
   * @param unit
   *   The time unit specifying how to add the interval.
   * @param interval
   *   The number of units to add.
   * @param datetime
   *   The date or datetime expression to which the interval will be added.
   */
  def TIMESTAMPADD(
    unit:     DateTime.TimeUnit,
    interval: Int,
    datetime: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[LocalDateTime], Encoder[LocalDateTime]): Column[LocalDateTime] =
    Column.function(s"TIMESTAMPADD(${ unit.toString }, $interval, '${ datetime.toString.replaceAll("T", " ") }')")

  /**
   * Function to calculate the difference between two date or datetime expressions.
   *
   * {{{
   *   TableQuery[DateTime].select(p => TIMESTAMPDIFF(DateTime.TimeUnit.MONTH, p.startDate, p.endDate))
   *   // SELECT TIMESTAMPDIFF(MONTH, start_date, end_date) FROM date_time
   * }}}
   *
   * @param unit
   *   The time unit for the difference calculation.
   * @param from
   *   The starting date or datetime expression.
   * @param to
   *   The ending date or datetime expression.
   */
  def TIMESTAMPDIFF[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime],
    B <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    unit: DateTime.TimeUnit,
    from: Column[A],
    to:   Column[B]
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"TIMESTAMPDIFF(${ unit.toString }, ${ from.name }, ${ to.name })")

  /**
   * Function to calculate the difference between two date or datetime expressions.
   *
   * {{{
   *   TableQuery[DateTime].select(p => TIMESTAMPDIFF(DateTime.TimeUnit.MONTH, LocalDate.of(2003, 2, 1), p.endDate))
   *   // SELECT TIMESTAMPDIFF(MONTH, '2003-02-01', end_date) FROM date_time
   * }}}
   *
   * @param unit
   *   The time unit for the difference calculation.
   * @param from
   *   The starting date or datetime expression.
   * @param to
   *   The ending date or datetime column.
   */
  def TIMESTAMPDIFF[
    B <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    unit: DateTime.TimeUnit,
    from: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime,
    to:   Column[B]
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"TIMESTAMPDIFF(${ unit.toString }, '${ from.toString.replaceAll("T", " ") }', ${ to.name })")

  /**
   * Function to calculate the difference between two date or datetime expressions.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => TIMESTAMPDIFF(DateTime.TimeUnit.MONTH, LocalDate.of(2003, 2, 1), LocalDate.of(2003, 5, 1)))
   *   // SELECT TIMESTAMPDIFF(MONTH, '2003-02-01', '2003-05-01') FROM date_time
   * }}}
   *
   * @param unit
   *   The time unit for the difference calculation.
   * @param from
   *   The starting date or datetime expression.
   * @param to
   *   The ending date or datetime expression.
   */
  def TIMESTAMPDIFF(
    unit: DateTime.TimeUnit,
    from: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime,
    to:   LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(
      s"TIMESTAMPDIFF(${ unit.toString }, '${ from.toString.replaceAll("T", " ") }', '${ to.toString.replaceAll("T", " ") }')"
    )

  /**
   * Function to convert a string to a date according to a format string.
   *
   * {{{
   *   TableQuery[DateTime].select(p => STR_TO_DATE(p.dateString, "%d,%m,%Y"))
   *   // SELECT STR_TO_DATE(date_string, '%d,%m,%Y') FROM date_time
   * }}}
   *
   * @param column
   *   The string column to be converted to a date.
   * @param format
   *   The format string specifying how to parse the input string.
   */
  def STR_TO_DATE[A <: String | Option[String]](
    column: Column[A],
    format: String
  )(using Decoder[Option[LocalDateTime]], Encoder[Option[LocalDateTime]]): Column[Option[LocalDateTime]] =
    Column.function(s"STR_TO_DATE(${ column.name }, '$format')")

  /**
   * Function to convert a string to a date according to a format string.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => STR_TO_DATE("01,5,2013", "%d,%m,%Y"))
   *   // SELECT STR_TO_DATE('01,5,2013', '%d,%m,%Y') FROM date_time
   * }}}
   *
   * @param str
   *   The input string to be converted to a date.
   * @param format
   *   The format string specifying how to parse the input string.
   */
  def STR_TO_DATE(
    str:    String,
    format: String
  )(using Decoder[Option[LocalDateTime]], Encoder[Option[LocalDateTime]]): Column[Option[LocalDateTime]] =
    Column.function(s"STR_TO_DATE('$str', '$format')")

  /**
   * Function to format a time value according to the format string.
   * @see https://dev.mysql.com/doc/refman/8.0/en/date-and-time-functions.html#function_time-format
   *
   * {{{
   *   TableQuery[DateTime].select(p => TIME_FORMAT(p.time, "%H:%i:%s"))
   *   // SELECT TIME_FORMAT(time, '%H:%i:%s') FROM date_time
   * }}}
   *
   * @param column
   *   The time column to be formatted.
   * @param format
   *   The format string.
   */
  def TIME_FORMAT[
    A <: LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A],
    format: String
  )(using Decoder[String], Encoder[String]): Column[String] =
    Column.function(s"TIME_FORMAT(${ column.name }, '$format')")

  /**
   * Function to format a time value according to the format string.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => TIME_FORMAT(LocalTime.of(10, 15, 30), "%H:%i:%s"))
   *   // SELECT TIME_FORMAT('10:15:30', '%H:%i:%s') FROM date_time
   * }}}
   *
   * @param time
   *   The time or date-time expression to be formatted.
   * @param format
   *   The format string.
   */
  def TIME_FORMAT(
    time:   LocalTime | LocalDateTime | OffsetDateTime | ZonedDateTime,
    format: String
  )(using Decoder[String], Encoder[String]): Column[String] =
    Column.function(s"TIME_FORMAT('${ time.toString.replaceAll("T", " ") }', '$format')")

  /**
   * Function to return the number of days since year 0.
   *
   * Use TO_DAYS() carefully with older dates. It is not designed to be used with values prior to the advent of the Gregorian calendar (1582).
   *
   * @see https://dev.mysql.com/doc/refman/8.0/en/mysql-calendar.html
   *
   * {{{
   *   TableQuery[DateTime].select(p => TO_DAYS(p.birthDate))
   *   // SELECT TO_DAYS(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *   The date column to convert to days.
   */
  def TO_DAYS[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[Option[Long]], Encoder[Option[Long]]): Column[Option[Long]] =
    Column.function(s"TO_DAYS(${ column.name })")

  /**
   * Function to return the number of days since year 0.
   *
   * Use TO_DAYS() carefully with older dates. It is not designed to be used with values prior to the advent of the Gregorian calendar (1582).
   *
   * @see https://dev.mysql.com/doc/refman/8.0/en/mysql-calendar.html
   *
   * {{{
   *   TableQuery[DateTime].select(_ => TO_DAYS(LocalDate.of(2007, 10, 7)))
   *   // SELECT TO_DAYS('2007-10-07') FROM date_time
   * }}}
   *
   * @param date
   *   The date or date-time expression to convert to days.
   */
  def TO_DAYS(
    date: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[Option[Long]], Encoder[Option[Long]]): Column[Option[Long]] =
    Column.function(s"TO_DAYS('${ date.toString }')")

  /**
   * Function to return the number of seconds since year 0.
   *
   * Use TO_SECONDS() carefully with older dates. It is not designed to be used with values prior to the advent of the Gregorian calendar (1582).
   *
   * @see https://dev.mysql.com/doc/refman/8.0/en/mysql-calendar.html
   *
   * {{{
   *   TableQuery[DateTime].select(p => TO_SECONDS(p.timestamp))
   *   // SELECT TO_SECONDS(timestamp) FROM date_time
   * }}}
   *
   * @param column
   *   The date or datetime column to convert to seconds.
   */
  def TO_SECONDS[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[Option[Long]], Encoder[Option[Long]]): Column[Option[Long]] =
    Column.function(s"TO_SECONDS(${ column.name })")

  /**
   * Function to return the number of seconds since year 0.
   *
   * Use TO_SECONDS() carefully with older dates. It is not designed to be used with values prior to the advent of the Gregorian calendar (1582).
   *
   * @see https://dev.mysql.com/doc/refman/8.0/en/mysql-calendar.html
   *
   * {{{
   *   TableQuery[DateTime].select(_ => TO_SECONDS(LocalDateTime.of(2009, 11, 29, 13, 43, 32)))
   *   // SELECT TO_SECONDS('2009-11-29 13:43:32') FROM date_time
   * }}}
   *
   * @param datetime
   *   The date or date-time expression to convert to seconds.
   */
  def TO_SECONDS(
    datetime: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[Option[Long]], Encoder[Option[Long]]): Column[Option[Long]] =
    Column.function(s"TO_SECONDS('${ datetime.toString.replaceAll("T", " ") }')")

  /**
   * Function to return a Unix timestamp.
   * Without arguments, returns the current Unix timestamp (seconds since '1970-01-01 00:00:00' UTC).
   *
   * {{{
   *   TableQuery[DateTime].select(_ => UNIX_TIMESTAMP())
   *   // SELECT UNIX_TIMESTAMP() FROM date_time
   * }}}
   */
  def UNIX_TIMESTAMP()(using Decoder[Long], Encoder[Long]): Column[Long] =
    Column.function("UNIX_TIMESTAMP()")

  /**
   * Function to return a Unix timestamp for a given date.
   * Returns seconds since '1970-01-01 00:00:00' UTC for the given date.
   *
   * {{{
   *   TableQuery[DateTime].select(p => UNIX_TIMESTAMP(p.timestamp))
   *   // SELECT UNIX_TIMESTAMP(timestamp) FROM date_time
   * }}}
   *
   * @param column
   *   The date or datetime column to convert to Unix timestamp.
   */
  def UNIX_TIMESTAMP[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[Option[Long]], Encoder[Option[Long]]): Column[Option[Long]] =
    Column.function(s"UNIX_TIMESTAMP(${ column.name })")

  /**
   * Function to return a Unix timestamp for a given date.
   * Returns seconds since '1970-01-01 00:00:00' UTC for the given date.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => UNIX_TIMESTAMP(LocalDateTime.of(2015, 11, 13, 10, 20, 19)))
   *   // SELECT UNIX_TIMESTAMP('2015-11-13 10:20:19') FROM date_time
   * }}}
   *
   * @param datetime
   *   The date or date-time expression to convert to Unix timestamp.
   */
  def UNIX_TIMESTAMP(
    datetime: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[Long], Encoder[Long]): Column[Long] =
    Column.function(s"UNIX_TIMESTAMP('${ datetime.toString.replaceAll("T", " ") }')")

  /**
   * Function to return the current UTC date in 'YYYY-MM-DD' format.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => UTC_DATE())
   *   // SELECT UTC_DATE() FROM date_time
   * }}}
   */
  def UTC_DATE()(using Decoder[LocalDate], Encoder[LocalDate]): Column[LocalDate] =
    Column.function("UTC_DATE()")

  /**
   * Function to return the current UTC time in 'HH:MM:SS' format.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => UTC_TIME())
   *   // SELECT UTC_TIME() FROM date_time
   * }}}
   */
  def UTC_TIME()(using Decoder[LocalTime], Encoder[LocalTime]): Column[LocalTime] =
    Column.function("UTC_TIME()")

  /**
   * Function to return the current UTC time with fractional seconds precision.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => UTC_TIME(6))
   *   // SELECT UTC_TIME(6) FROM date_time
   * }}}
   *
   * @param fsp
   *   The fractional seconds precision (0-6).
   */
  def UTC_TIME(fsp: Int)(using Decoder[LocalTime], Encoder[LocalTime]): Column[LocalTime] =
    require(fsp >= 0 && fsp <= 6, "Fractional seconds precision must be between 0 and 6")
    Column.function(s"UTC_TIME($fsp)")

  /**
   * Function to return the current UTC date and time in 'YYYY-MM-DD HH:MM:SS' format.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => UTC_TIMESTAMP())
   *   // SELECT UTC_TIMESTAMP() FROM date_time
   * }}}
   */
  def UTC_TIMESTAMP()(using Decoder[LocalDateTime], Encoder[LocalDateTime]): Column[LocalDateTime] =
    Column.function("UTC_TIMESTAMP()")

  /**
   * Function to return the current UTC date and time with fractional seconds precision.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => UTC_TIMESTAMP(6))
   *   // SELECT UTC_TIMESTAMP(6) FROM date_time
   * }}}
   *
   * @param fsp
   *   The fractional seconds precision (0-6).
   */
  def UTC_TIMESTAMP(fsp: Int)(using Decoder[LocalDateTime], Encoder[LocalDateTime]): Column[LocalDateTime] =
    require(fsp >= 0 && fsp <= 6, "Fractional seconds precision must be between 0 and 6")
    Column.function(s"UTC_TIMESTAMP($fsp)")

  /**
   * Function to return the week number for a date.
   *
   * {{{
   *   TableQuery[DateTime].select(p => WEEK(p.birthDate))
   *   // SELECT WEEK(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *   The date or date-time column from which to extract the week number.
   */
  def WEEK[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"WEEK(${ column.name })")

  /**
   * Function to return the week number for a date with mode specification.
   *
   * {{{
   *   TableQuery[DateTime].select(p => WEEK(p.birthDate, 0))
   *   // SELECT WEEK(birth_date, 0) FROM date_time
   * }}}
   *
   * @param column
   *   The date or date-time column from which to extract the week number.
   * @param mode
   *   The mode parameter for week calculation (0-7).
   */
  def WEEK[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A],
    mode:   Int
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    require(mode >= 0 && mode <= 7, "Week mode must be between 0 and 7")
    Column.function(s"WEEK(${ column.name }, $mode)")

  /**
   * Function to return the week number for a date.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => WEEK(LocalDate.of(2008, 2, 20)))
   *   // SELECT WEEK('2008-02-20') FROM date_time
   * }}}
   *
   * @param date
   *   The date or date-time expression from which to extract the week number.
   */
  def WEEK(
    date: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"WEEK('${ date.toString }')")

  /**
   * Function to return the week number for a date with mode specification.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => WEEK(LocalDate.of(2008, 2, 20), 1))
   *   // SELECT WEEK('2008-02-20', 1) FROM date_time
   * }}}
   *
   * @param date
   *   The date or date-time expression from which to extract the week number.
   * @param mode
   *   The mode parameter for week calculation (0-7).
   */
  def WEEK(
    date: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime,
    mode: Int
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    require(mode >= 0 && mode <= 7, "Week mode must be between 0 and 7")
    Column.function(s"WEEK('${ date.toString }', $mode)")

  /**
   * Function to return the weekday index for a date.
   * 0 = Monday, 1 = Tuesday, ... 6 = Sunday
   *
   * {{{
   *   TableQuery[DateTime].select(p => WEEKDAY(p.birthDate))
   *   // SELECT WEEKDAY(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *   The date or date-time column from which to extract the weekday index.
   */
  def WEEKDAY[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"WEEKDAY(${ column.name })")

  /**
   * Function to return the weekday index for a date.
   * 0 = Monday, 1 = Tuesday, ... 6 = Sunday
   *
   * {{{
   *   TableQuery[DateTime].select(_ => WEEKDAY(LocalDate.of(2008, 2, 3)))
   *   // SELECT WEEKDAY('2008-02-03') FROM date_time
   * }}}
   *
   * @param date
   *   The date or date-time expression from which to extract the weekday index.
   */
  def WEEKDAY(
    date: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"WEEKDAY('${ date.toString }')")

  /**
   * Function to return the calendar week of the date as a number in the range from 1 to 53.
   * WEEKOFYEAR() is a compatibility function that is equivalent to WEEK(date,3).
   *
   * {{{
   *   TableQuery[DateTime].select(p => WEEKOFYEAR(p.birthDate))
   *   // SELECT WEEKOFYEAR(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *   The date or date-time column from which to extract the week of year.
   */
  def WEEKOFYEAR[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"WEEKOFYEAR(${ column.name })")

  /**
   * Function to return the calendar week of the date as a number in the range from 1 to 53.
   * WEEKOFYEAR() is a compatibility function that is equivalent to WEEK(date,3).
   *
   * {{{
   *   TableQuery[DateTime].select(_ => WEEKOFYEAR(LocalDate.of(2008, 2, 20)))
   *   // SELECT WEEKOFYEAR('2008-02-20') FROM date_time
   * }}}
   *
   * @param date
   *   The date or date-time expression from which to extract the week of year.
   */
  def WEEKOFYEAR(
    date: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"WEEKOFYEAR('${ date.toString }')")

  /**
   * Function to return year and week for a date.
   * The year in the result may be different from the year in the date argument for the first and the last week of the year.
   *
   * {{{
   *   TableQuery[DateTime].select(p => YEARWEEK(p.birthDate))
   *   // SELECT YEARWEEK(birth_date) FROM date_time
   * }}}
   *
   * @param column
   *   The date or date-time column from which to extract the year and week.
   */
  def YEARWEEK[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"YEARWEEK(${ column.name })")

  /**
   * Function to return year and week for a date with mode specification.
   *
   * {{{
   *   TableQuery[DateTime].select(p => YEARWEEK(p.birthDate, 0))
   *   // SELECT YEARWEEK(birth_date, 0) FROM date_time
   * }}}
   *
   * @param column
   *   The date or date-time column from which to extract the year and week.
   * @param mode
   *   The mode parameter for week calculation (0-7).
   */
  def YEARWEEK[
    A <: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A],
    mode:   Int
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    require(mode >= 0 && mode <= 7, "Week mode must be between 0 and 7")
    Column.function(s"YEARWEEK(${ column.name }, $mode)")

  /**
   * Function to return year and week for a date.
   * The year in the result may be different from the year in the date argument for the first and the last week of the year.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => YEARWEEK(LocalDate.of(1987, 1, 1)))
   *   // SELECT YEARWEEK('1987-01-01') FROM date_time
   * }}}
   *
   * @param date
   *   The date or date-time expression from which to extract the year and week.
   */
  def YEARWEEK(
    date: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column.function(s"YEARWEEK('${ date.toString }')")

  /**
   * Function to return year and week for a date with mode specification.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => YEARWEEK(LocalDate.of(1987, 1, 1), 0))
   *   // SELECT YEARWEEK('1987-01-01', 0) FROM date_time
   * }}}
   *
   * @param date
   *   The date or date-time expression from which to extract the year and week.
   * @param mode
   *   The mode parameter for week calculation (0-7).
   */
  def YEARWEEK(
    date: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime,
    mode: Int
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    require(mode >= 0 && mode <= 7, "Week mode must be between 0 and 7")
    Column.function(s"YEARWEEK('${ date.toString }', $mode)")

object DateTime:

  /**
   * Extracts the date part of a date or datetime expression.
   *
   * @see https://dev.mysql.com/doc/refman/8.0/en/expressions.html#temporal-intervals
   *
   *  {{{
   *    INTERVAL expr unit
   *  }}}
   */
  enum Interval[A](expr: A, val unit: String):
    def statement: String = s"INTERVAL $expr $unit"
    case MICROSECOND(expr: Int)           extends Interval(expr, "MICROSECOND")
    case SECOND(expr: Int)                extends Interval(expr, "SECOND")
    case MINUTE(expr: Int)                extends Interval(expr, "MINUTE")
    case HOUR(expr: Int)                  extends Interval(expr, "HOUR")
    case DAY(expr: Int)                   extends Interval(expr, "DAY")
    case WEEK(expr: Int)                  extends Interval(expr, "WEEK")
    case MONTH(expr: Int)                 extends Interval(expr, "MONTH")
    case QUARTER(expr: Int)               extends Interval(expr, "QUARTER")
    case YEAR(expr: Int)                  extends Interval(expr, "YEAR")
    case SECOND_MICROSECOND(expr: String) extends Interval(expr, "SECOND_MICROSECOND")
    case MINUTE_MICROSECOND(expr: String) extends Interval(expr, "MINUTE_MICROSECOND")
    case MINUTE_SECOND(expr: String)      extends Interval(expr, "MINUTE_SECOND")
    case HOUR_MICROSECOND(expr: String)   extends Interval(expr, "HOUR_MICROSECOND")
    case HOUR_SECOND(expr: String)        extends Interval(expr, "HOUR_SECOND")
    case HOUR_MINUTE(expr: String)        extends Interval(expr, "HOUR_MINUTE")
    case DAY_MICROSECOND(expr: String)    extends Interval(expr, "DAY_MICROSECOND")
    case DAY_SECOND(expr: String)         extends Interval(expr, "DAY_SECOND")
    case DAY_MINUTE(expr: String)         extends Interval(expr, "DAY_MINUTE")
    case DAY_HOUR(expr: String)           extends Interval(expr, "DAY_HOUR")
    case YEAR_MONTH(expr: YearMonth)      extends Interval(expr, "YEAR_MONTH")

  /**
   * Time unit for the INTERVAL expression.
   */
  enum TimeUnit:
    case MICROSECOND, SECOND, MINUTE, HOUR, DAY, WEEK, MONTH, QUARTER, YEAR, SECOND_MICROSECOND, MINUTE_MICROSECOND,
      MINUTE_SECOND, HOUR_MICROSECOND, HOUR_SECOND, HOUR_MINUTE, DAY_MICROSECOND, DAY_SECOND, DAY_MINUTE, DAY_HOUR,
      YEAR_MONTH

  /**
   * Date type for the GET_FORMAT expression.
   */
  enum DateType:
    case DATE, TIME, DATETIME

  /**
   * Format type for the GET_FORMAT expression.
   */
  enum FormatType:
    case EUR, USA, JIS, ISO, INTERNAL
