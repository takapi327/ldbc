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
   *   TableQuery[Person].select(p => ADDDATE(p.birthDate, DateTime.Interval.YEAR(1)))
   *   // SELECT ADDDATE(birth_date, INTERVAL 1 YEAR) FROM person
   * }}}
   */
  def ADDDATE[A <: LocalDate | Option[LocalDate]](column: Column[A], interval: DateTime.Interval[Int]): Column[A] =
    Column(s"ADDDATE(${column.name}, ${interval.statement})")(using column.decoder, column.encoder)

  /**
   * Function to perform addition on a specified date type column.
   *
   * {{{
   *   TableQuery[Person].select(p => ADDDATE(LocalDate.now, DateTime.Interval.YEAR(1)))
   *   // SELECT ADDDATE('2008-02-02', INTERVAL 1 YEAR) FROM person
   * }}}
   */
  def ADDDATE(date: LocalDate, interval: DateTime.Interval[Int])(using Decoder[LocalDate], Encoder[LocalDate]): Column[LocalDate] =
    Column(s"ADDDATE('${date.toString}', ${interval.statement})")

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
    case MICROSECOND(expr: Int) extends Interval(expr, "MICROSECOND")
    case SECOND(expr: Int) extends Interval(expr, "SECOND")
    case MINUTE(expr: Int) extends Interval(expr, "MINUTE")
    case HOUR(expr: Int) extends Interval(expr, "HOUR")
    case DAY(expr: Int) extends Interval(expr, "DAY")
    case WEEK(expr: Int) extends Interval(expr, "WEEK")
    case MONTH(expr: Int) extends Interval(expr, "MONTH")
    case QUARTER(expr: Int) extends Interval(expr, "QUARTER")
    case YEAR(expr: Int) extends Interval(expr, "YEAR")
    case SECOND_MICROSECOND(expr: String) extends Interval(expr, "SECOND_MICROSECOND")
    case MINUTE_MICROSECOND(expr: String) extends Interval(expr, "MINUTE_MICROSECOND")
    case MINUTE_SECOND(expr: String) extends Interval(expr, "MINUTE_SECOND")
    case HOUR_MICROSECOND(expr: String) extends Interval(expr, "HOUR_MICROSECOND")
    case HOUR_SECOND(expr: String) extends Interval(expr, "HOUR_SECOND")
    case HOUR_MINUTE(expr: String) extends Interval(expr, "HOUR_MINUTE")
    case DAY_MICROSECOND(expr: String) extends Interval(expr, "DAY_MICROSECOND")
    case DAY_SECOND(expr: String) extends Interval(expr, "DAY_SECOND")
    case DAY_MINUTE(expr: String) extends Interval(expr, "DAY_MINUTE")
    case DAY_HOUR(expr: String) extends Interval(expr, "DAY_HOUR")
    case YEAR_MONTH(expr: YearMonth) extends Interval(expr, "YEAR_MONTH")
