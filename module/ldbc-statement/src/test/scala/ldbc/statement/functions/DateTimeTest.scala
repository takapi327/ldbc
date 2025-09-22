/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement.functions

import java.time.*

import org.scalatest.flatspec.AnyFlatSpec

import ldbc.statement.functions.DateTime.*
import ldbc.statement.Column

class DateTimeTest extends AnyFlatSpec, DateTime:

  private val c1  = Column.Impl[LocalDate]("local_date")
  private val c2  = Column.Impl[Option[LocalDate]]("local_date")
  private val c3  = Column.Impl[LocalTime]("local_time")
  private val c4  = Column.Impl[Option[LocalTime]]("local_time")
  private val c5  = Column.Impl[LocalDateTime]("local_date_time")
  private val c6  = Column.Impl[Option[LocalDateTime]]("local_date_time")
  private val c7  = Column.Impl[Int]("days")
  private val c8  = Column.Impl[Option[Int]]("days")
  private val c9  = Column.Impl[Year]("year")
  private val c10 = Column.Impl[Option[Year]]("year")
  private val c11 = Column.Impl[Int]("hour")
  private val c12 = Column.Impl[Option[Int]]("hour")
  private val c13 = Column.Impl[Int]("minute")
  private val c14 = Column.Impl[Option[Int]]("minute")
  private val c15 = Column.Impl[Int]("second")
  private val c16 = Column.Impl[Option[Int]]("second")
  private val c17 = Column.Impl[String]("date_string")
  private val c18 = Column.Impl[Option[String]]("date_string")

  it should "Statement generated using the DATE_ADD function matches the specified string." in {
    assert(DATE_ADD(c1, Interval.DAY(1)).name == "DATE_ADD(`local_date`, INTERVAL 1 DAY)")
    assert(DATE_ADD(c1, Interval.MONTH(1)).name == "DATE_ADD(`local_date`, INTERVAL 1 MONTH)")
    assert(DATE_ADD(c1, Interval.YEAR(1)).name == "DATE_ADD(`local_date`, INTERVAL 1 YEAR)")
    assert(DATE_ADD(c2, Interval.DAY(1)).name == "DATE_ADD(`local_date`, INTERVAL 1 DAY)")
    assert(DATE_ADD(c2, Interval.MONTH(1)).name == "DATE_ADD(`local_date`, INTERVAL 1 MONTH)")
    assert(DATE_ADD(c2, Interval.YEAR(1)).name == "DATE_ADD(`local_date`, INTERVAL 1 YEAR)")
    assert(DATE_ADD(LocalDate.of(2021, 1, 1), Interval.DAY(1)).name == "DATE_ADD('2021-01-01', INTERVAL 1 DAY)")
    assert(DATE_ADD(LocalDate.of(2021, 1, 1), Interval.MONTH(1)).name == "DATE_ADD('2021-01-01', INTERVAL 1 MONTH)")
    assert(DATE_ADD(LocalDate.of(2021, 1, 1), Interval.YEAR(1)).name == "DATE_ADD('2021-01-01', INTERVAL 1 YEAR)")
  }

  it should "Statement generated using the DATE_SUB function matches the specified string." in {
    assert(DATE_SUB(c1, Interval.DAY(1)).name == "DATE_SUB(`local_date`, INTERVAL 1 DAY)")
    assert(DATE_SUB(c1, Interval.MONTH(1)).name == "DATE_SUB(`local_date`, INTERVAL 1 MONTH)")
    assert(DATE_SUB(c1, Interval.YEAR(1)).name == "DATE_SUB(`local_date`, INTERVAL 1 YEAR)")
    assert(DATE_SUB(c2, Interval.DAY(1)).name == "DATE_SUB(`local_date`, INTERVAL 1 DAY)")
    assert(DATE_SUB(c2, Interval.MONTH(1)).name == "DATE_SUB(`local_date`, INTERVAL 1 MONTH)")
    assert(DATE_SUB(c2, Interval.YEAR(1)).name == "DATE_SUB(`local_date`, INTERVAL 1 YEAR)")
    assert(DATE_SUB(LocalDate.of(2021, 1, 1), Interval.DAY(1)).name == "DATE_SUB('2021-01-01', INTERVAL 1 DAY)")
    assert(DATE_SUB(LocalDate.of(2021, 1, 1), Interval.MONTH(1)).name == "DATE_SUB('2021-01-01', INTERVAL 1 MONTH)")
    assert(DATE_SUB(LocalDate.of(2021, 1, 1), Interval.YEAR(1)).name == "DATE_SUB('2021-01-01', INTERVAL 1 YEAR)")
  }

  it should "Statement generated using the ADDTIME function matches the specified string." in {
    assert(ADDTIME(c3, LocalTime.of(1, 1, 1, 1)).name == "ADDTIME(`local_time`, '01:01:01.000000001')")
    assert(ADDTIME(c4, LocalTime.of(1, 1, 1, 1)).name == "ADDTIME(`local_time`, '01:01:01.000000001')")
    assert(ADDTIME(LocalTime.of(1, 1, 1), LocalTime.of(1, 1, 1, 1)).name == "ADDTIME('01:01:01', '01:01:01.000000001')")
  }

  it should "Statement generated using the CONVERT_TZ function matches the specified string." in {
    assert(
      CONVERT_TZ(c5, LocalTime.of(0, 0), LocalTime.of(9, 0)).name == "CONVERT_TZ(`local_date_time`, '+0:0', '+9:0')"
    )
    assert(
      CONVERT_TZ(c6, LocalTime.of(0, 0), LocalTime.of(9, 0)).name == "CONVERT_TZ(`local_date_time`, '+0:0', '+9:0')"
    )
    assert(
      CONVERT_TZ(
        LocalDateTime.of(2021, 1, 1, 0, 0),
        LocalTime.of(0, 0),
        LocalTime.of(9, 0)
      ).name == "CONVERT_TZ('2021-01-01T00:00', '+0:0', '+9:0')"
    )
  }

  it should "Statement generated using the CURDATE function matches the specified string." in {
    assert(CURDATE.name == "CURDATE()")
  }

  it should "Statement generated using the CURTIME function matches the specified string." in {
    assert(CURTIME.name == "CURTIME()")
  }

  it should "Statement generated using the DATE function matches the specified string." in {
    assert(DATE(c5).name == "DATE(`local_date_time`)")
    assert(DATE(c6).name == "DATE(`local_date_time`)")
    assert(DATE(LocalDateTime.of(2021, 1, 1, 0, 0)).name == "DATE('2021-01-01T00:00')")
  }

  it should "Statement generated using the DATE_FORMAT function matches the specified string." in {
    assert(DATE_FORMAT(c5, "%Y-%m-%d").name == "DATE_FORMAT(`local_date_time`, '%Y-%m-%d')")
    assert(DATE_FORMAT(c6, "%Y-%m-%d").name == "DATE_FORMAT(`local_date_time`, '%Y-%m-%d')")
    assert(
      DATE_FORMAT(LocalDateTime.of(2021, 1, 1, 0, 0), "%Y-%m-%d").name == "DATE_FORMAT('2021-01-01T00:00', '%Y-%m-%d')"
    )
  }

  it should "Statement generated using the DATEDIFF function matches the specified string." in {
    assert(DATEDIFF(c5, c1).name == "DATEDIFF(`local_date_time`, `local_date`)")
    assert(DATEDIFF(c6, c2).name == "DATEDIFF(`local_date_time`, `local_date`)")
    assert(DATEDIFF(c5, LocalDate.of(2025, 1, 1)).name == "DATEDIFF(`local_date_time`, '2025-01-01')")
    assert(DATEDIFF(c6, LocalDate.of(2025, 1, 1)).name == "DATEDIFF(`local_date_time`, '2025-01-01')")
    assert(DATEDIFF(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)).name == "DATEDIFF('2024-01-01', '2025-01-01')")
  }

  it should "Statement generated using the DAYNAME function matches the specified string." in {
    assert(DAYNAME(c5).name == "DAYNAME(`local_date_time`)")
    assert(DAYNAME(c6).name == "DAYNAME(`local_date_time`)")
    assert(DAYNAME(LocalDate.of(2025, 1, 1)).name == "DAYNAME('2025-01-01')")
  }

  it should "Statement generated using the DAYOFMONTH function matches the specified string." in {
    assert(DAYOFMONTH(c5).name == "DAYOFMONTH(`local_date_time`)")
    assert(DAYOFMONTH(c6).name == "DAYOFMONTH(`local_date_time`)")
    assert(DAYOFMONTH(LocalDate.of(2025, 1, 1)).name == "DAYOFMONTH('2025-01-01')")
  }

  it should "Statement generated using the DAYOFWEEK function matches the specified string." in {
    assert(DAYOFWEEK(c5).name == "DAYOFWEEK(`local_date_time`)")
    assert(DAYOFWEEK(c6).name == "DAYOFWEEK(`local_date_time`)")
    assert(DAYOFWEEK(LocalDate.of(2025, 1, 1)).name == "DAYOFWEEK('2025-01-01')")
  }

  it should "Statement generated using the DAYOFYEAR function matches the specified string." in {
    assert(DAYOFYEAR(c5).name == "DAYOFYEAR(`local_date_time`)")
    assert(DAYOFYEAR(c6).name == "DAYOFYEAR(`local_date_time`)")
    assert(DAYOFYEAR(LocalDate.of(2025, 1, 1)).name == "DAYOFYEAR('2025-01-01')")
  }

  it should "Statement generated using the EXTRACT function matches the specified string." in {
    assert(EXTRACT(c5, TimeUnit.YEAR).name == "EXTRACT(YEAR FROM `local_date_time`)")
    assert(EXTRACT(c6, TimeUnit.MONTH).name == "EXTRACT(MONTH FROM `local_date_time`)")
    assert(EXTRACT(LocalDate.of(2025, 1, 1), TimeUnit.HOUR).name == "EXTRACT(HOUR FROM '2025-01-01')")
  }

  it should "Statement generated using the FROM_DAYS function matches the specified string." in {
    assert(FROM_DAYS(c7).name == "FROM_DAYS(`days`)")
    assert(FROM_DAYS(c8).name == "FROM_DAYS(`days`)")
    assert(FROM_DAYS(730669).name == "FROM_DAYS(730669)")
  }

  it should "Statement generated using the FROM_UNIXTIME function matches the specified string." in {
    assert(FROM_UNIXTIME(c7).name == "FROM_UNIXTIME(`days`)")
    assert(FROM_UNIXTIME(c8).name == "FROM_UNIXTIME(`days`)")
    assert(FROM_UNIXTIME(730669).name == "FROM_UNIXTIME(730669)")
  }

  it should "Statement generated using the HOUR function matches the specified string." in {
    assert(HOUR(c5).name == "HOUR(`local_date_time`)")
    assert(HOUR(c6).name == "HOUR(`local_date_time`)")
    assert(HOUR(LocalDateTime.of(2021, 1, 1, 0, 0)).name == "HOUR('2021-01-01 00:00')")
  }

  it should "Statement generated using the LAST_DAY function matches the specified string." in {
    assert(LAST_DAY(c5).name == "LAST_DAY(`local_date_time`)")
    assert(LAST_DAY(c6).name == "LAST_DAY(`local_date_time`)")
    assert(LAST_DAY(LocalDateTime.of(2021, 1, 1, 0, 0)).name == "LAST_DAY('2021-01-01T00:00')")
  }

  it should "Statement generated using the MAKEDATE function matches the specified string." in {
    assert(MAKEDATE(c9, 31).name == "MAKEDATE(`year`, 31)")
    assert(MAKEDATE(c10, 64).name == "MAKEDATE(`year`, 64)")
    assert(MAKEDATE(2025, 103).name == "MAKEDATE(2025, 103)")
    assert(MAKEDATE(Year.of(2025), 103).name == "MAKEDATE(2025, 103)")
  }

  it should "Statement generated using the MAKETIME function matches the specified string." in {
    assert(MAKETIME(c11, c13, c15).name == "MAKETIME(`hour`, `minute`, `second`)")
    assert(MAKETIME(c12, c14, c16).name == "MAKETIME(`hour`, `minute`, `second`)")
    assert(MAKETIME(24, 59, 59).name == "MAKETIME(24, 59, 59)")
  }

  it should "Statement generated using the MICROSECOND function matches the specified string." in {
    assert(MICROSECOND(c5).name == "MICROSECOND(`local_date_time`)")
    assert(MICROSECOND(c6).name == "MICROSECOND(`local_date_time`)")
    assert(MICROSECOND(LocalDateTime.of(2021, 1, 1, 0, 0)).name == "MICROSECOND('2021-01-01 00:00')")
  }

  it should "Statement generated using the MINUTE function matches the specified string." in {
    assert(MINUTE(c5).name == "MINUTE(`local_date_time`)")
    assert(MINUTE(c6).name == "MINUTE(`local_date_time`)")
    assert(MINUTE(LocalDateTime.of(2021, 1, 1, 0, 0)).name == "MINUTE('2021-01-01 00:00')")
  }

  it should "Statement generated using the MONTH function matches the specified string." in {
    assert(MONTH(c5).name == "MONTH(`local_date_time`)")
    assert(MONTH(c6).name == "MONTH(`local_date_time`)")
    assert(MONTH(LocalDateTime.of(2021, 1, 1, 0, 0)).name == "MONTH('2021-01-01 00:00')")
  }

  it should "Statement generated using the MONTHNAME function matches the specified string." in {
    assert(MONTHNAME(c5).name == "MONTHNAME(`local_date_time`)")
    assert(MONTHNAME(c6).name == "MONTHNAME(`local_date_time`)")
    assert(MONTHNAME(LocalDateTime.of(2021, 1, 1, 0, 0)).name == "MONTHNAME('2021-01-01T00:00')")
  }

  it should "Statement generated using the NOW function matches the specified string." in {
    assert(NOW().name == "NOW()")
  }

  it should "Statement generated using the PERIOD_ADD function matches the specified string." in {
    assert(PERIOD_ADD(YearMonth.of(2025, 1), 1).name == "PERIOD_ADD(202501, 1)")
  }

  it should "Statement generated using the QUARTER function matches the specified string." in {
    assert(QUARTER(c5).name == "QUARTER(`local_date_time`)")
    assert(QUARTER(c6).name == "QUARTER(`local_date_time`)")
    assert(QUARTER(LocalDateTime.of(2021, 1, 1, 0, 0)).name == "QUARTER('2021-01-01T00:00')")
  }

  it should "Statement generated using the SEC_TO_TIME function matches the specified string." in {
    assert(SEC_TO_TIME(c15).name == "SEC_TO_TIME(`second`)")
    assert(SEC_TO_TIME(c16).name == "SEC_TO_TIME(`second`)")
    assert(SEC_TO_TIME(2378).name == "SEC_TO_TIME(2378)")
  }

  it should "Statement generated using the SECOND function matches the specified string." in {
    assert(SECOND(c5).name == "SECOND(`local_date_time`)")
    assert(SECOND(c6).name == "SECOND(`local_date_time`)")
    assert(SECOND(LocalDateTime.of(2021, 1, 1, 0, 0)).name == "SECOND('2021-01-01 00:00')")
  }

  it should "Statement generated using the SUBTIME function matches the specified string." in {
    assert(SUBTIME(c5, c3).name == "SUBTIME(`local_date_time`, `local_time`)")
    assert(SUBTIME(c6, c4).name == "SUBTIME(`local_date_time`, `local_time`)")
    assert(
      SUBTIME(
        LocalDateTime.of(2021, 1, 1, 0, 0),
        LocalTime.of(1, 1, 1, 1)
      ).name == "SUBTIME('2021-01-01 00:00', '01:01:01.000000001')"
    )
  }

  it should "Statement generated using the SYSDATE function matches the specified string." in {
    assert(SYSDATE().name == "SYSDATE()")
  }

  it should "Statement generated using the TIME function matches the specified string." in {
    assert(TIME(c5).name == "TIME(`local_date_time`)")
    assert(TIME(c6).name == "TIME(`local_date_time`)")
    assert(TIME(LocalDateTime.of(2021, 1, 1, 0, 0)).name == "TIME('2021-01-01 00:00')")
  }

  it should "Statement generated using the TIME_TO_SEC function matches the specified string." in {
    assert(TIME_TO_SEC(c5).name == "TIME_TO_SEC(`local_date_time`)")
    assert(TIME_TO_SEC(c6).name == "TIME_TO_SEC(`local_date_time`)")
    assert(TIME_TO_SEC(LocalDateTime.of(2021, 1, 1, 0, 0)).name == "TIME_TO_SEC('2021-01-01 00:00')")
  }

  it should "Statement generated using the TIMEDIFF function matches the specified string." in {
    assert(TIMEDIFF(c5, c5).name == "TIMEDIFF(`local_date_time`, `local_date_time`)")
    assert(TIMEDIFF(c6, c6).name == "TIMEDIFF(`local_date_time`, `local_date_time`)")
    assert(TIMEDIFF(c5, LocalDateTime.of(2025, 1, 1, 1, 1)).name == "TIMEDIFF(`local_date_time`, '2025-01-01 01:01')")
    assert(TIMEDIFF(c6, LocalDateTime.of(2025, 1, 1, 1, 1)).name == "TIMEDIFF(`local_date_time`, '2025-01-01 01:01')")
    assert(
      TIMEDIFF(
        LocalDateTime.of(2021, 1, 1, 0, 0),
        LocalDateTime.of(2025, 1, 1, 1, 1)
      ).name == "TIMEDIFF('2021-01-01 00:00', '2025-01-01 01:01')"
    )
  }

  it should "Statement generated using the TIMESTAMP function matches the specified string." in {
    assert(TIMESTAMP(c1).name == "TIMESTAMP(`local_date`)")
    assert(TIMESTAMP(c2).name == "TIMESTAMP(`local_date`)")
    assert(TIMESTAMP(c5, c3).name == "TIMESTAMP(`local_date_time`, `local_time`)")
    assert(TIMESTAMP(c6, c4).name == "TIMESTAMP(`local_date_time`, `local_time`)")
    assert(TIMESTAMP(LocalDate.of(2021, 1, 1)).name == "TIMESTAMP('2021-01-01')")
  }

  it should "Statement generated using the GET_FORMAT function matches the specified string." in {
    assert(GET_FORMAT(DateType.DATE, FormatType.USA).name == "GET_FORMAT(DATE, 'USA')")
    assert(GET_FORMAT(DateType.TIME, FormatType.EUR).name == "GET_FORMAT(TIME, 'EUR')")
    assert(GET_FORMAT(DateType.DATETIME, FormatType.JIS).name == "GET_FORMAT(DATETIME, 'JIS')")
    assert(GET_FORMAT(DateType.DATE, FormatType.ISO).name == "GET_FORMAT(DATE, 'ISO')")
    assert(GET_FORMAT(DateType.DATETIME, FormatType.INTERNAL).name == "GET_FORMAT(DATETIME, 'INTERNAL')")
  }

  it should "Statement generated using the PERIOD_DIFF function matches the specified string." in {
    assert(PERIOD_DIFF(YearMonth.of(2008, 2), YearMonth.of(2007, 3)).name == "PERIOD_DIFF(200802, 200703)")
    assert(PERIOD_DIFF(YearMonth.of(2021, 12), YearMonth.of(2021, 1)).name == "PERIOD_DIFF(202112, 202101)")
  }

  it should "Statement generated using the TIMESTAMPADD function matches the specified string." in {
    assert(TIMESTAMPADD(TimeUnit.MINUTE, 1, c5).name == "TIMESTAMPADD(MINUTE, 1, `local_date_time`)")
    assert(TIMESTAMPADD(TimeUnit.HOUR, 24, c6).name == "TIMESTAMPADD(HOUR, 24, `local_date_time`)")
    assert(TIMESTAMPADD(TimeUnit.DAY, 7, c1).name == "TIMESTAMPADD(DAY, 7, `local_date`)")
    assert(TIMESTAMPADD(TimeUnit.MONTH, 3, c2).name == "TIMESTAMPADD(MONTH, 3, `local_date`)")
    assert(
      TIMESTAMPADD(TimeUnit.MINUTE, 1, LocalDateTime.of(2003, 1, 2, 0, 0))
        .name == "TIMESTAMPADD(MINUTE, 1, '2003-01-02 00:00')"
    )
  }

  it should "Statement generated using the TIMESTAMPDIFF function matches the specified string." in {
    assert(TIMESTAMPDIFF(TimeUnit.MONTH, c1, c5).name == "TIMESTAMPDIFF(MONTH, `local_date`, `local_date_time`)")
    assert(TIMESTAMPDIFF(TimeUnit.DAY, c2, c6).name == "TIMESTAMPDIFF(DAY, `local_date`, `local_date_time`)")
    assert(
      TIMESTAMPDIFF(TimeUnit.MONTH, LocalDate.of(2003, 2, 1), c5)
        .name == "TIMESTAMPDIFF(MONTH, '2003-02-01', `local_date_time`)"
    )
    assert(
      TIMESTAMPDIFF(TimeUnit.MONTH, LocalDate.of(2003, 2, 1), LocalDate.of(2003, 5, 1))
        .name == "TIMESTAMPDIFF(MONTH, '2003-02-01', '2003-05-01')"
    )
  }

  it should "Statement generated using the STR_TO_DATE function matches the specified string." in {
    assert(STR_TO_DATE(c17, "%d,%m,%Y").name == "STR_TO_DATE(`date_string`, '%d,%m,%Y')")
    assert(STR_TO_DATE(c18, "%Y-%m-%d").name == "STR_TO_DATE(`date_string`, '%Y-%m-%d')")
    assert(STR_TO_DATE("01,5,2013", "%d,%m,%Y").name == "STR_TO_DATE('01,5,2013', '%d,%m,%Y')")
    assert(STR_TO_DATE("May 1, 2013", "%M %d,%Y").name == "STR_TO_DATE('May 1, 2013', '%M %d,%Y')")
  }

  it should "Statement generated using the TIME_FORMAT function matches the specified string." in {
    assert(TIME_FORMAT(c3, "%H:%i:%s").name == "TIME_FORMAT(`local_time`, '%H:%i:%s')")
    assert(TIME_FORMAT(c4, "%h:%i %p").name == "TIME_FORMAT(`local_time`, '%h:%i %p')")
    assert(TIME_FORMAT(c5, "%T").name == "TIME_FORMAT(`local_date_time`, '%T')")
    assert(TIME_FORMAT(c6, "%H:%i:%s.%f").name == "TIME_FORMAT(`local_date_time`, '%H:%i:%s.%f')")
    assert(TIME_FORMAT(LocalTime.of(10, 15, 30), "%H:%i:%s").name == "TIME_FORMAT('10:15:30', '%H:%i:%s')")
  }

  it should "Statement generated using the TO_DAYS function matches the specified string." in {
    assert(TO_DAYS(c1).name == "TO_DAYS(`local_date`)")
    assert(TO_DAYS(c2).name == "TO_DAYS(`local_date`)")
    assert(TO_DAYS(c5).name == "TO_DAYS(`local_date_time`)")
    assert(TO_DAYS(c6).name == "TO_DAYS(`local_date_time`)")
    assert(TO_DAYS(LocalDate.of(2007, 10, 7)).name == "TO_DAYS('2007-10-07')")
  }

  it should "Statement generated using the TO_SECONDS function matches the specified string." in {
    assert(TO_SECONDS(c1).name == "TO_SECONDS(`local_date`)")
    assert(TO_SECONDS(c2).name == "TO_SECONDS(`local_date`)")
    assert(TO_SECONDS(c5).name == "TO_SECONDS(`local_date_time`)")
    assert(TO_SECONDS(c6).name == "TO_SECONDS(`local_date_time`)")
    assert(TO_SECONDS(LocalDateTime.of(2009, 11, 29, 13, 43, 32)).name == "TO_SECONDS('2009-11-29 13:43:32')")
  }

  it should "Statement generated using the UNIX_TIMESTAMP function matches the specified string." in {
    assert(UNIX_TIMESTAMP().name == "UNIX_TIMESTAMP()")
    assert(UNIX_TIMESTAMP(c1).name == "UNIX_TIMESTAMP(`local_date`)")
    assert(UNIX_TIMESTAMP(c2).name == "UNIX_TIMESTAMP(`local_date`)")
    assert(UNIX_TIMESTAMP(c5).name == "UNIX_TIMESTAMP(`local_date_time`)")
    assert(UNIX_TIMESTAMP(c6).name == "UNIX_TIMESTAMP(`local_date_time`)")
    assert(UNIX_TIMESTAMP(LocalDateTime.of(2015, 11, 13, 10, 20, 19)).name == "UNIX_TIMESTAMP('2015-11-13 10:20:19')")
  }

  it should "Statement generated using the UTC_DATE function matches the specified string." in {
    assert(UTC_DATE().name == "UTC_DATE()")
  }

  it should "Statement generated using the UTC_TIME function matches the specified string." in {
    assert(UTC_TIME().name == "UTC_TIME()")
    assert(UTC_TIME(3).name == "UTC_TIME(3)")
    assert(UTC_TIME(6).name == "UTC_TIME(6)")
  }

  it should "Statement generated using the UTC_TIMESTAMP function matches the specified string." in {
    assert(UTC_TIMESTAMP().name == "UTC_TIMESTAMP()")
    assert(UTC_TIMESTAMP(3).name == "UTC_TIMESTAMP(3)")
    assert(UTC_TIMESTAMP(6).name == "UTC_TIMESTAMP(6)")
  }

  it should "Statement generated using the WEEK function matches the specified string." in {
    assert(WEEK(c1).name == "WEEK(`local_date`)")
    assert(WEEK(c2).name == "WEEK(`local_date`)")
    assert(WEEK(c5).name == "WEEK(`local_date_time`)")
    assert(WEEK(c6).name == "WEEK(`local_date_time`)")
    assert(WEEK(c1, 0).name == "WEEK(`local_date`, 0)")
    assert(WEEK(c5, 1).name == "WEEK(`local_date_time`, 1)")
    assert(WEEK(LocalDate.of(2008, 2, 20)).name == "WEEK('2008-02-20')")
    assert(WEEK(LocalDate.of(2008, 2, 20), 1).name == "WEEK('2008-02-20', 1)")
  }

  it should "Statement generated using the WEEKDAY function matches the specified string." in {
    assert(WEEKDAY(c1).name == "WEEKDAY(`local_date`)")
    assert(WEEKDAY(c2).name == "WEEKDAY(`local_date`)")
    assert(WEEKDAY(c5).name == "WEEKDAY(`local_date_time`)")
    assert(WEEKDAY(c6).name == "WEEKDAY(`local_date_time`)")
    assert(WEEKDAY(LocalDate.of(2008, 2, 3)).name == "WEEKDAY('2008-02-03')")
  }

  it should "Statement generated using the WEEKOFYEAR function matches the specified string." in {
    assert(WEEKOFYEAR(c1).name == "WEEKOFYEAR(`local_date`)")
    assert(WEEKOFYEAR(c2).name == "WEEKOFYEAR(`local_date`)")
    assert(WEEKOFYEAR(c5).name == "WEEKOFYEAR(`local_date_time`)")
    assert(WEEKOFYEAR(c6).name == "WEEKOFYEAR(`local_date_time`)")
    assert(WEEKOFYEAR(LocalDate.of(2008, 2, 20)).name == "WEEKOFYEAR('2008-02-20')")
  }

  it should "Statement generated using the YEARWEEK function matches the specified string." in {
    assert(YEARWEEK(c1).name == "YEARWEEK(`local_date`)")
    assert(YEARWEEK(c2).name == "YEARWEEK(`local_date`)")
    assert(YEARWEEK(c5).name == "YEARWEEK(`local_date_time`)")
    assert(YEARWEEK(c6).name == "YEARWEEK(`local_date_time`)")
    assert(YEARWEEK(c1, 0).name == "YEARWEEK(`local_date`, 0)")
    assert(YEARWEEK(c5, 1).name == "YEARWEEK(`local_date_time`, 1)")
    assert(YEARWEEK(LocalDate.of(1987, 1, 1)).name == "YEARWEEK('1987-01-01')")
    assert(YEARWEEK(LocalDate.of(1987, 1, 1), 0).name == "YEARWEEK('1987-01-01', 0)")
  }
