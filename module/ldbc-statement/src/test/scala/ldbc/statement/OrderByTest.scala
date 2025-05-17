/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import org.scalatest.flatspec.AnyFlatSpec

import ldbc.dsl.*

class OrderByTest extends AnyFlatSpec:

  case class Table()

  it should "construct basic OrderBy statement correctly" in {
    val orderBy = OrderBy[Table, Int](
      Table(),
      Column[Int]("id"),
      "SELECT id FROM table_name ORDER BY id ASC",
      List()
    )
    assert(orderBy.statement === "SELECT id FROM table_name ORDER BY id ASC")
    assert(orderBy.params.isEmpty)
  }

  it should "combine OrderBy statement with SQL using ++ operator" in {
    val orderBy = OrderBy[Table, String](
      Table(),
      Column[String]("name"),
      "SELECT name FROM table_name ORDER BY name DESC",
      List()
    )
    val combined = orderBy ++ sql" LIMIT ${ 10 }"
    assert(combined.statement === "SELECT name FROM table_name ORDER BY name DESC LIMIT ?")
    assert(combined.params.length === 1)
  }

  it should "create ascending order expression correctly" in {
    val column = Column[Int]("id")
    val order  = OrderBy.Order.asc(column)
    assert(order.statement === "`id` ASC")
  }

  it should "create descending order expression correctly" in {
    val column = Column[Int]("id")
    val order  = OrderBy.Order.desc(column)
    assert(order.statement === "`id` DESC")
  }

  it should "use column alias in order expression when available" in {
    val column    = Column[Int]("id").as("user_id")
    val ascOrder  = OrderBy.Order.asc(column)
    val descOrder = OrderBy.Order.desc(column)

    assert(ascOrder.statement === "user_id ASC")
    assert(descOrder.statement === "user_id DESC")
  }

  it should "combine multiple order expressions using Applicative" in {
    val idColumn   = Column[Int]("id")
    val nameColumn = Column[String]("name")

    import OrderBy.Order.given
    import cats.syntax.apply.*

    val combinedOrder = (OrderBy.Order.asc(idColumn), OrderBy.Order.desc(nameColumn)).tupled
    assert(combinedOrder.statement === "`id` ASC, `name` DESC")
  }

  it should "chain OrderBy with limit" in {
    val orderBy = OrderBy[Table, Int](
      Table(),
      Column[Int]("id"),
      "SELECT id FROM table_name ORDER BY `id` ASC",
      List()
    )

    val limited = orderBy.limit(5)

    assert(limited.statement === "SELECT id FROM table_name ORDER BY `id` ASC LIMIT ?")
    assert(limited.params.length === 1)
  }

  it should "chain OrderBy with limit and offset" in {
    val orderBy = OrderBy[Table, Int](
      Table(),
      Column[Int]("id"),
      "SELECT id FROM table_name ORDER BY `id` ASC",
      List()
    )

    val limitedWithOffset = orderBy.limit(10).offset(5)

    assert(limitedWithOffset.statement === "SELECT id FROM table_name ORDER BY `id` ASC LIMIT ? OFFSET ?")
    assert(limitedWithOffset.params.length === 2)
  }
