/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder

import ldbc.dsl.Parameter
import ldbc.statement.{ TableQuery as AbstractTableQuery, * }

private[ldbc] case class TableQueryImpl[A <: SharedTable & AbstractTable[?]](
                              table:  A,
                              column: Column[AbstractTableQuery.Extract[A]],
                              name:   String,
                              params: List[Parameter.Dynamic]
                            ) extends AbstractTableQuery[A, Table.Opt[AbstractTableQuery.Extract[A]]]:

  override private[ldbc] def toOption: AbstractTableQuery[A, Table.Opt[AbstractTableQuery.Extract[A]]] =
    val opt = Table.Opt.Impl[AbstractTableQuery.Extract[A]](name, table.columns, table.*.opt.asInstanceOf[Column[Option[AbstractTableQuery.Extract[A]]]])
    TableQueryOpt[A, Table.Opt[AbstractTableQuery.Extract[A]]](opt, opt.*, opt.$name, params)
      .asInstanceOf[AbstractTableQuery[A, Table.Opt[AbstractTableQuery.Extract[A]]]]

private[ldbc] case class TableQueryOpt[A, O <: SharedTable](
                                                table: O,
                                                column: Column[AbstractTableQuery.Extract[O]],
                                                name: String,
                                                params: List[Parameter.Dynamic]
                                              ) extends AbstractTableQuery[O, A]:

  override private[ldbc] def toOption: AbstractTableQuery[O, A] = this

object TableQuery:

  def apply[P <: Product](using table: Table[P]): AbstractTableQuery[Table[P], Table.Opt[P]] =
    TableQueryImpl[Table[P]](table, table.*, table.$name, List.empty)

  def apply[P <: Product](name: String)(using table: Table[P]): AbstractTableQuery[Table[P], Table.Opt[P]] =
    val alias = table.setName(name)
    TableQueryImpl[Table[P]](alias, alias.*, alias.$name, List.empty)
