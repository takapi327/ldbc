/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement.functions

import java.time.*

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
   * @see https://dev.mysql.com/doc/refman/8.0/ja/date-and-time-functions.html#function_date-format
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
   * @see https://dev.mysql.com/doc/refman/8.0/ja/locale-support.html
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
   * @see https://dev.mysql.com/doc/refman/8.0/ja/locale-support.html
   *
   * {{{
   *   TableQuery[DateTime].select(_ => DAYNAME(LocalDate.of(2021, 1, 1)))
   *   // SELECT DAYNAME('2021-01-01') FROM date_time
   * }}}
   * 
   * @param date
   *   The date or date-time expression from which to extract the day of the week.
   */
  def DAYNAME(date: LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime)(using Decoder[String], Encoder[String]): Column[String] =
    Column(s"DAYNAME('${ date.toString }')")

object DateTime:

  /**
   * Extracts the date part of a date or datetime expression.
   *
   * @see https://dev.mysql.com/doc/refman/8.0/ja/expressions.html#temporal-intervals
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
