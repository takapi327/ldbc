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
