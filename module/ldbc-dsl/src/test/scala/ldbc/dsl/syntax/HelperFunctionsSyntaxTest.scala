/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.syntax

import cats.data.NonEmptyList
import cats.syntax.all.*

import munit.CatsEffectSuite

import ldbc.dsl.*
import ldbc.dsl.codec.{ Codec, Encoder }

class HelperFunctionsSyntaxTest extends CatsEffectSuite with HelperFunctionsSyntax:

  test("sc function should create static parameter") {
    val table = sc("users")
    assertEquals(table, Parameter.Static("users"))
  }

  test("sc function should be usable in SQL interpolation") {
    val table = sc("users")
    val sql   = sql"SELECT * FROM $table WHERE id = ${ 1L }"
    assertEquals(sql.statement, "SELECT * FROM users WHERE id = ?")
    assertEquals(sql.params.size, 1)
  }

  test("values function with List should create VALUES clause") {
    val data = List((1, "a"), (2, "b"), (3, "c"))
    data.toNel match
      case Some(nel) =>
        val sql = values(nel)
        assertEquals(sql.statement, "VALUES(?,?),(?,?),(?,?)")
        assertEquals(sql.params.size, 6)
      case None => fail("List should not be empty")
  }

  test("in function with varargs should create IN clause") {
    val sql = in(sql"`id`", 1, 2, 3, 4, 5)
    assertEquals(sql.statement, "(`id` IN (?,?,?,?,?))")
    assertEquals(sql.params.size, 5)
  }

  test("in function with empty collection should return None with inOpt") {
    val result = inOpt(sql"`id`", List.empty[Int])
    assert(result.isEmpty)
  }

  test("inOpt function with non-empty List should create IN clause") {
    val result = inOpt(sql"`id`", List(1, 2, 3))
    assert(result.isDefined)
    result.foreach { sql =>
      assertEquals(sql.statement, "(`id` IN (?,?,?))")
      assertEquals(sql.params.size, 3)
    }
  }

  test("notIn function with varargs should create NOT IN clause") {
    val sql = notIn(sql"`status`", "active", "pending")
    assertEquals(sql.statement, "(`status` NOT IN (?,?))")
    assertEquals(sql.params.size, 2)
  }

  test("notInOpt function with empty collection should return None") {
    val result = notInOpt(sql"`status`", List.empty[String])
    assert(result.isEmpty)
  }

  test("notInOpt function with non-empty collection should create NOT IN clause") {
    val result = notInOpt(sql"`status`", List("inactive", "deleted"))
    assert(result.isDefined)
    result.foreach { sql =>
      assertEquals(sql.statement, "(`status` NOT IN (?,?))")
      assertEquals(sql.params.size, 2)
    }
  }

  test("and function with grouping=false should not add outer parentheses") {
    val sql = and(NonEmptyList.of(sql"a = 1", sql"b = 2"), grouping = false)
    assertEquals(sql.statement, "(a = 1) AND (b = 2)")
  }

  test("and function with grouping=true should add outer parentheses") {
    val sql = and(NonEmptyList.of(sql"a = 1", sql"b = 2"), grouping = true)
    assertEquals(sql.statement, "((a = 1) AND (b = 2))")
  }

  test("andOpt with all None values should return None") {
    val result = andOpt(None, None, None)
    assert(result.isEmpty)
  }

  test("andOpt with mixed Some and None values should return AND of defined values") {
    val result = andOpt(Some(sql"a = 1"), None, Some(sql"c = 3"))
    assert(result.isDefined)
    result.foreach { sql =>
      assertEquals(sql.statement, "((a = 1) AND (c = 3))")
    }
  }

  test("andFallbackTrue with empty collection should return TRUE") {
    val sql = andFallbackTrue(List.empty[SQL])
    assertEquals(sql.statement, "TRUE")
  }

  test("andFallbackTrue with non-empty collection should return AND clause") {
    val sql = andFallbackTrue(List(sql"a = 1", sql"b = 2"))
    assertEquals(sql.statement, "((a = 1) AND (b = 2))")
  }

  test("or function with grouping=false should not add outer parentheses") {
    val sql = or(NonEmptyList.of(sql"x = 1", sql"y = 2"), grouping = false)
    assertEquals(sql.statement, "(x = 1) OR (y = 2)")
  }

  test("or function with varargs should create OR clause") {
    val sql = or(sql"x = 1", sql"y = 2", sql"z = 3")
    assertEquals(sql.statement, "((x = 1) OR (y = 2) OR (z = 3))")
  }

  test("orOpt with all None values should return None") {
    val result = orOpt(None, None)
    assert(result.isEmpty)
  }

  test("orOpt with collection should handle grouping parameter") {
    val result = orOpt(List(sql"a = 1", sql"b = 2"), grouping = false)
    assert(result.isDefined)
    result.foreach { sql =>
      assertEquals(sql.statement, "(a = 1) OR (b = 2)")
    }
  }

  test("orFallbackFalse with empty collection should return FALSE") {
    val sql = orFallbackFalse(List.empty[SQL])
    assertEquals(sql.statement, "FALSE")
  }

  test("orFallbackFalse with non-empty collection should return OR clause") {
    val sql = orFallbackFalse(List(sql"a = 1", sql"b = 2"))
    assertEquals(sql.statement, "((a = 1) OR (b = 2))")
  }

  test("whereAnd with NonEmptyList should create WHERE AND clause") {
    val sql = whereAnd(NonEmptyList.of(sql"active = true", sql"age > 18"))
    assertEquals(sql.statement, "WHERE (active = true) AND (age > 18)")
  }

  test("whereAndOpt with empty list should return empty SQL") {
    val sql = whereAndOpt(List.empty[SQL])
    assertEquals(sql.statement, "")
  }

  test("whereAndOpt with None values should return empty SQL") {
    val sql = whereAndOpt(None, None, None)
    assertEquals(sql.statement, "")
  }

  test("whereOr with NonEmptyList should create WHERE OR clause") {
    val sql = whereOr(NonEmptyList.of(sql"status = 'active'", sql"status = 'pending'"))
    assertEquals(sql.statement, "WHERE (status = 'active') OR (status = 'pending')")
  }

  test("whereOrOpt with empty list should return empty SQL") {
    val sql = whereOrOpt(List.empty[SQL])
    assertEquals(sql.statement, "")
  }

  test("set with NonEmptyList should create SET clause") {
    val sql = set(NonEmptyList.of(sql"name = 'John'", sql"age = 30"))
    assertEquals(sql.statement, "SET name = 'John',age = 30")
  }

  test("parentheses should wrap SQL in parentheses") {
    val sql = parentheses(sql"1 + 1 = 2")
    assertEquals(sql.statement, "(1 + 1 = 2)")
  }

  test("comma with NonEmptyList should join with commas") {
    val sql = comma(NonEmptyList.of(sql"a", sql"b", sql"c"))
    assertEquals(sql.statement, "a,b,c")
  }

  test("orderBy with varargs should create ORDER BY clause") {
    val sql = orderBy(sql"created_at DESC", sql"id ASC")
    assertEquals(sql.statement, "ORDER BY created_at DESC,id ASC")
  }

  test("orderByOpt with empty list should return empty SQL") {
    val sql = orderByOpt(List.empty[SQL])
    assertEquals(sql.statement, "")
  }

  test("orderByOpt with Some and None values should handle properly") {
    val sql = orderByOpt(Some(sql"name"), None, Some(sql"id"))
    assertEquals(sql.statement, "ORDER BY name,id")
  }

  test("Complex query composition using multiple helper functions") {
    val table      = sc("users")
    val conditions = List(
      Some(sql"active = ${ true }"),
      Some(sql"age >= ${ 18 }"),
      None
    )

    val query = sql"SELECT * FROM $table " ++
      whereAndOpt(conditions.flatten) ++
      sql" " ++
      orderBy(sql"created_at DESC")

    assertEquals(
      query.statement,
      "SELECT * FROM users WHERE (active = ?) AND (age >= ?) ORDER BY created_at DESC"
    )
    assertEquals(query.params.size, 2)
  }

  test("values function with custom case class using Codec") {
    case class User(id: Int, name: String, email: String)
    given Codec[User] = Codec.derived[User]

    val users = NonEmptyList.of(
      User(1, "Alice", "alice@example.com"),
      User(2, "Bob", "bob@example.com")
    )

    val sql = sql"INSERT INTO users (id, name, email) " ++ values(users)
    assertEquals(sql.statement, "INSERT INTO users (id, name, email) VALUES(?,?,?),(?,?,?)")
    assertEquals(sql.params.size, 6)
  }

  test("in function with custom type using Encoder") {
    case class UserId(value: Long)
    given Encoder[UserId] = Encoder[Long].contramap(_.value)

    val userIds = NonEmptyList.of(UserId(1), UserId(2), UserId(3))
    val sql     = in(sql"`user_id`", userIds)
    assertEquals(sql.statement, "(`user_id` IN (?,?,?))")
    assertEquals(sql.params.size, 3)
  }

  test("DBIO operations should be available through implicit conversion") {
    val dbio: DBIO[Int]     = DBIO.pure(42)
    val ops:  DBIO.Ops[Int] = dbio // implicit conversion should work

    // Verify the conversion works by checking instance type
    assert(ops.isInstanceOf[DBIO.Ops[_]])
  }

  test("syncDBIO should be available as implicit Sync instance") {
    val sync = syncDBIO

    // Test basic Sync operations
    val pureDBIO       = sync.pure(42)
    val mappedDBIO     = sync.map(pureDBIO)(_ + 1)
    val flatMappedDBIO = sync.flatMap(pureDBIO)(x => sync.pure(x * 2))

    // Just verify the operations compile and return DBIO instances
    assert(pureDBIO.isInstanceOf[DBIO[_]])
    assert(mappedDBIO.isInstanceOf[DBIO[_]])
    assert(flatMappedDBIO.isInstanceOf[DBIO[_]])
  }

  test("Complex WHERE clause with nested AND/OR conditions") {
    val statusCondition  = in(sql"`status`", "active", "pending")
    val ageCondition     = sql"`age` >= ${ 18 }"
    val countryCondition = sql"`country` = ${ "US" }"

    val whereClause = whereAndOpt(
      Some(statusCondition),
      Some(or(ageCondition, countryCondition))
    )

    assertEquals(
      whereClause.statement,
      "WHERE ((`status` IN (?,?))) AND (((`age` >= ?) OR (`country` = ?)))"
    )
  }

  test("SET clause with conditional updates") {
    val updates = List(
      Some(sql"`name` = ${ "John" }"),
      None, // Skip null update
      Some(sql"`updated_at` = NOW()")
    )

    updates.flatten.toNel match
      case Some(nel) =>
        val sql = sql"UPDATE users " ++ set(nel)
        assertEquals(sql.statement, "UPDATE users SET `name` = ?,`updated_at` = NOW()")
        assertEquals(sql.params.size, 1)
      case None => fail("Updates should not be empty")
  }

  test("Multiple comma-separated values with parentheses") {
    val values = NonEmptyList.of(
      parentheses(sql"1, 2"),
      parentheses(sql"3, 4"),
      parentheses(sql"5, 6")
    )

    val sql = comma(values)
    assertEquals(sql.statement, "(1, 2),(3, 4),(5, 6)")
  }
