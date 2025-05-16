/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import org.scalatest.flatspec.AnyFlatSpec

import ldbc.dsl.*
import ldbc.dsl.codec.{ Codec, Encoder }

class ExpressionTest extends AnyFlatSpec:

  "Expression.MatchCondition" should "construct the correct SQL statement" in {
    val expr = Expression.MatchCondition("id", false, 1L)(using Encoder[Long])
    assert(expr.statement === "id = ?")
    assert(expr.parameter.size === 1)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "NOT id = ?")
    assert(notExpr.parameter.size === 1)
  }
  
  "Expression.OrMore" should "construct the correct SQL statement" in {
    val expr = Expression.OrMore("id", false, 1L)(using Encoder[Long])
    assert(expr.statement === "id >= ?")
    assert(expr.parameter.size === 1)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "NOT id >= ?")
    assert(notExpr.parameter.size === 1)
  }
  
  "Expression.Over" should "construct the correct SQL statement" in {
    val expr = Expression.Over("id", false, 1L)(using Encoder[Long])
    assert(expr.statement === "id > ?")
    assert(expr.parameter.size === 1)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "NOT id > ?")
    assert(notExpr.parameter.size === 1)
  }
  
  "Expression.LessThanOrEqualTo" should "construct the correct SQL statement" in {
    val expr = Expression.LessThanOrEqualTo("id", false, 1L)(using Encoder[Long])
    assert(expr.statement === "id <= ?")
    assert(expr.parameter.size === 1)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "NOT id <= ?")
    assert(notExpr.parameter.size === 1)
  }
  
  "Expression.LessThan" should "construct the correct SQL statement" in {
    val expr = Expression.LessThan("id", false, 1L)(using Encoder[Long])
    assert(expr.statement === "id < ?")
    assert(expr.parameter.size === 1)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "NOT id < ?")
    assert(notExpr.parameter.size === 1)
  }
  
  "Expression.NotEqual" should "construct the correct SQL statement" in {
    val expr = Expression.NotEqual("<>", "id", false, 1L)(using Encoder[Long])
    assert(expr.statement === "id <> ?")
    assert(expr.parameter.size === 1)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "NOT id <> ?")
    assert(notExpr.parameter.size === 1)
  }
  
  "Expression.Is" should "construct the correct SQL statement" in {
    val expr = Expression.Is("id", false, "TRUE")
    assert(expr.statement === "id IS TRUE")
    assert(expr.parameter.isEmpty)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "id IS NOT TRUE")
    assert(notExpr.parameter.isEmpty)
    
    val nullExpr = Expression.Is("id", false, "NULL")
    assert(nullExpr.statement === "id IS NULL")
    
    val notNullExpr = Expression.Is("id", true, "NULL")
    assert(notNullExpr.statement === "id IS NOT NULL")
  }
  
  "Expression.NullSafeEqual" should "construct the correct SQL statement" in {
    val expr = Expression.NullSafeEqual("id", false, 1L)(using Encoder[Long])
    assert(expr.statement === "id <=> ?")
    assert(expr.parameter.size === 1)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "NOT id <=> ?")
    assert(notExpr.parameter.size === 1)
  }
  
  "Expression.In" should "construct the correct SQL statement" in {
    val expr = Expression.In("id", false, 1L, 2L, 3L)(using Encoder[Long])
    assert(expr.statement === "id IN (?, ?, ?)")
    assert(expr.parameter.size === 3)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "id NOT IN (?, ?, ?)")
    assert(notExpr.parameter.size === 3)
  }
  
  "Expression.Between" should "construct the correct SQL statement" in {
    val expr = Expression.Between("id", false, 1L, 10L)(using Encoder[Long])
    assert(expr.statement === "id BETWEEN ? AND ?")
    assert(expr.parameter.size === 2)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "id NOT BETWEEN ? AND ?")
    assert(notExpr.parameter.size === 2)
  }
  
  "Expression.Like" should "construct the correct SQL statement" in {
    val expr = Expression.Like("name", false, "%test%")(using Encoder[String])
    assert(expr.statement === "name LIKE ?")
    assert(expr.parameter.size === 1)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "NOT name LIKE ?")
    assert(notExpr.parameter.size === 1)
  }
  
  "Expression.LikeEscape" should "construct the correct SQL statement" in {
    val expr = Expression.LikeEscape("name", false, "T%", "$")(using Encoder[String])
    assert(expr.statement === "name LIKE ? ESCAPE ?")
    assert(expr.parameter.size === 2)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "NOT name LIKE ? ESCAPE ?")
    assert(notExpr.parameter.size === 2)
  }
  
  "Expression.Regexp" should "construct the correct SQL statement" in {
    val expr = Expression.Regexp("name", false, "^[A-Z].*")(using Encoder[String])
    assert(expr.statement === "name REGEXP ?")
    assert(expr.parameter.size === 1)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "NOT name REGEXP ?")
    assert(notExpr.parameter.size === 1)
  }
  
  "Expression.Div" should "construct the correct SQL statement" in {
    val expr = Expression.Div("id", false, 5L, 2L)(using Encoder[Long])
    assert(expr.statement === "id DIV ? = ?")
    assert(expr.parameter.size === 2)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "NOT id DIV ? = ?")
    assert(notExpr.parameter.size === 2)
  }
  
  "Expression.Mod" should "construct the correct SQL statement" in {
    val expr = Expression.Mod("MOD", "id", false, 5L, 2L)(using Encoder[Long])
    assert(expr.statement === "id MOD ? = ?")
    assert(expr.parameter.size === 2)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "NOT id MOD ? = ?")
    assert(notExpr.parameter.size === 2)
    
    val modExpr = Expression.Mod("%", "id", false, 5L, 2L)(using Encoder[Long])
    assert(modExpr.statement === "id % ? = ?")
  }
  
  "Expression.LeftShift" should "construct the correct SQL statement" in {
    val expr = Expression.LeftShift("id", false, 2L)(using Encoder[Long])
    assert(expr.statement === "id << ?")
    assert(expr.parameter.size === 1)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "NOT id << ?")
    assert(notExpr.parameter.size === 1)
  }
  
  "Expression.RightShift" should "construct the correct SQL statement" in {
    val expr = Expression.RightShift("id", false, 2L)(using Encoder[Long])
    assert(expr.statement === "id >> ?")
    assert(expr.parameter.size === 1)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "NOT id >> ?")
    assert(notExpr.parameter.size === 1)
  }
  
  "Expression.BitXOR" should "construct the correct SQL statement" in {
    val expr = Expression.BitXOR("id", false, 5L)(using Encoder[Long])
    assert(expr.statement === "id ^ ?")
    assert(expr.parameter.size === 1)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "NOT id ^ ?")
    assert(notExpr.parameter.size === 1)
  }
  
  "Expression.BitFlip" should "construct the correct SQL statement" in {
    val expr = Expression.BitFlip("id", false, 5L)(using Encoder[Long])
    assert(expr.statement === "~id = ?")
    assert(expr.parameter.size === 1)
    
    val notExpr = expr.NOT
    assert(notExpr.statement === "NOT ~id = ?")
    assert(notExpr.parameter.size === 1)
  }
  
  "Expression.SubQuery" should "construct the correct SQL statement" in {
    val subQuery = sql"SELECT id FROM users WHERE active = true"
    val expr = Expression.SubQuery("=", "id", subQuery)
    assert(expr.statement === "id = (SELECT id FROM users WHERE active = true)")
  }
  
  "Expression.JoinQuery" should "construct the correct SQL statement" in {
    val leftCol = Column.Impl[Long]("customer_id")
    val rightCol = Column.Impl[Long]("order_id")
    
    val expr = Expression.JoinQuery("=", leftCol, rightCol)
    assert(expr.statement === "`customer_id` = `order_id`")
    assert(expr.parameter.isEmpty)
    
    val leftAliased = leftCol.as("c")
    val rightAliased = rightCol.as("o")
    
    val aliasedExpr = Expression.JoinQuery("=", leftAliased, rightAliased)
    assert(aliasedExpr.statement === "c = o")
  }
  
  "Expression.Pair" should "combine expressions with logical operators" in {
    val expr1 = Expression.MatchCondition("id", false, 1L)(using Encoder[Long])
    val expr2 = Expression.Like("name", false, "test%")(using Encoder[String])
    
    // AND
    val andExpr = Expression.Pair(" AND ", expr1, expr2)
    assert(andExpr.statement === "(id = ? AND name LIKE ?)")
    assert(andExpr.parameter.size === 2)
    
    // OR
    val orExpr = Expression.Pair(" OR ", expr1, expr2)
    assert(orExpr.statement === "(id = ? OR name LIKE ?)")
    assert(orExpr.parameter.size === 2)
    
    // XOR
    val xorExpr = Expression.Pair(" XOR ", expr1, expr2)
    assert(xorExpr.statement === "(id = ? XOR name LIKE ?)")
    assert(xorExpr.parameter.size === 2)
    
    // Complex nesting
    val expr3 = Expression.In("age", false, 20, 30, 40)(using Encoder[Int])
    val nestedAnd = Expression.Pair(" AND ", expr1, expr2)
    val complexExpr = Expression.Pair(" OR ", nestedAnd, expr3)
    
    assert(complexExpr.statement === "(id = ? AND name LIKE ? OR age IN (?, ?, ?))")
    assert(complexExpr.parameter.size === 5)
  }
  
  "Expression" should "support logical operators" in {
    val expr1 = Expression.MatchCondition("id", false, 1L)(using Encoder[Long])
    val expr2 = Expression.Like("name", false, "test%")(using Encoder[String])
    val expr3 = Expression.In("age", false, 20, 30, 40)(using Encoder[Int])
    
    // AND operator
    val andExpr = expr1 and expr2
    assert(andExpr.statement === "(id = ? AND name LIKE ?)")
    
    // && operator alias
    val andExpr2 = expr1 && expr2
    assert(andExpr2.statement === "(id = ? AND name LIKE ?)")
    
    // OR operator
    val orExpr = expr1 or expr2
    assert(orExpr.statement === "(id = ? OR name LIKE ?)")
    
    // || operator alias
    val orExpr2 = expr1 || expr2
    assert(orExpr2.statement === "(id = ? OR name LIKE ?)")
    
    // XOR operator
    val xorExpr = expr1 xor expr2
    assert(xorExpr.statement === "(id = ? XOR name LIKE ?)")
    
    // Complex combinations
    val complex = (expr1 && expr2) || expr3
    assert(complex.statement === "(id = ? AND name LIKE ? OR age IN (?, ?, ?))")
  }
