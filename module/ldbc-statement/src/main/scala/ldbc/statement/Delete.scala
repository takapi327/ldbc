/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.annotation.targetName

import ldbc.dsl.{ Parameter, SQL }

/**
 * A model for constructing DELETE statements in MySQL.
 *
 * @param table
 *   Trait for generating SQL table information.
 * @param statement
 *   SQL statement string
 * @param params
 *   List of parameters
 * @tparam A
 *   Type representing Table
 */
case class Delete[A](
  table:     A,
  statement: String,
  params:    List[Parameter.Dynamic]
) extends Command,
          Limit.CommandProvider:

  @targetName("combine")
  override def ++(sql: SQL): SQL = this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

  /**
   * A method for setting the WHERE condition in a DELETE statement.
   * 
   * {{{
   *   TableQuery[City].delete.where(_.name === "Tokyo")
   * }}}
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def where(func: A => Expression): Where.C[A] =
    val expression = func(table)
    Where.C(
      table     = table,
      statement = statement ++ s" WHERE ${ expression.statement }",
      params    = params ++ expression.parameter
    )

  /**
   * A method for setting the WHERE condition in a DELETE statement.
   *
   * {{{
   *   val opt: Option[String] = ???
   *   TableQuery[City].delete.whereOpt(city => opt.map(value => city.name === value))
   * }}}
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def whereOpt(func: A => Option[Expression]): Where.C[A] =
    func(table) match
      case Some(expression) =>
        Where.C(
          table     = table,
          statement = statement ++ s" WHERE ${ expression.statement }",
          params    = params ++ expression.parameter
        )
      case None =>
        Where.C(
          table     = table,
          statement = statement,
          params    = params,
          isFirst   = true
        )

  /**
   * A method for setting the WHERE condition in a DELETE statement.
   *
   * {{{
   *   TableQuery[City].delete.where(_.name === "Tokyo")
   * }}}
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def whereOpt[B](opt: Option[B])(func: (A, B) => Expression): Where.C[A] =
    opt match
      case Some(value) =>
        val expression = func(table, value)
        Where.C(
          table     = table,
          statement = statement ++ s" WHERE ${ expression.statement }",
          params    = params ++ expression.parameter
        )
      case None =>
        Where.C(
          table,
          statement,
          params,
          isFirst = true
        )
