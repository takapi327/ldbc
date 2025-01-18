package ldbc.statement.functions

import java.time.*

/**
 * Provide functions that can be used to manipulate temporal values provided by MySQL.
 *
 * @see https://dev.mysql.com/doc/refman/8.0/en/date-and-time-functions.html
 */
trait DateTime
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
