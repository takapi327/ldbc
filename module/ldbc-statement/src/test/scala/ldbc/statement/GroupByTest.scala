/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import org.scalatest.flatspec.AnyFlatSpec

import ldbc.dsl.*

class GroupByTest extends AnyFlatSpec:

  case class Table()

  it should "construct basic GroupBy statement correctly" in {
    val groupBy = GroupBy[Table, Int](
      Table(),
      Column[Int]("country_code"),
      "SELECT SUM(population) FROM cities GROUP BY country_code",
      List()
    )
    assert(groupBy.statement === "SELECT SUM(population) FROM cities GROUP BY country_code")
    assert(groupBy.params.isEmpty)
  }

  it should "combine GroupBy statement with SQL using ++ operator" in {
    val groupBy = GroupBy[Table, String](
      Table(),
      Column[String]("category"),
      "SELECT category, COUNT(*) FROM products GROUP BY category",
      List()
    )
    val combined = groupBy ++ sql" ORDER BY COUNT(*) DESC"
    assert(combined.statement === "SELECT category, COUNT(*) FROM products GROUP BY category ORDER BY COUNT(*) DESC")
    assert(combined.params.isEmpty)
  }

  it should "chain GroupBy with having" in {
    val groupBy = GroupBy[Table, Int](
      Table(),
      Column[Int]("country_code"),
      "SELECT country_code, AVG(population) FROM cities GROUP BY country_code",
      List()
    )
    
    val having = groupBy.having(_ => Expression.Over("AVG(population)", false, 1000000))
    
    assert(having.statement === "SELECT country_code, AVG(population) FROM cities GROUP BY country_code HAVING AVG(population) > ?")
    assert(having.params.length === 1)
  }

  it should "chain GroupBy with limit" in {
    val groupBy = GroupBy[Table, Int](
      Table(),
      Column[Int]("department_id"),
      "SELECT department_id, SUM(salary) FROM employees GROUP BY department_id",
      List()
    )
    
    val limited = groupBy.limit(5)
    
    assert(limited.statement === "SELECT department_id, SUM(salary) FROM employees GROUP BY department_id LIMIT ?")
    assert(limited.params.length === 1)
  }

  it should "chain GroupBy with limit and offset" in {
    val groupBy = GroupBy[Table, Int](
      Table(),
      Column[Int]("year"),
      "SELECT year, COUNT(*) FROM sales GROUP BY year",
      List()
    )
    
    val limitedWithOffset = groupBy.limit(10).offset(5)
    
    assert(limitedWithOffset.statement === "SELECT year, COUNT(*) FROM sales GROUP BY year LIMIT ? OFFSET ?")
    assert(limitedWithOffset.params.length === 2)
  }

  it should "chain GroupBy with orderBy" in {
    val groupBy = GroupBy[Table, String](
      Table(),
      Column[String]("product_category"),
      "SELECT product_category, SUM(revenue) FROM sales GROUP BY product_category",
      List()
    )
    
    val ordered = groupBy.orderBy(_ => OrderBy.Order.desc(Column[Double]("SUM(revenue)")))
    
    assert(ordered.statement === "SELECT product_category, SUM(revenue) FROM sales GROUP BY product_category ORDER BY `SUM(revenue)` DESC")
    assert(ordered.params.isEmpty)
  }

  it should "chain GroupBy with having, orderBy and limit" in {
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
    
    assert(result.statement === "SELECT region, COUNT(*) as customer_count FROM customers GROUP BY region HAVING COUNT(*) > ? ORDER BY `customer_count` DESC LIMIT ?")
    assert(result.params.length === 2)
  }

  it should "use column aliases in GroupBy correctly" in {
    val column = Column[String]("department").as("dept_name")
    val groupBy = GroupBy[Table, String](
      Table(),
      column,
      "SELECT dept_name, AVG(salary) FROM employees GROUP BY dept_name",
      List()
    )
    
    assert(groupBy.statement === "SELECT dept_name, AVG(salary) FROM employees GROUP BY dept_name")
  }

  it should "handle parameters in having clauses properly" in {
    val groupBy = GroupBy[Table, Int](
      Table(),
      Column[Int]("age_group"),
      "SELECT age_group, COUNT(*) FROM users GROUP BY age_group",
      List()
    )
    
    val minCount = 50
    val having = groupBy.having(_ => Expression.Over("COUNT(*)", false, minCount))
    
    assert(having.statement === "SELECT age_group, COUNT(*) FROM users GROUP BY age_group HAVING COUNT(*) > ?")
    assert(having.params.length === 1)
  }