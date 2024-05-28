/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import cats.data.NonEmptyList

import munit.CatsEffectSuite

import ldbc.dsl.io.*

class HelperFunctionTest extends munit.CatsEffectSuite:

  test(
    "The statement that constructs VALUES with multiple values of the same type will be the same as the string specified."
  ) {
    val sql = q"INSERT INTO `table` (`column1`, `column2`) " ++ values(NonEmptyList.of(1, 2))
    assertEquals(sql.statement, "INSERT INTO `table` (`column1`, `column2`) VALUES(?),(?)")
  }

  test("A statement that constructs VALUES in multiple sql is the same as the specified string.") {
    case class Value(c1: Int, c2: String)
    val vs: NonEmptyList[Value] = NonEmptyList.of(Value(1, "value1"), Value(2, "value2"))
    val sql =
      q"INSERT INTO `table` (`column1`, `column2`) VALUES" ++ comma(vs.map(v => parentheses(p"${ v.c1 },${ v.c2 }")))
    assertEquals(sql.statement, "INSERT INTO `table` (`column1`, `column2`) VALUES(?,?),(?,?)")
  }

  test("The statement that constructs the IN clause with multiple values is the same as the string specified.") {
    val sql = q"SELECT * FROM `table` WHERE `column`" ++ in(NonEmptyList.of(1, 2))
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE `column`IN(?,?)")
  }

  test("The statement that constructs the AND clause with multiple values is the same as the string specified.") {
    val sql = q"SELECT * FROM `table` WHERE" ++ and(q"`column1` = 1", q"`column2` = 2")
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE(`column1` = 1 AND`column2` = 2)")
  }

  test("The statement that constructs the AND clause with multiple values is the same as the string specified.") {
    val sql = q"SELECT * FROM `table` WHERE" ++ andOpt(Some(q"`column1` = 1"), Some(q"`column2` = 2"))
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE(`column1` = 1 AND`column2` = 2)")
  }

  test("The statement that constructs the OR clause with multiple values is the same as the string specified.") {
    val sql = q"SELECT * FROM `table` WHERE" ++ or(NonEmptyList.of(q"`column1` = 1", q"`column2` = 2"))
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE(`column1` = 1 OR`column2` = 2)")
  }

  test("The statement that constructs the OR clause with multiple values is the same as the string specified.") {
    val sql = q"SELECT * FROM `table` WHERE" ++ orOpt(Some(q"`column1` = 1"), Some(q"`column2` = 2"))
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE(`column1` = 1 OR`column2` = 2)")
  }

  test("The statement that constructs the OR clause with multiple values is the same as the string specified.") {
    val sql = q"SELECT * FROM `table` WHERE" ++ orOpt(None, Some(q"`column2` = 2"))
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE(`column2` = 2)")
  }

  test("The statement that constructs the OR clause with multiple values is the same as the string specified.") {
    val sql = q"SELECT * FROM `table` WHERE" ++ orOpt(None, None)
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE")
  }

  test(
    "A statement that is constructed by ANDing Where clauses with multiple values is the same as the string specified."
  ) {
    val sql = q"SELECT * FROM `table` " ++ where("AND", q"`column1` = 1", q"`column2` = 2")
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE `column1` = 1 AND `column2` = 2")
  }

  test(
    "A statement that is constructed by ANDing Where clauses with multiple values is the same as the string specified."
  ) {
    val sql = q"SELECT * FROM `table` " ++ where("AND", q"`column1` = 1", None, Some(q"`column2` = 2"))
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE `column1` = 1 AND `column2` = 2")
  }

  test(
    "A statement that is constructed by ORing Where clauses with multiple values is the same as the string specified."
  ) {
    val sql = q"SELECT * FROM `table` " ++ where("OR", q"`column1` = 1", q"`column2` = 2")
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE `column1` = 1 OR `column2` = 2")
  }

  test(
    "A statement that is constructed by ORing Where clauses with multiple values is the same as the string specified."
  ) {
    val sql = q"SELECT * FROM `table` " ++ where("OR", q"`column1` = 1", None, Some(q"`column2` = 2"))
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE `column1` = 1 OR `column2` = 2")
  }

  test("The statement that constructs a SET clause with multiple values will be the same as the string specified.") {
    val sql = q"UPDATE `table` " ++ set(q"`column1` = 1", q"`column2` = 2", q"`column3` = 3")
    assertEquals(sql.statement, "UPDATE `table` SET`column1` = 1,`column2` = 2,`column3` = 3")
  }
