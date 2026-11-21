/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder

import ldbc.statement.functions.DateTime

/**
 * Tests that the string arguments of function columns are bound as parameters wherever the column is used.
 *
 * Regression guard for the finding that `STR_TO_DATE`, `DATE_FORMAT` and `TIME_FORMAT` wrote their string arguments
 * into the SQL text, so an argument containing a quote could change the structure of the statement. The values must
 * be bound at the position of their placeholder, in SELECT, WHERE and ORDER BY alike.
 */
class FunctionParameterTest extends munit.FunSuite, DateTime:

  test("A string argument of a function column is bound instead of being written into the SELECT statement.") {
    val payload = "x', '%d') IS NULL OR (SELECT SLEEP(5)) -- "
    val select  = query.select(_ => STR_TO_DATE(payload, "%Y"))
    assertEquals(select.statement, "SELECT STR_TO_DATE(?, ?) FROM test")
    assertEquals(select.params.map(_.value), List(payload, "%Y"))
  }

  test("The parameters of function columns follow the order of the placeholders in SELECT, WHERE and ORDER BY.") {
    val select = query
      .select(test => STR_TO_DATE(test.p2, "%Y-%m-%d"))
      .where(test => DATE_FORMAT(STR_TO_DATE(test.p2, "%d/%m/%Y"), "%Y") === "2024")
      .and(_.p1 > 1L)
      .orderBy(test => STR_TO_DATE(test.p2, "%m-%d").asc)
    assertEquals(
      select.statement,
      "SELECT STR_TO_DATE(`p2`, ?) FROM test WHERE DATE_FORMAT(STR_TO_DATE(`p2`, ?), ?) = ? AND test.`p1` > ? ORDER BY STR_TO_DATE(`p2`, ?) ASC"
    )
    assertEquals(select.params.map(_.value), List("%Y-%m-%d", "%d/%m/%Y", "%Y", "2024", "1", "%m-%d"))
  }
