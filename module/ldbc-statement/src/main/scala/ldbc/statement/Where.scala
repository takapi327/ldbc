/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import ldbc.dsl.{ Parameter, SQL }

import scala.annotation.targetName

/**
 * Trait for building Statements to be WHERE.
 *
 * @tparam A
 *   The type of Table. in the case of Join, it is a Tuple of type Table.
 */
sealed trait Where[A]:

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
   *   TableQuery[City]
   *     .select(_.name)
   *     .where(_.population > 1000000)
   *     .or(_.name == "Tokyo")
   * }}}
   * 
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def or(func: A => Expression): Self = union("OR", func(table))

  @targetName("OR")
  def ||(func: A => Expression): Self = union("||", func(table))

  /**
   * Function to set additional conditions on WHERE statement.
   * 
   * {{{
   *   TableQuery[City]
   *     .select(_.name)
   *     .where(_.population > 1000000)
   *     .xor(_.name == "Tokyo")
   * }}}
   * 
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def xor(func: A => Expression): Self = union("XOR", func(table))

  @targetName("AND")
  def &&(func: A => Expression): Self = union("&&", func(table))

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
   * @tparam A
   *   The type of Table. in the case of Join, it is a Tuple of type Table.
   * @tparam B
   *   Scala types to be converted by Decoder
   */
  case class Q[A, B](
    table:     A,
    columns:   Column[B],
    statement: String,
    params:    List[Parameter.Dynamic]
  ) extends Where[A],
            Query[A, B],
            OrderBy.Provider[A, B],
            Limit.QueryProvider[A, B]:

    override type Self = Q[A, B]

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

    override private[ldbc] def union(label: String, expression: Expression): Q[A, B] =
      this.copy(statement = statement ++ s" $label ${ expression.statement }", params = params ++ expression.parameter)

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
   * @tparam A
   * The type of Table. in the case of Join, it is a Tuple of type Table.
   */
  case class C[A](
    table:     A,
    statement: String,
    params:    List[Parameter.Dynamic]
  ) extends Where[A],
            Command,
            Limit.CommandProvider:

    override type Self = C[A]

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

    override private[ldbc] def union(label: String, expression: Expression): C[A] =
      this.copy(statement = statement ++ s" $label ${ expression.statement }", params = params ++ expression.parameter)
