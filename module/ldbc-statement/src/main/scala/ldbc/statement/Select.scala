/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.annotation.targetName

import ldbc.dsl.{ Parameter, SQL }

/**
 * A model for constructing SELECT statements in MySQL.
 *
 * @param table
 *   Trait for generating SQL table information.
 * @param columns
 *   Union-type column list
 * @param statement
 *   SQL statement string
 * @param params
 *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
 *   only.
 * @tparam A
 *   The type of Table. in the case of Join, it is a Tuple of type Table.
 * @tparam B
 *   Scala types to be converted by Decoder
 */
case class Select[A, B](
  table:     A,
  columns:   Column[B],
  statement: String,
  params:    List[Parameter.Dynamic]
) extends Query[A, B],
          OrderBy.Provider[A, B],
          Limit.QueryProvider[A, B]:

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

  /**
   * A method for setting the WHERE condition in a SELECT statement.
   * 
   * {{{
   *   TableQuery[City]
   *     .select(_.name)
   *     .where(_.name === "Tokyo")
   * }}}
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def where(func: A => Expression): Where.Q[A, B] =
    val expression = func(table)
    Where.Q[A, B](
      table     = table,
      columns   = columns,
      statement = statement ++ s" WHERE ${ expression.statement }",
      params    = params ++ expression.parameter
    )

  /**
   * A method for setting the WHERE condition in a SELECT statement.
   *
   * {{{
   *   val opt: Option[String] = ???
   *   TableQuery[City]
   *     .select(_.name)
   *     .whereOpt(city => opt.map(value => city.name === value))
   * }}}
   *
   * @param func
   * Function to construct an expression using the columns that Table has.
   */
  def whereOpt(func: A => Option[Expression]): Where.Q[A, B] =
    func(table) match
      case Some(expression) =>
        Where.Q[A, B](
          table = table,
          columns = columns,
          statement = statement ++ s" WHERE ${expression.statement}",
          params = params ++ expression.parameter
        )
      case None =>
        Where.Q[A, B](
          table = table,
          columns = columns,
          statement = statement,
          params = params,
          isFirst = true
        )

  /**
   * A method for setting the WHERE condition in a SELECT statement.
   *
   * {{{
   *   TableQuery[City]
   *     .select(_.name)
   *     .whereOpt(Some("Tokyo"))((city, value) => city.name === value)
   * }}}
   *
   * @param func
   * Function to construct an expression using the columns that Table has.
   */
  def whereOpt[C](opt: Option[C])(func: (A, C) => Expression): Where.Q[A, B] =
    opt match
      case Some(value) =>
        val expression = func(table, value)
        Where.Q[A, B](
          table     = table,
          columns   = columns,
          statement = statement ++ s" WHERE ${ expression.statement }",
          params    = params ++ expression.parameter
        )
      case None =>
        Where.Q[A, B](
          table     = table,
          columns   = columns,
          statement = statement,
          params    = params,
          isFirst   = true
        )

  /**
   * A method for setting the GROUP BY condition in a SELECT statement.
   * 
   * {{{
   *   TableQuery[City]
   *     .select(_.name)
   *     .groupBy(_.name)
   * }}}
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def groupBy[C](func: A => Column[C]): GroupBy[A, B] =
    GroupBy[A, B](
      table     = table,
      columns   = columns,
      statement = statement ++ s" GROUP BY ${ func(table).toString }",
      params    = params
    )
