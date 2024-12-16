/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder

import scala.deriving.Mirror

import ldbc.dsl.Parameter
import ldbc.dsl.codec.Decoder
import ldbc.statement.{ TableQuery as AbstractTableQuery, * }

private[ldbc] case class TableQueryImpl[A <: SharedTable & AbstractTable[?], B <: Product](
  table:  A,
  column: Column[AbstractTableQuery.Extract[A]],
  name:   String,
  params: List[Parameter.Dynamic]
)(using mirror: Mirror.ProductOf[B])
  extends AbstractTableQuery[A, Table.Opt[AbstractTableQuery.Extract[A]]]:

  override private[ldbc] def toOption: AbstractTableQuery[A, Table.Opt[AbstractTableQuery.Extract[A]]] =
    val columnOpt =
      val decoder: Decoder[Option[B]] = new Decoder[Option[B]]((resultSet, prefix) =>
        val decoded = table.columns.map(_.opt.decoder.decode(resultSet, prefix))
        if decoded.flatten.length == table.columns.length then
          Option(
            mirror.fromTuple(
              Tuple
                .fromArray(decoded.flatten.toArray)
                .asInstanceOf[mirror.MirroredElemTypes]
            )
          )
        else None
      )
      val alias = table.columns.flatMap(_.alias).mkString(", ")
      Column.Impl[Option[B]](
        table.columns.map(_.name).mkString(", "),
        if alias.isEmpty then None else Some(alias),
        decoder,
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

private[ldbc] case class TableQueryOpt[A, O <: SharedTable](
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
