/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
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
    Column(s"DATE_ADD(${ column.name }, ${ interval.statement })")(using column.decoder, column.encoder)

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
    Column(s"DATE_ADD('${ date.toString }', ${ interval.statement })")

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
    Column(s"DATE_SUB(${ column.name }, ${ interval.statement })")(using column.decoder, column.encoder)

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
    Column(s"DATE_SUB('${ date.toString }', ${ interval.statement })")

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
    Column(s"ADDTIME(${ column.name }, '${ time.toString }')")(using column.decoder, column.encoder)

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
    Column(s"ADDTIME('${ dateTime.toString }', '${ time.toString }')")

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
    Column(
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
    Column(
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
  def CURDATE(using Decoder[LocalDate], Encoder[LocalDate]): Column[LocalDate] = Column("CURDATE()")

  /**
   * Function to return the current time in 'hh:mm:ss' format.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => CURTIME)
   *   // SELECT CURTIME() FROM date_time
   * }}}
   */
  def CURTIME(using Decoder[LocalTime], Encoder[LocalTime]): Column[LocalTime] = Column("CURTIME()")

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
    Column(s"DATE(${ column.name })")

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
    Column(s"DATE('${ date.toString }')")

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
    Column(s"DATE_FORMAT(${ column.name }, '$format')")

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
    Column(s"DATE_FORMAT('${ date.toString }', '$format')")

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
    Column(s"DATEDIFF(${ from.name }, ${ to.name })")

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
    Column(s"DATEDIFF(${ from.name }, '${ to.toString }')")

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
    Column(s"DATEDIFF('${ from.toString }', '${ to.toString }')")

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
    Column(s"DAYNAME(${ column.name })")

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
    Column(s"DAYNAME('${ date.toString }')")

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
    Column(s"DAYOFMONTH(${ column.name })")

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
    Column(s"DAYOFMONTH('${ date.toString }')")

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
    Column(s"DAYOFWEEK(${ column.name })")

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
    Column(s"DAYOFWEEK('${ date.toString }')")

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
    Column(s"DAYOFYEAR(${ column.name })")

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
    Column(s"DAYOFYEAR('${ date.toString }')")

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
    Column(s"EXTRACT(${ timeUnit.toString } FROM ${ column.name })")

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
    Column(s"EXTRACT(${ timeUnit.toString } FROM '${ date.toString.replaceAll("T", " ") }')")

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
    Column(s"FROM_DAYS(${ column.name })")

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
    Column(s"FROM_DAYS($days)")

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
    Column(s"FROM_UNIXTIME(${ column.name })")

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
    Column(s"FROM_UNIXTIME($timestamp)")

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
    Column(s"HOUR(${ column.name })")

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
    Column(s"HOUR('${ date.toString.replaceAll("T", " ") }')")

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
    Column(s"LAST_DAY(${ column.name })")

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
    Column(s"LAST_DAY('${ date.toString }')")

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
    day: Int
  )(using Decoder[LocalDate], Encoder[LocalDate]): Column[LocalDate] =
    require(day > 0, "The annual total must be greater than 0.")
    Column(s"MAKEDATE(${ column.name }, $day)")

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
    Column(s"MAKEDATE($year, $day)")

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
    hour: Column[A],
    minute: Column[B],
    second: Column[C]
  )(using Decoder[LocalTime], Encoder[LocalTime]): Column[LocalTime] =
    Column(s"MAKETIME(${hour.name}, ${minute.name}, ${second.name})")

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
    hour: Int | Long,
    minute: Int | Long,
    second: Int | Long
  )(using Decoder[LocalTime], Encoder[LocalTime]): Column[LocalTime] =
    Column(s"MAKETIME($hour, $minute, $second)")

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
    A <: LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    column: Column[A]
  )(using Decoder[Int], Encoder[Int]): Column[Int] =
    Column(s"MICROSECOND(${ column.name })")

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
    Column(s"MICROSECOND('${ datetime.toString.replaceAll("T", " ") }')")

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
    Column(s"MINUTE(${ column.name })")

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
    Column(s"MINUTE('${ time.toString.replaceAll("T", " ") }')")

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
    Column(s"MONTH(${ column.name })")

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
    Column(s"MONTH('${ date.toString.replaceAll("T", " ") }')")

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
    Column(s"MONTHNAME(${ column.name })")

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
    Column(s"MONTHNAME('${ date.toString }')")

  /**
   * Function to return the current date and time in 'YYYY-MM-DD hh:mm:ss' format.
   *
   * {{{
   *   TableQuery[DateTime].select(_ => NOW)
   *   // SELECT NOW() FROM date_time
   * }}}
   */
  def NOW()(using Decoder[LocalDateTime], Encoder[LocalDateTime]): Column[LocalDateTime] = Column("NOW()")

  /**
   * Function to add the specified month to the yyyyMM value.
   *
   * @param period
   *   The period in yyyyMM format.
   * @param months
   *   The number of months to add.
   */
  def PERIOD_ADD(period: YearMonth, months: Int): Column[YearMonth] =
    val formatter = DateTimeFormatter.ofPattern("yyyyMM")
    given Codec[YearMonth] = Codec[String].imap { str =>
      YearMonth.parse(str, formatter)
    } { yearMonth =>
      yearMonth.format(formatter)
    }
    Column(s"PERIOD_ADD(${period.format(formatter)}, $months)")

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
