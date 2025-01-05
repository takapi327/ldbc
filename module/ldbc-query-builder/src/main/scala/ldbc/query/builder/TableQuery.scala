/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder

import scala.deriving.Mirror

import ldbc.dsl.Parameter
import ldbc.dsl.codec.*
import ldbc.statement.{ TableQuery as AbstractTableQuery, * }

case class TableQueryImpl[A <: SharedTable & AbstractTable[?], B <: Product](
  table:  A,
  column: Column[AbstractTableQuery.Extract[A]],
  name:   String,
  params: List[Parameter.Dynamic]
) extends AbstractTableQuery[A, Table.Opt[AbstractTableQuery.Extract[A]]]:

  override private[ldbc] def toOption: AbstractTableQuery[A, Table.Opt[AbstractTableQuery.Extract[A]]] =
    val columnOpt =
      val alias = table.columns.flatMap(_.alias).mkString(", ")
      Column.Impl[Option[B]](
        table.columns.map(_.name).mkString(", "),
        if alias.isEmpty then None else Some(alias),
        column.opt.decoder.asInstanceOf[Decoder[Option[B]]],
        column.opt.encoder.asInstanceOf[Encoder[Option[B]]],
        Some(table.columns.length),
        Some(table.columns.map(column => s"${ column.name } = ?").mkString(", "))
      )

    val opt = Table.Opt.Impl[AbstractTableQuery.Extract[A]](
      name,
      table.columns,
      columnOpt.asInstanceOf[Column[Option[AbstractTableQuery.Extract[A]]]]
    )
    TableQueryOpt[A, Table.Opt[AbstractTableQuery.Extract[A]]](opt, opt.*, opt.$name, params)
      .asInstanceOf[AbstractTableQuery[A, Table.Opt[AbstractTableQuery.Extract[A]]]]

case class TableQueryOpt[A, O <: SharedTable](
  table:  O,
  column: Column[AbstractTableQuery.Extract[O]],
  name:   String,
  params: List[Parameter.Dynamic]
) extends AbstractTableQuery[O, A]:

  override private[ldbc] def toOption: AbstractTableQuery[O, A] = this

object TableQuery:

  def apply[P <: Product](using
    table:  Table[P],
    mirror: Mirror.ProductOf[P]
  ): AbstractTableQuery[Table[P], Table.Opt[P]] =
    TableQueryImpl[Table[P], P](table, table.*, table.$name, List.empty)
