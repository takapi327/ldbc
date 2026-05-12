/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import ldbc.dsl.*

class GroupByTest extends munit.FunSuite:

  case class Table()

  test("construct basic GroupBy statement correctly") {
    val groupBy = GroupBy[Table, Int](
      Table(),
      Column[Int]("country_code"),
      "SELECT SUM(population) FROM cities GROUP BY country_code",
      List()
    )
    assertEquals(groupBy.statement, "SELECT SUM(population) FROM cities GROUP BY country_code")
    assert(groupBy.params.isEmpty)
  }

  test("combine GroupBy statement with SQL using ++ operator") {
    val groupBy = GroupBy[Table, String](
      Table(),
      Column[String]("category"),
      "SELECT category, COUNT(*) FROM products GROUP BY category",
      List()
    )
    val combined = groupBy ++ sql" ORDER BY COUNT(*) DESC"
    assertEquals(combined.statement, "SELECT category, COUNT(*) FROM products GROUP BY category ORDER BY COUNT(*) DESC")
    assert(combined.params.isEmpty)
  }

  test("chain GroupBy with having") {
    val groupBy = GroupBy[Table, Int](
      Table(),
      Column[Int]("country_code"),
      "SELECT country_code, AVG(population) FROM cities GROUP BY country_code",
      List()
    )

    val having = groupBy.having(_ => Expression.Over("AVG(population)", false, 1000000))

    assertEquals(
      having.statement,
      "SELECT country_code, AVG(population) FROM cities GROUP BY country_code HAVING AVG(population) > ?"
    )
    assertEquals(having.params.length, 1)
  }

  test("chain GroupBy with limit") {
    val groupBy = GroupBy[Table, Int](
      Table(),
      Column[Int]("department_id"),
      "SELECT department_id, SUM(salary) FROM employees GROUP BY department_id",
      List()
    )

    val limited = groupBy.limit(5)

    assertEquals(limited.statement, "SELECT department_id, SUM(salary) FROM employees GROUP BY department_id LIMIT ?")
    assertEquals(limited.params.length, 1)
  }

  test("chain GroupBy with limit and offset") {
    val groupBy = GroupBy[Table, Int](
      Table(),
      Column[Int]("year"),
      "SELECT year, COUNT(*) FROM sales GROUP BY year",
      List()
    )

    val limitedWithOffset = groupBy.limit(10).offset(5)

    assertEquals(limitedWithOffset.statement, "SELECT year, COUNT(*) FROM sales GROUP BY year LIMIT ? OFFSET ?")
    assertEquals(limitedWithOffset.params.length, 2)
  }

  test("chain GroupBy with orderBy") {
    val groupBy = GroupBy[Table, String](
      Table(),
      Column[String]("product_category"),
      "SELECT product_category, SUM(revenue) FROM sales GROUP BY product_category",
      List()
    )

    val ordered = groupBy.orderBy(_ => OrderBy.Order.desc(Column[Double]("SUM(revenue)")))

    assertEquals(
      ordered.statement,
      "SELECT product_category, SUM(revenue) FROM sales GROUP BY product_category ORDER BY `SUM(revenue)` DESC"
    )
    assert(ordered.params.isEmpty)
  }

  test("chain GroupBy with having, orderBy and limit") {
    val groupBy = GroupBy[Table, String](
      Table(),
      Column[String]("region"),
      "SELECT region, COUNT(*) as customer_count FROM customers GROUP BY region",
      List()
    )

    val expression = Expression.Over("COUNT(*)", false, 100)

    val result = groupBy
      .having(_ => expression)
      .orderBy(_ => OrderBy.Order.desc(Column[Int]("customer_count")))
      .limit(5)

    assertEquals(
      result.statement,
      "SELECT region, COUNT(*) as customer_count FROM customers GROUP BY region HAVING COUNT(*) > ? ORDER BY `customer_count` DESC LIMIT ?"
    )
    assertEquals(result.params.length, 2)
  }

  test("use column aliases in GroupBy correctly") {
    val column  = Column[String]("department").as("dept_name")
    val groupBy = GroupBy[Table, String](
      Table(),
      column,
      "SELECT dept_name, AVG(salary) FROM employees GROUP BY dept_name",
      List()
    )

    assertEquals(groupBy.statement, "SELECT dept_name, AVG(salary) FROM employees GROUP BY dept_name")
  }

  test("handle parameters in having clauses properly") {
    val groupBy = GroupBy[Table, Int](
      Table(),
      Column[Int]("age_group"),
      "SELECT age_group, COUNT(*) FROM users GROUP BY age_group",
      List()
    )

    val minCount = 50
    val having   = groupBy.having(_ => Expression.Over("COUNT(*)", false, minCount))

    assertEquals(having.statement, "SELECT age_group, COUNT(*) FROM users GROUP BY age_group HAVING COUNT(*) > ?")
    assertEquals(having.params.length, 1)
  }
