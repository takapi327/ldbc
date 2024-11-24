/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import scala.quoted.*

import ldbc.dsl.Parameter
import ldbc.statement.{ TableQuery as AbstractTableQuery, * }

case class TableQueryImpl[A](
  table:  A,
  column: Column[AbstractTableQuery.Extract[A]],
  name:   String,
  params: List[Parameter.Dynamic]
) extends AbstractTableQuery[A, A]:

  override private[ldbc] def toOption: AbstractTableQuery[A, A] = this

object TableQuery:

  def apply[E, T <: AbstractTable[E]](table: T): AbstractTableQuery[T, T] =
    TableQueryImpl[T](table, table.*, table.$name, List.empty)

  inline def apply[T <: AbstractTable[?]]: AbstractTableQuery[T, T] = ${ applyImpl[T] }

  private def applyImpl[T <: AbstractTable[?]](using quotes: Quotes, tpe: Type[T]): Expr[AbstractTableQuery[T, T]] =
    import quotes.reflect.*
    val tableType = TypeRepr.of[T]
    val table = Select
      .unique(New(TypeIdent(tableType.typeSymbol)), "<init>")
      .appliedToArgs(List.empty)
      .asExprOf[T]

    '{ apply[AbstractTableQuery.Extract[T], T]($table) }
