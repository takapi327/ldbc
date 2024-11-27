/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import ldbc.dsl.Parameter

/**
 * A model for constructing JOIN statements in MySQL.
 * 
 * @param typed
 *   Type of JOIN statement
 * @param left
 *   Left table
 * @param right
 *   Right table
 */
case class Join[A, B, AB, OO](
  typed: "JOIN" | "LEFT JOIN" | "RIGHT JOIN",
  left:  TableQuery[A, ?],
  right: TableQuery[B, ?]
):

  private val table: AB =
    Tuple.fromArray((left.asVector() ++ right.asVector()).map(_.table).toArray).asInstanceOf[AB]
  val statement:      String                  = s"${ left.name } $typed ${ right.name }"
  private val params: List[Parameter.Dynamic] = left.params ++ right.params

  /**
   * A method for combining ON statements.
   * 
   * @param expression
   *   Trait for the syntax of expressions available in MySQL.
   */
  def on(expression: AB => Expression): TableQuery[AB, OO] =
    val expr = expression(table)
    Join.On(left, right, table, s"$statement ON ${ expr.statement }", params ++ expr.parameter)

object Join:

  def apply[A, B, AB, OO](
    left:  TableQuery[A, ?],
    right: TableQuery[B, ?]
  ): Join[A, B, AB, OO] = Join("JOIN", left, right)

  def lef[A, B, AB, OO](
    left:  TableQuery[A, ?],
    right: TableQuery[B, ?]
  ): Join[A, B, AB, OO] = Join("LEFT JOIN", left, right)

  def right[A, B, AB, OO](
    left:  TableQuery[A, ?],
    right: TableQuery[B, ?]
  ): Join[A, B, AB, OO] = Join("RIGHT JOIN", left, right)

  case class On[A, B, AB, OO](
    left:   TableQuery[A, ?],
    right:  TableQuery[B, ?],
    table:  AB,
    name:   String,
    params: List[Parameter.Dynamic]
  ) extends TableQuery[AB, OO]:

    override def column: Column[Entity] = (left.column *: right.column).asInstanceOf[Column[Entity]]

    override def delete: Delete[AB] =
      val main =
        (left.asVector() ++ right.asVector()).headOption.getOrElse(throw new IllegalStateException("No table found."))
      Delete[AB](table, s"DELETE ${ main.name } FROM $name", params)

    override private[ldbc] def toOption: TableQuery[AB, OO] =
      this.copy(left.toOption, right.toOption)
