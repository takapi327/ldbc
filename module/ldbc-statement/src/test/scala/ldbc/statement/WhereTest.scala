/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import org.scalatest.flatspec.AnyFlatSpec

import ldbc.dsl.*
import ldbc.dsl.codec.{ Codec, Decoder, Encoder }

class WhereTest extends AnyFlatSpec:

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

  "Where.Q" should "add AND conditions" in {
    val query  = createQuery[String]("SELECT name FROM users", Nil)
    val result = query.and(_ => idEquals(1L))

    assert(result.statement === "SELECT name FROM users WHERE id = ?")
    assert(result.params.size === 1)

    val result2 = result.and(_ => nameEquals("John"))
    assert(result2.statement === "SELECT name FROM users WHERE id = ? AND name = ?")
    assert(result2.params.size === 2)
  }

  it should "add OR conditions" in {
    val query  = createQuery[String]("SELECT name FROM users", Nil)
    val result = query.and(_ => idEquals(1L))

    val result2 = result.or(_ => nameEquals("John"))
    assert(result2.statement === "SELECT name FROM users WHERE id = ? OR name = ?")
    assert(result2.params.size === 2)
  }

  it should "add XOR conditions" in {
    val query  = createQuery[String]("SELECT name FROM users", Nil)
    val result = query.and(_ => idEquals(1L))

    val result2 = result.xor(_ => nameEquals("John"))
    assert(result2.statement === "SELECT name FROM users WHERE id = ? XOR name = ?")
    assert(result2.params.size === 2)
  }

  it should "support && alias for AND" in {
    val query  = createQuery[String]("SELECT name FROM users", Nil)
    val result = query.and(_ => idEquals(1L))

    val result2 = result.&&(_ => nameEquals("John"))
    assert(result2.statement === "SELECT name FROM users WHERE id = ? && name = ?")
    assert(result2.params.size === 2)
  }

  it should "support || alias for OR" in {
    val query  = createQuery[String]("SELECT name FROM users", Nil)
    val result = query.and(_ => idEquals(1L))

    val result2 = result.||(_ => nameEquals("John"))
    assert(result2.statement === "SELECT name FROM users WHERE id = ? || name = ?")
    assert(result2.params.size === 2)
  }

  it should "handle optional AND conditions with andOpt(function)" in {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    // With Some value
    val someOption: Option[String] = Some("John")
    val result1 = initialQuery.andOpt(table => someOption.map(value => nameEquals(value)))
    assert(result1.statement === "SELECT name FROM users WHERE id = ? AND name = ?")
    assert(result1.params.size === 2)

    // With None value
    val noneOption: Option[String] = None
    val result2 = initialQuery.andOpt(table => noneOption.map(value => nameEquals(value)))
    assert(result2.statement === "SELECT name FROM users WHERE id = ?")
    assert(result2.params.size === 1)
  }

  it should "handle optional AND conditions with andOpt(option)(function)" in {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    // With Some value
    val someOption: Option[String] = Some("John")
    val result1 = initialQuery.andOpt(someOption)((_, value) => nameEquals(value))
    assert(result1.statement === "SELECT name FROM users WHERE id = ? AND name = ?")
    assert(result1.params.size === 2)

    // With None value
    val noneOption: Option[String] = None
    val result2 = initialQuery.andOpt(noneOption)((_, value) => nameEquals(value))
    assert(result2.statement === "SELECT name FROM users WHERE id = ?")
    assert(result2.params.size === 1)
  }

  it should "handle optional OR conditions with orOpt(function)" in {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    // With Some value
    val someOption: Option[String] = Some("John")
    val result1 = initialQuery.orOpt(table => someOption.map(value => nameEquals(value)))
    assert(result1.statement === "SELECT name FROM users WHERE id = ? OR name = ?")
    assert(result1.params.size === 2)

    // With None value
    val noneOption: Option[String] = None
    val result2 = initialQuery.orOpt(table => noneOption.map(value => nameEquals(value)))
    assert(result2.statement === "SELECT name FROM users WHERE id = ?")
    assert(result2.params.size === 1)
  }

  it should "handle optional OR conditions with orOpt(option)(function)" in {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    // With Some value
    val someOption: Option[String] = Some("John")
    val result1 = initialQuery.orOpt(someOption)((_, value) => nameEquals(value))
    assert(result1.statement === "SELECT name FROM users WHERE id = ? OR name = ?")
    assert(result1.params.size === 2)

    // With None value
    val noneOption: Option[String] = None
    val result2 = initialQuery.orOpt(noneOption)((_, value) => nameEquals(value))
    assert(result2.statement === "SELECT name FROM users WHERE id = ?")
    assert(result2.params.size === 1)
  }

  it should "handle optional XOR conditions with xorOpt(function)" in {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    // With Some value
    val someOption: Option[String] = Some("John")
    val result1 = initialQuery.xorOpt(table => someOption.map(value => nameEquals(value)))
    assert(result1.statement === "SELECT name FROM users WHERE id = ? XOR name = ?")
    assert(result1.params.size === 2)

    // With None value
    val noneOption: Option[String] = None
    val result2 = initialQuery.xorOpt(table => noneOption.map(value => nameEquals(value)))
    assert(result2.statement === "SELECT name FROM users WHERE id = ?")
    assert(result2.params.size === 1)
  }

  it should "handle optional XOR conditions with xorOpt(option)(function)" in {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    // With Some value
    val someOption: Option[String] = Some("John")
    val result1 = initialQuery.xorOpt(someOption)((_, value) => nameEquals(value))
    assert(result1.statement === "SELECT name FROM users WHERE id = ? XOR name = ?")
    assert(result1.params.size === 2)

    // With None value
    val noneOption: Option[String] = None
    val result2 = initialQuery.xorOpt(noneOption)((_, value) => nameEquals(value))
    assert(result2.statement === "SELECT name FROM users WHERE id = ?")
    assert(result2.params.size === 1)
  }

  it should "conditionally add AND based on boolean parameter" in {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    val result1 = initialQuery.and(_ => nameEquals("John"), true)
    assert(result1.statement === "SELECT name FROM users WHERE id = ? AND name = ?")
    assert(result1.params.size === 2)

    val result2 = initialQuery.and(_ => nameEquals("John"), false)
    assert(result2.statement === "SELECT name FROM users WHERE id = ?")
    assert(result2.params.size === 1)
  }

  it should "conditionally add OR based on boolean parameter" in {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    val result1 = initialQuery.or(_ => nameEquals("John"), true)
    assert(result1.statement === "SELECT name FROM users WHERE id = ? OR name = ?")
    assert(result1.params.size === 2)

    val result2 = initialQuery.or(_ => nameEquals("John"), false)
    assert(result2.statement === "SELECT name FROM users WHERE id = ?")
    assert(result2.params.size === 1)
  }

  it should "conditionally add XOR based on boolean parameter" in {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    val result1 = initialQuery.xor(_ => nameEquals("John"), true)
    assert(result1.statement === "SELECT name FROM users WHERE id = ? XOR name = ?")
    assert(result1.params.size === 2)

    val result2 = initialQuery.xor(_ => nameEquals("John"), false)
    assert(result2.statement === "SELECT name FROM users WHERE id = ?")
    assert(result2.params.size === 1)
  }

  it should "conditionally add && based on boolean parameter" in {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    val result1 = initialQuery.&&(_ => nameEquals("John"), true)
    assert(result1.statement === "SELECT name FROM users WHERE id = ? && name = ?")
    assert(result1.params.size === 2)

    val result2 = initialQuery.&&(_ => nameEquals("John"), false)
    assert(result2.statement === "SELECT name FROM users WHERE id = ?")
    assert(result2.params.size === 1)
  }

  it should "conditionally add || based on boolean parameter" in {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    val result1 = initialQuery.||(_ => nameEquals("John"), true)
    assert(result1.statement === "SELECT name FROM users WHERE id = ? || name = ?")
    assert(result1.params.size === 2)

    val result2 = initialQuery.||(_ => nameEquals("John"), false)
    assert(result2.statement === "SELECT name FROM users WHERE id = ?")
    assert(result2.params.size === 1)
  }

  it should "chain multiple conditions together" in {
    val query = createQuery[String]("SELECT name FROM users", Nil)

    val result = query
      .and(_ => idEquals(1L))
      .and(_ => nameEquals("John"))
      .or(_ => ageGreaterThan(30))

    assert(result.statement === "SELECT name FROM users WHERE id = ? AND name = ? OR age > ?")
    assert(result.params.size === 3)
  }

  it should "generate groupBy correctly" in {
    val query        = createQuery[String]("SELECT name FROM users", Nil)
    val initialQuery = query.and(_ => idEquals(1L))

    val groupedQuery = initialQuery.groupBy(_ => Column.Impl[String]("department"))
    assert(groupedQuery.statement === "SELECT name FROM users WHERE id = ? GROUP BY `department`")
    assert(groupedQuery.params.size === 1)

    val aliasedColumn       = Column.Impl[String]("department").as("dept")
    val groupedByAliasQuery = initialQuery.groupBy(_ => aliasedColumn)
    assert(groupedByAliasQuery.statement === "SELECT name FROM users WHERE id = ? GROUP BY dept")
    assert(groupedByAliasQuery.params.size === 1)
  }

  "Where.C" should "add AND conditions" in {
    val command = createCommand("DELETE FROM users", Nil)
    val result  = command.and(_ => idEquals(1L))

    assert(result.statement === "DELETE FROM users WHERE id = ?")
    assert(result.params.size === 1)

    val result2 = result.and(_ => nameEquals("John"))
    assert(result2.statement === "DELETE FROM users WHERE id = ? AND name = ?")
    assert(result2.params.size === 2)
  }

  it should "add OR conditions" in {
    val command = createCommand("DELETE FROM users", Nil)
    val result  = command.and(_ => idEquals(1L))

    val result2 = result.or(_ => nameEquals("John"))
    assert(result2.statement === "DELETE FROM users WHERE id = ? OR name = ?")
    assert(result2.params.size === 2)
  }

  it should "add XOR conditions" in {
    val command = createCommand("DELETE FROM users", Nil)
    val result  = command.and(_ => idEquals(1L))

    val result2 = result.xor(_ => nameEquals("John"))
    assert(result2.statement === "DELETE FROM users WHERE id = ? XOR name = ?")
    assert(result2.params.size === 2)
  }

  it should "support && alias for AND" in {
    val command = createCommand("DELETE FROM users", Nil)
    val result  = command.and(_ => idEquals(1L))

    val result2 = result.&&(_ => nameEquals("John"))
    assert(result2.statement === "DELETE FROM users WHERE id = ? && name = ?")
    assert(result2.params.size === 2)
  }

  it should "support || alias for OR" in {
    val command = createCommand("DELETE FROM users", Nil)
    val result  = command.and(_ => idEquals(1L))

    val result2 = result.||(_ => nameEquals("John"))
    assert(result2.statement === "DELETE FROM users WHERE id = ? || name = ?")
    assert(result2.params.size === 2)
  }

  it should "handle optional conditions and boolean flags similar to Where.Q" in {
    val command        = createCommand("UPDATE users SET active = true", Nil)
    val initialCommand = command.and(_ => idEquals(1L))

    // Test andOpt with function
    val someNameOption: Option[String] = Some("John")
    val result1 = initialCommand.andOpt(table => someNameOption.map(value => nameEquals(value)))
    assert(result1.statement === "UPDATE users SET active = true WHERE id = ? AND name = ?")
    assert(result1.params.size === 2)

    // Test conditional with boolean flag
    val result2 = initialCommand.or(_ => ageGreaterThan(30), false)
    assert(result2.statement === "UPDATE users SET active = true WHERE id = ?")
    assert(result2.params.size === 1)
  }

  it should "chain multiple conditions together" in {
    val command = createCommand("UPDATE users SET active = false", Nil)

    val result = command
      .and(_ => idEquals(1L))
      .and(_ => nameEquals("John"))
      .or(_ => ageGreaterThan(30))

    assert(result.statement === "UPDATE users SET active = false WHERE id = ? AND name = ? OR age > ?")
    assert(result.params.size === 3)
  }
