/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import ldbc.dsl.*
import ldbc.dsl.codec.{ Codec, Encoder }

class ExpressionTest extends munit.FunSuite:

  test("Expression.MatchCondition should construct the correct SQL statement") {
    val expr = Expression.MatchCondition("id", false, 1L)(using Encoder[Long])
    assertEquals(expr.statement, "id = ?")
    assertEquals(expr.parameter.size, 1)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "NOT id = ?")
    assertEquals(notExpr.parameter.size, 1)
  }

  test("Expression.OrMore should construct the correct SQL statement") {
    val expr = Expression.OrMore("id", false, 1L)(using Encoder[Long])
    assertEquals(expr.statement, "id >= ?")
    assertEquals(expr.parameter.size, 1)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "NOT id >= ?")
    assertEquals(notExpr.parameter.size, 1)
  }

  test("Expression.Over should construct the correct SQL statement") {
    val expr = Expression.Over("id", false, 1L)(using Encoder[Long])
    assertEquals(expr.statement, "id > ?")
    assertEquals(expr.parameter.size, 1)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "NOT id > ?")
    assertEquals(notExpr.parameter.size, 1)
  }

  test("Expression.LessThanOrEqualTo should construct the correct SQL statement") {
    val expr = Expression.LessThanOrEqualTo("id", false, 1L)(using Encoder[Long])
    assertEquals(expr.statement, "id <= ?")
    assertEquals(expr.parameter.size, 1)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "NOT id <= ?")
    assertEquals(notExpr.parameter.size, 1)
  }

  test("Expression.LessThan should construct the correct SQL statement") {
    val expr = Expression.LessThan("id", false, 1L)(using Encoder[Long])
    assertEquals(expr.statement, "id < ?")
    assertEquals(expr.parameter.size, 1)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "NOT id < ?")
    assertEquals(notExpr.parameter.size, 1)
  }

  test("Expression.NotEqual should construct the correct SQL statement") {
    val expr = Expression.NotEqual("<>", "id", false, 1L)(using Encoder[Long])
    assertEquals(expr.statement, "id <> ?")
    assertEquals(expr.parameter.size, 1)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "NOT id <> ?")
    assertEquals(notExpr.parameter.size, 1)
  }

  test("Expression.Is should construct the correct SQL statement") {
    val expr = Expression.Is("id", false, "TRUE")
    assertEquals(expr.statement, "id IS TRUE")
    assert(expr.parameter.isEmpty)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "id IS NOT TRUE")
    assert(notExpr.parameter.isEmpty)

    val nullExpr = Expression.Is("id", false, "NULL")
    assertEquals(nullExpr.statement, "id IS NULL")

    val notNullExpr = Expression.Is("id", true, "NULL")
    assertEquals(notNullExpr.statement, "id IS NOT NULL")
  }

  test("Expression.NullSafeEqual should construct the correct SQL statement") {
    val expr = Expression.NullSafeEqual("id", false, 1L)(using Encoder[Long])
    assertEquals(expr.statement, "id <=> ?")
    assertEquals(expr.parameter.size, 1)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "NOT id <=> ?")
    assertEquals(notExpr.parameter.size, 1)
  }

  test("Expression.In should construct the correct SQL statement") {
    val expr = Expression.In("id", false, 1L, 2L, 3L)(using Encoder[Long])
    assertEquals(expr.statement, "id IN (?, ?, ?)")
    assertEquals(expr.parameter.size, 3)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "id NOT IN (?, ?, ?)")
    assertEquals(notExpr.parameter.size, 3)
  }

  test("Expression.Between should construct the correct SQL statement") {
    val expr = Expression.Between("id", false, 1L, 10L)(using Encoder[Long])
    assertEquals(expr.statement, "id BETWEEN ? AND ?")
    assertEquals(expr.parameter.size, 2)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "id NOT BETWEEN ? AND ?")
    assertEquals(notExpr.parameter.size, 2)
  }

  test("Expression.Like should construct the correct SQL statement") {
    val expr = Expression.Like("name", false, "%test%")(using Encoder[String])
    assertEquals(expr.statement, "name LIKE ?")
    assertEquals(expr.parameter.size, 1)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "NOT name LIKE ?")
    assertEquals(notExpr.parameter.size, 1)
  }

  test("Expression.LikeEscape should construct the correct SQL statement") {
    val expr = Expression.LikeEscape("name", false, "T%", "$")(using Encoder[String])
    assertEquals(expr.statement, "name LIKE ? ESCAPE ?")
    assertEquals(expr.parameter.size, 2)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "NOT name LIKE ? ESCAPE ?")
    assertEquals(notExpr.parameter.size, 2)
  }

  test("Expression.Regexp should construct the correct SQL statement") {
    val expr = Expression.Regexp("name", false, "^[A-Z].*")(using Encoder[String])
    assertEquals(expr.statement, "name REGEXP ?")
    assertEquals(expr.parameter.size, 1)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "NOT name REGEXP ?")
    assertEquals(notExpr.parameter.size, 1)
  }

  test("Expression.Div should construct the correct SQL statement") {
    val expr = Expression.Div("id", false, 5L, 2L)(using Encoder[Long])
    assertEquals(expr.statement, "id DIV ? = ?")
    assertEquals(expr.parameter.size, 2)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "NOT id DIV ? = ?")
    assertEquals(notExpr.parameter.size, 2)
  }

  test("Expression.Mod should construct the correct SQL statement") {
    val expr = Expression.Mod("MOD", "id", false, 5L, 2L)(using Encoder[Long])
    assertEquals(expr.statement, "id MOD ? = ?")
    assertEquals(expr.parameter.size, 2)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "NOT id MOD ? = ?")
    assertEquals(notExpr.parameter.size, 2)

    val modExpr = Expression.Mod("%", "id", false, 5L, 2L)(using Encoder[Long])
    assertEquals(modExpr.statement, "id % ? = ?")
  }

  test("Expression.LeftShift should construct the correct SQL statement") {
    val expr = Expression.LeftShift("id", false, 2L)(using Encoder[Long])
    assertEquals(expr.statement, "id << ?")
    assertEquals(expr.parameter.size, 1)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "NOT id << ?")
    assertEquals(notExpr.parameter.size, 1)
  }

  test("Expression.RightShift should construct the correct SQL statement") {
    val expr = Expression.RightShift("id", false, 2L)(using Encoder[Long])
    assertEquals(expr.statement, "id >> ?")
    assertEquals(expr.parameter.size, 1)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "NOT id >> ?")
    assertEquals(notExpr.parameter.size, 1)
  }

  test("Expression.BitXOR should construct the correct SQL statement") {
    val expr = Expression.BitXOR("id", false, 5L)(using Encoder[Long])
    assertEquals(expr.statement, "id ^ ?")
    assertEquals(expr.parameter.size, 1)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "NOT id ^ ?")
    assertEquals(notExpr.parameter.size, 1)
  }

  test("Expression.BitFlip should construct the correct SQL statement") {
    val expr = Expression.BitFlip("id", false, 5L)(using Encoder[Long])
    assertEquals(expr.statement, "~id = ?")
    assertEquals(expr.parameter.size, 1)

    val notExpr = expr.NOT
    assertEquals(notExpr.statement, "NOT ~id = ?")
    assertEquals(notExpr.parameter.size, 1)
  }

  test("Expression.SubQuery should construct the correct SQL statement") {
    val subQuery = sql"SELECT id FROM users WHERE active = true"
    val expr     = Expression.SubQuery("=", "id", subQuery)
    assertEquals(expr.statement, "id = (SELECT id FROM users WHERE active = true)")
  }

  test("Expression.JoinQuery should construct the correct SQL statement") {
    val leftCol  = Column.Impl[Long]("customer_id")
    val rightCol = Column.Impl[Long]("order_id")

    val expr = Expression.JoinQuery("=", leftCol, rightCol)
    assertEquals(expr.statement, "`customer_id` = `order_id`")
    assert(expr.parameter.isEmpty)

    val leftAliased  = leftCol.as("c")
    val rightAliased = rightCol.as("o")

    val aliasedExpr = Expression.JoinQuery("=", leftAliased, rightAliased)
    assertEquals(aliasedExpr.statement, "c = o")
  }

  test("Expression.Pair should combine expressions with logical operators") {
    val expr1 = Expression.MatchCondition("id", false, 1L)(using Encoder[Long])
    val expr2 = Expression.Like("name", false, "test%")(using Encoder[String])

    // AND
    val andExpr = Expression.Pair(" AND ", expr1, expr2)
    assertEquals(andExpr.statement, "(id = ? AND name LIKE ?)")
    assertEquals(andExpr.parameter.size, 2)

    // OR
    val orExpr = Expression.Pair(" OR ", expr1, expr2)
    assertEquals(orExpr.statement, "(id = ? OR name LIKE ?)")
    assertEquals(orExpr.parameter.size, 2)

    // XOR
    val xorExpr = Expression.Pair(" XOR ", expr1, expr2)
    assertEquals(xorExpr.statement, "(id = ? XOR name LIKE ?)")
    assertEquals(xorExpr.parameter.size, 2)

    // Complex nesting
    val expr3       = Expression.In("age", false, 20, 30, 40)(using Encoder[Int])
    val nestedAnd   = Expression.Pair(" AND ", expr1, expr2)
    val complexExpr = Expression.Pair(" OR ", nestedAnd, expr3)

    assertEquals(complexExpr.statement, "(id = ? AND name LIKE ? OR age IN (?, ?, ?))")
    assertEquals(complexExpr.parameter.size, 5)
  }

  test("Expression should support logical operators") {
    val expr1 = Expression.MatchCondition("id", false, 1L)(using Encoder[Long])
    val expr2 = Expression.Like("name", false, "test%")(using Encoder[String])
    val expr3 = Expression.In("age", false, 20, 30, 40)(using Encoder[Int])

    // AND operator
    val andExpr = expr1 and expr2
    assertEquals(andExpr.statement, "(id = ? AND name LIKE ?)")

    // && operator alias
    val andExpr2 = expr1 && expr2
    assertEquals(andExpr2.statement, "(id = ? AND name LIKE ?)")

    // OR operator
    val orExpr = expr1 or expr2
    assertEquals(orExpr.statement, "(id = ? OR name LIKE ?)")

    // || operator alias
    val orExpr2 = expr1 || expr2
    assertEquals(orExpr2.statement, "(id = ? OR name LIKE ?)")

    // XOR operator
    val xorExpr = expr1 xor expr2
    assertEquals(xorExpr.statement, "(id = ? XOR name LIKE ?)")

    // Complex combinations
    val complex = (expr1 && expr2) || expr3
    assertEquals(complex.statement, "(id = ? AND name LIKE ? OR age IN (?, ?, ?))")
  }
