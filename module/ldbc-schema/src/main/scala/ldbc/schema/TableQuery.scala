/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import scala.quoted.*

import ldbc.dsl.Parameter

import ldbc.statement.{ TableQuery as AbstractTableQuery, * }

case class TableQueryImpl[A <: Table[?]](
  table:  A,
  column: Column[AbstractTableQuery.Extract[A]],
  name:   String,
  params: List[Parameter.Dynamic]
) extends AbstractTableQuery[A, Table.Opt[AbstractTableQuery.Extract[A]]]:

  override private[ldbc] def toOption: AbstractTableQuery[A, Table.Opt[AbstractTableQuery.Extract[A]]] =
    val columnOpt = Column.Impl[Option[AbstractTableQuery.Extract[A]]](
      column.name,
      column.alias,
      column.opt.decoder,
      column.opt.encoder,
      Some(column.values),
      Some(column.updateStatement)
    )
    val opt = Table.Opt[AbstractTableQuery.Extract[A]](
      name,
      column.list,
      columnOpt
    )

    TableQueryOpt[A, Table.Opt[AbstractTableQuery.Extract[A]]](opt, opt.*, name, params)
      .asInstanceOf[AbstractTableQuery[A, Table.Opt[AbstractTableQuery.Extract[A]]]]

private[ldbc] case class TableQueryOpt[A, O](
  table:  O,
  column: Column[AbstractTableQuery.Extract[O]],
  name:   String,
  params: List[Parameter.Dynamic]
) extends AbstractTableQuery[O, A]:

  override private[ldbc] def toOption: AbstractTableQuery[O, A] = this

object TableQuery:

  type Extract[T] = T match
    case Table[t]       => t
    case Table[t] *: tn => t *: Extract[tn]

  def apply[E, T <: Table[E]](table: T): AbstractTableQuery[T, Table.Opt[E]] =
    TableQueryImpl[T](table, table.*, table.$name, List.empty)

  inline def apply[T <: Table[?]]: AbstractTableQuery[T, Table.Opt[Extract[T]]] = ${
    applyImpl[T]
  }

  private def applyImpl[T <: Table[?]](using
    quotes: Quotes,
    tpe:    Type[T]
  ): Expr[AbstractTableQuery[T, Table.Opt[Extract[T]]]] =
    import quotes.reflect.*
    val tableType = TypeRepr.of[T]
    val table = Select
      .unique(New(TypeIdent(tableType.typeSymbol)), "<init>")
      .appliedToArgs(List.empty)
      .asExprOf[T]

    '{ apply[Extract[T], T]($table) }
