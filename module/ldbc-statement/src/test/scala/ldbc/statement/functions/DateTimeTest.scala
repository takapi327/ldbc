/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement.functions

import java.time.*

import ldbc.statement.Column
import ldbc.statement.functions.DateTime.*

import org.scalatest.flatspec.AnyFlatSpec

class DateTimeTest extends AnyFlatSpec, DateTime:

  private val c1 = Column.Impl[LocalDate]("local_date")
  private val c2 = Column.Impl[Option[LocalDate]]("local_date")
  private val c3 = Column.Impl[LocalTime]("local_time")
  private val c4 = Column.Impl[Option[LocalTime]]("local_time")
  private val c5 = Column.Impl[LocalDateTime]("local_date_time")
  private val c6 = Column.Impl[Option[LocalDateTime]]("local_date_time")

  it should "Statement generated using the ADDDATE function matches the specified string." in {
    assert(ADDDATE(c1, Interval.DAY(1)).name == "ADDDATE(local_date, INTERVAL 1 DAY)")
    assert(ADDDATE(c1, Interval.MONTH(1)).name == "ADDDATE(local_date, INTERVAL 1 MONTH)")
    assert(ADDDATE(c1, Interval.YEAR(1)).name == "ADDDATE(local_date, INTERVAL 1 YEAR)")
    assert(ADDDATE(c2, Interval.DAY(1)).name == "ADDDATE(local_date, INTERVAL 1 DAY)")
    assert(ADDDATE(c2, Interval.MONTH(1)).name == "ADDDATE(local_date, INTERVAL 1 MONTH)")
    assert(ADDDATE(c2, Interval.YEAR(1)).name == "ADDDATE(local_date, INTERVAL 1 YEAR)")
    assert(ADDDATE(LocalDate.of(2021, 1, 1), Interval.DAY(1)).name == "ADDDATE('2021-01-01', INTERVAL 1 DAY)")
    assert(ADDDATE(LocalDate.of(2021, 1, 1), Interval.MONTH(1)).name == "ADDDATE('2021-01-01', INTERVAL 1 MONTH)")
    assert(ADDDATE(LocalDate.of(2021, 1, 1), Interval.YEAR(1)).name == "ADDDATE('2021-01-01', INTERVAL 1 YEAR)")
  }

  it should "Statement generated using the ADDTIME function matches the specified string." in {
    assert(ADDTIME(c3, LocalTime.of(1, 1, 1, 1)).name == "ADDTIME(local_time, '01:01:01.000000001')")
    assert(ADDTIME(c4, LocalTime.of(1, 1, 1, 1)).name == "ADDTIME(local_time, '01:01:01.000000001')")
    assert(ADDTIME(LocalTime.of(1, 1, 1), LocalTime.of(1, 1, 1, 1)).name == "ADDTIME('01:01:01', '01:01:01.000000001')")
  }

  it should "Statement generated using the CONVERT_TZ function matches the specified string." in {
    assert(CONVERT_TZ(c5, LocalTime.of(0, 0), LocalTime.of(9, 0)).name == "CONVERT_TZ(local_date_time, '+0:0', '+9:0')")
    assert(CONVERT_TZ(c6, LocalTime.of(0, 0), LocalTime.of(9, 0)).name == "CONVERT_TZ(local_date_time, '+0:0', '+9:0')")
    assert(CONVERT_TZ(LocalDateTime.of(2021, 1, 1, 0, 0), LocalTime.of(0, 0), LocalTime.of(9, 0)).name == "CONVERT_TZ('2021-01-01T00:00', '+0:0', '+9:0')")
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
    assert(DATE_FORMAT(LocalDateTime.of(2021, 1, 1, 0, 0), "%Y-%m-%d").name == "DATE_FORMAT('2021-01-01T00:00', '%Y-%m-%d')")
  }
