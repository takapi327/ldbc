/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import ldbc.dsl.{ Parameter, SQL }

import scala.annotation.targetName

trait Where[A]:

  type Self

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

  def and(func: A => Expression): Self = union("AND", func(table))

  def or(func: A => Expression): Self = union("OR", func(table))

  @targetName("OR")
  def ||(func: A => Expression): Self = union("||", func(table))

  def xor(func: A => Expression): Self = union("XOR", func(table))

  @targetName("AND")
  def &&(func: A => Expression): Self = union("&&", func(table))

object Where:

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

    def groupBy[C](func: A => Column[C]): GroupBy[A, B] =
      val conditions = func(table)
      GroupBy[A, B](
        table     = table,
        columns   = columns,
        statement = statement ++ s" GROUP BY ${ conditions.alias.getOrElse(conditions.name) }",
        params    = params
      )

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
