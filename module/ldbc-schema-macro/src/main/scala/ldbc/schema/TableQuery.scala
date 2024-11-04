/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import ldbc.dsl.Parameter
import ldbc.query.builder.statement.Expression
import ldbc.query.builder.Column
import ldbc.schema.internal.QueryConcat
import ldbc.schema.statement.*

trait TableQuery[A]:

  private[ldbc] def table: A
  
  private[ldbc] def params: List[Parameter.Dynamic]

  def statement: String

  def join[B, AB](other: TableQuery[B])(using QueryConcat.Aux[A, B, AB]): TableQuery.Join[A, B, AB] =
    TableQuery.Join(this, other)

  def select[C](func: A => Column[C]): Select[A, C] =
    val columns = func(table)
    Select(table, columns, s"SELECT ${columns.toString} FROM $statement", params)

  private[ldbc] def asVector(): Vector[TableQuery[?]] =
    this match
      case TableQuery.Join.On(left, right, _, _, _) => left.asVector() ++ right.asVector()
      case TableQuery.Join(left, right)             => left.asVector() ++ right.asVector()
      case r: TableQuery[?]             => Vector(r)

object TableQuery:

  def apply[T <: Table[?]](table: T): TableQuery[T] = Impl(table, table.statement, List.empty)

  private[ldbc] case class Impl[A](
                                      table: A,
                                      statement: String,
                                      params: List[Parameter.Dynamic]
                                    ) extends TableQuery[A]

  case class Join[A, B, AB](
                             left: TableQuery[A],
                             right: TableQuery[B]
                            ) extends TableQuery[AB]:

    override def table: AB = Tuple.fromArray((left.asVector() ++ right.asVector()).map(_.table).toArray).asInstanceOf[AB]
    override def statement: String = s"${left.statement} JOIN ${right.statement}"
    override def params: List[Parameter.Dynamic] = left.params ++ right.params

    def on(expression: AB => Expression): TableQuery[AB] =
      val expr = expression(table)
      Join.On(left, right, table, s"$statement ON ${expr.statement}", params ++ expr.parameter)

  object Join:

    case class On[A, B, AB](
                             left: TableQuery[A],
                             right: TableQuery[B],
                             table: AB,
                             statement: String,
                             params: List[Parameter.Dynamic]
                           ) extends TableQuery[AB]
