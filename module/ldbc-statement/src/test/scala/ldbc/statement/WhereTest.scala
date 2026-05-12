/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import ldbc.dsl.*
import ldbc.dsl.codec.{ Codec, Decoder, Encoder }

class WhereTest extends munit.FunSuite:

  // Simple table representation for testing
  case class TestTable()
  val table = TestTable()

  // Helper function to create an empty Column for testing
  def emptyColumn[B](using decoder: Decoder[B], encoder: Encoder[B]): Column[B] =
    Column.Impl[B]("empty", None, decoder, encoder)

  // Helper function to create a simple Where.Q instance
  def createQuery[B](statement: String, params: List[Parameter.Dynamic])(using
    Decoder[B],
    Encoder[B]
  ): Where.Q[TestTable, B] =
    Where.Q[TestTable, B](table, emptyColumn[B], statement, params, isFirst = true)

  // Helper function to create a simple Where.C instance
  def createCommand(statement: String, params: List[Parameter.Dynamic]): Where.C[TestTable] =
    Where.C[TestTable](table, statement, params, isFirst = true)

  // Create expressions for testing
  def idEquals(value:       Long):   Expression = Expression.MatchCondition("id", false, value)(using Encoder[Long])
  def nameEquals(value:     String): Expression = Expression.MatchCondition("name", false, value)(using Encoder[String])
  def ageGreaterThan(value: Int):    Expression = Expression.Over("age", false, value)(using Encoder[Int])

  test("Where.Q should add AND conditions") {
    val query  = createQuery[String]("SELECT name FROM users", Nil)
    val result = query.and(_ => idEquals(1L))

    assertEquals(result.statement, "SELECT name FROM users WHERE id = ?")
    assertEquals(result.params.size, 1)

    val result2 = result.and(_ => nameEquals("John"))
    assertEquals(result2.statement, "SELECT name FROM users WHERE id = ? AND name = ?")
    assertEquals(result2.params.size, 2)
  }

  test("Where.Q should add OR conditions") {
    val query  = createQuery[String]("SELECT name FROM users", Nil)
    val result = query.and(_ => idEquals(1L))

    val result2 = result.or(_ => nameEquals("John"))
    assertEquals(result2.statement, "SELECT name FROM users WHERE id = ? OR name = ?")
    assertEquals(result2.params.size, 2)
  }

  test("Where.Q should add XOR conditions") {
    val query  = createQuery[String]("SELECT name FROM users", Nil)
    val result = query.and(_ => idEquals(1L))

    val result2 = result.xor(_ => nameEquals("John"))
    assertEquals(result2.statement, "SELECT name FROM users WHERE id = ? XOR name = ?")
    assertEquals(result2.params.size, 2)
  }

  test("Where.Q should support && alias for AND") {
    val query  = createQuery[String]("SELECT name FROM users", Nil)
    val result = query.and(_ => idEquals(1L))

    val result2 = result.&&(_ => nameEquals("John"))
    assertEquals(result2.statement, "SELECT name FROM users WHERE id = ? && name = ?")
    assertEquals(result2.params.size, 2)
  }

  test("Where.Q should support || alias for OR") {
    val query  = createQuery[String]("SELECT name FROM users", Nil)
    val result = query.and(_ => idEquals(1L))

    val result2 = result.||(_ => nameEquals("John"))
    assertEquals(result2.statement, "SELECT name FROM users WHERE id = ? || name = ?")
    assertEquals(result2.params.size, 2)
  }

  test("Where.Q should handle optional AND conditions with andOpt(function)") {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    // With Some value
    val someOption: Option[String] = Some("John")
    val result1 = initialQuery.andOpt(_ => someOption.map(value => nameEquals(value)))
    assertEquals(result1.statement, "SELECT name FROM users WHERE id = ? AND name = ?")
    assertEquals(result1.params.size, 2)

    // With None value
    val noneOption: Option[String] = None
    val result2 = initialQuery.andOpt(_ => noneOption.map(value => nameEquals(value)))
    assertEquals(result2.statement, "SELECT name FROM users WHERE id = ?")
    assertEquals(result2.params.size, 1)
  }

  test("Where.Q should handle optional AND conditions with andOpt(option)(function)") {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    // With Some value
    val someOption: Option[String] = Some("John")
    val result1 = initialQuery.andOpt(someOption)((_, value) => nameEquals(value))
    assertEquals(result1.statement, "SELECT name FROM users WHERE id = ? AND name = ?")
    assertEquals(result1.params.size, 2)

    // With None value
    val noneOption: Option[String] = None
    val result2 = initialQuery.andOpt(noneOption)((_, value) => nameEquals(value))
    assertEquals(result2.statement, "SELECT name FROM users WHERE id = ?")
    assertEquals(result2.params.size, 1)
  }

  test("Where.Q should handle optional OR conditions with orOpt(function)") {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    // With Some value
    val someOption: Option[String] = Some("John")
    val result1 = initialQuery.orOpt(_ => someOption.map(value => nameEquals(value)))
    assertEquals(result1.statement, "SELECT name FROM users WHERE id = ? OR name = ?")
    assertEquals(result1.params.size, 2)

    // With None value
    val noneOption: Option[String] = None
    val result2 = initialQuery.orOpt(_ => noneOption.map(value => nameEquals(value)))
    assertEquals(result2.statement, "SELECT name FROM users WHERE id = ?")
    assertEquals(result2.params.size, 1)
  }

  test("Where.Q should handle optional OR conditions with orOpt(option)(function)") {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    // With Some value
    val someOption: Option[String] = Some("John")
    val result1 = initialQuery.orOpt(someOption)((_, value) => nameEquals(value))
    assertEquals(result1.statement, "SELECT name FROM users WHERE id = ? OR name = ?")
    assertEquals(result1.params.size, 2)

    // With None value
    val noneOption: Option[String] = None
    val result2 = initialQuery.orOpt(noneOption)((_, value) => nameEquals(value))
    assertEquals(result2.statement, "SELECT name FROM users WHERE id = ?")
    assertEquals(result2.params.size, 1)
  }

  test("Where.Q should handle optional XOR conditions with xorOpt(function)") {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    // With Some value
    val someOption: Option[String] = Some("John")
    val result1 = initialQuery.xorOpt(_ => someOption.map(value => nameEquals(value)))
    assertEquals(result1.statement, "SELECT name FROM users WHERE id = ? XOR name = ?")
    assertEquals(result1.params.size, 2)

    // With None value
    val noneOption: Option[String] = None
    val result2 = initialQuery.xorOpt(_ => noneOption.map(value => nameEquals(value)))
    assertEquals(result2.statement, "SELECT name FROM users WHERE id = ?")
    assertEquals(result2.params.size, 1)
  }

  test("Where.Q should handle optional XOR conditions with xorOpt(option)(function)") {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    // With Some value
    val someOption: Option[String] = Some("John")
    val result1 = initialQuery.xorOpt(someOption)((_, value) => nameEquals(value))
    assertEquals(result1.statement, "SELECT name FROM users WHERE id = ? XOR name = ?")
    assertEquals(result1.params.size, 2)

    // With None value
    val noneOption: Option[String] = None
    val result2 = initialQuery.xorOpt(noneOption)((_, value) => nameEquals(value))
    assertEquals(result2.statement, "SELECT name FROM users WHERE id = ?")
    assertEquals(result2.params.size, 1)
  }

  test("Where.Q should conditionally add AND based on boolean parameter") {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    val result1 = initialQuery.and(_ => nameEquals("John"), true)
    assertEquals(result1.statement, "SELECT name FROM users WHERE id = ? AND name = ?")
    assertEquals(result1.params.size, 2)

    val result2 = initialQuery.and(_ => nameEquals("John"), false)
    assertEquals(result2.statement, "SELECT name FROM users WHERE id = ?")
    assertEquals(result2.params.size, 1)
  }

  test("Where.Q should conditionally add OR based on boolean parameter") {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    val result1 = initialQuery.or(_ => nameEquals("John"), true)
    assertEquals(result1.statement, "SELECT name FROM users WHERE id = ? OR name = ?")
    assertEquals(result1.params.size, 2)

    val result2 = initialQuery.or(_ => nameEquals("John"), false)
    assertEquals(result2.statement, "SELECT name FROM users WHERE id = ?")
    assertEquals(result2.params.size, 1)
  }

  test("Where.Q should conditionally add XOR based on boolean parameter") {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    val result1 = initialQuery.xor(_ => nameEquals("John"), true)
    assertEquals(result1.statement, "SELECT name FROM users WHERE id = ? XOR name = ?")
    assertEquals(result1.params.size, 2)

    val result2 = initialQuery.xor(_ => nameEquals("John"), false)
    assertEquals(result2.statement, "SELECT name FROM users WHERE id = ?")
    assertEquals(result2.params.size, 1)
  }

  test("Where.Q should conditionally add && based on boolean parameter") {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    val result1 = initialQuery.&&(_ => nameEquals("John"), true)
    assertEquals(result1.statement, "SELECT name FROM users WHERE id = ? && name = ?")
    assertEquals(result1.params.size, 2)

    val result2 = initialQuery.&&(_ => nameEquals("John"), false)
    assertEquals(result2.statement, "SELECT name FROM users WHERE id = ?")
    assertEquals(result2.params.size, 1)
  }

  test("Where.Q should conditionally add || based on boolean parameter") {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    val result1 = initialQuery.||(_ => nameEquals("John"), true)
    assertEquals(result1.statement, "SELECT name FROM users WHERE id = ? || name = ?")
    assertEquals(result1.params.size, 2)

    val result2 = initialQuery.||(_ => nameEquals("John"), false)
    assertEquals(result2.statement, "SELECT name FROM users WHERE id = ?")
    assertEquals(result2.params.size, 1)
  }

  test("Where.Q should chain multiple conditions together") {
    val query = createQuery[String]("SELECT name FROM users", Nil)

    val result = query
      .and(_ => idEquals(1L))
      .and(_ => nameEquals("John"))
      .or(_ => ageGreaterThan(30))

    assertEquals(result.statement, "SELECT name FROM users WHERE id = ? AND name = ? OR age > ?")
    assertEquals(result.params.size, 3)
  }

  test("Where.Q should generate groupBy correctly") {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    val groupedQuery = initialQuery.groupBy(_ => Column.Impl[String]("department"))
    assertEquals(groupedQuery.statement, "SELECT name FROM users WHERE id = ? GROUP BY `department`")
    assertEquals(groupedQuery.params.size, 1)

    val aliasedColumn       = Column.Impl[String]("department").as("dept")
    val groupedByAliasQuery = initialQuery.groupBy(_ => aliasedColumn)
    assertEquals(groupedByAliasQuery.statement, "SELECT name FROM users WHERE id = ? GROUP BY dept")
    assertEquals(groupedByAliasQuery.params.size, 1)
  }

  test("Where.C should add AND conditions") {
    val command = createCommand("DELETE FROM users", Nil)
    val result  = command.and(_ => idEquals(1L))

    assertEquals(result.statement, "DELETE FROM users WHERE id = ?")
    assertEquals(result.params.size, 1)

    val result2 = result.and(_ => nameEquals("John"))
    assertEquals(result2.statement, "DELETE FROM users WHERE id = ? AND name = ?")
    assertEquals(result2.params.size, 2)
  }

  test("Where.C should add OR conditions") {
    val command = createCommand("DELETE FROM users", Nil)
    val result  = command.and(_ => idEquals(1L))

    val result2 = result.or(_ => nameEquals("John"))
    assertEquals(result2.statement, "DELETE FROM users WHERE id = ? OR name = ?")
    assertEquals(result2.params.size, 2)
  }

  test("Where.C should add XOR conditions") {
    val command = createCommand("DELETE FROM users", Nil)
    val result  = command.and(_ => idEquals(1L))

    val result2 = result.xor(_ => nameEquals("John"))
    assertEquals(result2.statement, "DELETE FROM users WHERE id = ? XOR name = ?")
    assertEquals(result2.params.size, 2)
  }

  test("Where.C should support && alias for AND") {
    val command = createCommand("DELETE FROM users", Nil)
    val result  = command.and(_ => idEquals(1L))

    val result2 = result.&&(_ => nameEquals("John"))
    assertEquals(result2.statement, "DELETE FROM users WHERE id = ? && name = ?")
    assertEquals(result2.params.size, 2)
  }

  test("Where.C should support || alias for OR") {
    val command = createCommand("DELETE FROM users", Nil)
    val result  = command.and(_ => idEquals(1L))

    val result2 = result.||(_ => nameEquals("John"))
    assertEquals(result2.statement, "DELETE FROM users WHERE id = ? || name = ?")
    assertEquals(result2.params.size, 2)
  }

  test("Where.C should handle optional conditions and boolean flags similar to Where.Q") {
    val command        = createCommand("UPDATE users SET active = true", Nil)
    val initialCommand = command.and(_ => idEquals(1L))

    // Test andOpt with function
    val someNameOption: Option[String] = Some("John")
    val result1 = initialCommand.andOpt(_ => someNameOption.map(value => nameEquals(value)))
    assertEquals(result1.statement, "UPDATE users SET active = true WHERE id = ? AND name = ?")
    assertEquals(result1.params.size, 2)

    // Test conditional with boolean flag
    val result2 = initialCommand.or(_ => ageGreaterThan(30), false)
    assertEquals(result2.statement, "UPDATE users SET active = true WHERE id = ?")
    assertEquals(result2.params.size, 1)
  }

  test("Where.C should chain multiple conditions together") {
    val command = createCommand("UPDATE users SET active = false", Nil)

    val result = command
      .and(_ => idEquals(1L))
      .and(_ => nameEquals("John"))
      .or(_ => ageGreaterThan(30))

    assertEquals(result.statement, "UPDATE users SET active = false WHERE id = ? AND name = ? OR age > ?")
    assertEquals(result.params.size, 3)
  }
