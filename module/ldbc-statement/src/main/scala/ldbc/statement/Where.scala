/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.annotation.targetName

import ldbc.dsl.{ Parameter, SQL }

/**
 * Trait for building Statements to be WHERE.
 *
 * @tparam A
 *   The type of Table. in the case of Join, it is a Tuple of type Table.
 */
sealed transparent trait Where[A]:
  self: Self =>

  type Self

  /** Trait for generating SQL table information. */
  def table: A

  /**
   * A method for combining WHERE statements.
   *
   * @param label
   *   A conjunctive expression to join WHERE statements together.
   * @param expression
   *   Trait for the syntax of expressions available in MySQL.
   */
  private[ldbc] def union(label: String, expression: Expression): Self

  /**
   * Function to set additional conditions on WHERE statement.
   * 
   * {{{
   *   TableQuery[City]
   *     .select(_.name)
   *     .where(_.population > 1000000)
   *     .and(_.name == "Tokyo")
   *   // SELECT name FROM city WHERE population > ? AND name = ?
   * }}}
   * 
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def and(func: A => Expression): Self = union("AND", func(table))

  /**
   * Function to set additional conditions on WHERE statement.
   *
   * {{{
   *   val opt: Option[String] = ???
   *   TableQuery[City]
   *     .select(_.name)
   *     .where(_.population > 1000000)
   *     .andOpt((city => opt.map(value => city.name === value))
   *   // SELECT name FROM city WHERE population > ? AND name = ?
   * }}}
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def andOpt(func: A => Option[Expression]): Self =
    func(table).fold(self)(expression => union("AND", expression))

  /**
   * Function to set additional conditions on WHERE statement.
   *
   * {{{
   *   TableQuery[City]
   *     .select(_.name)
   *     .where(_.population > 1000000)
   *     .andOpt(Some("Tokyo"))((city, value) => city.name == value)
   *   // SELECT name FROM city WHERE population > ? AND name = ?
   * }}}
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def andOpt[B](opt: Option[B])(func: (A, B) => Expression): Self =
    opt.fold(self)(value => union("AND", func(table, value)))

  /**
   * Function to set additional conditions on WHERE statement.
   *
   * {{{
   *   TableQuery[City]
   *     .select(_.name)
   *     .where(_.population > 1000000)
   *     .and(_.name == "Tokyo", false)
   *   // SELECT name FROM city WHERE population > ?
   * }}}
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   * @param bool
   *   Determine whether to add to the conditions. If false, the AND condition is not added to the query.
   */
  def and(func: A => Expression, bool: Boolean): Self =
    if bool then and(func) else self

  /**
   * Function to set additional conditions on WHERE statement.
   * 
   * {{{
   *   TableQuery[City]
   *     .select(_.name)
   *     .where(_.population > 1000000)
   *     .or(_.name == "Tokyo")
   *   // SELECT name FROM city WHERE population > ? OR name = ?
   * }}}
   * 
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def or(func: A => Expression): Self = union("OR", func(table))

  /**
   * Function to set additional conditions on WHERE statement.
   *
   * {{{
   *   val opt: Option[String] = ???
   *   TableQuery[City]
   *     .select(_.name)
   *     .where(_.population > 1000000)
   *     .orOpt((city => opt.map(value => city.name === value))
   *   // SELECT name FROM city WHERE population > ? OR name = ?
   * }}}
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def orOpt(func: A => Option[Expression]): Self =
    func(table).fold(self)(expression => union("OR", expression))

  /**
   * Function to set additional conditions on WHERE statement.
   *
   * {{{
   *   TableQuery[City]
   *     .select(_.name)
   *     .where(_.population > 1000000)
   *     .orOpt(Some("Tokyo"))((city, value) => city.name == value)
   *   // SELECT name FROM city WHERE population > ? OR name = ?
   * }}}
   *
   * @param func
   * Function to construct an expression using the columns that Table has.
   */
  def orOpt[B](opt: Option[B])(func: (A, B) => Expression): Self =
    opt.fold(self)(value => union("OR", func(table, value)))

  /**
   * Function to set additional conditions on WHERE statement.
   *
   * {{{
   *   TableQuery[City]
   *     .select(_.name)
   *     .where(_.population > 1000000)
   *     .or(_.name == "Tokyo", false)
   *   // SELECT name FROM city WHERE population > ?
   * }}}
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   * @param bool
   *   Determine whether to add to the conditions. If false, the OR condition is not added to the query.
   */
  def or(func: A => Expression, bool: Boolean): Self =
    if bool then or(func) else self

  @targetName("OR")
  def ||(func: A => Expression): Self = union("||", func(table))

  @targetName("OR")
  def ||(func: A => Expression, bool: Boolean): Self =
    if bool then ||(func) else self

  /**
   * Function to set additional conditions on WHERE statement.
   * 
   * {{{
   *   TableQuery[City]
   *     .select(_.name)
   *     .where(_.population > 1000000)
   *     .xor(_.name == "Tokyo")
   *   // SELECT name FROM city WHERE population > ? XOR name = ?
   * }}}
   * 
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def xor(func: A => Expression): Self = union("XOR", func(table))

  /**
   * Function to set additional conditions on WHERE statement.
   *
   * {{{
   *   val opt: Option[String] = ???
   *   TableQuery[City]
   *     .select(_.name)
   *     .where(_.population > 1000000)
   *     .xorOpt((city => opt.map(value => city.name === value))
   *   // SELECT name FROM city WHERE population > ? XOR name = ?
   * }}}
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def xorOpt(func: A => Option[Expression]): Self =
    func(table).fold(self)(expression => union("XOR", expression))

  /**
   * Function to set additional conditions on WHERE statement.
   *
   * {{{
   *   TableQuery[City]
   *     .select(_.name)
   *     .where(_.population > 1000000)
   *     .xorOpt(Some("Tokyo"))((city, value) => city.name == value)
   *   // SELECT name FROM city WHERE population > ? XOR name = ?
   * }}}
   *
   * @param func
   * Function to construct an expression using the columns that Table has.
   */
  def xorOpt[B](opt: Option[B])(func: (A, B) => Expression): Self =
    opt.fold(self)(value => union("XOR", func(table, value)))

  /**
   * Function to set additional conditions on WHERE statement.
   *
   * {{{
   *   TableQuery[City]
   *     .select(_.name)
   *     .where(_.population > 1000000)
   *     .xor(_.name == "Tokyo", false)
   *   // SELECT name FROM city WHERE population > ?
   * }}}
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   * @param bool
   *   Determine whether to add to the conditions. If false, the XOR condition is not added to the query.
   */
  def xor(func: A => Expression, bool: Boolean): Self =
    if bool then xor(func) else self

  @targetName("AND")
  def &&(func: A => Expression): Self = union("&&", func(table))

  @targetName("AND")
  def &&(func: A => Expression, bool: Boolean): Self =
    if bool then &&(func) else self

object Where:

  /**
   * A model for constructing read-only query WHERE statements in MySQL.
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
   * @param isFirst
   *   If True, this condition is added first, so the specified join condition is ignored and a WHERE statement is started.
   * @tparam A
   *   The type of Table. in the case of Join, it is a Tuple of type Table.
   * @tparam B
   *   Scala types to be converted by Decoder
   */
  case class Q[A, B](
    table:     A,
    columns:   Column[B],
    statement: String,
    params:    List[Parameter.Dynamic],
    isFirst:   Boolean = false
  ) extends Where[A],
            Query[A, B],
            OrderBy.Provider[A, B],
            Limit.QueryProvider[A, B]:

    override type Self = Q[A, B]

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

    override private[ldbc] def union(label: String, expression: Expression): Q[A, B] =
      if isFirst then
        this.copy(
          statement = statement ++ s" WHERE ${ expression.statement }",
          params    = params ++ expression.parameter,
          isFirst   = false
        )
      else
        this.copy(
          statement = statement ++ s" $label ${ expression.statement }",
          params    = params ++ expression.parameter
        )

    /**
     * A method for setting the GROUP BY condition in a SELECT statement.
     * 
     * @param func
     *   Function to construct an expression using the columns that Table has.
     * @tparam C
     *   Scala types to be converted by Decoder
     */
    def groupBy[C](func: A => Column[C]): GroupBy[A, B] =
      val conditions = func(table)
      GroupBy[A, B](
        table     = table,
        columns   = columns,
        statement = statement ++ s" GROUP BY ${ conditions.alias.getOrElse(conditions.name) }",
        params    = params
      )

  /**
   * A model for constructing write-only query WHERE statements in MySQL.
   *
   * @param table
   *   Trait for generating SQL table information.
   * @param statement
   * SQL statement string
   * @param params
   *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
   *   only.
   * @param isFirst
   *   If True, this condition is added first, so the specified join condition is ignored and a WHERE statement is started.
   * @tparam A
   * The type of Table. in the case of Join, it is a Tuple of type Table.
   */
  case class C[A](
    table:     A,
    statement: String,
    params:    List[Parameter.Dynamic],
    isFirst:   Boolean = false
  ) extends Where[A],
            Command,
            Limit.CommandProvider:

    override type Self = C[A]

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

    override private[ldbc] def union(label: String, expression: Expression): C[A] =
      if isFirst then
        this.copy(
          statement = statement ++ s" WHERE ${ expression.statement }",
          params    = params ++ expression.parameter,
          isFirst   = false
        )
      else
        this.copy(
          statement = statement ++ s" $label ${ expression.statement }",
          params    = params ++ expression.parameter
        )
