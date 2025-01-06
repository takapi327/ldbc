/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import cats.data.NonEmptyList

import ldbc.dsl.io.*

import munit.CatsEffectSuite

class HelperFunctionTest extends munit.CatsEffectSuite:

  test(
    "The statement that constructs VALUES with multiple values of the same type will be the same as the string specified."
  ) {
    val sql = sql"INSERT INTO `table` (`column1`, `column2`) " ++ values(NonEmptyList.of((1, 2), (3, 4), (5, 6)))
    assertEquals(sql.statement, "INSERT INTO `table` (`column1`, `column2`) VALUES(?,?),(?,?),(?,?)")
  }

  test(
    "Statements that comprise VALUES with a single value of the same type will be the same as the specified string."
  ) {
    val sql = sql"INSERT INTO `table` (`column1`) " ++ values(NonEmptyList.of(1, 2, 3, 4))
    assertEquals(sql.statement, "INSERT INTO `table` (`column1`) VALUES(?),(?),(?),(?)")
  }

  test("A statement that constructs VALUES in multiple sql is the same as the specified string.") {
    case class Value(c1: Int, c2: String)
    val sql = sql"INSERT INTO `table` (`column1`, `column2`) " ++ values(
      NonEmptyList.of(Value(1, "value1"), Value(2, "value2"))
    )
    assertEquals(sql.statement, "INSERT INTO `table` (`column1`, `column2`) VALUES(?,?),(?,?)")
  }

  test("The statement that constructs WHERE AND with multiple SQLs will be the same as the string specified.") {
    val sql = sql"SELECT * FROM `table` " ++ whereAndOpt(Some(sql"column1 = ?"), Some(sql"column2 = ?"))
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE (column1 = ?) AND (column2 = ?)")
  }

  test("The statement that constructs the IN clause with multiple values is the same as the string specified.") {
    val sql = sql"SELECT * FROM `table` WHERE " ++ in(sql"`column`", NonEmptyList.of(1, 2))
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE (`column` IN (?,?))")
  }

  test("The statement that constructs the IN clause with multiple values is the same as the string specified.") {
    val sql = sql"SELECT * FROM `table` WHERE " ++ notIn(sql"`column`", 1, 2)
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE (`column` NOT IN (?,?))")
  }

  test("The statement that constructs the AND clause with multiple values is the same as the string specified.") {
    val sql = sql"SELECT * FROM `table` WHERE" ++ and(sql"`column1` = 1", sql"`column2` = 2")
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE((`column1` = 1) AND (`column2` = 2))")
  }

  test("The statement that constructs the AND clause with multiple values is the same as the string specified.") {
    val sql =
      sql"SELECT * FROM `table` WHERE" ++ andOpt(Some(sql"`column1` = 1"), Some(sql"`column2` = 2")).getOrElse(sql"")
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE((`column1` = 1) AND (`column2` = 2))")
  }

  test("The statement that constructs the OR clause with multiple values is the same as the string specified.") {
    val sql = sql"SELECT * FROM `table` WHERE" ++ or(NonEmptyList.of(sql"`column1` = 1", sql"`column2` = 2"))
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE((`column1` = 1) OR (`column2` = 2))")
  }

  test("The statement that constructs the OR clause with multiple values is the same as the string specified.") {
    val sql =
      sql"SELECT * FROM `table` WHERE" ++ orOpt(Some(sql"`column1` = 1"), Some(sql"`column2` = 2")).getOrElse(sql"")
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE((`column1` = 1) OR (`column2` = 2))")
  }

  test("The statement that constructs WHERE AND with multiple SQLs will be the same as the string specified.") {
    val sql = sql"SELECT * FROM `table` " ++ whereAnd(sql"column1 = ?", sql"column2 = ?")
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE (column1 = ?) AND (column2 = ?)")
  }

  test("The statement that constructs WHERE AND with multiple SQLs will be the same as the string specified.") {
    val sql = sql"SELECT * FROM `table` " ++ whereAndOpt(Some(sql"column1 = ?"), Some(sql"column2 = ?"))
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE (column1 = ?) AND (column2 = ?)")
  }

  test("The statement that constructs WHERE OR with multiple SQLs will be the same as the string specified.") {
    val sql = sql"SELECT * FROM `table` " ++ whereOr(sql"column1 = ?", sql"column2 = ?")
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE (column1 = ?) OR (column2 = ?)")
  }

  test("The statement that constructs WHERE OR with multiple SQLs will be the same as the string specified.") {
    val sql = sql"SELECT * FROM `table` " ++ whereOrOpt(Some(sql"column1 = ?"), Some(sql"column2 = ?"))
    assertEquals(sql.statement, "SELECT * FROM `table` WHERE (column1 = ?) OR (column2 = ?)")
  }

  test("The statement that constructs SET with multiple SQLs will be the same as the string specified.") {
    val sql = sql"UPDATE `table` " ++ set(sql"column1 = ?", sql"column2 = ?")
    assertEquals(sql.statement, "UPDATE `table` SET column1 = ?,column2 = ?")
  }

  test("The statement that constructs parentheses with SQL will be the same as the string specified.") {
    val sql = parentheses(sql"column1 = ?")
    assertEquals(sql.statement, "(column1 = ?)")
  }

  test("The statement that constructs comma with multiple SQLs will be the same as the string specified.") {
    val sql = comma(sql"column1 = ?", sql"column2 = ?")
    assertEquals(sql.statement, "column1 = ?,column2 = ?")
  }

  test("The statement that constructs ORDER BY with multiple SQLs will be the same as the string specified.") {
    val sql = sql"SELECT * FROM `table` " ++ orderBy(sql"column1", sql"column2")
    assertEquals(sql.statement, "SELECT * FROM `table` ORDER BY column1,column2")
  }
