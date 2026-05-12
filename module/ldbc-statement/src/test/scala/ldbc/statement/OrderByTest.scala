/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import ldbc.dsl.*

class OrderByTest extends munit.FunSuite:

  case class Table()

  test("construct basic OrderBy statement correctly") {
    val orderBy = OrderBy[Table, Int](
      Table(),
      Column[Int]("id"),
      "SELECT id FROM table_name ORDER BY id ASC",
      List()
    )
    assertEquals(orderBy.statement, "SELECT id FROM table_name ORDER BY id ASC")
    assert(orderBy.params.isEmpty)
  }

  test("combine OrderBy statement with SQL using ++ operator") {
    val orderBy = OrderBy[Table, String](
      Table(),
      Column[String]("name"),
      "SELECT name FROM table_name ORDER BY name DESC",
      List()
    )
    val combined = orderBy ++ sql" LIMIT ${ 10 }"
    assertEquals(combined.statement, "SELECT name FROM table_name ORDER BY name DESC LIMIT ?")
    assertEquals(combined.params.length, 1)
  }

  test("create ascending order expression correctly") {
    val column = Column[Int]("id")
    val order  = OrderBy.Order.asc(column)
    assertEquals(order.statement, "`id` ASC")
  }

  test("create descending order expression correctly") {
    val column = Column[Int]("id")
    val order  = OrderBy.Order.desc(column)
    assertEquals(order.statement, "`id` DESC")
  }

  test("use column alias in order expression when available") {
    val column    = Column[Int]("id").as("user_id")
    val ascOrder  = OrderBy.Order.asc(column)
    val descOrder = OrderBy.Order.desc(column)

    assertEquals(ascOrder.statement, "user_id ASC")
    assertEquals(descOrder.statement, "user_id DESC")
  }

  test("combine multiple order expressions using Applicative") {
    val idColumn   = Column[Int]("id")
    val nameColumn = Column[String]("name")

    import OrderBy.Order.given
    import cats.syntax.apply.*

    val combinedOrder = (OrderBy.Order.asc(idColumn), OrderBy.Order.desc(nameColumn)).tupled
    assertEquals(combinedOrder.statement, "`id` ASC, `name` DESC")
  }

  test("chain OrderBy with limit") {
    val orderBy = OrderBy[Table, Int](
      Table(),
      Column[Int]("id"),
      "SELECT id FROM table_name ORDER BY `id` ASC",
      List()
    )

    val limited = orderBy.limit(5)

    assertEquals(limited.statement, "SELECT id FROM table_name ORDER BY `id` ASC LIMIT ?")
    assertEquals(limited.params.length, 1)
  }

  test("chain OrderBy with limit and offset") {
    val orderBy = OrderBy[Table, Int](
      Table(),
      Column[Int]("id"),
      "SELECT id FROM table_name ORDER BY `id` ASC",
      List()
    )

    val limitedWithOffset = orderBy.limit(10).offset(5)

    assertEquals(limitedWithOffset.statement, "SELECT id FROM table_name ORDER BY `id` ASC LIMIT ? OFFSET ?")
    assertEquals(limitedWithOffset.params.length, 2)
  }
