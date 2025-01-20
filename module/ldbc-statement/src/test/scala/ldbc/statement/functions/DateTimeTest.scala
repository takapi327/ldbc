/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement.functions

import java.time.*

import org.scalatest.flatspec.AnyFlatSpec

import ldbc.statement.functions.DateTime.*
import ldbc.statement.Column

class DateTimeTest extends AnyFlatSpec, DateTime:

  private val c1 = Column.Impl[LocalDate]("local_date")
  private val c2 = Column.Impl[Option[LocalDate]]("local_date")
  private val c3 = Column.Impl[LocalTime]("local_time")
  private val c4 = Column.Impl[Option[LocalTime]]("local_time")
  private val c5 = Column.Impl[LocalDateTime]("local_date_time")
  private val c6 = Column.Impl[Option[LocalDateTime]]("local_date_time")
  private val c7 = Column.Impl[Int]("days")
  private val c8 = Column.Impl[Option[Int]]("days")
  private val c9 = Column.Impl[Year]("year")
  private val c10 = Column.Impl[Option[Year]]("year")
  private val c11 = Column.Impl[Int]("hour")
  private val c12 = Column.Impl[Option[Int]]("hour")
  private val c13 = Column.Impl[Int]("minute")
  private val c14 = Column.Impl[Option[Int]]("minute")
  private val c15 = Column.Impl[Int]("second")
  private val c16 = Column.Impl[Option[Int]]("second")

  it should "Statement generated using the DATE_ADD function matches the specified string." in {
    assert(DATE_ADD(c1, Interval.DAY(1)).name == "DATE_ADD(local_date, INTERVAL 1 DAY)")
    assert(DATE_ADD(c1, Interval.MONTH(1)).name == "DATE_ADD(local_date, INTERVAL 1 MONTH)")
    assert(DATE_ADD(c1, Interval.YEAR(1)).name == "DATE_ADD(local_date, INTERVAL 1 YEAR)")
    assert(DATE_ADD(c2, Interval.DAY(1)).name == "DATE_ADD(local_date, INTERVAL 1 DAY)")
    assert(DATE_ADD(c2, Interval.MONTH(1)).name == "DATE_ADD(local_date, INTERVAL 1 MONTH)")
    assert(DATE_ADD(c2, Interval.YEAR(1)).name == "DATE_ADD(local_date, INTERVAL 1 YEAR)")
    assert(DATE_ADD(LocalDate.of(2021, 1, 1), Interval.DAY(1)).name == "DATE_ADD('2021-01-01', INTERVAL 1 DAY)")
    assert(DATE_ADD(LocalDate.of(2021, 1, 1), Interval.MONTH(1)).name == "DATE_ADD('2021-01-01', INTERVAL 1 MONTH)")
    assert(DATE_ADD(LocalDate.of(2021, 1, 1), Interval.YEAR(1)).name == "DATE_ADD('2021-01-01', INTERVAL 1 YEAR)")
  }

  it should "Statement generated using the DATE_SUB function matches the specified string." in {
    assert(DATE_SUB(c1, Interval.DAY(1)).name == "DATE_SUB(local_date, INTERVAL 1 DAY)")
    assert(DATE_SUB(c1, Interval.MONTH(1)).name == "DATE_SUB(local_date, INTERVAL 1 MONTH)")
    assert(DATE_SUB(c1, Interval.YEAR(1)).name == "DATE_SUB(local_date, INTERVAL 1 YEAR)")
    assert(DATE_SUB(c2, Interval.DAY(1)).name == "DATE_SUB(local_date, INTERVAL 1 DAY)")
    assert(DATE_SUB(c2, Interval.MONTH(1)).name == "DATE_SUB(local_date, INTERVAL 1 MONTH)")
    assert(DATE_SUB(c2, Interval.YEAR(1)).name == "DATE_SUB(local_date, INTERVAL 1 YEAR)")
    assert(DATE_SUB(LocalDate.of(2021, 1, 1), Interval.DAY(1)).name == "DATE_SUB('2021-01-01', INTERVAL 1 DAY)")
    assert(DATE_SUB(LocalDate.of(2021, 1, 1), Interval.MONTH(1)).name == "DATE_SUB('2021-01-01', INTERVAL 1 MONTH)")
    assert(DATE_SUB(LocalDate.of(2021, 1, 1), Interval.YEAR(1)).name == "DATE_SUB('2021-01-01', INTERVAL 1 YEAR)")
  }

  it should "Statement generated using the ADDTIME function matches the specified string." in {
    assert(ADDTIME(c3, LocalTime.of(1, 1, 1, 1)).name == "ADDTIME(local_time, '01:01:01.000000001')")
    assert(ADDTIME(c4, LocalTime.of(1, 1, 1, 1)).name == "ADDTIME(local_time, '01:01:01.000000001')")
    assert(ADDTIME(LocalTime.of(1, 1, 1), LocalTime.of(1, 1, 1, 1)).name == "ADDTIME('01:01:01', '01:01:01.000000001')")
  }

  it should "Statement generated using the CONVERT_TZ function matches the specified string." in {
    assert(CONVERT_TZ(c5, LocalTime.of(0, 0), LocalTime.of(9, 0)).name == "CONVERT_TZ(local_date_time, '+0:0', '+9:0')")
    assert(CONVERT_TZ(c6, LocalTime.of(0, 0), LocalTime.of(9, 0)).name == "CONVERT_TZ(local_date_time, '+0:0', '+9:0')")
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
    assert(DATE(c5).name == "DATE(local_date_time)")
    assert(DATE(c6).name == "DATE(local_date_time)")
    assert(DATE(LocalDateTime.of(2021, 1, 1, 0, 0)).name == "DATE('2021-01-01T00:00')")
  }

  it should "Statement generated using the DATE_FORMAT function matches the specified string." in {
    assert(DATE_FORMAT(c5, "%Y-%m-%d").name == "DATE_FORMAT(local_date_time, '%Y-%m-%d')")
    assert(DATE_FORMAT(c6, "%Y-%m-%d").name == "DATE_FORMAT(local_date_time, '%Y-%m-%d')")
    assert(
      DATE_FORMAT(LocalDateTime.of(2021, 1, 1, 0, 0), "%Y-%m-%d").name == "DATE_FORMAT('2021-01-01T00:00', '%Y-%m-%d')"
    )
  }

  it should "Statement generated using the DATEDIFF function matches the specified string." in {
    assert(DATEDIFF(c5, c1).name == "DATEDIFF(local_date_time, local_date)")
    assert(DATEDIFF(c6, c2).name == "DATEDIFF(local_date_time, local_date)")
    assert(DATEDIFF(c5, LocalDate.of(2025, 1, 1)).name == "DATEDIFF(local_date_time, '2025-01-01')")
    assert(DATEDIFF(c6, LocalDate.of(2025, 1, 1)).name == "DATEDIFF(local_date_time, '2025-01-01')")
    assert(DATEDIFF(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)).name == "DATEDIFF('2024-01-01', '2025-01-01')")
  }

  it should "Statement generated using the DAYNAME function matches the specified string." in {
    assert(DAYNAME(c5).name == "DAYNAME(local_date_time)")
    assert(DAYNAME(c6).name == "DAYNAME(local_date_time)")
    assert(DAYNAME(LocalDate.of(2025, 1, 1)).name == "DAYNAME('2025-01-01')")
  }

  it should "Statement generated using the DAYOFMONTH function matches the specified string." in {
    assert(DAYOFMONTH(c5).name == "DAYOFMONTH(local_date_time)")
    assert(DAYOFMONTH(c6).name == "DAYOFMONTH(local_date_time)")
    assert(DAYOFMONTH(LocalDate.of(2025, 1, 1)).name == "DAYOFMONTH('2025-01-01')")
  }

  it should "Statement generated using the DAYOFWEEK function matches the specified string." in {
    assert(DAYOFWEEK(c5).name == "DAYOFWEEK(local_date_time)")
    assert(DAYOFWEEK(c6).name == "DAYOFWEEK(local_date_time)")
    assert(DAYOFWEEK(LocalDate.of(2025, 1, 1)).name == "DAYOFWEEK('2025-01-01')")
  }

  it should "Statement generated using the DAYOFYEAR function matches the specified string." in {
    assert(DAYOFYEAR(c5).name == "DAYOFYEAR(local_date_time)")
    assert(DAYOFYEAR(c6).name == "DAYOFYEAR(local_date_time)")
    assert(DAYOFYEAR(LocalDate.of(2025, 1, 1)).name == "DAYOFYEAR('2025-01-01')")
  }

  it should "Statement generated using the EXTRACT function matches the specified string." in {
    assert(EXTRACT(c5, TimeUnit.YEAR).name == "EXTRACT(YEAR FROM local_date_time)")
    assert(EXTRACT(c6, TimeUnit.MONTH).name == "EXTRACT(MONTH FROM local_date_time)")
    assert(EXTRACT(LocalDate.of(2025, 1, 1), TimeUnit.HOUR).name == "EXTRACT(HOUR FROM '2025-01-01')")
  }

  it should "Statement generated using the FROM_DAYS function matches the specified string." in {
    assert(FROM_DAYS(c7).name == "FROM_DAYS(days)")
    assert(FROM_DAYS(c8).name == "FROM_DAYS(days)")
    assert(FROM_DAYS(730669).name == "FROM_DAYS(730669)")
  }

  it should "Statement generated using the FROM_UNIXTIME function matches the specified string." in {
    assert(FROM_UNIXTIME(c7).name == "FROM_UNIXTIME(days)")
    assert(FROM_UNIXTIME(c8).name == "FROM_UNIXTIME(days)")
    assert(FROM_UNIXTIME(730669).name == "FROM_UNIXTIME(730669)")
  }

  it should "Statement generated using the HOUR function matches the specified string." in {
    assert(HOUR(c5).name == "HOUR(local_date_time)")
    assert(HOUR(c6).name == "HOUR(local_date_time)")
    assert(HOUR(LocalDateTime.of(2021, 1, 1, 0, 0)).name == "HOUR('2021-01-01 00:00')")
  }

  it should "Statement generated using the LAST_DAY function matches the specified string." in {
    assert(LAST_DAY(c5).name == "LAST_DAY(local_date_time)")
    assert(LAST_DAY(c6).name == "LAST_DAY(local_date_time)")
    assert(LAST_DAY(LocalDateTime.of(2021, 1, 1, 0, 0)).name == "LAST_DAY('2021-01-01T00:00')")
  }

  it should "Statement generated using the MAKEDATE function matches the specified string." in {
    assert(MAKEDATE(c9, 31).name == "MAKEDATE(year, 31)")
    assert(MAKEDATE(c10, 64).name == "MAKEDATE(year, 64)")
    assert(MAKEDATE(2025, 103).name == "MAKEDATE(2025, 103)")
    assert(MAKEDATE(Year.of(2025), 103).name == "MAKEDATE(2025, 103)")
  }

  it should "Statement generated using the MAKETIME function matches the specified string." in {
    assert(MAKETIME(c11, c13, c15).name == "MAKETIME(hour, minute, second)")
    assert(MAKETIME(c12, c14, c16).name == "MAKETIME(hour, minute, second)")
    assert(MAKETIME(24, 59, 59).name == "MAKETIME(24, 59, 59)")
  }
